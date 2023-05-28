@file:Suppress("NOTHING_TO_INLINE")

package korlibs.korge3d.tween

import korlibs.korge.tween.get
import korlibs.korge.tween.tween
import korlibs.korge3d.View3D
import korlibs.math.interpolation.Easing
import korlibs.time.*

@PublishedApi
internal val DEFAULT_EASING = Easing.EASE_IN_OUT_QUAD

@PublishedApi
internal val DEFAULT_TIME = 1.seconds

//suspend fun View3D.show(time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) =
//	tween(this::alpha[1.0], time = time, easing = easing) { this.visible = true }

//suspend fun View3D.hide(time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) =
//	tween(this::alpha[0.0], time = time, easing = easing)

suspend inline fun View3D.moveTo(x: Float, y: Float, z: Float, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) = tween(this::x[x], this::y[y], this::z[z], time = time, easing = easing)
suspend inline fun View3D.moveBy(dx: Float, dy: Float, dz: Float, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) = tween(this::x[this.x + dx], this::y[this.y + dy], this::z[this.z + dz], time = time, easing = easing)
suspend inline fun View3D.scaleTo(sx: Float, sy: Float, sz: Float, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) = tween(this::scaleX[sx], this::scaleY[sy], this::scaleZ[sz], time = time, easing = easing)

//suspend inline fun View3D.rotateTo(deg: Angle, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) =
//	tween(this::rotationRadians[deg.radians], time = time, easing = easing)

//suspend inline fun View3D.rotateBy(ddeg: Angle, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) =
//	tween(this::rotationRadians[this.rotationRadians + ddeg.radians], time = time, easing = easing)
