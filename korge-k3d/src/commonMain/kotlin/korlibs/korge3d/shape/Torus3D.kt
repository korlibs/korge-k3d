package korlibs.korge3d.shape

import korlibs.graphics.*
import korlibs.korge3d.*
import korlibs.korge3d.util.*
import korlibs.math.geom.*
import kotlin.math.*

inline fun Container3D.torus(scale: Number = 1f, callback: Torus3D.() -> Unit = {}): Torus3D =
    torus(scale.toFloat(), callback)

inline fun Container3D.torus(
    scale: Float = 1f,
    callback: Torus3D.() -> Unit = {}
): Torus3D = Torus3D(scale).addTo(this, callback)


class Torus3D(var scale: Float) : ShapeViewWithMesh3D(mesh) {
    override fun prepareExtraModelMatrix() = Matrix4.scale(scale, scale, scale)

    companion object {
        val mesh = MeshBuilder3D(drawType = AGDrawType.TRIANGLES) {
            val majorSegments = 16
            val minorSegments = 16
            val majorRadius = .5f
            val minorRadius = .25f

            for (i in 0..majorSegments) {
                val u = i.toFloat() / majorSegments
                val theta = u * PIf * 2f

                for (j in 0..minorSegments) {
                    val v = j.toFloat() / minorSegments
                    val phi = v * PI * 2f

                    // Calculate position of point on torus surface
                    val x = (majorRadius + minorRadius * cos(phi)) * cos(theta)
                    val y = minorRadius * sin(phi)
                    val z = (majorRadius + minorRadius * cos(phi)) * sin(theta)

                    val p = Vector3(x.toFloat(), y.toFloat(), z.toFloat())

                    addVertex(p, p.normalized(), Vector2(u, v))
                }
            }

            // Generate indices
            for (i in 0 until majorSegments) {
                for (j in 0 until minorSegments) {
                    val first = (i * (minorSegments + 1)) + j
                    val second = first + minorSegments + 1

                    addIndices(first, second, first + 1)
                    addIndices(second, second + 1, first + 1)
                }
            }
        }
    }
}
