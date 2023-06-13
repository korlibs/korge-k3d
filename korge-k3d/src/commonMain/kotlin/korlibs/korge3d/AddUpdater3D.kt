package korlibs.korge3d

import korlibs.io.lang.*
import korlibs.korge.view.*
import korlibs.time.*

fun <T : View3D> T.addUpdater(first: Boolean = true, firstTime: TimeSpan = TimeSpan.ZERO, updatable: T.(dt: TimeSpan) -> Unit): CloseableCancellable {
    if (first) updatable(this, firstTime)
    return onEvent(UpdateEvent) { updatable(this, it.deltaTime * this.globalSpeed) }
}
fun <T : View3D> T.addUpdater(updatable: T.(dt: TimeSpan) -> Unit): CloseableCancellable = addUpdater(true, updatable = updatable)
