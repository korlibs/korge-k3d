package korlibs.math.geom

// Like a Segment3D with a radius
data class Capsule3D(val p0: Vector3, val p1: Vector3, val radius: Float) {
    val segment = Segment3D(p0, p1)
    companion object
}
