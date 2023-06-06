package korlibs.math.geom

// Like a Segment3D with a radius
data class Capsule3D(val p0: Vector3, val p1: Vector3, val radius: Float) {
    val A get() = p0
    val B get() = p1
    val direction = (p1 - p0).normalized()
    val base = p0 - direction * radius
    val tip = p1 + direction * radius
    val segment = Segment3D(p0, p1)
    companion object
}
