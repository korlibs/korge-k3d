package korlibs.korge3d

import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.math.geom.MVector4


fun Container3D.light(
	color: RGBA = Colors.WHITE,
	constantAttenuation: Float = 1.0f,
	linearAttenuation: Float = 0.0f,
	quadraticAttenuation: Float = 0.00111109f,
	callback: Light3D.() -> Unit = {}
) = Light3D(color, constantAttenuation, linearAttenuation, quadraticAttenuation).addTo(this, callback)


open class Light3D(
	var color: RGBA = Colors.WHITE,
	var constantAttenuation: Float = 1.0f,
	var linearAttenuation: Float = 0.0f,
	var quadraticAttenuation: Float = 0.00111109f
) : View3D() {
	internal val colorVec = MVector4()
	internal val attenuationVec = MVector4()

	fun setTo(
		color: RGBA = Colors.WHITE,
		constantAttenuation: Float = 1.0f,
		linearAttenuation: Float = 0.0f,
		quadraticAttenuation: Float = 0.00111109f
	): Light3D {
		this.color = color
		this.constantAttenuation = constantAttenuation
		this.linearAttenuation = linearAttenuation
		this.quadraticAttenuation = quadraticAttenuation
        return this
	}

	override fun render(ctx: RenderContext3D) {
	}
}
