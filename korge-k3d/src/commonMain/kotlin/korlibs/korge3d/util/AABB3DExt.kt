package korlibs.korge3d.util

import korlibs.math.geom.*

fun List<AABB3D>.combineBounds(): AABB3D {
    var min = Vector3(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    var max = Vector3(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
    for (bounds in this) {
        min = Vector3.func { kotlin.math.min(min[it], bounds.min[it]) }
        max = Vector3.func { kotlin.math.max(max[it], bounds.max[it]) }
    }
    return AABB3D(min, max)
}

val AABB3D.size: Vector3 get() = max - min
