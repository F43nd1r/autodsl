package com.faendir.kotlin.autodsl.ksp

import com.faendir.kotlin.autodsl.AnnotationFinder
import com.faendir.kotlin.autodsl.SourceInfoResolver
import com.google.devtools.ksp.KSTypeNotPresentException
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isInternal
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.toTypeParameterResolver
import com.squareup.kotlinpoet.ksp.toTypeVariableName
import com.squareup.kotlinpoet.metadata.classinspectors.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.classinspectors.ReflectiveClassInspector
import com.squareup.kotlinpoet.metadata.specs.classFor
import com.squareup.kotlinpoet.metadata.specs.toFileSpec
import java.io.File
import kotlin.metadata.KmPackage
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import com.google.devtools.ksp.getConstructors as superGetConstructors

@OptIn(KspExperimental::class)
class KspSourceInfoResolver(
    private val resolver: Resolver,
) : AnnotationFinder<KSAnnotated>(),
    SourceInfoResolver<KSAnnotated, KSClassDeclaration, KSFunctionDeclaration, KSValueParameter> {
    private fun getClassesWithAnnotation(annotation: String): List<KSClassDeclaration> =
        resolver.getSymbolsWithAnnotation(annotation).filterIsInstance<KSClassDeclaration>().toList()

    override fun getClassesWithAnnotation(annotation: KClass<out Annotation>) = getClassesWithAnnotation(annotation.qualifiedName!!)

    override fun getClassesWithAnnotation(annotation: KSClassDeclaration) = getClassesWithAnnotation(annotation.qualifiedName!!.asString())

    override fun KSClassDeclaration.getClassKind(): ClassKind = classKind

    override fun KSAnnotated.hasAnnotation(annotation: KClass<out Annotation>): Boolean = findAnnotation(annotation) != null

    override fun <T : Annotation> KSAnnotated.getAnnotationTypeProperty(
        annotation: KClass<T>,
        property: KProperty1<T, KClass<*>>,
    ): ClassName? =
        findAnnotation(annotation)?.let { annotationValue ->
            try {
                property.get(annotationValue).asClassName()
            } catch (e: KSTypeNotPresentException) {
                e.ksType.toClassName()
            }
        }

    override fun <T : Annotation, V> KSAnnotated.getAnnotationProperty(
        annotation: KClass<T>,
        property: KProperty1<T, V>,
    ): V? = findAnnotation(annotation)?.let { property.get(it) }

    override fun <T : Annotation> KSAnnotated.getDirectAnnotation(annotation: KClass<T>): T? =
        getAnnotationsByType(annotation).firstOrNull()

    override fun KSAnnotated.getDirectAnnotations(): Iterable<KSAnnotated> =
        annotations
            .mapNotNull {
                it.annotationType.resolve().declaration as? KSClassDeclaration
            }.asIterable()

    override fun KSAnnotated.getQualifiedName(): String? = (this as? KSClassDeclaration)?.qualifiedName?.asString()

    override fun KSClassDeclaration.isAbstract(): Boolean = modifiers.contains(Modifier.ABSTRACT)

    override fun KSClassDeclaration.isInner(): Boolean = modifiers.contains(Modifier.INNER)

    override fun KSClassDeclaration.getConstructors(): List<KSFunctionDeclaration> = superGetConstructors().toList()

    override fun KSFunctionDeclaration.isAccessible(): Boolean = isPublic() || isInternal()

    override fun KSClassDeclaration.getPrimaryConstructor(): KSFunctionDeclaration? = primaryConstructor

    override fun KSFunctionDeclaration.isValid(): Boolean = validate()

    override fun KSFunctionDeclaration.getParameters(): List<KSValueParameter> = parameters

    override fun KSClassDeclaration.asClassName(): ClassName = toClassName()

    override fun KSClassDeclaration.getTypeParameters(): List<TypeVariableName> {
        val resolver = typeParameters.toTypeParameterResolver()
        return typeParameters.map { it.toTypeVariableName(resolver) }
    }

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

    private fun KSFile.toFileText(): String? =
        try {
            // Read directly from disk using the file path
            File(filePath).readText()
        } catch (e: Exception) {
            null
        }

    override fun KSClassDeclaration.clonePrivateTopLevels(): SourceInfoResolver.ClonedPrivateTopLevels {
        val file = containingFile ?: return SourceInfoResolver.ClonedPrivateTopLevels(null)
        val fileText = file.toFileText() ?: return SourceInfoResolver.ClonedPrivateTopLevels(null)
        val ktFile = psiFactory.createFile(file.fileName, fileText)

        val privateDecls =
            ktFile.declarations.filter {
                it.hasModifier(KtTokens.PRIVATE_KEYWORD)
            }

        if (privateDecls.isEmpty()) return SourceInfoResolver.ClonedPrivateTopLevels(ktFile)

        val rawCode = privateDecls.joinToString("\n\n") { it.text }

        return SourceInfoResolver.ClonedPrivateTopLevels(ktFile, rawCode)
    }

    override fun KSValueParameter.getTypeDeclaration(): KSClassDeclaration? = type.resolve().declaration as? KSClassDeclaration

    override fun KSValueParameter.getTypeArguments(): List<KSClassDeclaration> =
        type.resolve().arguments.mapNotNull { it.type?.resolve()?.declaration as? KSClassDeclaration }

    override fun KSValueParameter.getTypeName(enclosingType: KSClassDeclaration): TypeName =
        type.toTypeName(enclosingType.typeParameters.toTypeParameterResolver())

    override fun KSValueParameter.getName(): String = name!!.asString()

    override fun KSValueParameter.hasDefault(): Boolean = hasDefault

    override fun KSValueParameter.getDoc(): String? = null
}
