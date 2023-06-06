package korlibs.korge3d.shape

import korlibs.graphics.*
import korlibs.korge3d.*
import korlibs.korge3d.util.*
import korlibs.math.geom.*
import kotlin.math.*

// https://chat.openai.com/share/a9e4292c-929e-4d0b-93f9-4a501957ce42

inline fun Container3D.cone(radius: Number, callback: Cone3D.() -> Unit = {}): Cone3D =
    cone(radius.toFloat(), callback)

inline fun Container3D.cone(
    radius: Float = 1f,
    callback: Cone3D.() -> Unit = {}
): Cone3D = Cone3D(radius).addTo(this, callback)


class Cone3D(var radius: Float) : ShapeViewWithMesh3D(mesh) {
    override fun prepareExtraModelMatrix() = Matrix4.scale(radius, radius, radius)

    companion object {
        val mesh = MeshBuilder3D(drawType = AGDrawType.TRIANGLES) {
            val height = 1f
            val radius = 0.5f
            val slices = 16

            // Top point of the cone
            addVertex(
                Vector3(0f, height, 0f),
                Vector3(0f, 1f, 0f), // Normal is straight up for the tip
                Vector2(0.5f, 1f)
            )

            // Base circle points
            for (i in 0..slices) {
                val theta = i.toFloat() / slices * 2f * PIf
                val x = radius * cos(theta)
                val z = radius * sin(theta)

                // Calculate normal
                val slope = atan2(height, radius)
                val normalX = cos(slope) * cos(theta)
                val normalY = sin(slope)
                val normalZ = cos(slope) * sin(theta)

                addVertex(
                    Vector3(x, 0f, z),
                    Vector3(normalX, normalY, normalZ),
                    Vector2((cos(theta) / 2 + 0.5).toFloat(), (sin(theta) / 2 + 0.5).toFloat())
                )
            }

            // Generate indices
            for (i in 1 until slices) {
                // Triangles should be CCW to be viewed from the outside
                addIndices(0, i, i + 1)
            }
            // Last triangle
            addIndices(0, slices, 1)

            // Generate indices for the base
            val baseCenterIndex = totalVertices - 1
            for (i in 1 until slices) {
                addIndex(baseCenterIndex)
                addIndex(baseCenterIndex - i)
                addIndex(baseCenterIndex - i - 1)
            }
            // Last triangle of the base
            addIndex(baseCenterIndex)
            addIndex(baseCenterIndex - slices)
            addIndex(baseCenterIndex - 1)
        }
    }
}
