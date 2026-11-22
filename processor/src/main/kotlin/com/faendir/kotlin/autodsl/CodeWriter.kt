package com.faendir.kotlin.autodsl

import com.squareup.kotlinpoet.FileSpec

interface CodeWriter<S> {
    fun emit(
        source: S,
        fileSpec: FileSpec,
        extra: String = "",
    )
}

fun <S> FileSpec.writeTo(
    source: S,
    codeWriter: CodeWriter<S>,
    extra: String = "",
) = codeWriter.emit(source, this, extra)
