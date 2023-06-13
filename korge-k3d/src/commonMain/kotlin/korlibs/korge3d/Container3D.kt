package korlibs.korge3d

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.event.*

fun Container3D.container3D(block: Container3D.() -> Unit): Container3D = Container3D().addTo(this, block)

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
		child.parent = this
		child.transform.parent = this.transform
        invalidateRender()
	}

	operator fun plusAssign(child: View3D) = addChild(child)

	override fun render(ctx: RenderContext3D) {
        fastForEachChild {
			it.render(ctx)
		}
	}

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Listeners
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private var __tempDispatchChildren: FastArrayList<View3D>? = null
    override fun <T : BEvent> dispatchChildren(type: EventType<T>, event: T, result: EventResult?) {
        if (__tempDispatchChildren == null) __tempDispatchChildren = FastArrayList(children.size)
        __children.fastForEachWithTemp(__tempDispatchChildren!!) {
            it.dispatch(type, event, result)
        }
    }
}


fun View3D.removeFromParent() {
    parent?.removeChild(this)
}
