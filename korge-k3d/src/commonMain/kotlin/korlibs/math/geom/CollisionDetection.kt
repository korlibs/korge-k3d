package korlibs.math.geom

import kotlin.math.*

// https://wickedengine.net/2020/04/26/capsule-collision-detection/
// https://chat.openai.com/share/615a7c3e-409c-40b8-b7e7-109a6acdff3c
object CollisionDetection {
    //data class Result(val p1: Vector3, val p2: Vector3, val collides: Boolean) {
    //    val depth: Float = (p2 - p1).length
    //    val normal = (p2 - p1).normalized()
    //}
    //fun collision(s1: Sphere3D, s2: Sphere3D): Result {
    //    val distanceBetweenCenters = (s2.center - s1.center).length
    //    val D = s2.center - s1.center
    //    val N = if (D.lengthSquared == 0f) Vector3.ZERO else D.normalized()
    //    return Result(
    //        s1.center + N * s1.radius,
    //        s2.center - N * s2.radius,
    //        collides = distanceBetweenCenters < s1.radius + s2.radius
    //    )
    //}

    data class CollisionResult(val intersects: Boolean, val penetrationDepth: Float = 0.0f, val penetrationNormal: Vector3? = null, val contactPoint: Vector3? = null)

    fun collision(sphere1: Sphere3D, sphere2: Sphere3D): CollisionResult {
        val vecBetweenCenters = sphere2.center - sphere1.center
        val distanceBetweenCenters = vecBetweenCenters.length
        val totalRadius = sphere1.radius + sphere2.radius

        // Check for intersection
        if (distanceBetweenCenters >= totalRadius) {
            return CollisionResult(false)
        }

        // Calculate penetration depth and normal
        val penetrationDepth = totalRadius - distanceBetweenCenters
        val penetrationNormal = vecBetweenCenters.normalized()

        // Calculate contact point as the point on the line between the centers
        // that lies on the surface of the first sphere and towards the second sphere
        val contactPoint = sphere1.center + penetrationNormal * sphere1.radius

        return CollisionResult(true, penetrationDepth, penetrationNormal, contactPoint)
    }

    fun collision(a: Capsule3D, b: Capsule3D): CollisionResult {
        val aNormal = (a.tip - a.base).normalized()
        val aLineEndOffset = aNormal * a.radius
        val aA = a.base + aLineEndOffset
        val aB = a.tip - aLineEndOffset

        val bNormal = (b.tip - b.base).normalized()
        val bLineEndOffset = bNormal * b.radius
        val bA = b.base + bLineEndOffset
        val bB = b.tip - bLineEndOffset

        val v0 = bA - aA
        val v1 = bB - aA
        val v2 = bA - aB
        val v3 = bB - aB

        val d0 = v0.dot(v0)
        val d1 = v1.dot(v1)
        val d2 = v2.dot(v2)
        val d3 = v3.dot(v3)

        var bestA = if (d2 < d0 || d2 < d1 || d3 < d0 || d3 < d1) aB else aA

        var bestB = closestPointOnLineSegment(bA, bB, bestA)

        bestA = closestPointOnLineSegment(aA, aB, bestB)

        val penetrationNormal = bestA - bestB
        val len = penetrationNormal.length
        val normalizedPenetrationNormal = penetrationNormal / len
        val penetrationDepth = a.radius + b.radius - len
        val intersects = penetrationDepth > 0

        return CollisionResult(intersects, penetrationDepth, normalizedPenetrationNormal, if (intersects) bestA else null)
    }

