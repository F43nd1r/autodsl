package com.faendir.kotlin.autodsl

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import org.junit.jupiter.api.TestFactory

class FailTest {

    @TestFactory
    fun `interface`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                interface Entity
            """,
        expect = COMPILATION_ERROR
    )

    @TestFactory
    fun `enum class`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                enum class Entity { A, B, X }
            """,
        expect = COMPILATION_ERROR
    )

    @TestFactory
    fun `object`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                object Entity
            """,
        expect = COMPILATION_ERROR
    )

    @TestFactory
    fun `abstract class`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                abstract class Entity
            """,
        expect = COMPILATION_ERROR
    )

    @TestFactory
    fun `private constructor`() = compile(
        """
                import com.faendir.kotlin.autodsl.AutoDsl
                @AutoDsl
                class Entity private constructor()
            """,
        expect = COMPILATION_ERROR
    )
}