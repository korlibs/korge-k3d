package korlibs.korge3d.shape

import korlibs.graphics.*
import korlibs.korge3d.*
import korlibs.korge3d.util.*
import korlibs.math.geom.*
import kotlin.math.*

inline fun Container3D.sphere(radius: Number, callback: Sphere3D.() -> Unit = {}): Sphere3D =
    sphere(radius.toFloat(), callback)

inline fun Container3D.sphere(
    radius: Float = 1f,
    callback: Sphere3D.() -> Unit = {}
): Sphere3D = Sphere3D(radius).addTo(this, callback)


class Sphere3D(var radius: Float) : ShapeViewWithMesh3D(mesh) {
    override fun prepareExtraModelMatrix() = Matrix4.scale(radius, radius, radius)

    companion object {
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

                    val p = Vector3(x, y, z) * 2f

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
