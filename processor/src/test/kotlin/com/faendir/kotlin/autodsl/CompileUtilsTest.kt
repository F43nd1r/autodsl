package com.faendir.kotlin.autodsl

import strikt.api.expectThrows
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import java.lang.reflect.InvocationTargetException
import kotlin.test.Test

class CompileUtilsTest {
    @Test
    fun `test function is evaluated for ksp`() {
        expectThrows<InvocationTargetException> {
            compileKsp("fun main() {}", """fun test() { kotlin.test.fail("this is expected") }""")
        }.get { cause }.isA<AssertionError>().get { message }.isEqualTo("this is expected")
    }
    @Test
    fun `test function is evaluated for kapt`() {
        expectThrows<InvocationTargetException> {
            compileKapt("fun main() {}", """fun test() { kotlin.test.fail("this is expected") }""")
        }.get { cause }.isA<AssertionError>().get { message }.isEqualTo("this is expected")
    }
}