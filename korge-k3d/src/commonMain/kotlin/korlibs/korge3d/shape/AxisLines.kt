package korlibs.korge3d.shape

import korlibs.image.color.*
import korlibs.korge3d.*
import korlibs.math.geom.*

fun Container3D.axisLines(basePosition: Vector3 = Vector3.ZERO, length: Float = 10f, lengthWhiteScale: Float = .25f): Container3D {
    val ll = length
    val l2 = length * lengthWhiteScale
    return container3D {
        position(basePosition)
        polyline3D(Colors["#e20050"]) {
            moveTo(Vector3(-ll, 0f, 0f))
            lineTo(Vector3(ll, 0f, 0f))
        }
        polyline3D(Colors.MEDIUMVIOLETRED) {
            moveTo(Vector3.DOWN * ll)
            lineTo(Vector3.UP * ll)
        }
        polyline3D(Colors["#8cb04d"]) {
            moveTo(Vector3(0f, 0f, -ll))
            lineTo(Vector3(0f, 0f, ll))
        }
        polyline3D(Colors.WHITE) {
            moveTo(Vector3(0f, 0f, 0f))
            lineTo(Vector3(l2, 0f, 0f))
            moveTo(Vector3(0f, 0f, 0f))
            lineTo(Vector3(0f, l2, 0f))
            moveTo(Vector3(0f, 0f, 0f))
            lineTo(Vector3(0f, 0f, l2))
        }
    }
}
