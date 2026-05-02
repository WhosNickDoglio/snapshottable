package com.tunjid.snapshottable

import com.tunjid.snapshottable.compat.CompatContext
import com.tunjid.snapshottable.fir.SnapshottableFilters
import com.tunjid.snapshottable.fir.checkers.SnapshottableAdditionalCheckersExtension
import com.tunjid.snapshottable.fir.checkers.SnapshottableDiagnostics
import com.tunjid.snapshottable.fir.snapshottableClassGenerator
import com.tunjid.snapshottable.fir.snapshottableStatusTransformer
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class SnapshottablePluginRegistrar(
    private val compatContext: CompatContext,
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::SnapshottableFilters
        +compatContext::snapshottableClassGenerator
        +compatContext::snapshottableStatusTransformer
        +::SnapshottableAdditionalCheckersExtension

        registerDiagnosticContainers(SnapshottableDiagnostics)
    }
}
