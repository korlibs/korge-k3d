package korlibs.math.geom

import kotlin.test.*

class Capsule3DTest {
    @Test
    fun test() {
        val c1 = Capsule3D(Vector3.ZERO, Vector3.UP, 1f)
        val c2 = Capsule3D(Vector3.ZERO + Vector3(3f, 0f, 0f), Vector3.UP + Vector3(3f, 0f, 0f), 1f)
        val se2 = Segment3D(Vector3.ZERO + Vector3(3f, 0f, 0f), Vector3.UP + Vector3(3f, 0f, 0f))
        val s2 = Sphere3D(Vector3.ZERO + Vector3(3f, 0f, 0f), 1f)
        assertEquals(1f, c1.distanceTo(c2), 0.0001f)
        assertEquals(1f, c1.distanceTo(s2), 0.0001f)
        assertEquals(2f, c1.distanceTo(se2), 0.0001f)
    }
}
