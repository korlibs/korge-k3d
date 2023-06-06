package korlibs.math.geom

data class Triangle3D(val p0: Vector3, val p1: Vector3, val p2: Vector3) {
    companion object
    val plane: Plane3D = Plane3D.fromTriangle(this)
    val normal: Vector3 = plane.normal
}
