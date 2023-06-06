package korlibs.math.geom

import kotlin.test.*

class CollisionDetectionTest {
    @Test
    fun test() {
        val s1 = Sphere3D(Vector3.ZERO, 1f)
        val s2 = Sphere3D(Vector3.ZERO + Vector3.RIGHT * .1f, .5f)
        //val s2 = Sphere3D(Vector3.ZERO, .5f)
        //val s2 = Sphere3D(Vector3.ZERO, .5f)
        println(CollisionDetection.collision(s1, s2))
    }

    @Test
    fun test2() {
        println(CollisionDetection.collision(
            Capsule3D(Vector3.ZERO, Vector3.UP, .1f),
            Triangle3D(Vector3.ZERO + Vector3.RIGHT * .1, Vector3.UP * 2, Vector3.LEFT)
        ))
    }
}
