package korlibs.korge3d.util

import korlibs.math.geom.*

fun Quaternion.Companion.fromVectors(v1: Vector3, v2: Vector3): Quaternion {
    // Normalize input vectors
    val start = v1.normalized()
    val dest = v2.normalized()

    val dot = start.dot(dest)

    // If vectors are opposite
    when {
        dot < -0.9999999f -> {
            val tmp = Vector3(start.y, -start.x, 0f).normalized()
            return Quaternion(tmp.x, tmp.y, tmp.z, 0f)
        }
        dot > 0.9999999f -> {
            // If vectors are same
            return Quaternion()
        }
        else -> {
            val s = kotlin.math.sqrt((1 + dot) * 2)
            val invs = 1 / s

            val c = start.cross(dest)

            return Quaternion(
                c.x * invs,
                c.y * invs,
                c.z * invs,
                s * 0.5f,
            ).normalized()
        }
    }
}
