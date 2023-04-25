package korlibs.gltf

import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.korge.gltf.*
import kotlin.test.*

class GLTF2Test {
    @Test
    fun test() = suspendTest {
        val gltf2 = GLTF2.readGLB(resourcesVfs["Box.glb"])
        GLTF2MeshPrimitive(gltf2, gltf2.meshes.first().primitives.first())
    }
}
