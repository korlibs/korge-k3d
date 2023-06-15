package korlibs.korge3d

import korlibs.datastructure.*
import korlibs.math.geom.*

class Transform3D {
    //var view: View3D? = null

    @PublishedApi
    internal var matrixDirty = false
    @PublishedApi
    internal var transformDirty = false

    companion object {
        private val identityMat = MMatrix3D()
    }

    val viewMatrixUncached: MMatrix3D = MMatrix3D()
        get() {
            val parent = parent?.globalMatrixUncached ?: identityMat
            field.multiply(parent, matrix)
            return field
        }

    fun concatMatrixUpTo(root: Transform3D? = null, including: Boolean = true): Matrix4 {
        if (this == root || parent == null) {
            return if (including) this.matrix.immutable else Matrix4.IDENTITY
        }
        return this.parent!!.concatMatrixUpTo(root, including) * this.matrix.immutable
    }

    fun concatMatrixUpTo(root: View3D? = null, including: Boolean = true): Matrix4 {
        return concatMatrixUpTo(root?.transform, including)
    }

    val globalMatrixUncached: MMatrix3D = MMatrix3D()
        get() {
            val parent = parent?.globalMatrixUncached ?: identityMat
            field.multiply(parent, matrix)
            return field
        }

    val globalMatrix: MMatrix3D
        get() = globalMatrixUncached // @TODO: Cache!

    val matrix: MMatrix3D = MMatrix3D()
        get() {
            if (matrixDirty) {
                matrixDirty = false
                field.setTRS(translation, rotation, scale)
            }
            return field
        }

    var children: ArrayList<Transform3D> = arrayListOf()

    var parent: Transform3D? = null
        set(value) {
            field?.children?.remove(this)
            field = value
            field?.children?.add(this)
        }

    private val _translation = MVector4(0, 0, 0)
    private var _rotation = Quaternion()
    private val _scale = MVector4(1, 1, 1)
    @PublishedApi
    internal var _eulerRotationDirty: Boolean = true

    private fun updateTRS() {
        transformDirty = false
        matrix.getTRS(_translation, Ref(rotation), _scale)
        _eulerRotationDirty = true
        transformDirty = false
    }

    @PublishedApi
    internal fun updateTRSIfRequired(): Transform3D {
        if (transformDirty) updateTRS()
        return this
    }

    val translation: MPosition3D get() = updateTRSIfRequired()._translation
    var rotation: Quaternion
        get() = updateTRSIfRequired()._rotation
        set(value) {
            updateTRSIfRequired()._rotation = value
        }
    val scale: MScale3D get() = updateTRSIfRequired()._scale

    var rotationEuler: EulerRotation
        get() = rotation.toEuler()
        set(value) {
            updateTRSIfRequired()._rotation = value.toQuaternion()
        }

    /////////////////
    /////////////////

    fun setMatrix(mat: MMatrix3D): Transform3D {
        transformDirty = true
        this.matrix.copyFrom(mat)
        return this
    }

    fun setTranslation(x: Float, y: Float, z: Float, w: Float = 1f) = updatingTRS {
        updateTRSIfRequired()
        matrixDirty = true
        translation.setTo(x, y, z, w)
    }
    fun setTranslation(x: Int, y: Int, z: Int, w: Int = 1) = setTranslation(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    fun setRotation(quat: Quaternion) = updatingTRS {
        updateTRSIfRequired()
        matrixDirty = true
        _eulerRotationDirty = true
        rotation = (quat)
    }

    fun setRotation(x: Float, y: Float, z: Float, w: Float) = updatingTRS {
        _eulerRotationDirty = true
        rotation = Quaternion(x, y, z, w)
    }

    fun setRotation(x: Int, y: Int, z: Int, w: Int) = setRotation(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    fun setRotation(euler: EulerRotation) = updatingTRS {
        _eulerRotationDirty = true
        rotationEuler = (euler)
    }

    fun setRotation(x: Angle, y: Angle, z: Angle) = updatingTRS {
        _eulerRotationDirty = true
        rotationEuler = EulerRotation(x, y, z)
    }

    fun setScale(x: Float = 1f, y: Float = 1f, z: Float = 1f, w: Float = 1f) = updatingTRS {
        scale.setTo(x, y, z, w)
    }
    fun setScale(x: Int, y: Int, z: Int, w: Int) = setScale(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    @PublishedApi
    internal inline fun updatingTRS(callback: () -> Unit): Transform3D {
        updateTRSIfRequired()
        matrixDirty = true
        callback()
        return this
    }

    /////////////////
    /////////////////

    @PublishedApi
    internal val UP = MVector4(0f, 1f, 0f)

    @PublishedApi
    internal val tempMat1 = MMatrix3D()
    @PublishedApi
    internal val tempMat2 = MMatrix3D()
    @PublishedApi
    internal val tempVec1 = MVector4()
    @PublishedApi
    internal val tempVec2 = MVector4()

    fun lookAt(tx: Float, ty: Float, tz: Float, up: MVector4 = UP): Transform3D {
        tempMat1.setToLookAt(translation, tempVec1.setTo(tx, ty, tz, 1f), up)
        rotation = Quaternion.fromRotationMatrix(tempMat1.immutable)
        return this
    }
    fun lookAt(tx: Int, ty: Int, tz: Int, up: MVector4 = UP) = lookAt(tx.toFloat(), ty.toFloat(), tz.toFloat(), up)

    //setTranslation(px, py, pz)
    //lookUp(tx, ty, tz, up)
    fun setTranslationAndLookAt(
        px: Float, py: Float, pz: Float,
        tx: Float, ty: Float, tz: Float,
        up: MVector4 = UP
    ): Transform3D = setMatrix(
        matrix.multiply(
            tempMat1.setToTranslation(px, py, pz),
            tempMat2.setToLookAt(tempVec1.setTo(px, py, pz), tempVec2.setTo(tx, ty, tz), up)
        )
    )

    private var tempEuler = EulerRotation()
    fun rotate(x: Angle, y: Angle, z: Angle): Transform3D {
        val re = this.rotationEuler
        tempEuler = EulerRotation(re.x+x,re.y+y, re.z+z)
        setRotation(tempEuler)
        return this
    }

    fun translate(vec:MVector4) : Transform3D {
        this.setTranslation( this.translation.x + vec.x, this.translation.y + vec.y, this.translation.z+vec.z )
        return this
    }

    fun copyFrom(localTransform: Transform3D) {
        this.setMatrix(localTransform.matrix)
    }

    fun setToInterpolated(a: Transform3D, b: Transform3D, t: Float): Transform3D {
        _translation.setToInterpolated(a.translation, b.translation, t.toDouble())
        _rotation = Quaternion.interpolated(a.rotation, b.rotation, t.toFloat())
        _scale.setToInterpolated(a.scale, b.scale, t.toDouble())
        matrixDirty = true
        return this
    }

    override fun toString(): String = "Transform3D(translation=$translation,rotation=$rotation,scale=$scale)"
    fun clone(): Transform3D = Transform3D().setMatrix(this.matrix)
}
