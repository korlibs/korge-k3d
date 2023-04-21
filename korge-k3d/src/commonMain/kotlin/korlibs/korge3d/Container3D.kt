package korlibs.korge3d

import korlibs.datastructure.FastArrayList
import korlibs.datastructure.iterators.fastForEach
import korlibs.korev.EventResult
import korlibs.korge.component.Component
import korlibs.korge.component.ComponentType

@Korge3DExperimental
open class Container3D : View3D() {
	val children = arrayListOf<View3D>()

    inline fun fastForEachChild(block: (View3D) -> Unit) {
        children.fastForEach(block)
    }

    fun removeChild(child: View3D) {
		children.remove(child)
        __updateChildListenerCount(child, add = false)
        invalidateRender()
	}

	fun addChild(child: View3D) {
		child.removeFromParent()
		children += child
		child._parent = this
		child.transform.parent = this.transform
        __updateChildListenerCount(child, add = true)
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

    override fun <T : Component> getComponentOfTypeRecursiveChildren(type: ComponentType<T>, out: FastArrayList<T>, results: EventResult?) {
        fastForEachChild {
            val childEventListenerCount = it.getComponentCountInDescendants(type)
            if (childEventListenerCount > 0) {
                it.getComponentOfTypeRecursive(type, out, results)
            }
        }
    }
}
