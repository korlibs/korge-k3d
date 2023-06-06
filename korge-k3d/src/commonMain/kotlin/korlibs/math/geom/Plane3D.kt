package korlibs.math.geom

data class Plane3D(val p: Vector3, val normal: Vector3) {
    companion object {
        fun fromTriangle(t: Triangle3D): Plane3D = fromTriangle(t.p0, t.p1, t.p2)
        fun fromTriangle(p0: Vector3, p1: Vector3, p2: Vector3): Plane3D {
            val normal = ((p1 - p0) cross (p2 - p0)).normalized()
            //val p = (p0 + p1 + p2) / 3f
            val p = p0
            return Plane3D(p, normal)
        }
    }
}

fun Distances.distance(sphere: Sphere3D, plane: Plane3D): Float {
    val dist = (sphere.center - plane.p) dot plane.normal // signed distance between sphere and plane
    return dist - sphere.radius
}

fun Plane3D.distanceTo(sphere: Sphere3D): Float = Distances.distance(sphere, this)
fun Sphere3D.distanceTo(plane: Plane3D): Float = Distances.distance(this, plane)
