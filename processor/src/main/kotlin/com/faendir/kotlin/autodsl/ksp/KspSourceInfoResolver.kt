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
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.TypeParameterResolver
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.toTypeParameterResolver
import com.squareup.kotlinpoet.ksp.toTypeVariableName
import io.github.enjoydambience.kotlinbard.nullable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
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

    override fun KSClassDeclaration.isAbstract(): Boolean = Modifier.ABSTRACT in modifiers

    override fun KSClassDeclaration.isDataClass(): Boolean = Modifier.DATA in modifiers

    override fun KSClassDeclaration.getConstructors(): List<KSFunctionDeclaration> = superGetConstructors().toList()

    override fun KSClassDeclaration.getPropertyNames(): Set<String> = getAllProperties().map { it.simpleName.asString() }.toSet()

    override fun KSFunctionDeclaration.isAccessible(): Boolean = isPublic() || isInternal()

    override fun KSClassDeclaration.getPrimaryConstructor(): KSFunctionDeclaration? = primaryConstructor

    override fun KSFunctionDeclaration.getParentType(): KSClassDeclaration = parentDeclaration as KSClassDeclaration

    override fun KSFunctionDeclaration.isValid(): Boolean = validate()

    override fun KSFunctionDeclaration.getParameters(): List<KSValueParameter> = parameters

    override fun KSClassDeclaration.asClassName(): ClassName = toClassName()

    override fun KSValueParameter.getTypeDeclaration(): KSClassDeclaration? = type.resolve().declaration as? KSClassDeclaration

    override fun KSValueParameter.getTypeArguments(): List<KSClassDeclaration> =
        type.resolve().arguments.mapNotNull { it.type?.resolve()?.declaration as? KSClassDeclaration }

    override fun KSValueParameter.getTypeName(parent: KSClassDeclaration?): TypeName =
        type.toTypeName(parent?.typeParameters?.toTypeParameterResolver() ?: TypeParameterResolver.EMPTY)

    override fun KSValueParameter.getName(): String = name!!.asString()

    override fun KSValueParameter.hasDefault(): Boolean = hasDefault

    override fun KSValueParameter.getDoc(): String? = null

    override fun KSClassDeclaration.getTypeVariableNames(): List<TypeVariableName> =
        typeParameters.map { it.toTypeVariableName(typeParameters.toTypeParameterResolver()) }.map { typeVariableName ->
            if (typeVariableName.bounds.contains(ClassName("kotlin", "Any").nullable)) {
                typeVariableName.copy(bounds = typeVariableName.bounds.filterNot { it == ClassName("kotlin", "Any").nullable })
            } else {
                typeVariableName
            }
        }
}
