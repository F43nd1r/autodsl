package com.faendir.kotlin.autodsl

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtPsiFactory

@OptIn(CompilerConfiguration.Internals::class, K1Deprecation::class)
class PsiElementFactory : Disposable {
    private val disposable = Disposer.newDisposable()
    val ktPsiFactory: KtPsiFactory

    init {
        val env =
            KotlinCoreEnvironment.createForProduction(
                disposable,
                CompilerConfiguration(),
                EnvironmentConfigFiles.METADATA_CONFIG_FILES,
            )
        ktPsiFactory = KtPsiFactory(env.project)
    }

    override fun dispose() {
        Disposer.dispose(disposable)
    }
}
