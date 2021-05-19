package com.faendir.kotlin.autodsl

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import org.junit.jupiter.api.Test

class FailTest {

    @Test
    fun `interface`() {
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                interface Entity
            """,
            expect = COMPILATION_ERROR
        )
    }

    @Test
    fun `enum class`() {
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                enum class Entity { A, B, X }
            """,
            expect = COMPILATION_ERROR
        )
    }

    @Test
    fun `object`() {
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                object Entity
            """,
            expect = COMPILATION_ERROR
        )
    }

    @Test
    fun `abstract class`() {
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                abstract class Entity
            """,
            expect = COMPILATION_ERROR
        )
    }

    @Test
    fun `private constructor`() {
        compile(
            """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity private constructor()
            """,
            expect = COMPILATION_ERROR
        )
    }
}