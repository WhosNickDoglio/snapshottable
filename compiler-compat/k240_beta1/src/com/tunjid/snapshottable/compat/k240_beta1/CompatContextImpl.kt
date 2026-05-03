package com.tunjid.snapshottable.compat.k240_beta1

import com.tunjid.snapshottable.compat.CompatContext
import com.tunjid.snapshottable.compat.k240_dev_2124.CompatContextImpl as DelegateType

// 2.4.0-Beta1 changed APIs that don't sit on the snapshottable compat surface (FirEvaluatorResult
// shape, IrDiagnosticReporter.at(...).report() chain). The module exists as a chain link so that
// factory selection prefers it over k240_dev_2124 when an IDE bundles a Kotlin matching this
// version exactly, and as a hook for any Beta1-specific snapshottable APIs we might add later.
public open class CompatContextImpl : CompatContext by DelegateType() {

    public class Factory : CompatContext.Factory {
        override val minVersion: String = "2.4.0-Beta1"
        override fun create(): CompatContext = CompatContextImpl()
    }
}
