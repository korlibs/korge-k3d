package korlibs.korge3d

import korlibs.graphics.*

abstract class ViewWithMaterial3D(
) : View3D() {

    abstract val material: Material3D?

    fun setMaterialLight(
        ctx: RenderContext3D,
        uniform: Shaders3D.MaterialUB,
        actual: Material3D.Light
    ) {
        ctx.rctx[uniform].push {
            //println("uniform=$uniform, actual=$actual")
            when (actual) {
                is Material3D.LightColor -> {
                    it[u_color] = actual.color.premultiplied
                }
                is Material3D.LightTexture -> {
                    //println("TEXTURE LIGHT: ${actual.bitmap}")
                    ctx.rctx.textureUnits.set(
                        u_texUnit.index,
                        actual.bitmap?.let { ctx.rctx.agBitmapTextureManager.getTextureBase(it).base },
                        AGTextureUnitInfo.DEFAULT.withLinear(true).withWrap(AGWrapMode.REPEAT)
                    )
                }
            }
        }
    }

    override fun putUniforms(ctx: RenderContext3D) {
        super.putUniforms(ctx)

        val meshMaterial = material

        if (meshMaterial != null) {
            setMaterialLight(ctx, Shaders3D.ambient, meshMaterial.ambient)
            setMaterialLight(ctx, Shaders3D.diffuse, meshMaterial.diffuse)
            setMaterialLight(ctx, Shaders3D.emission, meshMaterial.emission)
            setMaterialLight(ctx, Shaders3D.specular, meshMaterial.specular)

            if (meshMaterial.occlusionTexture != null) {
                ctx.rctx.textureUnits.set(
                    Shaders3D.u_OcclussionTexUnit.index,
                    ctx.rctx.agBitmapTextureManager.getTextureBase(meshMaterial.occlusionTexture).base,
                    AGTextureUnitInfo.DEFAULT.withLinear(true).withWrap(AGWrapMode.REPEAT)
                )
            }
        }
    }

}
