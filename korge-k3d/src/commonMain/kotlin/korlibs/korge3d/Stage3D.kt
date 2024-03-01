package korlibs.korge3d

import korlibs.event.*
import korlibs.graphics.*
import korlibs.image.color.*
import korlibs.korge.render.*
import korlibs.korge.view.*
import korlibs.math.*
import korlibs.math.geom.*


inline fun Container.scene3D(size: Size = unscaledSize, views: Views3D = Views3D(stage!!.views, this), callback: Stage3D.() -> Unit = {}): Stage3DView {
    val view = Stage3DView(views, this, size)
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


class Stage3DView(val views: Views3D, val container: Container, size: Size) : View() {
    override var unscaledSize: Size = size
        set(value) {
            field = value
            invalidateMatrix()
        }
    override fun getLocalBoundsInternal(): Rectangle = Rectangle(Point.ZERO, unscaledSize)

    val stage3D: Stage3D = Stage3D(views, this)

	private val ctx3D = RenderContext3D()
	override fun renderInternal(ctx: RenderContext) {
		ctx.flush()
		ctx.clear(depth = 1f, clearColor = false)
		//ctx.ag.clear(color = Colors.RED)
		ctx3D.ag = ctx.ag
		ctx3D.rctx = ctx
        val viewport = getClippingBounds(ctx).transformed(ctx.viewMat2D).normalized()
        val previousScissor = ctx.batch.scissor.toRectOrNull()
        val finalScissor = if (previousScissor != null) viewport.intersection(previousScissor) else viewport
        ctx3D.scissor = finalScissor.toAGScissor()
        val viewportCenter = viewport.center.toFloat()

        val dx = viewportCenter.x.convertRange(0f, ctx.backWidth.toFloat(), -1f, +1f)
        val dy = viewportCenter.y.convertRange(0f, ctx.backHeight.toFloat(), -1f, +1f)
        val width = viewport.width.toFloat() / ctx.backWidth.toFloat()
        val height = viewport.height.toFloat() / ctx.backHeight.toFloat()
        val transformScale = (width + height) * 0.5f

        val adjust = Matrix4.fromTRS(Vector4F(dx, -dy, 0f, 0f), Quaternion.IDENTITY, Vector4F(transformScale, transformScale, 1f, 1f))

        ctx3D.projMat.multiply(adjust.mutable, stage3D.camera.getProjMatrix(ctx.backWidth.toFloat(), ctx.backHeight.toFloat()))

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
