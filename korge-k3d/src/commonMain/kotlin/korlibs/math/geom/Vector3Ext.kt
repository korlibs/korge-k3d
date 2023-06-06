package korlibs.math.geom

import kotlin.math.*

val Vector3.absoluteValue: Vector3 get() = Vector3(x.absoluteValue, y.absoluteValue, z.absoluteValue)
fun Vector3.min(v: Float): Vector3 = Vector3(kotlin.math.min(x, v), kotlin.math.min(y, v), kotlin.math.min(z, v))
fun Vector3.max(v: Float): Vector3 = Vector3(kotlin.math.max(x, v), kotlin.math.max(y, v), kotlin.math.max(z, v))
