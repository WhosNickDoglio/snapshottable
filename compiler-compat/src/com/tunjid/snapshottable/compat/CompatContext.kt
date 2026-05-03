package com.tunjid.snapshottable.compat

import java.util.ServiceLoader
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.plugin.ClassBuildingContext
import org.jetbrains.kotlin.fir.plugin.PropertyBuildingContext
import org.jetbrains.kotlin.fir.plugin.SimpleFunctionBuildingContext
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.Name

/**
 * Service-locator-dispatched compatibility surface for Kotlin compiler APIs that change shape
 * across versions.
 *
 * A [Factory] implementation lives in each per-version sibling module (e.g. `compiler-compat:k2310`)
 * and is registered via `META-INF/services`. At runtime, [Companion.create] reads the Kotlin
 * compiler version from `META-INF/compiler.version` on the classpath and picks the factory with the
 * highest `minVersion` that is still `<=` the current compiler version.
 *
 * The surface is intentionally narrow — methods enter this interface only when an actual
 * incompatibility between supported Kotlin versions can be demonstrated.
 */
public interface CompatContext {

    // ---- Extension registration ----

    /**
     * Registers a [FirExtensionRegistrar]. The registration API moved in 2.4.x; implementations
     * adapt to whichever surface the hosting compiler exposes.
     *
     * Called from `SnapshottablePluginComponentRegistrar.registerExtensions`.
     */
    public fun CompilerPluginRegistrar.ExtensionStorage.registerFirExtensionCompat(
        extension: FirExtensionRegistrar,
    )

    /**
     * Registers an [IrGenerationExtension]. Counterpart to [registerFirExtensionCompat].
     *
     * Called from `SnapshottablePluginComponentRegistrar.registerExtensions`.
     */
    public fun CompilerPluginRegistrar.ExtensionStorage.registerIrExtensionCompat(
        extension: IrGenerationExtension,
    )

    // ---- FIR declaration-generation DSL (churn hotspot) ----

    /**
     * Wraps `org.jetbrains.kotlin.fir.plugin.createMemberFunction`. The underlying
     * `FirSimpleFunctionBuilder` was renamed to `FirNamedFunctionBuilder` in 2.3.20; the inlined
     * DSL can produce runtime linkage errors when the plugin is compiled against a different
     * builder type than the host compiler provides.
     *
     * Returns the function symbol directly — the concrete FIR function class is named
     * differently in each Kotlin version, so the symbol (whose type is stable) is the friendlier
     * return value. Callers that need to mutate the underlying declaration can do so via
     * `symbol.fir`.
     *
     * Called from `fir/Factory.kt` (`createFunSnapshotUpdate`, `createFunConversion`).
     */
    public fun FirExtension.createMemberFunctionCompat(
        owner: FirClassSymbol<*>,
        key: GeneratedDeclarationKey,
        name: Name,
        returnType: ConeKotlinType,
        config: SimpleFunctionBuildingContext.() -> Unit = {},
    ): FirNamedFunctionSymbol

    /**
     * Wraps `org.jetbrains.kotlin.fir.plugin.createMemberProperty`.
     *
     * Called from `fir/Factory.kt` (`maybeCreatePropertyOnInterfaceOrMutableClass`).
     */
    public fun FirExtension.createMemberPropertyCompat(
        owner: FirClassSymbol<*>,
        key: GeneratedDeclarationKey,
        name: Name,
        returnType: ConeKotlinType,
        isVal: Boolean = true,
        hasBackingField: Boolean = true,
        config: PropertyBuildingContext.() -> Unit = {},
    ): FirProperty

    /**
     * Wraps `org.jetbrains.kotlin.fir.plugin.createNestedClass`.
     *
     * Called from `fir/Factory.kt` (`generateMutableClass`).
     */
    public fun FirExtension.createNestedClassCompat(
        owner: FirClassSymbol<*>,
        name: Name,
        key: GeneratedDeclarationKey,
        classKind: ClassKind = ClassKind.CLASS,
        config: ClassBuildingContext.() -> Unit = {},
    ): FirRegularClass

    // ---- Defensive wrap: FirDeclarationStatus.copy ----

    /**
     * Wraps `FirDeclarationStatus.copy(...)`. The native copy signature has churned (e.g. added
     * `hasMustUseReturnValue`/`returnValueStatus` parameters) across 2.3.x. Snapshottable only
     * needs to flip `isOverride`, so the compat surface exposes just the three fields we care
     * about.
     *
     * Called from `fir/SnapshottableStatusTransformer.transformStatus`.
     */
    public fun FirDeclarationStatus.copyCompat(
        isOverride: Boolean = this.isOverride,
        visibility: Visibility? = this.visibility,
        modality: Modality? = this.modality,
    ): FirDeclarationStatus

    // ---- Factory / ServiceLoader plumbing ----

