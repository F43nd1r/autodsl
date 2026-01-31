package com.faendir.kotlin.autodsl

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import org.junit.jupiter.api.TestFactory

class FailTest {

    @TestFactory
    fun `interface`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl($SAFETY)
                interface Entity
            """,
        expect = COMPILATION_ERROR
    )

    @TestFactory
    fun `enum class`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl($SAFETY)
                enum class Entity { A, B, X }
            """,
        expect = COMPILATION_ERROR
    )

    @TestFactory
    fun `object`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl($SAFETY)
                object Entity
            """,
        expect = COMPILATION_ERROR
    )

    @TestFactory
    fun `abstract class`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl($SAFETY)
                abstract class Entity
            """,
        expect = COMPILATION_ERROR
    )

    @TestFactory
    fun `private constructor`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl($SAFETY)
                class Entity private constructor()
            """,
        expect = COMPILATION_ERROR
    )
}