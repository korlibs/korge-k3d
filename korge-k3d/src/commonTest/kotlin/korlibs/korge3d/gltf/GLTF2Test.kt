package korlibs.korge3d.gltf

import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.korge3d.format.gltf2.*
import korlibs.math.geom.*
import kotlin.test.*

class GLTF2Test {
    @Test
    fun test() = suspendTest {
        val gltf = resourcesVfs["Box.glb"].readGLTF2()
        val mesh = gltf.meshes.first()
        GLTF2View(gltf, autoAnimate = false)
        //GLTF2ViewPrimitive(gltf, mesh.primitives.first(), mesh, GLTF2ViewMesh(gltf, mesh, gltf.nodes.first()))
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
        //for (n in 0 until 10) {
        run {
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
                "RiggedFigure.glb",
            )) {
                println("Parsing...$fileName")
                resourcesVfs["gltf/$fileName"].readGLTF2()
            }
        }
    }

    @Test
    fun testSimpleMorphing() = suspendTest {
        val gltf = resourcesVfs["gltf/AnimatedMorphCube.glb"].readGLTF2()
        val sampler = gltf.animations[0].samplers[0]

        val time = 0.4f
        val mesh = gltf.nodes.first().mesh(gltf)
        val view = GLTF2View(gltf, autoAnimate = false)
        val primitive = view.sceneViews.first().rootNodes.first().meshView!!.primitiveViews.first()
        //val view = GLTF2ViewPrimitive(gltf, mesh.primitives.first(), mesh)
        primitive.weights = sampler.doLookup(gltf, time, GLTF2.Animation.Sampler.LookupKind.NORMAL).toVector4()
        assertEquals(Vector4(0.042968746f, 0f, 0f, 0f), primitive.weights)
    }

    @Test
    fun testSimpleSkinning() = suspendTest {
        val gltf = resourcesVfs["gltf/RiggedFigure.glb"].readGLTF2()
        val view = GLTF2View(gltf)
        val matrices = gltf.skins[0].inverseBindMatricesAccessor(gltf)
        val accessor = GLTF2AccessorVectorMAT4(GLTF2AccessorVector(matrices, matrices.bufferSlice(gltf)))
        println(accessor)
    }
}
