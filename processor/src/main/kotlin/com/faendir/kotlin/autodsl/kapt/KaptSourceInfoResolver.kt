package com.faendir.kotlin.autodsl.kapt

import com.faendir.kotlin.autodsl.AnnotationFinder
import com.faendir.kotlin.autodsl.SourceInfoResolver
import com.faendir.kotlin.autodsl.nonnull
import com.faendir.kotlin.autodsl.toRawType
import com.faendir.kotlin.autodsl.withoutAnnotations
import com.faendir.kotlin.autodsl.withoutVariance
import com.google.devtools.ksp.symbol.ClassKind
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.asTypeVariableName
import com.squareup.kotlinpoet.metadata.classinspectors.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.specs.classFor
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror
import kotlin.collections.mapNotNull
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

@OptIn(DelicateKotlinPoetApi::class)
class KaptSourceInfoResolver(
    private val processingEnv: ProcessingEnvironment,
    private val roundEnv: RoundEnvironment,
) : AnnotationFinder<Annotated>(),
    SourceInfoResolver<Annotated, Type, Constructor, Parameter> {
    private val classInspector = ElementsClassInspector.create(lenient = true, processingEnv.elementUtils, processingEnv.typeUtils)

    private fun Set<Element>.mapToTypes() = filterIsInstance<TypeElement>().map { Type(it, it.toTypeSpec()) }

    override fun getClassesWithAnnotation(annotation: KClass<out Annotation>): List<Type> =
        roundEnv.getElementsAnnotatedWith(annotation.java).mapToTypes()

    override fun getClassesWithAnnotation(annotation: Type): List<Type> = roundEnv.getElementsAnnotatedWith(annotation.element).mapToTypes()

    override fun Type.getClassKind(): ClassKind =
        when (typeSpec.kind) {
            TypeSpec.Kind.CLASS -> {
                when {
                    typeSpec.modifiers.contains(KModifier.ENUM) -> ClassKind.ENUM_CLASS
                    typeSpec.modifiers.contains(KModifier.ANNOTATION) -> ClassKind.ANNOTATION_CLASS
                    element.kind == ElementKind.ENUM_CONSTANT -> ClassKind.ENUM_ENTRY
                    else -> ClassKind.CLASS
                }
            }

            TypeSpec.Kind.OBJECT -> {
                ClassKind.OBJECT
            }

            TypeSpec.Kind.INTERFACE -> {
                ClassKind.INTERFACE
            }
        }

    override fun Annotated.hasAnnotation(annotation: KClass<out Annotation>): Boolean = findAnnotation(annotation) != null

    override fun <T : Annotation> Annotated.getAnnotationTypeProperty(
        annotation: KClass<T>,
        property: KProperty1<T, KClass<*>>,
    ): ClassName? =
        try {
            findAnnotation(annotation)?.let(property)?.asClassName()
        } catch (e: MirroredTypeException) {
            (e.typeMirror.asTypeName() as? ClassName)
        }?.mapToKotlin()

    override fun <T : Annotation, V> Annotated.getAnnotationProperty(
        annotation: KClass<T>,
        property: KProperty1<T, V>,
    ): V? = findAnnotation(annotation)?.let(property)

    override fun <T : Annotation> Annotated.getDirectAnnotation(annotation: KClass<T>): T? = getAnnotation(annotation)

    override fun Annotated.getDirectAnnotations(): Iterable<Annotated> =
        getAnnotationMirrors()
            .mapNotNull {
                it.annotationType.toType()?.element
            }.map { Type(it, it.toTypeSpec()) }

    override fun Annotated.getQualifiedName(): String? = (this as? Type)?.element?.qualifiedName?.toString()

    override fun Type.isAbstract(): Boolean = KModifier.ABSTRACT in typeSpec.modifiers

    override fun Type.isDataClass(): Boolean = KModifier.DATA in typeSpec.modifiers

    override fun Type.getConstructors(): List<Constructor> {
        val constructorElements =
            element.enclosedElements
                .filterIsInstance<ExecutableElement>()
                .filter {
                    it.kind == ElementKind.CONSTRUCTOR
                }.toMutableList()
        return (
            listOfNotNull(typeSpec.primaryConstructor?.let { it to true }) +
                typeSpec.funSpecs
                    .filter { it.isConstructor }
                    .map { it to false }
        ).map { (constructorSpec, isPrimary) ->
            val element =
                constructorElements.first { element ->
                    element.parameters.size == constructorSpec.parameters.size &&
                        (element.parameters zip constructorSpec.parameters).all { (e, k) ->
                            val eType = e.asType().asTypeName().mapToKotlin()
                            val kType = k.type.nonnull
                            when (eType) {
                                is ParameterizedTypeName if kType is ParameterizedTypeName -> {
                                    // Invariant kotlin parameters are variant in java, just check erased type
                                    eType.rawType == kType.rawType
                                }

                                is ParameterizedTypeName if kType is LambdaTypeName -> {
                                    // Lambdas are kotlin.FunctionX types in java
                                    eType.typeArguments.map { it.toRawType() } ==
                                        listOfNotNull(kType.receiver) + kType.parameters.map { it.type.withoutAnnotations() } +
                                        kType.returnType
                                }

                                is TypeVariableName if kType is TypeVariableName -> {
                                    eType.name == kType.name
                                }

                                else -> {
                                    eType.withoutVariance() == kType.withoutVariance()
                                }
                            }
                        }
                }
            Constructor(this, element, constructorSpec, isPrimary)
        }
    }

    override fun Type.getPropertyNames(): Set<String> =
        this.typeSpec.propertySpecs
            .map { it.name }
            .toSet()

    override fun Constructor.isAccessible(): Boolean =
        KModifier.PRIVATE !in constructorSpec.modifiers && KModifier.PROTECTED !in constructorSpec.modifiers

    override fun Type.getPrimaryConstructor(): Constructor? = getConstructors().find { it.isPrimary }

    override fun Constructor.getParentType(): Type = parent

    override fun Constructor.isValid(): Boolean = true

    override fun Constructor.getParameters(): List<Parameter> =
        (element.parameters zip constructorSpec.parameters).map { (element, parameterSpec) ->
            Parameter(
                element,
                parameterSpec.toBuilder(type = parameterSpec.type.withoutAnnotations()).build(),
            )
        }

    override fun Type.asClassName(): ClassName = element.asClassName()

    private fun TypeMirror.toType(): Type? =
        (processingEnv.typeUtils.asElement(this) as? TypeElement)?.let { element ->
            try {
                Type(element, element.toTypeSpec())
            } catch (_: Exception) {
                null
            }
        }

    private fun TypeName.toType(): Type? {
        val className = (this as? ClassName) ?: (this as? ParameterizedTypeName)?.rawType ?: return null
        val typeElement = processingEnv.elementUtils.getTypeElement(className.canonicalName) ?: return null

        return Type(typeElement, typeElement.toTypeSpec())
    }

    override fun Parameter.getTypeDeclaration(): Type? = element.asType().toType()

    override fun Parameter.getTypeArguments(): List<Type> {
        val type = parameterSpec.type

        val parameterizedType = type as? ParameterizedTypeName ?: return emptyList()

        return parameterizedType.typeArguments.mapNotNull { typeName ->
            typeName.toType()
        }
    }

    override fun Parameter.getTypeName(parent: Type?): TypeName = parameterSpec.type

    override fun Parameter.getName(): String = parameterSpec.name

    override fun Parameter.hasDefault(): Boolean = parameterSpec.defaultValue != null

    override fun Parameter.getDoc(): String? = processingEnv.elementUtils.getDocComment(element)

    private fun TypeElement.toTypeSpec(): TypeSpec =
        if (this.getAnnotation(Metadata::class.java) != null) {
            try {
                toTypeSpec(lenient = true, classInspector)
            } catch (_: Exception) {
                TypeSpec.classBuilder(this.simpleName.toString()).build()
            }
        } else {
            TypeSpec.classBuilder(this.simpleName.toString()).build()
        }

    override fun Type.getTypeVariableNames(): List<TypeVariableName> =
        typeSpec.typeVariables.map { typeVariableName ->
            typeVariableName.copy(
                bounds =
                    typeVariableName.bounds.filterNot {
                        it == ClassName("kotlin", "Any") ||
                            it == ClassName("java.lang", "Object")
                    },
            )
        }
}

