package korlibs.korge3d

import korlibs.datastructure.iterators.*
import korlibs.graphics.*
import korlibs.korge.view.*
import korlibs.korge3d.material.*
import korlibs.korge3d.util.*
import korlibs.math.geom.*


abstract class View3D : BaseView() {
    val stage3D: Stage3D? get() = if (this is Stage3D) this else this.parent?.stage3D

    //TODO: I don't think that a Camera, Container, Light, ViewWithMesh, Text3D should all have this as supertype
    // they are not all 'types' of View ?

    var active = true
	var id: String? = null
	var name: String? = null
    //var isModelRoot = false
	val transform = Transform3D().also {
        //it.view = this
    }
    var speed: Float = 1f

    open val globalSpeed: Float get() = parent?.globalSpeed?.times(speed) ?: speed

    var blendMode: BlendMode = BlendMode.NONE

	///////

    override fun invalidateRender() {
        val stage3D = root as? Stage3D?
        //println("View3D.invalidateRender: stage3D=$stage3D")
        stage3D?.views?.views?.invalidatedView(this)
    }

    var x: Float
		set(localX) {
            transform.setTranslation(localX, y, z, localW)
            invalidateRender()
        }
		get() = transform.mtranslation.x

	var y: Float
		set(localY) {
            transform.setTranslation(x, localY, z, localW)
            invalidateRender()
        }
		get() = transform.mtranslation.y

	var z: Float
		set(localZ) { transform.setTranslation(x, y, localZ, localW); invalidateRender() }
		get() = transform.mtranslation.z

	var localW: Float
		set(localW) { transform.setTranslation(x, y, z, localW); invalidateRender() }
		get() = transform.mtranslation.w

	///////

	var scaleX: Float
		set(scaleX) { transform.setScale(scaleX, scaleY, scaleZ, localScaleW); invalidateRender() }
		get() = transform.mscale.x

	var scaleY: Float
		set(scaleY) { transform.setScale(scaleX, scaleY, scaleZ, localScaleW); invalidateRender() }
		get() = transform.mscale.y

	var scaleZ: Float
		set(scaleZ) { transform.setScale(scaleX, scaleY, scaleZ, localScaleW); invalidateRender() }
		get() = transform.mscale.z

	var localScaleW: Float
		set(scaleW) { transform.setScale(scaleX, scaleY, scaleZ, scaleW); invalidateRender() }
		get() = transform.mscale.w

	///////

	var rotationX: Angle
		set(rotationX) { transform.setRotation(rotationX, rotationY, rotationZ); invalidateRender() }
		get() = transform.rotationEuler.x

	var rotationY: Angle
		set(rotationY) { transform.setRotation(rotationX, rotationY, rotationZ); invalidateRender() }
		get() = transform.rotationEuler.y

	var rotationZ: Angle
		set(rotationZ) { transform.setRotation(rotationX, rotationY, rotationZ); invalidateRender() }
		get() = transform.rotationEuler.z

	///////

	var rotationQuatX: Float
		set(rotationQuatX) { transform.setRotation(rotationQuatX, rotationQuatY, rotationQuatZ, rotationQuatW); invalidateRender() }
		get() = transform.rotation.x

	var rotationQuatY: Float
		set(rotationQuatY) { transform.setRotation(rotationQuatX, rotationQuatY, rotationQuatZ, rotationQuatW); invalidateRender() }
		get() = transform.rotation.y

	var rotationQuatZ: Float
		set(rotationQuatZ) { transform.setRotation(rotationQuatX, rotationQuatY, rotationQuatZ, rotationQuatW); invalidateRender() }
		get() = transform.rotation.z

	var rotationQuatW: Float
		set(rotationQuatW) { transform.setRotation(rotationQuatX, rotationQuatY, rotationQuatZ, rotationQuatW); invalidateRender() }
		get() = transform.rotation.w

	///////

	internal var _parent: Container3D? = null
    var parent: Container3D?
        get() = _parent
        internal set(value) {
            if (_parent === value) return
            //_parent?.removeChild(this)
            _parent = value
            //_parent?.addChild(this)
            //_stage = _parent?._stage
            //setInvalidateNotifier()
            onParentChanged()
            invalidateRender()
            changeEventListenerParent(value)
        }

    protected open fun onParentChanged() {
    }

    protected open fun prepareExtraModelMatrix(): Matrix4 {
        return Matrix4.IDENTITY
    }


