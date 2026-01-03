package com.faendir.kotlin.autodsl.inspections

import com.faendir.kotlin.autodsl.DslInspect
import com.faendir.kotlin.autodsl.DslMandatory
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.renderAsSourceCode
import org.jetbrains.kotlin.analysis.api.components.expandedSymbol
import org.jetbrains.kotlin.analysis.api.components.memberScope
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@ExperimentalContracts
class DslInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {

            override fun visitLambdaExpression(lambda: KtLambdaExpression) {
                analyze(lambda) {
                    val receiver = (lambda.expressionType as? KaFunctionType)?.receiverType
                    if (receiver.isDSLintClass()) {
                        visitDslLambda(holder, lambda, receiver)
                    }
                }
            }
        }
    }

    context(session: KaSession)
    private fun visitDslLambda(holder: ProblemsHolder, lambda: KtLambdaExpression, receiver: KaType) {
        val mandatory = receiver.getMandatoryProperties()
        val body = lambda.bodyExpression ?: return
        val present = body.statements.filterIsInstance<KtBinaryExpression>()
            .filter { it.operationToken == KtTokens.EQ }
            .mapNotNull { extractPropertyName(it.left) }
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

    private fun extractPropertyName(expression: Any?): String? {
        return when (expression) {
            // Handle direct assignment: propertyName = value
            is KtNameReferenceExpression -> expression.getReferencedName()
            // Handle this assignment: this.propertyName = value
            is KtDotQualifiedExpression -> {
                val receiver = expression.receiverExpression as? KtThisExpression
                // Only accept unqualified 'this' (not this@Label)
                if (receiver != null && receiver.getLabelName() == null) {
                    (expression.selectorExpression as? KtNameReferenceExpression)?.getReferencedName()
                } else {
                    null
                }
            }
            else -> null
        }
    }

    class InsertMissingFix(private val missing: String) : LocalQuickFix {
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

    @OptIn(KaContextParameterApi::class)
    context(_: KaSession)
    private fun KaType.getMandatoryProperties(): Map<String, List<String>> {
        val descriptors = this.expandedSymbol?.memberScope?.callables.orEmpty().toList()
        return (descriptors.filterIsInstance<KaPropertySymbol>().map { it.setter?.annotations to it.name.identifier }
                + descriptors.filterIsInstance<KaNamedFunctionSymbol>().map { it.annotations to it.name.identifier })
            .mapNotNull { (annotations, name) -> annotations?.get(DSL_MANDATORY)?.firstOrNull()?.let { (it.findGroup() ?: name) to name } }
            .groupBy({ it.first }, { it.second })
    }

    private fun KaAnnotation.findGroup(): String? = arguments.find { it.name.identifier == "group" }?.expression?.renderAsSourceCode()?.takeIf { it.isNotEmpty() }
}

private val DSL_INSPECT = ClassId.topLevel(FqName(DslInspect::class.java.name))
private val DSL_MANDATORY = ClassId.topLevel(FqName(DslMandatory::class.java.name))

@ExperimentalContracts
private fun KaType?.isDSLintClass(): Boolean {
    contract {
        returns(true) implies (this@isDSLintClass != null)
    }
    return this?.symbol?.annotations?.contains(DSL_INSPECT) ?: false
}