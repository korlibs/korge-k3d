package korlibs.korge3d.util

import korlibs.math.geom.*

// @TODO: Move to KorMA
inline fun Vector3F.Companion.func(func: (index: Int) -> Float): Vector3F = Vector3F(func(0), func(1), func(2))
inline fun Vector4F.Companion.func(func: (index: Int) -> Float): Vector4F = Vector4F(func(0), func(1), func(2), func(3))

val MVector4.immutable: Vector4 get() = Vector4(x, y, z, w)
