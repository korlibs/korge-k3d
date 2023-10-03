package korlibs.math.geom

import korlibs.math.*
import korlibs.memory.*

object ContactPoints {
    fun contact(s1: Sphere3D, s2: Sphere3D): Vector3 {
        // Compute the vector from the center of the first sphere to the center of the second
        val centerToCenter = s2.center - s1.center
        // Normalize this vector to get the direction from the first sphere to the second
        val direction = centerToCenter.normalized()
        // The contact point is then the center of the first sphere plus the direction vector scaled by the sphere's radius
        return s1.center + direction * s1.radius
    }

    data class Result(val v1: Vector3, val v2: Vector3) {
        val middle: Vector3 = (v1 + v2) * 0.5f
    }

    fun contact(c1: Capsule3D, c2: Capsule3D): Result {
        val closestPointOnC1 = closestPoint(c1.segment, closestPoint(c2.segment, c1.p0))
        val closestPointOnC2 = closestPoint(c2.segment, closestPoint(c1.segment, c2.p0))
        return Result(closestPointOnC1, closestPointOnC2)
    }

    fun contact(c: Capsule3D, s: Sphere3D): Vector3 {
        // Calculate the closest point on the capsule line segment to the sphere center
        val closestPointOnCapsuleSegment = closestPoint(c.segment, s.center)
        // Calculate the vector from the closest point to the sphere center
        val toSphereCenter = s.center - closestPointOnCapsuleSegment
        // If the distance to the sphere center is less than the capsule radius, the sphere is intersecting with the cylindrical part of the capsule
        return when {
            toSphereCenter.length <= c.radius -> closestPointOnCapsuleSegment
            // Otherwise, the sphere is intersecting with the hemispherical part of the capsule
            else -> closestPointOnCapsuleSegment + toSphereCenter.normalized() * c.radius
        }
    }

    fun closestPoint(s: Segment3D, p: Vector3): Vector3 {
        val ap = p - s.p0
        val ab = s.p1 - s.p0
        val t = ((ap dot ab) / (ab dot ab)).clamp01()
        return s.p0 + ab * t
    }
}
