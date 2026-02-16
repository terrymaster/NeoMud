package com.neomud.server.world

import java.io.InputStream
import java.util.zip.ZipFile

class NmdBundleDataSource(private val zipFile: ZipFile) : WorldDataSource {

    override fun readText(path: String): String? {
        val entry = zipFile.getEntry(path) ?: return null
        return zipFile.getInputStream(entry).bufferedReader().use { it.readText() }
    }

    override fun readBytes(path: String): ByteArray? {
        val entry = zipFile.getEntry(path) ?: return null
        return zipFile.getInputStream(entry).use { it.readBytes() }
    }

    override fun list(prefix: String, suffix: String): List<String> {
        return zipFile.entries().asSequence()
            .map { it.name }
            .filter { it.startsWith(prefix) && it.endsWith(suffix) }
            .toList()
    }

    override fun openStream(path: String): InputStream? {
        val entry = zipFile.getEntry(path) ?: return null
        return zipFile.getInputStream(entry)
    }
}
