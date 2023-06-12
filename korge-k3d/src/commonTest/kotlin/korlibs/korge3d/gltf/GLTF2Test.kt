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

    @Test
    fun testSimpleSkin() = suspendTest {
        val gltf2 = resourcesVfs["SimpleSkin/SimpleSkin.gltf"].readGLTF2()
    }

    @Test
    fun testSimpleSkinSeparated() = suspendTest {
        val gltf2 = resourcesVfs["SimpleSkin/separated/SimpleSkin.gltf"].readGLTF2()
    }

    @Test
    fun testParsingAll() = suspendTest {
        for (fileName in listOf(
            "AnimatedMorphCube.glb",
            "AttenuationTest.glb",
            "Box.glb",
            "CesiumMan.glb",
            "CesiumMilkTruck.glb",
            "ClearCoatTest.glb",
            "MiniAvocado.glb",
            "MiniBoomBox.glb",
            "MiniDamagedHelmet.glb",
            "SpecGlossVsMetalRough.glb",
        )) {
            println("Parsing...$fileName")
            resourcesVfs["gltf/$fileName"].readGLTF2()
        }
    }
}
