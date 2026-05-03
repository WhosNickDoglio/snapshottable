package com.tunjid.snapshottable.compat.k240_beta2

import com.tunjid.snapshottable.compat.CompatContext
import com.tunjid.snapshottable.compat.k2320.CompatContextImpl as DelegateType
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

public class CompatContextImpl : CompatContext by DelegateType() {

    // In 2.4.0-Beta2, CompilerPluginRegistrar.ExtensionStorage.registerExtension switched its
    // first parameter from ProjectExtensionDescriptor to ExtensionPointDescriptor. The k2320
    // delegate's bytecode encodes the older descriptor; recompiling these wrappers here pins the
    // call site to the new shape.
    override fun CompilerPluginRegistrar.ExtensionStorage.registerFirExtensionCompat(
        extension: FirExtensionRegistrar,
    ) {
        FirExtensionRegistrarAdapter.registerExtension(extension)
    }

    override fun CompilerPluginRegistrar.ExtensionStorage.registerIrExtensionCompat(
        extension: IrGenerationExtension,
    ) {
        IrGenerationExtension.registerExtension(extension)
    }

    public class Factory : CompatContext.Factory {
        override val minVersion: String = "2.4.0-Beta2"
        override fun create(): CompatContext = CompatContextImpl()
    }
}
