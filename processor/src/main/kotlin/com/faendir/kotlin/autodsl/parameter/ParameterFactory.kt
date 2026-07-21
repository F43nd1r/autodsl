package com.faendir.kotlin.autodsl.parameter

import com.faendir.kotlin.autodsl.AutoDsl
import com.faendir.kotlin.autodsl.AutoDslDoc
import com.faendir.kotlin.autodsl.AutoDslRequired
import com.faendir.kotlin.autodsl.AutoDslSingular
import com.faendir.kotlin.autodsl.SourceInfoResolver
import com.faendir.kotlin.autodsl.kapt.asFqName
import com.faendir.kotlin.autodsl.parameter.CollectionType.ListType
import com.faendir.kotlin.autodsl.parameter.CollectionType.SetType
import com.faendir.kotlin.autodsl.toRawType
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.shadow.pluralize.singularize
import com.shadow.pluralize.utils.Plurality
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.toTypeParameterResolver
import java.io.File
import kotlin.reflect.KClass
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.allConstructors
import org.jetbrains.kotlin.utils.filterIsInstanceAnd

class ParameterFactory<A, T : A, C : A, P : A>(
    resolver: SourceInfoResolver<A, T, C, P>,
) : SourceInfoResolver<A, T, C, P> by resolver {
    private val set = Set::class.asClassName()
    private val list = List::class.asClassName()
    private val collection = Collection::class.asClassName()
    private val iterable = Iterable::class.asClassName()

    @OptIn(CompilerConfiguration.Internals::class, K1Deprecation::class)
    private val psiFactory by lazy {
        val disposable = Disposer.newDisposable()
        val env =
            KotlinCoreEnvironment.createForProduction(
                disposable,
                CompilerConfiguration(),
                EnvironmentConfigFiles.METADATA_CONFIG_FILES,
            )
        KtPsiFactory(env.project)
    }

    @JvmName("getParametersFromConstructor")
    fun getParameters(
        constructor: C,
        enclosingType: T,
    ): List<Parameter> {
        val defaults = extractDefaultsAsCodeBlocks(enclosingType)
        return constructor.getParameters().withIndex().map { (index, parameter) ->
            val type = parameter.getTypeName(enclosingType)
            val rawType = type.toRawType()
            val (hasNestedDsl, collectionType) =
                when (rawType)
                {
                    set -> {
                        parameter.hasAnnotatedTypeArgument(AutoDsl::class) to SetType(findSingular(parameter, index))
                    }

                    list, collection, iterable -> {
                        parameter.hasAnnotatedTypeArgument(AutoDsl::class) to
                            ListType(findSingular(parameter, index))
                    }

                    else -> {
                        (parameter.getTypeDeclaration()?.hasAnnotation(AutoDsl::class) == true) to null
                    }
                }
            Parameter(
                typeName = type,
                name = parameter.getName(),
                doc = parameter.getAnnotationProperty(AutoDslDoc::class, AutoDslDoc::kDoc) ?: parameter.getDoc(),
                hasDefault = parameter.hasDefault(),
                defaultValue = defaults[parameter.getName()],
                requiredGroup = parameter.getAnnotationProperty(AutoDslRequired::class, AutoDslRequired::group),
                index = index,
                hasNestedDsl = hasNestedDsl,
                collectionType = collectionType,
            )
        }
    }

    private data class ResolvedRef(
        val range: TextRange,
        val className: ClassName? = null,
        val member: MemberName? = null,
    )

    private fun resolveReferences(
        defaultExpr: KtExpression,
        imports: List<KtImportDirective>,
    ): List<ResolvedRef> {
        val explicit =
            imports
                .filterNot { it.isAllUnder }
                .mapNotNull { d -> d.importedFqName?.let { fq -> (d.aliasName ?: fq.shortName().asString()) to fq } }
                .toMap()
        val refs = mutableListOf<ResolvedRef>()
        defaultExpr.accept(
            object : KtTreeVisitorVoid() {
                override fun visitSimpleNameExpression(expr: KtSimpleNameExpression) {
                    val parent = expr.parent
                    if (parent is KtDotQualifiedExpression && parent.selectorExpression == expr) return
                    val fq = explicit[expr.getReferencedName()] ?: return
                    refs +=
                        if (expr.getReferencedName().first().isUpperCase()) {
                            ResolvedRef(expr.textRange, className = ClassName.bestGuess(fq.asString()))
                        } else {
                            ResolvedRef(expr.textRange, member = MemberName(fq.parent().asString(), fq.shortName().asString()))
                        }
                }
            },
        )
        return refs.sortedBy { it.range.startOffset }
    }

    private fun buildDefaultValueCodeBlock(
        defaultExpr: KtExpression,
        imports: List<KtImportDirective>,
    ): CodeBlock {
        val text = defaultExpr.text
        val base = defaultExpr.textRange.startOffset
        var cursor = 0
        val builder = CodeBlock.builder()
        for (ref in resolveReferences(defaultExpr, imports)) {
            val start = ref.range.startOffset - base
            val end = ref.range.endOffset - base
            builder.add("%L", text.substring(cursor, start))
            ref.className?.let { builder.add("%T", it) }
            ref.member?.let { builder.add("%M", it) }
            cursor = end
        }
        builder.add("%L", text.substring(cursor))
        return builder.build()
    }

    private fun extractDefaultsAsCodeBlocks(enclosingType: T): Map<String, CodeBlock> =
        try {
            if (enclosingType is KSClassDeclaration) {
                val fileLocation = enclosingType.location as? FileLocation ?: return emptyMap()
                val ktFile = psiFactory.createFile(File(fileLocation.filePath).readText())

                val targetName = enclosingType.simpleName.asString()
                val ktClass = ktFile.findClassByName(targetName) ?: return emptyMap()

                val imports = ktFile.importDirectives
                val matchingConstructor =
                    ktClass.allConstructors.firstOrNull { c ->
                        c.annotationEntries.any { it.shortName?.asString() == "AutoDslConstructor" }
                    } ?: ktClass.primaryConstructor ?: ktClass.allConstructors.firstOrNull()

                matchingConstructor
                    ?.valueParameters
                    ?.mapNotNull { param -> param.defaultValue?.let { param.name!! to it } }
                    ?.associate { (name, defaultValue) -> name to buildDefaultValueCodeBlock(defaultValue, imports) }
                    ?: emptyMap()
            } else {
                emptyMap()
            }
        } catch (_: Exception) {
            emptyMap()
        }

    private fun KtFile.findClassByName(targetName: String): KtClass? {
        fun search(declarations: List<org.jetbrains.kotlin.psi.KtDeclaration>): KtClass? {
            for (decl in declarations) {
                if (decl is KtClass) {
                    if (decl.name == targetName) return decl
                    search(decl.declarations)?.let { return it }
                }
            }
            return null
        }
        return search(this.declarations)
    }

    private fun P.hasAnnotatedTypeArgument(annotation: KClass<out Annotation>): Boolean =
        getTypeArguments().firstOrNull()?.hasAnnotation(annotation) ?: false

    private fun findSingular(
        parameter: P,
        index: Int,
    ): String =
        when {
            parameter.hasAnnotation(
                AutoDslSingular::class,
            ) -> parameter.getAnnotationProperty(AutoDslSingular::class, AutoDslSingular::value)

            parameter.getName().length < 3 -> parameter.getName()

            else -> parameter.getName().singularize(Plurality.CouldBeEither)
        } ?: "var$index"
}
