package korlibs.korge3d.util

import korlibs.math.geom.*
import kotlin.math.*

const val PIf = PI.toFloat()

fun TRS4.toMatrix() = Matrix4.fromTRS(this)
