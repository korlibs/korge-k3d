import korlibs.event.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.io.async.launchImmediately
import korlibs.io.file.std.resourcesVfs
import korlibs.io.lang.*
import korlibs.korge.KeepOnReload
import korlibs.korge.input.*
import korlibs.korge.scene.Scene
import korlibs.korge.tween.tween
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.korge3d.*
import korlibs.korge3d.format.gltf2.*
import korlibs.korge3d.material.*
import korlibs.korge3d.shape.*
import korlibs.math.geom.*
import korlibs.time.*
import korlibs.korge.input.onSwipe

class CratesScene : Scene() {
    @KeepOnReload
    var trans = Transform3D()
    @KeepOnReload
    var tick = 0.0

    override suspend fun SContainer.sceneInit() {
        //sceneInit2()
        sceneInit3()
    }

    suspend fun SContainer.sceneInit3() {
        var rotationY = 0.degrees
        var rotationX = 0.degrees
        scene3D {
            camera = Camera3D.Perspective()
            axisLines(length = 4f)
            val quat = Quaternion.IDENTITY
            //val quat = Quaternion(x=0.0f, y=0.45737463f, z=0.0f, w=0.047847807f)
            //val quat = Quaternion(x=0.0, y=0.48204702, z=0.0, w=0.8758331)

            //val quat = Quaternion.fromAxisAngle(Vector3.RIGHT, 180.degrees)
            //val quat = Quaternion.fromVectors(Vector3.DOWN, Vector3.UP).scaled(0.5f) * Quaternion.fromVectors(Vector3.LEFT, Vector3.RIGHT).scaled(0.5f)

            //val view = gltf2View(resourcesVfs["gltf/Box.glb"].readGLTF2())
            //val view = gltf2View(resourcesVfs["gltf/MiniAvocado.glb"].readGLTF2()).scale(50f)
            val view = gltf2View(resourcesVfs["gltf/CesiumMilkTruck.glb"].readGLTF2()).scale(1f)
            //val view = gltf2View(resourcesVfs["gltf/MiniDamagedHelmet.glb"].readGLTF2()).scale(3f)
            //val view = gltf2View(resourcesVfs["gltf/SpecGlossVsMetalRough.glb"].readGLTF2()).scale(25f)
            //val view = gltf2View(resourcesVfs["gltf/ClearCoatTest.glb"].readGLTF2()).scale(1f)
            //val view = gltf2View(resourcesVfs["gltf/AttenuationTest.glb"].readGLTF2()).scale(.5f)
            //val view = gltf2View(resourcesVfs["gltf/AnimatedMorphCube.glb"].readGLTF2()).scale(.5f)
            //val view = gltf2View(resourcesVfs["gltf/cube/Cube.gltf"].readGLTF2())
            //val view = gltf2View(resourcesVfs["gltf/MiniBoomBox.glb"].readGLTF2()).scale(300f)
            //val view = gltf2View(resourcesVfs["gltf/RiggedFigure.glb"].readGLTF2()).scale(2f)
            //val view = gltf2View(resourcesVfs["gltf/SimpleSkin/SimpleSkin.gltf"].readGLTF2()).scale(1f)
            //val view = gltf2View(resourcesVfs["gltf/CesiumMan.glb"].readGLTF2()).scale(2f)

            camera = view.gltf.cameras.firstOrNull()?.perspective?.toCamera() ?: Camera3D.Perspective()
            onMagnify {
                //camera.position.setTo(0f, 1f, camera.position.z + it.amount)
                camera.position += Vector4.ZERO.copy(z = -it.amount * 2)
            }
            onScroll {
                //println("onscroll: ${it.scrollDeltaXPixels}, ${it.scrollDeltaYPixels}")
                //zoom -= (it.scrollDeltaYPixels / 240)
                //updateZoom()
                camera.position += Vector4(
                    it.scrollDeltaXPixels * 0.25f,
                    -it.scrollDeltaYPixels * 0.25f,
                    0f, 0f,
                )
            }

            view.rotation(quat)
            fun rotate(deltaY: Angle, deltaX: Angle) {
                view.rotation(quat * Quaternion.fromAxisAngle(Vector3.UP, rotationY) * Quaternion.fromAxisAngle(Vector3.RIGHT, rotationX))
                rotationY += deltaY
                rotationX += deltaX
            }

            val slider = uiSlider(1f, min = -1f, max = 2f, step = .0125f)
                .also { slider -> slider.onChange { stage3D!!.occlusionStrength = slider.value.toFloat() } }
                .xy(30, 30)
                .scale(1)

            keys {
                downFrame(Key.LEFT, 4.milliseconds) { rotate(+1.degrees, 0.degrees) }
                downFrame(Key.RIGHT, 4.milliseconds) { rotate(-1.degrees, 0.degrees) }
                downFrame(Key.UP, 4.milliseconds) { slider.value += .01 }
                downFrame(Key.DOWN, 4.milliseconds) { slider.value -= .01 }
            }

            solidRect(2000, 1000, Colors.TRANSPARENT).xy(0, 100).onMouseDrag {
                rotate(-it.deltaDx.degrees, -it.deltaDy.degrees)
            }

            addUpdater {
            }
            //gltf2View(resourcesVfs["gltf/AttenuationTest.glb"].readGLTF2()).scale(50f)
        }
    }

