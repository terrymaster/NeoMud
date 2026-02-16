plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ktor) apply false
}

abstract class UpdateVersionTask : DefaultTask() {
    @get:Input
    abstract val gitDescribe: Property<String>

    @get:OutputFile
    abstract val versionFile: RegularFileProperty

    @TaskAction
    fun update() {
        val desc = gitDescribe.get()
        val match = Regex("""^v(\d+)\.(\d+)\.(\d+)-(\d+)-g[0-9a-f]+$""").find(desc)
            ?: error("Could not parse git describe output: $desc. Ensure a tag like v0.0.0 exists.")

        val (major, minor, patch, commits) = match.destructured
        val versionCode = "$major.$minor.$patch.$commits"

        val file = versionFile.get().asFile
        val content = file.readText()
        val updated = content.replace(
            Regex("""internal const val VERSION_CODE = ".*""""),
            """internal const val VERSION_CODE = "$versionCode""""
        )
        file.writeText(updated)

        println("Version updated to: alpha $versionCode (from $desc)")
    }
}

tasks.register<Exec>("runMaker") {
    group = "application"
    description = "Start the NeoMUD Maker web server (Vite + Express)"
    workingDir = file("maker")
    commandLine("npm", "run", "dev")
}

tasks.register<UpdateVersionTask>("updateVersion") {
    group = "versioning"
    description = "Update NeoMudVersion.kt from git tags (tag format: v0.0.0)"

    gitDescribe.set(providers.exec {
        commandLine("git", "describe", "--tags", "--long")
    }.standardOutput.asText.map { it.trim() })

    versionFile.set(layout.projectDirectory.file(
        "shared/src/commonMain/kotlin/com/neomud/shared/NeoMudVersion.kt"
    ))
}
