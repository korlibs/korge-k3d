import korlibs.image.bitmap.mipmaps
import korlibs.image.format.readNativeImage
import korlibs.io.async.launchImmediately
import korlibs.io.file.std.resourcesVfs
import korlibs.korge.KeepOnReload
import korlibs.korge.scene.Scene
import korlibs.korge.tween.tween
import korlibs.korge.view.*
import korlibs.korge3d.*
import korlibs.math.geom.cos
import korlibs.math.geom.degrees
import korlibs.math.geom.sin
import korlibs.time.seconds

@Korge3DExperimental
class CratesScene : Scene() {
    @KeepOnReload
    var trans = Transform3D()
    @KeepOnReload
    var tick = 0

    override suspend fun SContainer.sceneInit() {

        val korgeTex = resourcesVfs["korge.png"].readNativeImage().mipmaps(false)
        val crateTex = KR.crate.read().mipmaps(true)
        //val crateTex = KR.dice.__file.readBitmap(QOI).mipmaps(true)
        val crateMaterial = Material3D(diffuse = Material3D.LightTexture(crateTex))

        image(korgeTex).alpha(0.5)

        scene3D {
            //camera.set(fov = 60.degrees, near = 0.3, far = 1000.0)

            //light().position(0, 0, -3)

            val cube1 = cube().material(crateMaterial)
            sphere(2).position(1, 0, 0).material(crateMaterial)
            //cube(2.0, 2.0)
            val cube2 = cube().position(0, 2, 0).scale(1, 2, 1).rotation(0.degrees, 0.degrees, 45.degrees).material(crateMaterial)
            val cube3 = cube().position(-5, 0, 0).material(crateMaterial)
            val cube4 = cube().position(+5, 0, 0).material(crateMaterial)
            val cube5 = cube().position(0, -5, 0).material(crateMaterial)
            val cube6 = cube().position(0, +5, 0).material(crateMaterial)
            val cube7 = cube().position(0, 0, -5).material(crateMaterial)
            val cube8 = cube().position(0, 0, +5).material(crateMaterial)

            addUpdater {
                val angle = (tick / 4.0).degrees
                trans.setTranslationAndLookAt(
                    cos(angle * 2) * 4, cos(angle * 3) * 4, -sin(angle) * 4, // Orbiting camera
                    0f, 1f, 0f
                )
                camera.transform.copyFrom(trans)
                camera.invalidateRender()
                tick++
            }

            launchImmediately {
                while (true) {
                    tween(time = 16.seconds) {
                        cube1.modelMat.identity().rotate((it * 360).degrees, 0.degrees, 0.degrees)
                        cube2.modelMat.identity().rotate(0.degrees, (it * 360).degrees, 0.degrees)
                    }
                }
            }
        }

        image(korgeTex).position(views.virtualWidth, 0).anchor(1, 0).alpha(0.5)
    }
}
