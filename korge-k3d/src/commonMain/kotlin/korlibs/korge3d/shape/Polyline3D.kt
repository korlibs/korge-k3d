package korlibs.korge3d.shape

import korlibs.datastructure.iterators.*
import korlibs.graphics.*
import korlibs.image.color.*
import korlibs.korge3d.*
import korlibs.korge3d.material.*
import korlibs.math.geom.*

inline fun Container3D.polyline3D(
    color: RGBA = Colors.WHITE,
    block: RenderContext3D.(ViewWithMesh3D) -> Unit
): ViewWithMesh3D = Polyline3D(color).also { it.updateShape(block) }.addTo(this)

class Polyline3D(
    var color: RGBA = Colors.WHITE
) : ViewWithMesh3D(Mesh3D.EMPTY) {
    inline fun updateShape(block: RenderContext3D.(ViewWithMesh3D) -> Unit) {
        val ctx = RenderContext3D().also { block(it, this) }
        mesh = MeshBuilder3D(AGDrawType.LINES) {
            var s = 1
            ctx.polylines.fastForEach { polyline ->
                polyline.fastForEachGeneric {
                    addVertex(Vector3(polyline[it, 0], polyline[it, 1], polyline[it, 2]))
                }
                for (n in 0 until polyline.size - 1) {
                    addIndex(s + n - 1)
                    addIndex(s + n)
                }
                s += polyline.size
            }
        }.also {
            it.material = PBRMaterial3D(diffuse = PBRMaterial3D.LightColor(color))
        }
    }

    init {
    }
}

class RenderContext3D {
    val polylines = arrayListOf<DoubleVectorArrayList>().also { it.add(DoubleVectorArrayList(3)) }
    private var currentPos = Vector3.ZERO

    fun moveTo(x: Float, y: Float, z: Float) = moveTo(Vector3(x, y, z))
    fun moveTo(p: Vector3) {
        polylines += DoubleVectorArrayList(3).also { it.add(p.x, p.y, p.z) }
        currentPos = p
    }
    fun lineTo(x: Float, y: Float, z: Float) = lineTo(Vector3(x, y, z))
    fun lineTo(p: Vector3) {
        polylines.last().add(p.x, p.y, p.z)
        currentPos = p
    }
}
