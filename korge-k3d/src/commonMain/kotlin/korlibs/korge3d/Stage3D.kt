package korlibs.korge3d

import korlibs.event.*
import korlibs.image.color.*
import korlibs.korge.render.*
import korlibs.korge.view.*


inline fun Container.scene3D(views: Views3D = Views3D(stage!!.views, this), callback: Stage3D.() -> Unit = {}): Stage3DView {
    val view = Stage3DView(views, this)
    view.addTo(this)
    view.stage3D.apply(callback)
    return view
}


class Views3D(val views: Views, val container: Container)

class Stage3D(val views: Views3D, val viewParent: Stage3DView) : Container3D() {
    override val globalSpeed: Float get() = views.container.globalSpeed.toFloat()

    init {
        changeEventListenerParent(viewParent)
    }
	//var ambientColor: RGBA = Colors.WHITE
    var occlusionStrength: Float = 1f
	var ambientColor: RGBA = Colors.BLACK // No ambient light
	var ambientPower: Float = 0.3f
	var camera: Camera3D = Camera3D.Perspective().apply {
        this.root = this@Stage3D
		//positionLookingAt(0, 1, 10, 0, 0, 0)
	}
}


class Stage3DView(val views: Views3D, val container: Container) : View() {
    val stage3D: Stage3D = Stage3D(views, this)

	private val ctx3D = RenderContext3D()
	override fun renderInternal(ctx: RenderContext) {
		ctx.flush()
		ctx.clear(depth = 1f, clearColor = false)
		//ctx.ag.clear(color = Colors.RED)
		ctx3D.ag = ctx.ag
		ctx3D.rctx = ctx
		ctx3D.projMat.copyFrom(stage3D.camera.getProjMatrix(ctx.backWidth.toFloat(), ctx.backHeight.toFloat()))
		ctx3D.cameraMat.copyFrom(stage3D.camera.transform.matrix)
		ctx3D.ambientColor.setToColorPremultiplied(stage3D.ambientColor).scale(stage3D.ambientPower)
        ctx3D.occlusionStrength = stage3D.occlusionStrength
		ctx3D.cameraMatInv.invert(stage3D.camera.transform.matrix)
		ctx3D.projCameraMat.multiply(ctx3D.projMat, ctx3D.cameraMatInv)
		ctx3D.lights.clear()
		stage3D.foreachDescendant {
			if (it is Light3D) {
				if (it.active) ctx3D.lights.add(it)
			}
		}
		stage3D.render(ctx3D)
	}

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Listeners
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    override fun <T : BEvent> dispatchChildren(type: EventType<T>, event: T, result: EventResult?) {
        stage3D.dispatch(type, event, result)
    }
}
