package korlibs.korge3d.shape

import korlibs.korge3d.*
import korlibs.korge3d.internal.*
import korlibs.korge3d.util.*
import korlibs.math.geom.*

inline fun Container3D.cube(width: Number, height: Number, depth: Number, callback: Cube3D.() -> Unit = {}): Cube3D =
    cube(width.toFloat(), height.toFloat(), depth.toFloat(), callback)

inline fun Container3D.cube(
    width: Float = 1f,
    height: Float = width,
    depth: Float = height,
    callback: Cube3D.() -> Unit = {}
): Cube3D = Cube3D(width, height, depth).addTo(this, callback)

class Cube3D(var width: Float, var height: Float, var depth: Float) : ShapeViewWithMesh3D(mesh) {
    override fun prepareExtraModelMatrix(): Matrix4 = Matrix4.scale(width, height, depth)

    companion object {
        val mesh = MeshBuilder3D {
            vector3DTemps {
                fun face(pos: Vector3) {
                    val dims = (0 until 3).filter { pos[it] == 0f }
                    val normal = Vector3.func { if (pos[it] != 0f) 1f else 0f }
                    val dirs = Array(2) { dim -> Vector3.func { if (it == dims[dim]) .5f else 0f } }
                    val dx = dirs[0]
                    val dy = dirs[1]

                    val i0 = addVertex(pos - dx - dy, normal, Vector2(0f, 0f))
                    val i1 = addVertex(pos + dx - dy, normal, Vector2(1f, 0f))
                    val i2 = addVertex(pos - dx + dy, normal, Vector2(0f, 1f))

                    val i3 = addVertex(pos - dx + dy, normal, Vector2(0f, 1f))
                    val i4 = addVertex(pos + dx - dy, normal, Vector2(1f, 0f))
                    val i5 = addVertex(pos + dx + dy, normal, Vector2(1f, 1f))

                    addIndices(i0, i1, i2, i3, i4, i5)
                }

                face(Vector3(0f, +.5f, 0f))
                face(Vector3(0f, -.5f, 0f))

                face(Vector3(+.5f, 0f, 0f))
                face(Vector3(-.5f, 0f, 0f))

                face(Vector3(0f, 0f, +.5f))
                face(Vector3(0f, 0f, -.5f))
            }
        }
    }
}
