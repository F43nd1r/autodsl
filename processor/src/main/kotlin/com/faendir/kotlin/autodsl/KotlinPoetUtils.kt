package com.faendir.kotlin.autodsl

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import org.jetbrains.kotlin.psi.KtFile

@Suppress("UNCHECKED_CAST")
val <T : TypeName> T.nonnull: T
    get() = copy(nullable = false) as T

fun TypeName.toRawType(): ClassName =
    when (this) {
        is ParameterizedTypeName -> this.rawType
        is ClassName -> this
        is WildcardTypeName -> this.inTypes.firstOrNull()?.toRawType() ?: this.outTypes.first().toRawType()
        is LambdaTypeName -> ClassName("kotlin", "Function${(if (receiver != null) 1 else 0) + parameters.size}")
        is TypeVariableName -> Any::class.asClassName()
        else -> throw IllegalArgumentException("Unsupported conversion to raw type from $this")
    }

fun TypeName.withoutAnnotations(): TypeName =
    when (this) {
        is ParameterizedTypeName -> {
            val typeArguments = typeArguments.map { it.withoutAnnotations() }
            if (typeArguments == this.typeArguments) this else rawType.parameterizedBy(*typeArguments.toTypedArray())
        }

        is WildcardTypeName -> {
            if (inTypes.size == 1) {
                val inType = inTypes.first().withoutAnnotations()
                if (inType == this.inTypes.first()) this else WildcardTypeName.consumerOf(inType)
            } else if (outTypes.size == 1) {
                val outType = outTypes.first().withoutAnnotations()
                if (outType == this.outTypes.first()) this else WildcardTypeName.producerOf(outType)
            } else {
                this
            }
        }

        is LambdaTypeName -> {
            val receiver = receiver?.withoutAnnotations()
            val parameters = parameters.map { it.toBuilder(type = it.type.withoutAnnotations()).build() }
            val returnType = returnType.withoutAnnotations()
            if (receiver == this.receiver &&
                parameters == this.parameters &&
                returnType == this.returnType
            ) {
                this
            } else {
                LambdaTypeName.get(receiver, parameters, returnType)
            }
        }

        is ClassName -> {
            this.copy(annotations = emptyList())
        }

        else -> {
            this
        }
    }

fun ClassName.withBuilderSuffix() = ClassName(packageName, "${simpleNames.joinToString("")}Builder")

fun TypeName.withBuilderSuffix() = toRawType().withBuilderSuffix()

fun TypeName.asLambdaReceiver() = LambdaTypeName.get(receiver = this, returnType = Unit::class.asClassName())

fun FileSpec.Builder.addImportsFrom(cloned: SourceInfoResolver.ClonedPrivateTopLevels): FileSpec.Builder {
    val bodyOnlyText =
        build()
            .toString()
            .lines()
            .filterNot { line ->
                val trimmed = line.trim()
                trimmed.startsWith("import ") || trimmed.startsWith("package ")
            }.joinToString("\n") + "\n${cloned.rawCode}"

    cloned.file
        ?.importDirectives
        ?.filter { directive ->
            if (directive.isAllUnder) return@filter true

            val nameToCheck =
                directive.aliasName ?: directive.importedFqName?.shortName()?.asString()
                    ?: return@filter false

            val regex = Regex("""\b${Regex.escape(nameToCheck)}\b""")
            regex.containsMatchIn(bodyOnlyText)
        }?.forEach { directive ->
            val fqName = directive.importedFqName ?: return@forEach
            val fqString = fqName.asString()
            val alias = directive.aliasName

            when {
                directive.isAllUnder -> {
                    addDefaultPackageImport(fqString)
                }

                alias != null -> {
                    val className = ClassName.bestGuess(fqString)
                    addAliasedImport(className, alias)
                }

                else -> {
                    val packageName = fqName.parent().asString()
                    val shortName = fqName.shortName().asString()

                    if (shortName.first().isUpperCase()) {
                        addImport(packageName, shortName)
                    } else {
                        addImport(MemberName(packageName, shortName))
                    }
                }
            }
        }
    return this
}
