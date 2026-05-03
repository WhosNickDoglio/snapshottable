package com.tunjid.snapshottable.compat.k2320

import com.tunjid.snapshottable.compat.CompatContext
import com.tunjid.snapshottable.compat.k230.CompatContextImpl as DelegateType
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.copy as copyDeclarationNative
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.plugin.SimpleFunctionBuildingContext
import org.jetbrains.kotlin.fir.plugin.createMemberFunction as createMemberFunctionNative
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.Name

public open class CompatContextImpl : CompatContext by DelegateType() {

    // Compiled against kotlin-compiler:2.3.20, where createMemberFunction returns FirNamedFunction.
    // The k230 delegate's bytecode references FirSimpleFunction (renamed away in 2.3.20), so this
    // override is the one that actually links on a 2.3.20+ runtime.
    override fun FirExtension.createMemberFunctionCompat(
        owner: FirClassSymbol<*>,
        key: GeneratedDeclarationKey,
        name: Name,
        returnType: ConeKotlinType,
        config: SimpleFunctionBuildingContext.() -> Unit,
    ): FirFunction = createMemberFunctionNative(
        owner = owner,
        key = key,
        name = name,
        returnType = returnType,
        config = config,
    )

    // Defensive override: FirDeclarationStatus.copy gained extra parameters across the 2.3.x dev
    // cycle. Recompiling this trivial wrapper here pins the call descriptor to whatever the 2.3.20
    // artifact exposed, instead of inheriting the 2.3.0-shaped descriptor from the delegate.
    override fun FirDeclarationStatus.copyCompat(
        isOverride: Boolean,
        visibility: Visibility?,
        modality: Modality?,
    ): FirDeclarationStatus = copyDeclarationNative(
        isOverride = isOverride,
        visibility = visibility,
        modality = modality,
    )

    public class Factory : CompatContext.Factory {
        override val minVersion: String = "2.3.20"
        override fun create(): CompatContext = CompatContextImpl()
    }
}
