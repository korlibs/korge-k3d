package korlibs.korge3d

import korlibs.math.geom.*

data class Orbit3D private constructor(
    val t: Vector3,
    val distance: Float,
    val azimuth: Angle,
    val elevation: Angle,
    val unit: Unit
) {
    constructor(
        t: Vector3,
        distance: Float,
        azimuth: Angle,
        elevation: Angle,
    ) : this(
        t, distance, azimuth, adjustOrbitElevation(elevation), Unit
    )

    companion object {
        fun setOrbitAround(transform3D: Transform3D, t: Vector3, distance: Float, azimuth: Angle, elevation: Angle) {
            val r = distance
            val theta = azimuth
            val phi = adjustOrbitElevation(elevation)
            val sinPhi = sin(phi)
            val p = Vector3(
                r * sinPhi * sin(theta), // x
                r * cos(phi),            // y
                r * sinPhi * cos(theta)  // z
            )
            transform3D.setTranslationAndLookAt(t + p, t)
        }

        fun adjustOrbitElevation(angle: Angle): Angle =
            angle.clamp(Angle.ZERO + 0.01.degrees, Angle.HALF - 0.01.degrees)
        private fun Angle.clamp(min: Angle, max: Angle): Angle = min(max(this, min), max)
    }
}

fun <T : View3D> T.orbitAround(t: Vector3, distance: Float, azimuth: Angle, elevation: Angle): T {
    Orbit3D.setOrbitAround(this.transform, t, distance, azimuth, elevation)
    invalidateRender()
    return this
}
