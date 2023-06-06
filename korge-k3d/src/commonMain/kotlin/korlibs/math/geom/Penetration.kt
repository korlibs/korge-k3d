package korlibs.math.geom

data class Penetration(val depth: Float, val normal: Vector3, val contactPoint: Vector3) {
    companion object {
        val EPS = 0.0001f

        val Capsule3D.start: Vector3 get() = p0
        val Capsule3D.end: Vector3 get() = p1

        fun contact(capsule1: Capsule3D, capsule2: Capsule3D): Penetration {
            val line1 = capsule1.end - capsule1.start
            val line2 = capsule2.end - capsule2.start

            val d = line1.normalized()
            val e = line2.normalized()

            val r = capsule1.radius + capsule2.radius

            val w0 = capsule1.start - capsule2.start
            val a = d.dot(d)
            val b = d.dot(e)
            val c = e.dot(e)
            val d_ = d.dot(w0)
            val e_ = e.dot(w0)

            val sc: Float
            val tc: Float

            // Compute the line parameters of the two closest points
            if (a <= EPS && b <= EPS) {          // the two lines are almost parallel
                sc = 0f
                tc = if (d_ > e_) d_ / b else e_ / c  // use the largest denominator
            } else {
                sc = (b * e_ - c * d_) / (a * c - b * b)
                tc = (a * e_ - b * d_) / (a * c - b * b)
            }

            // get the difference of the two closest points
            val dP = w0 + (d * sc) - (e * tc)  // =  L1(sc) - L2(tc)

            var norm = dP.normalized()
            var penetrationDepth = r - dP.length

            val contactPoint: Vector3
            // Check the hemispherical ends if not intersecting
            if (penetrationDepth < 0) {
                val startDist = (capsule1.start - capsule2.start).length
                val endDist = (capsule1.end - capsule2.end).length

                if (startDist < endDist) {
                    penetrationDepth = r - startDist
                    norm = (capsule1.start - capsule2.start).normalized()
                    contactPoint = (capsule1.start + capsule2.start) / 2f
                } else {
                    penetrationDepth = r - endDist
                    norm = (capsule1.end - capsule2.end).normalized()
                    contactPoint = (capsule1.end + capsule2.end) / 2f
                }
            } else {
                contactPoint = capsule1.start + (d * sc) + (norm * capsule1.radius)
            }

            return Penetration(penetrationDepth, norm, contactPoint)
        }
    }
}
