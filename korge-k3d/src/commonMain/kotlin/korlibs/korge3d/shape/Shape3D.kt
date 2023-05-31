package korlibs.korge3d.shape

import korlibs.korge3d.*
import korlibs.korge3d.material.*
import korlibs.math.geom.*


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

    override fun prepareExtraModelMatrix() = Matrix4.scale(width, height, depth)

    companion object {

        fun createMesh(drawCommands: MeshBuilder3D.() -> Unit) = MeshBuilder3D {
            drawCommands()
        }
    }
}

abstract class ShapeViewWithMesh3D(mesh: Mesh3D) : ViewWithMesh3D(mesh.copy()) {
    override var material: PBRMaterial3D?
        get() = mesh.material
        set(value) {
            mesh.material = value
            invalidateRender()
        }
}

fun <T : ShapeViewWithMesh3D> T.material(material: PBRMaterial3D?): T {
    this.material = material
    return this
}

