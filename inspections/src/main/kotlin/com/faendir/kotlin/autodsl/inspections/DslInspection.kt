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
import org.jetbrains.kotlin.idea.debugger.sequence.psi.resolveType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
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
        val missing = mandatory - present
        if (missing.isNotEmpty()) {
            holder.registerProblem(
                lambda.functionLiteral.rBrace!!,
                "Missing properties: ${missing.joinToString()}",
                InsertMissingFix(missing)
            )
        }

    }

    inner class InsertMissingFix(private val missing: List<String>) : LocalQuickFix {
        override fun getFamilyName(): String = "Insert missing assignments"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val body = (descriptor.psiElement.parent as? KtFunctionLiteral)?.bodyBlockExpression
            if (body != null) {
                val factory = KtPsiFactory(project)
                for (property in missing) {
                    body.add(factory.createNewLine())
                    body.add(factory.createNameIdentifier(property))
                    body.add(factory.createEQ())
                    body.add(factory.createExpression("TODO()"))
                }
            }
        }

        override fun getName(): String = "Insert assignments for ${missing.joinToString()}"

        override fun availableInBatchMode(): Boolean = false
    }

    private fun KotlinType.getMandatoryProperties(): List<String> {
        val descriptors = memberScope.getContributedDescriptors(DescriptorKindFilter.FUNCTIONS)
        return (descriptors.filterIsInstance<PropertyDescriptor>()
            .filter { it.setter?.annotations?.hasAnnotation(FqName(DslMandatory::class.java.name)) ?: false } +
                descriptors.filterIsInstance<FunctionDescriptor>()
                    .filter { it.annotations.hasAnnotation(FqName(DslMandatory::class.java.name)) })
            .map { it.name.identifier }
    }
}

@ExperimentalContracts
private fun KotlinType?.isDSLintClass(): Boolean {
    contract {
        returns(true) implies (this@isDSLintClass != null)
    }
    return this?.constructor?.declarationDescriptor?.annotations?.hasAnnotation(FqName(DslInspect::class.java.name))
        ?: false
}