package korlibs.math.geom

import kotlin.test.*

class Capsule3DTest {
    val offset = Vector3(3f, 0f, 0f)
    val c1 = Capsule3D(Vector3.ZERO, Vector3.UP, 1f)
    val c2 = Capsule3D(Vector3.ZERO + offset, Vector3.UP + offset, 1f)
    val se2 = Segment3D(Vector3.ZERO + offset, Vector3.UP + offset)
    val s2 = Sphere3D(Vector3.ZERO + offset, 1f)

    @Test
    fun test() {
        assertEquals(1f, c1.distanceTo(c2), 0.0001f)
        assertEquals(1f, c1.distanceTo(s2), 0.0001f)
        assertEquals(2f, c1.distanceTo(se2), 0.0001f)
    }

    @Test
    fun test2() {
        val offset = Vector3(.5f, 0f, 0f)
        val c1 = Capsule3D(Vector3.ZERO, Vector3.UP, 1f)
        val c2 = Capsule3D(Vector3.ZERO + offset, Vector3.UP + offset + Vector3.DOWN * 3f, 1f)

        println(Penetration.contact(c1, c2))
        //val contact = ContactPoints.contact(c1, c2)
        println(ContactPoints.contact(c1, c2))
        println(ContactPoints.contact(c2, c1))
        //println(ContactPoints.closestPoint(c1.segment, contact))
    }
}
