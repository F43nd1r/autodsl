package com.faendir.kotlin.autodsl.kapt

import com.faendir.kotlin.autodsl.Logger
import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

class KaptLogger(private val delegate: Messager): Logger<Type> {
    override fun error(message: String, source: Type?) = delegate.printMessage(Diagnostic.Kind.ERROR, message, source?.element)
}