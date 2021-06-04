package com.faendir.kotlin.autodsl.inspections

import com.faendir.kotlin.autodsl.DslInspect
import com.faendir.kotlin.autodsl.DslMandatory
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.idea.debugger.sequence.psi.resolveType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.KotlinType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@ExperimentalContracts
class DslInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {

            override fun visitLambdaExpression(lambda: KtLambdaExpression) {
                val receiver = lambda.resolveType().getReceiverTypeFromFunctionType()
                if (receiver.isDSLintClass()) {
                    visitDslLambda(holder, lambda, receiver)
                }
            }
        }
    }

    private fun visitDslLambda(holder: ProblemsHolder, lambda: KtLambdaExpression, receiver: KotlinType) {
        val mandatory = receiver.getMandatoryProperties()
        val body = lambda.bodyExpression ?: return
        val present = body.statements.filterIsInstance<KtBinaryExpression>()
            .filter { it.operationToken == KtTokens.EQ }
            .mapNotNull { (it.left as? KtNameReferenceExpression)?.getReferencedName() }
        mandatory.values.forEach { group ->
            if (present.none { group.contains(it) }) {
                holder.registerProblem(
                    lambda.functionLiteral.rBrace!!,
                    if (group.size == 1) "Missing property: ${group.first()}" else
                        "Missing property: One of ${group.joinToString()}",
                    *group.map { InsertMissingFix(it) }.toTypedArray()
                )
            }
        }
    }

    inner class InsertMissingFix(private val missing: String) : LocalQuickFix {
        override fun getFamilyName(): String = "Insert missing assignment"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val body = (descriptor.psiElement.parent as? KtFunctionLiteral)?.bodyBlockExpression
            if (body != null) {
                val factory = KtPsiFactory(project)
                body.add(factory.createNewLine())
                body.add(factory.createNameIdentifier(missing))
                body.add(factory.createEQ())
                body.add(factory.createExpression("TODO()"))
            }
        }

        override fun getName(): String = "Insert assignment for $missing"

        override fun availableInBatchMode(): Boolean = false
    }

    private fun KotlinType.getMandatoryProperties(): Map<String, List<String>> {
        val descriptors = memberScope.getContributedDescriptors(DescriptorKindFilter.FUNCTIONS)
        return (descriptors.filterIsInstance<PropertyDescriptor>().map { it.setter?.annotations to it.name.identifier }
                + descriptors.filterIsInstance<FunctionDescriptor>().map { it.annotations to it.name.identifier })
            .mapNotNull { (annotations, name) -> annotations?.findAnnotation(FqName(DslMandatory::class.java.name))?.let { (it.findGroup() ?: name) to name } }
            .groupBy({ it.first }, { it.second })
    }

    private fun AnnotationDescriptor.findGroup(): String? = argumentValue("group")?.value?.toString()?.takeIf { it.isNotEmpty() }
}

@ExperimentalContracts
private fun KotlinType?.isDSLintClass(): Boolean {
    contract {
        returns(true) implies (this@isDSLintClass != null)
    }
    return this?.constructor?.declarationDescriptor?.annotations?.hasAnnotation(FqName(DslInspect::class.java.name))
        ?: false
}