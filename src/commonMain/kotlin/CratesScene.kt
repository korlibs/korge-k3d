import korlibs.datastructure.*
import korlibs.event.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.io.async.launchImmediately
import korlibs.io.file.std.resourcesVfs
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
import korlibs.korge3d.util.*
import korlibs.math.geom.*
import korlibs.time.*

data class RigidBody3D(
    val mass: Float,
    val useGravity: Boolean
) {
    var acceleration = Vector3.ZERO
    var velocity = Vector3.ZERO
}

var View3D.collider: Collider? by Extra.Property { null }
fun <T : View3D> T.collider(collider: Collider): T {
    this.collider = collider
    return this
}

var View3D.rigidBody: RigidBody3D? by Extra.Property { null }
fun <T : View3D> T.rigidBody(rigidBody: RigidBody3D): T {
    this.rigidBody = rigidBody
    return this
}

class CratesScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        var rotation = 0.degrees
        val rubber = PhysicsMaterial(bounciness = .75f)
        val gravity = Vector3.DOWN * 9.8f
        val initialQuadScale = .05f
        val bquat = Quaternion.fromVectors(Vector3.UP, Vector3.RIGHT)
        val quat = bquat.scaled(initialQuadScale)
        //val quat = Quaternion.IDENTITY

        lateinit var cube: Cube3D
        val scene3D = scene3D {
            axisLines(length = 1f)
            //cube(1f, .2f, 4f).position(Vector3.UP)
            sphere(1f)
                .position(Vector3.UP * 4f)
                .collider(SphereCollider(Vector3.ZERO, 1f, rubber))
                .material(PBRMaterial3D(diffuse = PBRMaterial3D.LightColor(Colors.RED)))
                .rigidBody(RigidBody3D(1f, true))
            cube = cube(4f, .01f, 4f)
                .position(Vector3.DOWN * 1f)
                .rotation(quat)
                //.scale(1f)
                //.rotation(x = 45.degrees)
                .collider(PlaneCollider(Vector3.ZERO, Vector3.UP, rubber))
                .material(PBRMaterial3D(diffuse = PBRMaterial3D.LightColor(Colors.GREEN)))
                .rigidBody(RigidBody3D(1f, false))
        }


        uiVerticalStack {
            uiButton("RESTART") {
                onClick {
                    sceneContainer.changeTo({ CratesScene() })
                }
            }
            uiSlider(initialQuadScale, -1f, 1f, .0001f, decimalPlaces = 4) {
                onChange {
                    cube.rotation(bquat.scaled(this.value.toFloat()))
                }
            }
        }

        // @TODO: Use BVH3D
        var firstCollision = 0
        addUpdater { dt ->
            scene3D.stage3D.foreachDescendant { view ->
                val rigidBody = view.rigidBody
                val collider = view.collider
                if (rigidBody != null && collider != null && rigidBody.useGravity) {
                    rigidBody.acceleration = gravity
                    rigidBody.velocity += rigidBody.acceleration * dt.seconds
                    var collides = false
                    scene3D.stage3D.foreachDescendant { other ->
                        if (other !== view) {
                            val otherCollider = other.collider
                            if (otherCollider != null) {
                                if (firstCollision == 0) {
                                    println(rigidBody.velocity)
                                }
                                val collision = Colliders.collide(collider, view.transform, otherCollider, other.transform)
                                if (collision != null) {
                                    view.position += rigidBody.velocity.normalized() * collision.separation
                                    val ratio = ((collider.material.bounciness + otherCollider.material.bounciness) * 0.5)
                                    //val velocityLength = rigidBody.velocity.length

                                    if (firstCollision == 0) {
                                        firstCollision = 1
                                        println("ratio=$ratio, rigidBody.velocity=${rigidBody.velocity}, reflected=${rigidBody.velocity.reflected(collision.normal)}, collision=$collision")
                                    }

                                    rigidBody.velocity = rigidBody.velocity.reflected(collision.normal) * ratio
                                    //rigidBody.velocity = -rigidBody.velocity * ratio
                                    //println("rigidBody.velocity=${rigidBody.velocity}")
                                    collides = true
                                    //println("rigidBody.velocity=${rigidBody.velocity}")
                                }
                                //println("$collider -- $otherCollider")
                            }
                        }
                    }
                    //if (!collides) {
                        view.position = (view.position + rigidBody.velocity * dt.seconds)
                    //}
                }
                //if (it.rigidBody != null) println(it.rigidBody)
                //if (it.collider != null) println(it.collider)
            }
        }
    }
}


class CratesScene2 : Scene() {
    @KeepOnReload
    var trans = Transform3D()
    @KeepOnReload
    var tick = 0.0

    override suspend fun SContainer.sceneInit() {
        //sceneInit2()
        sceneInit3()
    }

    suspend fun SContainer.sceneInit3() {
        var rotation = 0.degrees
        scene3D {
            camera = Camera3D.Perspective()
            axisLines(length = 4f)
            //val view = gltf2View(resourcesVfs["gltf/Box.glb"].readGLTF2())
            //val view = gltf2View(resourcesVfs["gltf/MiniAvocado.glb"].readGLTF2()).scale(50f)
            //val view = gltf2View(resourcesVfs["gltf/CesiumMilkTruck.glb"].readGLTF2()).scale(1f)
            val view = gltf2View(resourcesVfs["gltf/MiniDamagedHelmet.glb"].readGLTF2()).scale(3f)
                .rotation(Quaternion.fromAxisAngle(Vector3.RIGHT, 180.degrees))
                //.rotation(x = -90.degrees, y = -90.degrees, z = 0.degrees)
            //val view = gltf2View(resourcesVfs["gltf/SpecGlossVsMetalRough.glb"].readGLTF2()).scale(25f)
            //val view = gltf2View(resourcesVfs["gltf/ClearCoatTest.glb"].readGLTF2()).scale(1f)

            //val view = gltf2View(resourcesVfs["gltf/Box.glb"].readGLTF2())
            //val view = gltf2View(resourcesVfs["gltf/AnimatedMorphCube.glb"].readGLTF2()).scale(.5f)
            //val view = gltf2View(resourcesVfs["gltf/AttenuationTest.glb"].readGLTF2()).scale(.5f)
            //val view = gltf2View(resourcesVfs["gltf/cube/Cube.gltf"].readGLTF2())
            //val view = gltf2View(resourcesVfs["gltf/MiniBoomBox.glb"].readGLTF2()).scale(1f)
            //val view = gltf2View(resourcesVfs["gltf/CesiumMan.glb"].readGLTF2()).scale(1f)

            fun rotateY(delta: Angle) {
                view.rotation(Quaternion.fromAxisAngle(Vector3.RIGHT, 180.degrees) * Quaternion.fromAxisAngle(Vector3.UP, rotation))
                rotation += delta
            }

            val slider = uiSlider(1f, min = -1f, max = 2f, step = .0125f)
                .also { slider -> slider.onChange { stage3D!!.occlusionStrength = slider.value.toFloat() } }
                .xy(30, 30)
                .scale(1)

            keys {
                downFrame(Key.LEFT, 4.milliseconds) { rotateY(+1.degrees) }
                downFrame(Key.RIGHT, 4.milliseconds) { rotateY(-1.degrees) }
                downFrame(Key.UP, 4.milliseconds) { slider.value += .01 }
                downFrame(Key.DOWN, 4.milliseconds) { slider.value -= .01 }
            }

            solidRect(2000, 1000, Colors.TRANSPARENT).xy(0, 100).onMouseDrag {
                rotateY(-it.deltaDx.degrees)
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
