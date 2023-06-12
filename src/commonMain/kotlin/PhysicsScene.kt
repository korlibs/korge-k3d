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

class PhysicsScene : Scene() {
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
                    sceneContainer.changeTo({ PhysicsScene() })
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