    fun collision(capsule: Capsule3D, triangle: Triangle3D): CollisionResult {
        val base = capsule.base
        val tip = capsule.tip
        val radius = capsule.radius
        val p0 = triangle.p0
        val p1 = triangle.p1
        val p2 = triangle.p2
        val N = triangle.normal

        val CapsuleNormal = (tip - base).normalized()
        val LineEndOffset = CapsuleNormal * radius
        val A = base + LineEndOffset
        val B = tip - LineEndOffset

        val dotProduct = N.dot(CapsuleNormal)
        var reference_point: Vector3 = Vector3.ZERO
        var line_plane_intersection: Vector3? = null

        var inside: Boolean = false

        if (dotProduct.absoluteValue >= 1e-6) { // Capsule line is not parallel to the triangle plane
            val t = N.dot((p0 - base) / dotProduct)
            line_plane_intersection = base + CapsuleNormal * t

            val c0 = (line_plane_intersection - p0).cross(p1 - p0)
            val c1 = (line_plane_intersection - p1).cross(p2 - p1)
            val c2 = (line_plane_intersection - p2).cross(p0 - p2)
            inside = c0.dot(N) <= 0 && c1.dot(N) <= 0 && c2.dot(N) <= 0

            if (inside) {
                reference_point = line_plane_intersection
            }
        }

        if (line_plane_intersection == null || !inside) {
            // Find the closest point on the triangle to the line_plane_intersection or an arbitrary point on the capsule line
            val target_point = line_plane_intersection ?: A
            // Edge 1:
            val point1 = closestPointOnLineSegment(p0, p1, target_point)
            var v1 = target_point - point1
            var distsq = v1.dot(v1)
            var best_dist = distsq
            reference_point = point1

            // Edge 2:
            val point2 = closestPointOnLineSegment(p1, p2, target_point)
            v1 = target_point - point2
            distsq = v1.dot(v1)
            if(distsq < best_dist)
            {
                reference_point = point2
                best_dist = distsq
            }

            // Edge 3:
            val point3 = closestPointOnLineSegment(p2, p0, target_point)
            v1 = target_point - point3
            distsq = v1.dot(v1)
            if(distsq < best_dist)
            {
                reference_point = point3
                best_dist = distsq
            }
        }

        val center = closestPointOnLineSegment(A, B, reference_point)

        val intersects = (center - reference_point).length <= radius

        return CollisionResult(intersects, if (intersects) radius - (center - reference_point).length else 0f, if (intersects) (CapsuleNormal * -1) else null, if (intersects) reference_point else null)
    }

    //private fun normalize(v: Vector3): Vector3 = v.normalized()
    //private fun cross(a: Vector3, b: Vector3): Vector3 = a cross b
    //private fun dot(a: Vector3, b: Vector3): Float = a dot b

    fun collision(sphere: Sphere3D, triangle: Triangle3D, isDoubleSided: Boolean): CollisionResult {
        val radius = sphere.radius
        val radiusSq = radius * radius

        val N = (triangle.p1 - triangle.p0).cross(triangle.p2 - triangle.p0).normalized()

        val dist = (sphere.center - triangle.p0).dot(N)

        if (!isDoubleSided && dist > 0) return CollisionResult(false)
        if (dist < -radius || dist > radius) return CollisionResult(false)

        val point0 = sphere.center - N * dist

        val inside = listOf(triangle.p0, triangle.p1, triangle.p2).all { p -> (point0 - p).cross(p - triangle.p0).dot(N) <= 0 }

        val points = listOf(
            closestPointOnLineSegment(triangle.p0, triangle.p1, sphere.center),
            closestPointOnLineSegment(triangle.p1, triangle.p2, sphere.center),
            closestPointOnLineSegment(triangle.p2, triangle.p0, sphere.center)
        )

        val distances = points.map { point -> (sphere.center - point).dot(sphere.center - point) }
        val intersects = distances.any { it < radiusSq }

        if (inside || intersects) {
            var bestPoint = point0
            var intersectionVec = sphere.center - point0

            if (!inside) {
                val minIndex = distances.indices.minByOrNull { distances[it] }!!
                bestPoint = points[minIndex]
                intersectionVec = sphere.center - bestPoint
            }

            val len = intersectionVec.length
            val penetrationNormal = intersectionVec / len
            val penetrationDepth = radius - len
            return CollisionResult(true, penetrationDepth, penetrationNormal, bestPoint)
        }

        return CollisionResult(false)
    }

    fun closestPointOnLineSegment(segment: Segment3D, p: Vector3): Vector3 {
        return closestPointOnLineSegment(segment.p0, segment.p1, p)
    }

    fun closestPointOnLineSegment(A: Vector3, B: Vector3, Point: Vector3): Vector3 {
        val AB = B - A
        val t = (Point - A).dot(AB) / AB.dot(AB)
        val satT = t.coerceIn(0f, 1f)
        return A + AB * satT
    }
}
