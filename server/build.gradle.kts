plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    application
}

application {
    mainClass.set("com.neomud.server.ApplicationKt")
}

tasks.named<JavaExec>("run") {
    workingDir = project.projectDir
    dependsOn("packageWorld")
    environment("NEOMUD_ADMINS", System.getenv("NEOMUD_ADMINS") ?: "bob")
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.sqlite.jdbc)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.logback.classic)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.named<Test>("test") {
    dependsOn("packageWorld")
    workingDir = project.projectDir
}

kotlin {
    jvmToolchain(21)
}

tasks.register<Copy>("packageWorld") {
    description = "Copies the maker-built default world bundle into build/worlds/"
    group = "build"
    from("../maker/default_world.nmd")
    into(layout.buildDirectory.dir("worlds"))
    rename { "default-world.nmd" }
}
