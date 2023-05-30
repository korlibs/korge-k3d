package korlibs.korge3d.gltf

import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.korge3d.format.gltf2.*
import kotlin.test.*

class GLTF2Test {
    @Test
    fun test() = suspendTest {
        val gltf2 = resourcesVfs["Box.glb"].readGLTF2()
        GLTF2ViewPrimitive(gltf2, gltf2.meshes.first().primitives.first())
    }
}
