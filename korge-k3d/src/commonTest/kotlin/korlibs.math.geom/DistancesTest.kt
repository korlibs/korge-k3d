package korlibs.math.geom

import kotlin.test.*

class DistancesTest {
    @Test
    fun test() {
        val s = Sphere3D(Vector3.UP * 3, 1f)

        val p = Plane3D(Vector3.ZERO, Vector3.LEFT)
        println(p.distanceTo(s))
    }
}
