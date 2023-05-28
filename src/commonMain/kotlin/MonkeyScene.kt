import korlibs.datastructure.values
import korlibs.image.color.Colors
import korlibs.io.async.launchImmediately
import korlibs.io.file.std.resourcesVfs
import korlibs.korge.scene.Scene
import korlibs.korge.tween.*
import korlibs.korge.view.SContainer
import korlibs.korge.view.addUpdater
import korlibs.korge3d.*
import korlibs.korge3d.format.readColladaLibrary
import korlibs.math.geom.cos
import korlibs.math.geom.degrees
import korlibs.math.geom.sin
import korlibs.math.interpolation.Easing
import korlibs.time.seconds


class MonkeyScene : Scene() {
    override suspend fun SContainer.sceneInit() {
        //delay(10.seconds)
        //println("delay")
        scene3D {
            val light1 = light().position(0, 10, +10).setTo(Colors.RED)
            val light2 = light().position(10, 0, +10).setTo(Colors.BLUE)

            launchImmediately {
                while (true) {
                    tween(light1::y[-20], light2::x[-20], time = 1.seconds, easing = Easing.SMOOTH)
                    tween(light1::y[+20], light2::x[+20], time = 1.seconds, easing = Easing.SMOOTH)
                }
            }

            val library = resourcesVfs["monkey-smooth.dae"].readColladaLibrary()
            val model = library.geometryDefs.values.first()
            val view = mesh(model.mesh).rotation(-90.degrees, 0.degrees, 0.degrees)

            var tick = 0
            addUpdater {
                val angle = (tick / 1.0).degrees
                camera.positionLookingAt(
                    cos(angle * 1) * 4, 0f, -sin(angle * 1) * 4, // Orbiting camera
                    0f, 0f, 0f
                )
                tick++
            }
        }
    }

}
