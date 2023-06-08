package korlibs.math.geom

import kotlin.math.*

val Vector3.absoluteValue: Vector3 get() = Vector3(x.absoluteValue, y.absoluteValue, z.absoluteValue)
fun Vector3.min(v: Float): Vector3 = Vector3(kotlin.math.min(x, v), kotlin.math.min(y, v), kotlin.math.min(z, v))
fun Vector3.max(v: Float): Vector3 = Vector3(kotlin.math.max(x, v), kotlin.math.max(y, v), kotlin.math.max(z, v))
operator fun Vector3.times(v: Int): Vector3 = this * v.toFloat()
operator fun Vector3.div(v: Int): Vector3 = this / v.toFloat()
operator fun Vector3.times(v: Double): Vector3 = this * v.toFloat()
operator fun Vector3.div(v: Double): Vector3 = this / v.toFloat()

// https://math.stackexchange.com/questions/13261/how-to-get-a-reflection-vector
//𝑟=𝑑−2(𝑑⋅𝑛)𝑛
fun Vector3.reflected(surfaceNormal: Vector3): Vector3 {
    val d = this
    val n = surfaceNormal
    return d - 2f * (d dot n) * n
}

operator fun Float.times(v: Vector3): Vector3 = v * this
operator fun Float.div(v: Vector3): Vector3 = v / this
