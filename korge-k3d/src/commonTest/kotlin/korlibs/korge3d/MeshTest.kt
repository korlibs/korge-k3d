package korlibs.korge3d

import korlibs.korge.view.Mesh
import korlibs.math.geom.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(Korge3DExperimental::class)
class MeshTest {
    @Test
    fun testCube() {
        val cube = Cube3D(1.0, 1.0, 1.0)
        val vertexCount = cube.mesh.vertexCount
        assertTrue(vertexCount > 0, "cube.mesh.vertexCount(=$vertexCount) is not positive")
    }

    @Test
    fun testEmpty() {
        assertEquals(Unit, Mesh().getLocalBoundsInternal(MRectangle()), "Doesn't throw with mutability exception")
    }
}
