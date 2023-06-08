package korlibs.math.geom

val Quaternion.Companion.IDENTITY: Quaternion get() = Quaternion()

fun Quaternion.scaled(scale: Float): Quaternion {
    return Quaternion.interpolated(Quaternion.IDENTITY, this, scale)
}