interface Annotated {
    fun <T : Annotation> getAnnotation(annotation: KClass<T>): T?

    fun getAnnotationMirrors(): List<AnnotationMirror>
}

class Type(
    internal val element: TypeElement,
    internal val typeSpec: TypeSpec,
) : Annotated {
    override fun <T : Annotation> getAnnotation(annotation: KClass<T>): T? = element.getAnnotation(annotation.java)

    override fun getAnnotationMirrors(): List<AnnotationMirror> = element.annotationMirrors

    override fun toString(): String = element.toString()
}

class Constructor(
    internal val parent: Type,
    internal val element: ExecutableElement,
    internal val constructorSpec: FunSpec,
    internal val isPrimary: Boolean,
) : Annotated {
    override fun <T : Annotation> getAnnotation(annotation: KClass<T>): T? = element.getAnnotation(annotation.java)

    override fun getAnnotationMirrors(): List<AnnotationMirror> = element.annotationMirrors
}

class Parameter(
    internal val element: VariableElement,
    internal val parameterSpec: ParameterSpec,
) : Annotated {
    override fun <T : Annotation> getAnnotation(annotation: KClass<T>): T? = element.getAnnotation(annotation.java)

    override fun getAnnotationMirrors(): List<AnnotationMirror> = element.annotationMirrors
}
