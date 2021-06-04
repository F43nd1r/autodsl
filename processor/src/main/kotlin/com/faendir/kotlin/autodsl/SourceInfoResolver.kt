package com.faendir.kotlin.autodsl

import com.google.devtools.ksp.symbol.ClassKind
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface SourceInfoResolver<ANNOTATED, TYPE : ANNOTATED, CONSTRUCTOR : ANNOTATED, PARAMETER : ANNOTATED> {

    fun getClassesWithAnnotation(annotation: KClass<out Annotation>): List<TYPE>

    fun getClassesWithAnnotation(annotation: TYPE): List<TYPE>

    fun TYPE.getClassKind(): ClassKind

    fun ANNOTATED.hasAnnotation(annotation: KClass<out Annotation>): Boolean

    fun <T : Annotation> ANNOTATED.getAnnotationTypeProperty(annotation: KClass<T>, property: KProperty1<T, KClass<*>>): ClassName?

    fun <T : Annotation, V> ANNOTATED.getAnnotationProperty(annotation: KClass<T>, property: KProperty1<T, V>): V?

    fun TYPE.isAbstract(): Boolean

    fun TYPE.getConstructors(): List<CONSTRUCTOR>

    fun CONSTRUCTOR.isAccessible(): Boolean

    fun TYPE.getPrimaryConstructor(): CONSTRUCTOR?

    fun CONSTRUCTOR.isValid(): Boolean

    fun CONSTRUCTOR.getParameters(): List<PARAMETER>

    fun TYPE.asClassName(): ClassName

    fun PARAMETER.getTypeDeclaration(): TYPE?

    fun PARAMETER.getTypeArguments(): List<TYPE>

    fun PARAMETER.getTypeName(): TypeName

    fun PARAMETER.getName(): String

    fun PARAMETER.hasDefault(): Boolean

    fun PARAMETER.getDoc(): String?
}