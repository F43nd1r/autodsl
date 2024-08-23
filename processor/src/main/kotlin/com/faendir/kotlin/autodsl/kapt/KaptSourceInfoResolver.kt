@file:OptIn(KotlinPoetMetadataPreview::class)

package com.faendir.kotlin.autodsl.kapt

import com.faendir.kotlin.autodsl.SourceInfoResolver
import com.faendir.kotlin.autodsl.nonnull
import com.faendir.kotlin.autodsl.toRawType
import com.google.devtools.ksp.symbol.ClassKind
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.toKmClass
import kotlinx.metadata.KmClass
import kotlinx.metadata.KmConstructor
import kotlinx.metadata.KmValueParameter
import kotlinx.metadata.Modality
import kotlinx.metadata.Visibility
import kotlinx.metadata.declaresDefaultValue
import kotlinx.metadata.isSecondary
import kotlinx.metadata.kind
import kotlinx.metadata.modality
import kotlinx.metadata.visibility
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.MirroredTypeException
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class KaptSourceInfoResolver(private val processingEnv: ProcessingEnvironment, private val roundEnv: RoundEnvironment) :
    SourceInfoResolver<Annotated, Type, Constructor, Parameter> {
    override fun getClassesWithAnnotation(annotation: KClass<out Annotation>): List<Type> =
        roundEnv.getElementsAnnotatedWith(annotation.java).filterIsInstance<TypeElement>().map { Type(it) }

    override fun getClassesWithAnnotation(annotation: Type): List<Type> =
        roundEnv.getElementsAnnotatedWith(annotation.element).filterIsInstance<TypeElement>().map { Type(it) }

    override fun Type.getClassKind(): ClassKind = when (kmClass.kind) {
        kotlinx.metadata.ClassKind.CLASS -> ClassKind.CLASS
        kotlinx.metadata.ClassKind.INTERFACE -> ClassKind.INTERFACE
        kotlinx.metadata.ClassKind.ENUM_CLASS -> ClassKind.ENUM_CLASS
        kotlinx.metadata.ClassKind.ENUM_ENTRY -> ClassKind.ENUM_ENTRY
        kotlinx.metadata.ClassKind.ANNOTATION_CLASS -> ClassKind.ANNOTATION_CLASS
        kotlinx.metadata.ClassKind.OBJECT -> ClassKind.OBJECT
        kotlinx.metadata.ClassKind.COMPANION_OBJECT -> ClassKind.OBJECT
    }

    override fun <T : Annotation> Annotated.getAnnotationTypeProperty(annotation: KClass<T>, property: KProperty1<T, KClass<*>>): ClassName? = try {
        getAnnotation(annotation)?.let(property)?.asClassName()
    } catch (e: MirroredTypeException) {
        (e.typeMirror.asTypeName() as? ClassName)
    }?.mapToKotlin()

    override fun Annotated.hasAnnotation(annotation: KClass<out Annotation>): Boolean = getAnnotation(annotation) != null

    override fun <T : Annotation, V> Annotated.getAnnotationProperty(annotation: KClass<T>, property: KProperty1<T, V>): V? =
        getAnnotation(annotation)?.let(property)

    override fun Type.isAbstract(): Boolean = kmClass.modality == Modality.ABSTRACT

    override fun Type.getConstructors(): List<Constructor> {
        val constructorElements = element.enclosedElements.filterIsInstance<ExecutableElement>().filter { it.kind == ElementKind.CONSTRUCTOR }.toMutableList()
        return kmClass.constructors.associateWith { kmConstructor ->
            constructorElements.first { element ->
                element.parameters.size == kmConstructor.valueParameters.size && (element.parameters zip kmConstructor.valueParameters).all { (e, k) ->
                    val eType = e.asType().asTypeName().mapToKotlin()
                    val kType = k.type.asTypeName().nonnull
                    if (eType is ParameterizedTypeName && kType is ParameterizedTypeName) {
                        //Invariant kotlin parameters are variant in java, just check erased type
                        eType.rawType == kType.rawType
                    } else if (eType is ParameterizedTypeName && kType is LambdaTypeName) {
                        eType.typeArguments.map { it.toRawType() } == listOfNotNull(kType.receiver) + kType.parameters.map { it.type } + kType.returnType
                    } else {
                        eType == kType
                    }
                }
            }
        }.map { (kmConstructor, element) -> Constructor(element, kmConstructor) }
    }

    override fun Constructor.isAccessible(): Boolean = kmConstructor.visibility in listOf(Visibility.PUBLIC, Visibility.INTERNAL)

    override fun Type.getPrimaryConstructor(): Constructor? = getConstructors().find { !it.kmConstructor.isSecondary }

    override fun Constructor.isValid(): Boolean = true

    override fun Constructor.getParameters(): List<Parameter> =
        (element.parameters zip kmConstructor.valueParameters).map { (element, kmValueParameter) -> Parameter(element, kmValueParameter) }

    override fun Type.asClassName(): ClassName = kmClass.asClassName()

    @OptIn(KotlinPoetMetadataPreview::class)
    override fun Parameter.getTypeDeclaration(): Type? {
        val element = processingEnv.typeUtils.asElement(element.asType()) as? TypeElement
        val kmType = try {
            element?.toKmClass()
        } catch (e: IllegalStateException) {
            null
        }
        return if (element != null && kmType != null) Type(element, kmType) else null
    }

    override fun Parameter.getTypeArguments(): List<Type> =
        (element.asType() as DeclaredType).typeArguments.mapNotNull {
            try {
                Type(processingEnv.typeUtils.asElement(it) as TypeElement)
            } catch (e: Exception) {
                null
            }
        }

    override fun Parameter.getTypeName(): TypeName = kmValueParameter.type.asTypeName()

    override fun Parameter.getName(): String = kmValueParameter.name

    override fun Parameter.hasDefault(): Boolean = kmValueParameter.declaresDefaultValue

    override fun Parameter.getDoc(): String? = processingEnv.elementUtils.getDocComment(element)
}

interface Annotated {
    fun <T : Annotation> getAnnotation(annotation: KClass<T>): T?
}

class Type(internal val element: TypeElement, internal val kmClass: KmClass) : Annotated {
    constructor(element: TypeElement) : this(element, element.toKmClass())

    override fun <T : Annotation> getAnnotation(annotation: KClass<T>): T? = element.getAnnotation(annotation.java)
}

class Constructor(internal val element: ExecutableElement, internal val kmConstructor: KmConstructor) : Annotated {
    override fun <T : Annotation> getAnnotation(annotation: KClass<T>): T? = element.getAnnotation(annotation.java)
}

class Parameter(internal val element: VariableElement, internal val kmValueParameter: KmValueParameter) : Annotated {
    override fun <T : Annotation> getAnnotation(annotation: KClass<T>): T? = element.getAnnotation(annotation.java)
}