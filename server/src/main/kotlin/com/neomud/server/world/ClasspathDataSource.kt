package com.neomud.server.world

import java.io.InputStream

class ClasspathDataSource(
    private val knownZoneFiles: List<String> = listOf("world/town.zone.json", "world/forest.zone.json")
) : WorldDataSource {

    private val classLoader: ClassLoader = ClasspathDataSource::class.java.classLoader

    override fun readText(path: String): String? {
        return classLoader.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() }
    }

    override fun readBytes(path: String): ByteArray? {
        return classLoader.getResourceAsStream(path)?.use { it.readBytes() }
    }

    override fun list(prefix: String, suffix: String): List<String> {
        // Classpath doesn't support directory listing easily,
        // so for zone files we use the known list filtered by prefix/suffix
        return knownZoneFiles.filter { it.startsWith(prefix) && it.endsWith(suffix) }
    }

    override fun openStream(path: String): InputStream? {
        return classLoader.getResourceAsStream(path)
    }
}
