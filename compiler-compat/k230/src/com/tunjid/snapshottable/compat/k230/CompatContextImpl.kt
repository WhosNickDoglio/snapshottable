package com.tunjid.snapshottable.compat.k230

import com.tunjid.snapshottable.compat.CompatContext
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.fir.plugin.ClassBuildingContext
import org.jetbrains.kotlin.fir.plugin.PropertyBuildingContext
import org.jetbrains.kotlin.fir.plugin.SimpleFunctionBuildingContext
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.Name

public open class CompatContextImpl : CompatContext {

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

    // Compiled against kotlin-compiler:2.3.0, where createMemberFunction returns FirSimpleFunction.
    // The JVM call-site descriptor will reference that concrete class. Source-level the result is
    // upcast to FirFunction so the interface signature stays stable across Kotlin versions.
    override fun FirExtension.createMemberFunctionCompat(
        owner: FirClassSymbol<*>,
        key: GeneratedDeclarationKey,
        name: Name,
        returnType: ConeKotlinType,
        config: SimpleFunctionBuildingContext.() -> Unit,
    ): FirFunction = createMemberFunction(
        owner = owner,
        key = key,
        name = name,
        returnType = returnType,
        config = config,
    )

    override fun FirExtension.createMemberPropertyCompat(
        owner: FirClassSymbol<*>,
        key: GeneratedDeclarationKey,
        name: Name,
        returnType: ConeKotlinType,
        isVal: Boolean,
        hasBackingField: Boolean,
        config: PropertyBuildingContext.() -> Unit,
    ): FirProperty = createMemberProperty(
        owner = owner,
        key = key,
        name = name,
        returnType = returnType,
        isVal = isVal,
        hasBackingField = hasBackingField,
        config = config,
    )

    override fun FirExtension.createNestedClassCompat(
        owner: FirClassSymbol<*>,
        name: Name,
        key: GeneratedDeclarationKey,
        classKind: ClassKind,
        config: ClassBuildingContext.() -> Unit,
    ): FirRegularClass = createNestedClass(
        owner = owner,
        name = name,
        key = key,
        classKind = classKind,
        config = config,
    )

    override fun FirRegularClass.replaceAnnotationsCompat(
        annotations: List<FirAnnotation>,
    ) {
        replaceAnnotations(annotations)
    }

    override fun FirDeclarationStatus.copyCompat(
        isOverride: Boolean,
        visibility: Visibility?,
        modality: Modality?,
    ): FirDeclarationStatus = copy(
        isOverride = isOverride,
        visibility = visibility,
        modality = modality,
    )

    public class Factory : CompatContext.Factory {
        override val minVersion: String = "2.3.0"
        override fun create(): CompatContext = CompatContextImpl()
    }
}
