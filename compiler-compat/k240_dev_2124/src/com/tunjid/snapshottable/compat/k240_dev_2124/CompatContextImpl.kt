package com.tunjid.snapshottable.compat.k240_dev_2124

import com.tunjid.snapshottable.compat.CompatContext
import com.tunjid.snapshottable.compat.k2320.CompatContextImpl as DelegateType
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

public open class CompatContextImpl : CompatContext by DelegateType() {

    // Compiled against kotlin-compiler:2.4.0-dev-2124. The static
    // CompilerPluginRegistrar.ExtensionStorage.registerExtension call this dispatches to
    // gained the ExtensionPointDescriptor receiver shape during the 2.4.0 dev cycle (this is
    // also where metro had to introduce a context(_: CompilerPluginRegistrar) override). The
    // k2320 delegate's bytecode still references the older ProjectExtensionDescriptor
    // descriptor; pinning the call site here lets later 2.4.0 modules inherit the new shape.
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
        override val minVersion: String = "2.4.0-dev-2124"
        override fun create(): CompatContext = CompatContextImpl()
    }
}
