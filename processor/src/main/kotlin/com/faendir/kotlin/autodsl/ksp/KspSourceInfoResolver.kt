package com.faendir.kotlin.autodsl.ksp

import com.faendir.kotlin.autodsl.SourceInfoResolver
import com.google.devtools.ksp.KSTypeNotPresentException
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
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
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import com.google.devtools.ksp.getConstructors as superGetConstructors

@OptIn(KspExperimental::class)
class KspSourceInfoResolver(private val resolver: Resolver) : SourceInfoResolver<KSAnnotated, KSClassDeclaration, KSFunctionDeclaration, KSValueParameter> {
    private fun getClassesWithAnnotation(annotation: String): List<KSClassDeclaration> =
        resolver.getSymbolsWithAnnotation(annotation).filterIsInstance<KSClassDeclaration>().toList()

    override fun getClassesWithAnnotation(annotation: KClass<out Annotation>) = getClassesWithAnnotation(annotation.qualifiedName!!)

    override fun getClassesWithAnnotation(annotation: KSClassDeclaration) = getClassesWithAnnotation(annotation.qualifiedName!!.asString())

    override fun KSClassDeclaration.getClassKind(): ClassKind = classKind

    override fun KSAnnotated.hasAnnotation(annotation: KClass<out Annotation>): Boolean = isAnnotationPresent(annotation)

    override fun <T : Annotation> KSAnnotated.getAnnotationTypeProperty(annotation: KClass<T>, property: KProperty1<T, KClass<*>>): ClassName? =
        getAnnotationsByType(annotation).firstOrNull()?.let { annotationValue ->
            try {
                property.get(annotationValue).asClassName()
            } catch (e: KSTypeNotPresentException) {
                e.ksType.toClassName()
            }
        }

    override fun <T : Annotation, V> KSAnnotated.getAnnotationProperty(annotation: KClass<T>, property: KProperty1<T, V>): V? =
        getAnnotationsByType(annotation).firstOrNull()?.let { property.get(it) }

    override fun KSClassDeclaration.isAbstract(): Boolean = modifiers.contains(Modifier.ABSTRACT)

    override fun KSClassDeclaration.getConstructors(): List<KSFunctionDeclaration> = superGetConstructors().toList()

    override fun KSFunctionDeclaration.isAccessible(): Boolean = isPublic() || isInternal()

    override fun KSClassDeclaration.getPrimaryConstructor(): KSFunctionDeclaration? = primaryConstructor

    override fun KSFunctionDeclaration.isValid(): Boolean = validate()

    override fun KSFunctionDeclaration.getParameters(): List<KSValueParameter> = parameters

    override fun KSClassDeclaration.asClassName(): ClassName = toClassName()

    override fun KSValueParameter.getTypeDeclaration(): KSClassDeclaration? = type.resolve().declaration as? KSClassDeclaration

    override fun KSValueParameter.getTypeArguments(): List<KSClassDeclaration> =
        type.resolve().arguments.mapNotNull { it.type?.resolve()?.declaration as? KSClassDeclaration }

    override fun KSValueParameter.getTypeName(): TypeName = type.resolve().asTypeName()

    override fun KSValueParameter.getName(): String = name!!.asString()

    override fun KSValueParameter.hasDefault(): Boolean = hasDefault

    override fun KSValueParameter.getDoc(): String? = null
}