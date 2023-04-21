package korlibs.korge3d

import korlibs.logger.*
import korlibs.korge3d.format.readColladaLibrary
import korlibs.io.async.suspendTest
import korlibs.io.file.std.resourcesVfs
import doIOTest
import kotlin.test.Test

class Library3DTest {
    val logger = Logger("Library3DTest")

    @Test
    fun test() = suspendTest({ doIOTest }) {
        val library = resourcesVfs["scene.dae"].readColladaLibrary()
        logger.info { library }
    }
}
