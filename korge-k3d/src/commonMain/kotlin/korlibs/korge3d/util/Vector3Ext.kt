package korlibs.korge3d.util

import korlibs.math.geom.*

fun Vector3.maxComponent(): Float = kotlin.math.max(kotlin.math.max(x, y), z)
