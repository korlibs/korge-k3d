package korlibs.math.geom

import korlibs.korge3d.*
import korlibs.korge3d.util.*
import kotlin.math.*

data class ContactPoint(
    val point: Vector3,
    val normal: Vector3,
    val collider0: Collider,
    val collider1: Collider,
    val separation: Float
)

data class PhysicsMaterial(
    val bounciness: Float = 0f
)

//class RaycastHit3D(val position: Vector3)
//data class VectorWithNormal(val p: Vector3, val n: Vector3)

interface Collider {
    val material: PhysicsMaterial
    fun sdf(p: Vector3): Float = TODO()
    fun getClosestPoint(p: Vector3): Vector3 = TODO()
    //fun getNormalVector(p: Vector3): Vector3 = TODO()
    fun raycast(ray: Ray3F, maxDistance: Float = Float.POSITIVE_INFINITY): Vector3? = TODO()
    //fun getBoundingAABB(): AABB3D
}

data class PlaneCollider(val p: Vector3, val normal: Vector3, override val material: PhysicsMaterial) : Collider {
    override fun sdf(p: Vector3): Float {
        return getClosestPoint(p) distanceTo p
    }

    override fun getClosestPoint(p: Vector3): Vector3 {
        val plane = this
        val v = p - plane.p
        val dist = v dot plane.normal
        return p - plane.normal * dist
    }
    //override fun getNormalVector(p: Vector3): Vector3 = TODO("Not yet implemented")
}

data class BoxCollider(val center: Vector3, val size: Vector3, override val material: PhysicsMaterial) : Collider {
    //override fun getClosestPoint(p: Vector3): Vector3 = TODO("Not yet implemented")
    //override fun getNormalVector(p: Vector3): Vector3 = TODO("Not yet implemented")
}

data class SphereCollider(val center: Vector3, val radius: Float, override val material: PhysicsMaterial) : Collider {
    override fun sdf(p: Vector3): Float {
        return (p distanceTo center) - radius
    }

    override fun getClosestPoint(p: Vector3): Vector3 = (p - center).normalized() * radius
    //override fun getNormalVector(p: Vector3): Vector3 = (p - center).normalized()

    override fun raycast(ray: Ray3F, maxDistance: Float): Vector3? {
        val sphere = this
        val oc = ray.pos - sphere.center

        val a = ray.dir dot ray.dir
        val b = 2.0 * (oc dot ray.dir)
        val c = (oc dot oc) - sphere.radius * sphere.radius

        val discriminant = b * b - 4 * a * c

        return when {
            discriminant < 0 -> null
            else -> {
                val t = (-b - sqrt(discriminant)) / (2.0 * a)
                if (t < 0) {
                    val t = (-b + sqrt(discriminant)) / (2.0 * a)
                    if (t < 0) null else ray.pos + ray.dir * t
                } else {
                    ray.pos + ray.dir * t
                }
            }
        }
    }
}

private infix fun Vector3.distanceTo(other: Vector3): Float = (other - this).length

object Colliders {
    fun collide(s: SphereCollider, p: PlaneCollider): ContactPoint? {
        val dist = (p.getClosestPoint(s.center) distanceTo s.center) - s.radius
        return if (dist >= 0f) null else ContactPoint(p.getClosestPoint(s.center), p.normal, s, p, dist)
    }

    fun collide(s: SphereCollider, m1: Transform3D, p: PlaneCollider, m2: Transform3D): ContactPoint? {
        val ss = SphereCollider(s.center + m1.mtranslation.immutable.toVector3(), s.radius, s.material)

        val pp = PlaneCollider(
            m2.matrix.immutable.transform(p.p),
            m2.rotation.toRotation3x3Matrix().inverted().transform(p.normal),
            p.material
        )

        //println("m2.rotation=${m2.rotation.normalized().toMatrix()}")

        //println("pp=$pp")
        //println(m2.rotation)
        //val distancePlaneToCenter = p.getClosestPoint(s.center) distanceTo s.center
        //println("distancePlaneToCenter=$distancePlaneToCenter, radius=${s.radius}")
        //println("pp.getClosestPoint(ss.center)=${pp.getClosestPoint(ss.center)}")
        //println("ss.center=${ss.center}")
        return collide(ss, pp)
        //return false
    }

    fun collide(a: Collider, m1: Transform3D, b: Collider, m2: Transform3D): ContactPoint? {
        if (a is SphereCollider && b is PlaneCollider) return collide(a, m1, b, m2)
        TODO()
    }
}
