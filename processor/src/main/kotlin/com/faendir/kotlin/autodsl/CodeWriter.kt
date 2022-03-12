package com.faendir.kotlin.autodsl

import com.squareup.kotlinpoet.FileSpec

interface CodeWriter<S> {
    fun emit(source: S, fileSpec: FileSpec)
}

fun <S> FileSpec.writeTo(source: S, codeWriter: CodeWriter<S>) = codeWriter.emit(source, this)