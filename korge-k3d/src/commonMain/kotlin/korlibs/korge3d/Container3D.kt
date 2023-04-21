package korlibs.korge3d

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*

@Korge3DExperimental
open class Container3D : View3D() {
    private val __children: FastArrayList<View3D> = FastArrayList()
    val children: List<View3D> get() = __children

    fun getChildAtOrNull(index: Int): View3D? = __children.getOrNull(index)

    inline fun fastForEachChild(block: (View3D) -> Unit) {
        children.fastForEach(block)
    }

    fun removeChildAt(index: Int) {
        getChildAtOrNull(index)?.let { removeChild(it) }
    }

    fun removeChild(child: View3D) {
		if (!__children.remove(child)) return
        child.parent = null
	}

	fun addChild(child: View3D) {
		child.removeFromParent()
		__children += child
		child._parent = this
		child.transform.parent = this.transform
        invalidateRender()
	}

	operator fun plusAssign(child: View3D) = addChild(child)

	override fun render(ctx: RenderContext3D) {
        fastForEachChild {
			it.render(ctx)
		}
	}

    //override fun <T : TEvent<T>> dispatchChildren(type: EventType<T>, event: T, result: EventResult?) {
    //    // @TODO: What if we mutate the list now
    //    fastForEachChild {
    //        if (it.onEventCount(type) > 0) it.dispatch(type, event, result)
    //    }
    //}
}

@Korge3DExperimental
fun View3D.removeFromParent() {
    parent?.removeChild(this)
}
