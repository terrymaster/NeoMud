package com.neomud.server

import com.neomud.server.world.NmdBundleDataSource
import com.neomud.server.world.WorldDataSource
import java.io.File
import java.util.zip.ZipFile

/** Returns a [WorldDataSource] backed by the default-world.nmd built by packageWorld. */
fun defaultWorldSource(): WorldDataSource {
    val file = File("build/worlds/default-world.nmd")
    check(file.exists()) { "default-world.nmd not found. Run './gradlew packageWorld' first." }
    return NmdBundleDataSource(ZipFile(file))
}
