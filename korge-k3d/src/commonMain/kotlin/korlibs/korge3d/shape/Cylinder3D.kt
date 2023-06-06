package korlibs.korge3d.shape

import korlibs.graphics.*
import korlibs.korge3d.*
import korlibs.korge3d.util.*
import korlibs.math.geom.*
import kotlin.math.*

// https://chat.openai.com/share/abe38a64-ade0-4f25-a458-14f7330c6c93

inline fun Container3D.cylinder(radius: Number, callback: Cylinder3D.() -> Unit = {}): Cylinder3D =
    cylinder(radius.toFloat(), callback)

inline fun Container3D.cylinder(
    radius: Float = 1f,
    callback: Cylinder3D.() -> Unit = {}
): Cylinder3D = Cylinder3D(radius).addTo(this, callback)


class Cylinder3D(var radius: Float) : ShapeViewWithMesh3D(mesh) {
    override fun prepareExtraModelMatrix() = Matrix4.scale(radius, radius, radius)

    companion object {
        val mesh = MeshBuilder3D(drawType = AGDrawType.TRIANGLES) {
            val height = 1f
            val radius = 0.5f
            val segments = 16

            // Generate vertices, normals and UVs
            for (i in 0 until segments) {
                val angle = (2.0 * PIf * i) / segments
                val x = radius * cos(angle)
                val z = radius * sin(angle)

                // Top circle vertex
                addVertex(Vector3(x.toFloat(), height / 2, z.toFloat()), Vector3(x.toFloat(), 0f, z.toFloat()), Vector2(i.toFloat() / segments, 1f))
                // Bottom circle vertex
                addVertex(Vector3(x.toFloat(), -height / 2, z.toFloat()), Vector3(x.toFloat(), 0f, z.toFloat()), Vector2(i.toFloat() / segments, 0f))
            }

            // Generate indices
            for (i in 0 until segments) {
                val next = (i + 1) % segments

                // Top face
                addIndex(i * 2)
                addIndex(next * 2)
                addIndex((segments * 2) + 2)  // Center of top circle

                // Bottom face
                addIndex((i * 2) + 1)
                addIndex((segments * 2) + 3)  // Center of bottom circle
                addIndex(next * 2 + 1)

                // Side faces
                addIndex(i * 2)
                addIndex(next * 2)
                addIndex(i * 2 + 1)

                addIndex(i * 2 + 1)
                addIndex(next * 2)
                addIndex(next * 2 + 1)
            }
        }
    }
}