    suspend fun SContainer.sceneInit2() {

        val korgeTex = KR.korge.read()

        //val crateTex = NativeImage(64, 64).context2d {
        //    fill(Colors.ROSYBROWN) {
        //        rect(0, 0, 64, 64)
        //    }
        //    stroke(Colors.SADDLEBROWN, 8f) {
        //        rect(0, 0, 63, 63)
        //        line(Point(0, 0), Point(63, 63))
        //    }
        //}

        val crateTex = KR.crate.read().mipmaps(true)
        //val crateTex = KR.dice.__file.readBitmap(QOI).mipmaps(true)
        val crateMaterial = PBRMaterial3D(diffuse = PBRMaterial3D.LightTexture(crateTex))

        val transparentMaterial = PBRMaterial3D(diffuse = PBRMaterial3D.LightColor(Colors.RED.withAd(0.25)))

        //solidRect(512, 512, MaterialColors.AMBER_200).alpha(0.5)
        image(korgeTex).alpha(0.5)

        scene3D {
            //camera.set(fov = 60.degrees, near = 0.3, far = 1000.0)

            //light().position(0, 0, -3)

            //polyline3d { }
            axisLines()
            val cube1 = cube().material(crateMaterial)
            sphere(1f).position(1, 0, 0).material(crateMaterial)
            torus(1f).position(-1, 0, 0).material(crateMaterial)
            cone(1f).position(0, -1, 0).material(crateMaterial)
            cylinder(1f).position(0, -2, 0).material(crateMaterial)
            //cube(2.0, 2.0)
            gltf2View(resourcesVfs["gltf/MiniAvocado.glb"].readGLTF2()).position(Vector3(3, 0, 0)).scale(10f).rotation(y = 180.degrees)

            val cube2 = cube().position(0, 2, 0).scale(1, 2, 1).rotation(0.degrees, 0.degrees, 45.degrees).material(crateMaterial)
            val cube3 = cube().position(-5, 0, 0).material(crateMaterial)
            val cube4 = cube().position(+5, 0, 0).material(crateMaterial)
            val cube5 = cube().position(0, -5, 0).material(crateMaterial)
            val cube6 = cube().position(0, +5, 0).material(crateMaterial)
            val cube7 = cube().position(0, 0, -5).material(crateMaterial)
            val cube8 = cube().position(0, 0, +5).material(crateMaterial)
            plane(Vector3.UP, 10f, 3f).material(transparentMaterial).also {
                it.blendMode = BlendMode.NORMAL
            }

            addUpdater {
                val angle = (tick / 4.0).degrees
                trans.setTranslationAndLookAt(
                    cos(angle * 2) * 4, cos(angle * 3) * 4, -sin(angle) * 4, // Orbiting camera
                    0f, 1f, 0f
                )
                camera.transform.copyFrom(trans)
                camera.invalidateRender()
                tick += it.milliseconds / 16.0
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

        //solidRect(512, 512, Colors.BLUEVIOLET).position(views.virtualWidth, 0).anchor(1, 0).alpha(0.5)
        image(korgeTex).position(views.virtualWidth, 0).anchor(1, 0).alpha(0.5)
    }
}
