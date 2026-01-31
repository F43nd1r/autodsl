package com.faendir.kotlin.autodsl.inspections

import com.faendir.kotlin.autodsl.DslInspect
import com.faendir.kotlin.autodsl.DslMandatory
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.renderAsSourceCode
import org.jetbrains.kotlin.analysis.api.components.expandedSymbol
import org.jetbrains.kotlin.analysis.api.components.isFunctionType
import org.jetbrains.kotlin.analysis.api.components.isUnitType
import org.jetbrains.kotlin.analysis.api.components.memberScope
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.idea.codeinsight.utils.ConvertLambdaToReferenceUtils.getCallReferencedName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid

@ExperimentalContracts
class DslInspection : LocalInspectionTool() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
    ): PsiElementVisitor =
        object : KtVisitorVoid() {
            override fun visitLambdaExpression(lambda: KtLambdaExpression) {
                analyze(lambda) {
                    val receiver = (lambda.expressionType as? KaFunctionType)?.receiverType
                    if (receiver.isDSLintClass()) {
                        visitDslLambda(holder, lambda, receiver)
                    }
                }
            }
        }

    context(session: KaSession)
    private fun visitDslLambda(
        holder: ProblemsHolder,
        lambda: KtLambdaExpression,
        receiver: KaType,
    ) {
        val mandatory = receiver.getMandatoryProperties()
        val body = lambda.bodyExpression ?: return
        val present = body.statements.mapNotNull { extractPropertyName(it) }
        mandatory.values.forEach { group ->
            if (present.none { group.any { prop -> prop.name == it } }) {
                holder.registerProblem(
                    lambda.functionLiteral.rBrace!!,
                    if (group.size == 1) {
                        "Missing property: ${group.first()}"
                    } else {
                        "Missing property: One of ${group.joinToString()}"
                    },
                    *group.map { InsertMissingFix(it.name, it.hasNestedBuilder) }.toTypedArray(),
                )
            }
        }
    }

    private fun extractPropertyName(expression: KtExpression?): String? =
        when (expression) {
            // Handle assignments: x = value
            is KtBinaryExpression -> {
                if (expression.operationToken == KtTokens.EQ) {
                    extractPropertyName(expression.left)
                } else {
                    null
                }
            }

            // Handle this assignment/call: this.propertyName
            is KtDotQualifiedExpression -> {
                val receiver = expression.receiverExpression as? KtThisExpression
                // Only accept unqualified 'this' (not this@Label)
                if (receiver != null && receiver.getLabelName() == null) {
                    extractPropertyName(expression.selectorExpression)
                } else {
                    null
                }
            }

            // Handle property: propertyName = value
            is KtNameReferenceExpression -> {
                expression.getReferencedName()
            }

            // Handle nested DSL call: propertyName { ... }
            is KtCallExpression -> {
                expression.getCallReferencedName()
            }

            else -> {
                null
            }
        }

    class InsertMissingFix(
        private val missing: String,
        private val nestedBuilder: Boolean,
    ) : LocalQuickFix {
        override fun getFamilyName(): String = "Insert missing assignment"

        override fun applyFix(
            project: Project,
            descriptor: ProblemDescriptor,
        ) {
            val body = (descriptor.psiElement.parent as? KtFunctionLiteral)?.bodyBlockExpression
            if (body != null) {
                val factory = KtPsiFactory(project)
                body.add(factory.createNewLine())
                body.add(factory.createNameIdentifier(missing))
                if (nestedBuilder) {
                    body.add(factory.createWhiteSpace())
                    body.add(factory.createExpression("{ TODO() }"))
                } else {
                    body.add(factory.createEQ())
                    body.add(factory.createExpression("TODO()"))
                }
            }
        }

        override fun getName(): String = "Insert assignment for $missing"

        override fun availableInBatchMode(): Boolean = false
    }

    @OptIn(KaContextParameterApi::class)
    context(_: KaSession)
    private fun KaType.getMandatoryProperties(): Map<String, List<MandatoryProperty>> {
        val descriptors =
            this.expandedSymbol
                ?.memberScope
                ?.callables
                ?.toList()
                .orEmpty()
        val relevantDescriptors =
            descriptors.filter {
                it is KaPropertySymbol || (it is KaNamedFunctionSymbol && it.isBuilderFunction())
            }
        val mandatoryProps =
            relevantDescriptors.groupBy { it.name?.identifier }.mapNotNull { (name, descriptors) ->
                if (name == null) return@mapNotNull null
                val dslMandatoryAnnotation =
                    descriptors
                        .filterIsInstance<KaPropertySymbol>()
                        .firstOrNull()
                        ?.setter
                        ?.annotations
                        ?.get(DSL_MANDATORY)
                        ?.firstOrNull() ?: return@mapNotNull null
                val groupName = dslMandatoryAnnotation.findGroup() ?: name
                val hasNestedBuilder = descriptors.any { it is KaNamedFunctionSymbol }
                groupName to MandatoryProperty(name, hasNestedBuilder)
            }
        return mandatoryProps.groupBy({ it.first }, { it.second })
    }

    @OptIn(KaContextParameterApi::class)
    context(_: KaSession)
    private fun KaNamedFunctionSymbol.isBuilderFunction(): Boolean {
        if (valueParameters.size != 1) return false
        val parameterType = valueParameters.first().returnType
        if (!parameterType.isFunctionType) return false
        val typeArguments = (parameterType as KaFunctionType).typeArguments
        if (typeArguments.size != 2) return false
        if (typeArguments.first().type?.isDSLintClass() != true) return false
        if (typeArguments.last().type?.isUnitType != true) return false
        return true
    }

    private fun KaAnnotation.findGroup(): String? =
        arguments
            .find {
                it.name.identifier == "group"
            }?.expression
            ?.renderAsSourceCode()
            ?.takeIf { it.isNotEmpty() }
}

data class MandatoryProperty(
    val name: String,
    val hasNestedBuilder: Boolean,
)

private val DSL_INSPECT = ClassId.topLevel(FqName(DslInspect::class.java.name))
private val DSL_MANDATORY = ClassId.topLevel(FqName(DslMandatory::class.java.name))

@ExperimentalContracts
private fun KaType?.isDSLintClass(): Boolean {
    contract {
        returns(true) implies (this@isDSLintClass != null)
    }
    return this?.symbol?.annotations?.contains(DSL_INSPECT) ?: false
}
