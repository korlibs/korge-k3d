package korlibs.math.geom

import korlibs.math.*
import korlibs.math.geom.*
import korlibs.memory.*
import kotlin.math.*

private fun sq(v: Float): Float = v * v

// NOTE: a sphere is a capsule of 0 length, and a segment is a capsule of 0 radius

object Distances {
    fun distance(c1: Segment3D, c2: Segment3D): Float {
        val dP1 = c1.p1 - c1.p0
        val dP2 = c2.p1 - c2.p0
        val r = c1.p0 - c2.p0
        val a = dP1 dot dP1
        val e = dP2 dot dP2
        val f = dP2 dot r
        val c: Float = a * e - (dP1 dot dP2).pow(2)
        val s: Float = (if (c != 0f) (((dP1 dot r) * e - (dP1 dot dP2) * f) / c) else 0.0f).clamp01()
        val t: Float = (if (c != 0f) (((dP1 dot dP2) * s + f) / e) else (f / e)).clamp01()
        val dP = r + (dP1 * s) - (dP2 * t)
        return dP.length
    }

    fun distance(c1: Segment3D, c2: Capsule3D): Float {
        return distance(c1, c2.segment) - (c2.radius)
    }

    fun distance(c1: Capsule3D, c2: Capsule3D): Float {
        return distance(c1.segment, c2.segment) - (c1.radius + c2.radius)
    }

    fun distance(c: Segment3D, s: Sphere3D): Float {
        val dP = c.p1 - c.p0
        val a = dP dot dP
        val r = s.center - c.p0
        val f = dP dot r
        val t = if (a != 0f) (f / a).clamp01() else 0.0f
        val dP2 = r - (dP * t)
        return dP2.length - s.radius
    }

    fun distance(c: Capsule3D, s: Sphere3D): Float = distance(c.segment, s) - c.radius

    fun distance(s1: Sphere3D, s2: Sphere3D): Float {
        return (s2.center - s1.center).length - (s1.radius + s2.radius)
    }

    fun distance(s: Sphere3D, b: AABB3D): Float {
        var sqDist = 0f

        if (s.center.x < b.min.x) {
            sqDist += sq(s.center.x - b.min.x)
        } else if (s.center.x > b.max.x) {
            sqDist += sq(s.center.x - b.max.x)
        }

        if (s.center.y < b.min.y) {
            sqDist += sq(s.center.y - b.min.y)
        } else if (s.center.y > b.max.y) {
            sqDist += sq(s.center.y - b.max.y)
        }

        if (s.center.z < b.min.z) {
            sqDist += sq(s.center.z - b.min.z)
        } else if (s.center.z > b.max.z) {
            sqDist += sq(s.center.z - b.max.z)
        }

        return if (sqDist > 0f) sqrt(sqDist) - s.radius else -s.radius
    }

    fun distance(segment: Segment3D, box: AABB3D): Float {
        // Centre of the box
        val boxCenter = (box.min + box.max) * 0.5f
        // Half-size of the box
        val boxHalfSize = (box.max - box.min) * 0.5f
        // Segment's center point and half-length vector
        val segmentCenter = (segment.p0 + segment.p1) * 0.5f
        val segmentHalfLength: Vector3 = (segment.p1 - segment.p0) * 0.5f
        // Translate the segment and the box to origin
        val t = segmentCenter - boxCenter
        // Calculate the projection of the segment onto box
        val projection = boxHalfSize + segmentHalfLength.absoluteValue
        // Calculate the distance between the segment and the box
        val dist = (projection - t.absoluteValue).max(0.0f)
        return dist.length
    }

    fun distance(segment: Capsule3D, box: AABB3D): Float {
        return distance(segment.segment, box) - segment.radius
    }
}

fun Segment3D.distanceTo(that: Segment3D): Float = Distances.distance(this, that)
fun Segment3D.distanceTo(that: Capsule3D): Float = Distances.distance(this, that)
fun Segment3D.distanceTo(that: Sphere3D): Float = Distances.distance(this, that)
fun Segment3D.distanceTo(that: AABB3D): Float = Distances.distance(this, that)

fun Capsule3D.distanceTo(that: Segment3D): Float = Distances.distance(that, this)
fun Capsule3D.distanceTo(that: Capsule3D): Float = Distances.distance(this, that)
fun Capsule3D.distanceTo(that: Sphere3D): Float = Distances.distance(this, that)
fun Capsule3D.distanceTo(that: AABB3D): Float = Distances.distance(this, that)

fun Sphere3D.distanceTo(that: Segment3D): Float = Distances.distance(that, this)
fun Sphere3D.distanceTo(that: Capsule3D): Float = Distances.distance(that, this)
fun Sphere3D.distanceTo(that: Sphere3D): Float = Distances.distance(this, that)
fun Sphere3D.distanceTo(that: AABB3D): Float = Distances.distance(this, that)