    public companion object Companion {
        private fun loadFactories(): Sequence<Factory> {
            return ServiceLoader.load(Factory::class.java, Factory::class.java.classLoader).asSequence()
        }

        /**
         * Load [factories][Factory] and pick the highest compatible version (by [Factory.minVersion]).
         *
         * `dev` track versions are special-cased to avoid issues with divergent release tracks.
         *
         * When the current version is a dev build:
         * 1. First, look for dev track factories and compare only within the dev track
         * 2. If no dev factory matches, fall back to non-dev factories
         *
         * This ensures that a dev build like 2.3.20-dev-7791 doesn't incorrectly match a 2.3.20-Beta1
         * factory just because beta > dev in maturity ordering.
         */
        internal fun resolveFactory(
            knownVersion: KotlinToolingVersion? = null,
            factories: Sequence<Factory> = loadFactories(),
        ): Factory {
            // TODO short-circuit if we hit a factory with the exact version
            val factoryDataList =
                factories
                    .mapNotNull { factory ->
                        // Filter out any factories that can't compute the Kotlin version, as
                        // they're _definitely_ not compatible
                        try {
                            FactoryData(factory.currentVersion, factory)
                        } catch (_: Throwable) {
                            null
                        }
                    }
                    .toList()

            val currentVersion =
                knownVersion ?: factoryDataList.firstOrNull()?.version ?: error("No factories available")

            val targetFactory = resolveFactoryForVersion(currentVersion, factoryDataList)
            return targetFactory
                ?: error(
                    """
            Unrecognized Kotlin version!

            Available factories for: ${factories.joinToString(separator = "\n") { it.minVersion }}
            Detected version(s): ${factories.map { it.currentVersion }.distinct().joinToString(separator = "\n")}
          """
                        .trimIndent()
                )
        }

        private fun resolveFactoryForVersion(
            currentVersion: KotlinToolingVersion,
            factoryDataList: List<FactoryData>,
        ): Factory? {
            // If current version is DEV, try DEV track factories first
            if (currentVersion.isDev) {
                val devFactories = factoryDataList.filter {
                    KotlinToolingVersion(it.factory.minVersion).isDev
                }
                val devMatch = findHighestCompatibleFactory(currentVersion, devFactories)
                if (devMatch != null) {
                    return devMatch
                }

                // Fall back to non-DEV factories.
                // Use the base version (strip dev classifier) for comparison, because
                // 2.2.20-dev-5812 is a dev build OF 2.2.20 and should match the 2.2.20 factory,
                // but KotlinToolingVersion ordering puts DEV < STABLE so the comparison would
                // otherwise exclude it.
                val nonDevFactories = factoryDataList.filter {
                    !KotlinToolingVersion(it.factory.minVersion).isDev
                }
                val baseVersion =
                    KotlinToolingVersion(
                        currentVersion.major,
                        currentVersion.minor,
                        currentVersion.patch,
                        null,
                    )
                return findHighestCompatibleFactory(baseVersion, nonDevFactories)
            }

            // For non-DEV versions, only consider non-DEV factories
            val nonDevFactories = factoryDataList.filter {
                !KotlinToolingVersion(it.factory.minVersion).isDev
            }
            return findHighestCompatibleFactory(currentVersion, nonDevFactories)
        }

        private fun findHighestCompatibleFactory(
            currentVersion: KotlinToolingVersion,
            factoryDataList: List<FactoryData>,
        ): Factory? {
            return factoryDataList
                .filter { (_, factory) -> currentVersion >= KotlinToolingVersion(factory.minVersion) }
                .maxByOrNull { (_, factory) -> KotlinToolingVersion(factory.minVersion) }
                ?.factory
        }

        public fun create(knownVersion: KotlinToolingVersion? = null): CompatContext =
            resolveFactory(knownVersion).create()
    }

    public interface Factory {
        public val minVersion: String

        /** Attempts to get the current compiler version or throws and exception if it cannot. */
        public val currentVersion: String
            get() = loadCompilerVersionString()

        public fun create(): CompatContext

        public companion object Companion {
            private const val COMPILER_VERSION_FILE = "META-INF/compiler.version"

            public fun loadCompilerVersion(): KotlinToolingVersion {
                return KotlinToolingVersion(loadCompilerVersionString())
            }

            public fun loadCompilerVersionOrNull(): KotlinToolingVersion? {
                return loadCompilerVersionStringOrNull()?.let(::KotlinToolingVersion)
            }

            public fun loadCompilerVersionString(): String {
                return loadCompilerVersionStringOrNull()
                    ?: throw AssertionError(
                        "'$COMPILER_VERSION_FILE' not found in the classpath or was blank"
                    )
            }

            public fun loadCompilerVersionStringOrNull(): String? {
                val inputStream =
                    FirExtensionRegistrar::class.java.classLoader?.getResourceAsStream(COMPILER_VERSION_FILE)
                        ?: return null
                return inputStream.bufferedReader().use { it.readText() }.takeUnless { it.isBlank() }
            }
        }
    }

    private data class FactoryData(
        val version: KotlinToolingVersion,
        val factory: Factory,
    ) {
        companion object {
            operator fun invoke(version: String, factory: Factory): FactoryData =
                FactoryData(KotlinToolingVersion(version), factory)
        }
    }
}
