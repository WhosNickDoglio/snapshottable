package com.tunjid.snapshottable.compat.k240_beta2

import com.tunjid.snapshottable.compat.CompatContext
import com.tunjid.snapshottable.compat.k240_beta1.CompatContextImpl as DelegateType

// 2.4.0-Beta2 changed APIs that don't sit on the snapshottable compat surface (FirAnnotation-
// Container deprecation lookup split, FirValueParameter copy-builder churn). The registerExtension
// shape that did matter for us was already pinned by k240_dev_2124 and inherits through the chain.
// This module exists for forward-compatible factory selection and as a hook for any Beta2-specific
// APIs we might add later.
public class CompatContextImpl : CompatContext by DelegateType() {

    public class Factory : CompatContext.Factory {
        override val minVersion: String = "2.4.0-Beta2"
        override fun create(): CompatContext = CompatContextImpl()
    }
}
