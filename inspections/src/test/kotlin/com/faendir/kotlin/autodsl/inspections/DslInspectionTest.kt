package com.faendir.kotlin.autodsl.inspections

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File
import kotlin.contracts.ExperimentalContracts
import org.intellij.lang.annotations.Language
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalContracts::class)
@RunWith(JUnit4::class)
abstract class DslInspectionTest(
    @param:Language("kotlin") val dsl: String,
) : BasePlatformTestCase() {
    override fun getTestDataPath(): String = "src/test/testData"

    private fun configureByExternalFile(path: String) {
        val file = File(path)
        myFixture.configureByText(file.name, file.readText())
    }

    override fun setUp() {
        super.setUp()
        configureByExternalFile("../annotations/src/main/kotlin/com/faendir/kotlin/autodsl/DslInspect.kt")
        configureByExternalFile("../annotations/src/main/kotlin/com/faendir/kotlin/autodsl/DslMandatory.kt")
        myFixture.configureByText(
            "Dsl.kt",
            """
import com.faendir.kotlin.autodsl.DslInspect
import com.faendir.kotlin.autodsl.DslMandatory

$dsl
            """,
        )
        myFixture.enableInspections(DslInspection::class.java)
    }

    fun configureTest(
        @Language("kotlin") code: String,
    ) {
        myFixture.configureByText(
            "Test.kt",
            """
import com.faendir.kotlin.autodsl.DslInspect
import com.faendir.kotlin.autodsl.DslMandatory

fun test() {
$code
}
            """,
        )
    }

    fun checkHighlighting() {
        myFixture.checkHighlighting(true, false, false)
    }

    fun checkIntention(
        property: String,
        code: String,
    ) {
        val intention = myFixture.findSingleIntention("Insert assignment for $property")
        myFixture.launchAction(intention)
        myFixture.checkResult(
            """
import com.faendir.kotlin.autodsl.DslInspect
import com.faendir.kotlin.autodsl.DslMandatory

fun test() {
$code
}
            """,
        )
    }
}
