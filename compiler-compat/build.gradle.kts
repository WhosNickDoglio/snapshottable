plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.buildconfig)
    id("kotlin-jvm-convention")
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
    }
}

dependencies {
    compileOnly(libs.kotlin.compiler)
    compileOnly(libs.kotlin.stdlib)
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}

buildConfig {
    packageName("com.tunjid.snapshottable.compat")
    kotlin {
        useKotlinOutput {
            internalVisibility = true
            topLevelConstants = true
        }
    }
    buildConfigField(
        "kotlin.collections.Map<String, String>",
        "BUILT_IN_COMPILER_VERSION_ALIASES",
        providers
            .fileContents(layout.projectDirectory.file("ide-mappings.txt"))
            .asText
            .map { text ->
                text.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .joinToString(
                        prefix = "mapOf(\n",
                        postfix = "\n)",
                        separator = ",\n",
                    ) { line ->
                        val (from, to) = line.split('=', limit = 2)
                        "  \"$from\" to \"$to\""
                    }
            },
    )
}
