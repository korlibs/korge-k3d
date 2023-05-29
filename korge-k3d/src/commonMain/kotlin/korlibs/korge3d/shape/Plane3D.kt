package korlibs.korge3d.shape

import korlibs.korge3d.*
import korlibs.korge3d.internal.*
import korlibs.korge3d.util.*
import korlibs.math.geom.*

inline fun Container3D.plane(normal: Vector3, callback: Plane3D.() -> Unit = {}): Plane3D =
    Plane3D(normal).addTo(this, callback)

class Plane3D(var normal: Vector3) : BaseViewWithMesh3D(mesh) {
    override fun prepareExtraModelMatrix(): Matrix4 {
        return Quaternion.fromVectors(Vector3.UP, normal).toMatrix()
    }

    companion object {
        val mesh = MeshBuilder3D {
            vector3DTemps {
                val size = 10000f
                addVertex(Vector3(-size, 0f, -size), Vector3.UP, Vector2(0f, 0f))
                addVertex(Vector3(+size, 0f, -size), Vector3.UP, Vector2(1f, 0f))
                addVertex(Vector3(+size, 0f, +size), Vector3.UP, Vector2(1f, 1f))
                addVertex(Vector3(-size, 0f, +size), Vector3.UP, Vector2(0f, 1f))
                addIndices(0, 1, 2)
                addIndices(0, 2, 3)
            }
        }
    }
}
