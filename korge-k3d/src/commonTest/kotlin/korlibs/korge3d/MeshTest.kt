package korlibs.korge3d

import korlibs.korge3d.shape.*
import kotlin.test.*

class MeshTest {
    @Test
    fun testCube() {
        val cube = Cube3D(1f, 1f, 1f)
        val vertexCount = cube.mesh.vertexCount
        assertTrue(vertexCount > 0, "cube.mesh.vertexCount(=$vertexCount) is not positive")
    }

    @Test
    fun testEmpty() {
        //assertEquals(Rectangle(), Mesh().getLocalBoundsInternal(), "Doesn't throw with mutability exception")
    }
}
