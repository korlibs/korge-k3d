package korlibs.korge3d.shape

import korlibs.graphics.*
import korlibs.korge3d.*

class Capsule3D(var height: Float, var radius: Float) : ShapeViewWithMesh3D(Capsule3D.mesh) {
    companion object {
        val mesh = MeshBuilder3D(drawType = AGDrawType.TRIANGLES) {
            // TODO()
        }
    }
}
