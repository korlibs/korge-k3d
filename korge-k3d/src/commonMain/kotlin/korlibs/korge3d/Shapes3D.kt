package korlibs.korge3d

import korlibs.graphics.*
import korlibs.korge3d.internal.vector3DTemps
import korlibs.korge3d.util.*
import korlibs.math.geom.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


fun Container3D.shape3D(width: Float = 1f, height: Float = 1f, depth: Float = 1f, drawCommands: MeshBuilder3D.() -> Unit): Shape3D {
   return  Shape3D(width, height, depth, drawCommands).addTo(this)
}

/*
 * Note: To draw solid quads, you can use [Bitmaps.white] + [AgBitmapTextureManager] as texture and the [colorMul] as quad color.
 */

class Shape3D(
    initWidth: Float, initHeight: Float, initDepth: Float,
    drawCommands: MeshBuilder3D.() -> Unit
) : ViewWithMesh3D(createMesh(drawCommands).copy()) {

    var width: Float = initWidth
    var height: Float = initHeight
    var depth: Float = initDepth

    override fun prepareExtraModelMatrix(mat: MMatrix3D) {
        mat.identity().scale(width, height, depth)
    }

    companion object {

        fun createMesh(drawCommands: MeshBuilder3D.() -> Unit) = MeshBuilder3D {
            drawCommands()
        }
    }
}



inline fun Container3D.cube(width: Int, height: Int, depth: Int, callback: Cube3D.() -> Unit = {}): Cube3D = cube(width.toFloat(), height.toFloat(), depth.toFloat(), callback)


inline fun Container3D.cube(
    width: Float = 1f,
    height: Float = width,
    depth: Float = height,
    callback: Cube3D.() -> Unit = {}
): Cube3D = Cube3D(width, height, depth).addTo(this, callback)


abstract class BaseViewWithMesh3D(mesh: Mesh3D) : ViewWithMesh3D(mesh.copy()) {
    var material: Material3D?
        get() = mesh.material
        set(value) {
            mesh.material = value
            invalidateRender()
        }
}

fun <T : BaseViewWithMesh3D> T.material(material: Material3D?): T {
    this.material = material
    return this
}


class Cube3D(var width: Float, var height: Float, var depth: Float) : BaseViewWithMesh3D(mesh) {
    override fun prepareExtraModelMatrix(mat: MMatrix3D) {
        mat.identity().scale(width, height, depth)
    }

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


inline fun Container3D.sphere(radius: Int, callback: Sphere3D.() -> Unit = {}): Sphere3D = sphere(radius.toFloat(), callback)


inline fun Container3D.sphere(
    radius: Float = 1f,
    callback: Sphere3D.() -> Unit = {}
): Sphere3D = Sphere3D(radius).addTo(this, callback)


class Sphere3D(var radius: Float) : BaseViewWithMesh3D(mesh) {
    override fun prepareExtraModelMatrix(mat: MMatrix3D) {
        mat.identity().scale(radius, radius, radius)
    }

    companion object {
        private const val PIf = PI.toFloat()

        val mesh = MeshBuilder3D(drawType = AGDrawType.TRIANGLES) {
            val STACKS = 16
            val SLICES = 16
            val radius = .5f

            for (i in 0..STACKS) {
                val v = i.toFloat() / STACKS
                val phi = v * PIf

                for (j in 0..SLICES) {
                    val u = j.toFloat() / SLICES
                    val theta = u * PIf * 2f

                    // Convert spherical coordinates to cartesian coordinates
                    val x = (radius * sin(phi) * cos(theta))
                    val y = (radius * cos(phi))
                    val z = (radius * sin(phi) * sin(theta))

                    val p = Vector3(x, y, z)

                    addVertex(p, p.normalized(), Vector2(u, v))

                    // Generate indices
                    // Triangles should be CCW to be viewed from the outside
                    val first = i * (SLICES + 1) + j
                    val second = first + SLICES + 1
                    addIndices(first, second, first + 1)
                    addIndices(second, second + 1, first + 1)

                }
            }
        }
    }
}
