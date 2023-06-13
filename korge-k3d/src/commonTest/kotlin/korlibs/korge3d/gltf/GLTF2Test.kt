package korlibs.korge3d.gltf

import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.korge3d.format.gltf2.*
import korlibs.memory.*
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
        val view = GLTF2ViewPrimitive(gltf, gltf.nodes.first().mesh(gltf).primitives.first())
        view.weights = sampler.doLookup(gltf, time, view.nweights).toVector4()
        println(view.weights)


        /*
        //println(sampler.times(gltf).size)
        println(sampler.lookup(gltf, -1f))
        println(sampler.lookup(gltf, 0f))
        println(sampler.lookup(gltf, 0.04f))
        println(sampler.lookup(gltf, 0.07f))
        println(sampler.lookup(gltf, 0.2f))
        println(sampler.lookup(gltf, 4f))
        println(sampler.lookup(gltf, 100f))


        val value = GLTF2Vector(2, 1)
        val times = sampler.times(gltf)
        println("times=" + times.toFloatArray().toList())
        val weights = sampler.outputBuffer(gltf, 2)

        val lookup = sampler.lookup(gltf, 0.04f)
        value.setInterpolated(0, weights, lookup.lowIndex, weights, lookup.highIndex, lookup.ratio)
        for (n in 0 until 36) {
            val time = 0.125f * n
            println("time=$time, value=" + sampler.doLookup(gltf, time, dims = 2))
        }

        //println(buffer.toFloatArray().toList())
        println(gltf.animations.joinToString("\n"))

         */
    }
}