    protected open fun putUniforms(ctx: RenderContext3D) {
        ctx.rctx[DefaultShaders.ProjViewUB].push {
            it[u_ProjMat] = ctx.projCameraMat.immutable
            it[u_ViewMat] = transform.globalMatrix.immutable
            //this[u_ModMat] = tempMat2.multiply(tempMat1.apply { prepareExtraModelMatrix(this) }, modelMat)
        }
        ctx.rctx[Shaders3D.K3DPropsUB].push {
            it[u_NormMat] = Matrix4.IDENTITY
            it[u_ModMat] = prepareExtraModelMatrix() * modelMat.immutable
            it[u_OcclusionStrength] = ctx.occlusionStrength
        }

        ctx.lights.fastForEachWithIndex { index, light: Light3D ->
            ctx.rctx[Shaders3D.lights[index]].push {
                it[u_SourcePos] = light.transform.mtranslation.immutable
                it[u_Color] = light.color
                it[u_Attenuation] = light.attenuationVec.setTo(
                    light.constantAttenuation,
                    light.linearAttenuation,
                    light.quadraticAttenuation
                ).immutable
            }
        }
    }


    open val root: View3D get() = parent?.root ?: this

	val modelMat = MMatrix3D()
	//val position = Vector3D()


	abstract fun render(ctx: RenderContext3D)
}


fun View3D?.foreachDescendant(handler: (View3D) -> Unit) {
	if (this != null) {
		handler(this)
		if (this is Container3D) {
			this.children.fastForEach { child ->
				child.foreachDescendant(handler)
			}
		}
	}
}


inline fun <reified T : View3D> View3D?.findByType() = sequence<T> {
	for (it in descendants()) {
		if (it is T) yield(it)
	}
}

inline fun View3D?.findByName(name: String) = sequence<View3D> {
    for (it in descendants()) {
        if (it.name == name) yield(it)
    }
}

inline fun <reified T : View3D> View3D?.findByTypeWithName(name: String) = sequence<T> {
	for (it in descendants()) {
		if (it is T && it.name == name) yield(it)
	}
}


fun View3D?.descendants(): Sequence<View3D> = sequence<View3D> {
	val view = this@descendants ?: return@sequence
	yield(view)
	if (view is Container3D) {
		view.children.fastForEach {
			yieldAll(it.descendants())
		}
	}
}


operator fun View3D?.get(name: String): View3D? {
	if (this?.id == name) return this
	if (this?.name == name) return this
	if (this is Container3D) {
		this.children.fastForEach {
			val result = it[name]
			if (result != null) return result
		}
	}
	return null
}


fun <T : View3D> T.name(name: String): T {
    this.name = name
    return this
}


fun <T : View3D> T.position(x: Float, y: Float, z: Float, w: Float = 1f): T {
    transform.setTranslation(x, y, z, w)
    invalidateRender()
    return this
}

fun <T : View3D> T.position(x: Int, y: Int, z: Int, w: Int = 1): T = position(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

fun <T : View3D> T.position(pos: Vector3): T = position(pos.x, pos.y, pos.z)
var View3D.position: Vector3
    get() = transform.mtranslation.immutable.toVector3()
    set(value) {
        position(value)
    }

fun <T : View3D> T.rotation(x: Angle = 0.degrees, y: Angle = 0.degrees, z: Angle = 0.degrees): T {
	transform.setRotation(x, y, z)
    invalidateRender()
    return this
}

fun <T : View3D> T.rotation(quat: Quaternion): T {
    transform.setRotation(quat)
    invalidateRender()
    return this
}

fun <T : View3D> T.scale(x: Float = 1f, y: Float = x, z: Float = x, w: Float = 1f): T {
    transform.setScale(x, y, z, w)
    return this
}


inline fun <T : View3D> T.scale(x: Int = 1, y: Int = x, z: Int = x, w: Int = 1): T = scale(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

inline fun <T : View3D> T.scale(v: Vector3): T = scale(v.x, v.y, v.z)
inline fun <T : View3D> T.scale(v: Vector4): T = scale(v.x, v.y, v.z, v.w)


fun <T : View3D> T.lookAt(x: Float, y: Float, z: Float): T {
    transform.lookAt(x, y, z)
    return this
}


inline fun <T : View3D> T.lookAt(x: Int, y: Int, z: Int): T = lookAt(x.toFloat(), y.toFloat(), z.toFloat())

fun <T : View3D> T.positionLookingAt(p: Vector3, t: Vector3): T {
    transform.setTranslationAndLookAt(p.x, p.y, p.z, t.x, t.y, t.z)
    invalidateRender()
    return this
}

fun <T : View3D> T.positionLookingAt(px: Float, py: Float, pz: Float, tx: Float, ty: Float, tz: Float): T {
    transform.setTranslationAndLookAt(px, py, pz, tx, ty, tz)
    invalidateRender()
    return this
}

fun <T : View3D> T.positionLookingAt(px: Int, py: Int, pz: Int, tx: Int, ty: Int, tz: Int): T = positionLookingAt(px.toFloat(), py.toFloat(), pz.toFloat(), tx.toFloat(), ty.toFloat(), tz.toFloat())


fun <T : View3D> T.addTo(container: Container3D): T {
	container.addChild(this)
    return this
}

inline fun <T : View3D> T.addTo(container: Container3D, callback: T.() -> Unit): T = addTo(container).apply(callback)
