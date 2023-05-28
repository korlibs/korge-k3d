package korlibs.korge3d.util

import korlibs.math.geom.*

// @TODO: Move to KorMA
inline fun Vector3.Companion.func(func: (index: Int) -> Float): Vector3 = Vector3(func(0), func(1), func(2))
inline fun Vector4.Companion.func(func: (index: Int) -> Float): Vector4 = Vector4(func(0), func(1), func(2), func(3))
