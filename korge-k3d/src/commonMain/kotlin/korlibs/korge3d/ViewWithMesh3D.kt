package korlibs.korge3d

import korlibs.datastructure.iterators.*
import korlibs.memory.clamp
import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.math.geom.*


inline fun Container3D.mesh(mesh: Mesh3D, callback: ViewWithMesh3D.() -> Unit = {}): ViewWithMesh3D {
    return ViewWithMesh3D(mesh).addTo(this, callback)
}


open class ViewWithMesh3D(
    var mesh: Mesh3D,
    var skeleton: Skeleton3D? = null
) : View3D() {

    //private val uniformValues = AGUniformValues()
    private val rs = AGDepthAndFrontFace.DEFAULT.withDepthFunc(depthFunc = AGCompareMode.LESS_EQUAL)
    //private val rs = AGRenderState(depthFunc = AGCompareMode.ALWAYS)

    private val tempMat1 = MMatrix3D()
    private val tempMat2 = MMatrix3D()
    private val tempMat3 = MMatrix3D()

    protected open fun prepareExtraModelMatrix(): Matrix4 {
        return Matrix4.IDENTITY
    }

    fun setMaterialLight(
        ctx: RenderContext3D,
        uniform: Shaders3D.MaterialUB,
        actual: Material3D.Light
    ) {
        ctx.rctx[uniform].push {
            //println("uniform=$uniform, actual=$actual")
            when (actual) {
                is Material3D.LightColor -> {
                    it[u_color] = actual.colorVec.immutable
                }
                is Material3D.LightTexture -> {
                    ctx.rctx.textureUnits.set(
                        u_texUnit.index,
                        actual.bitmap?.let { ctx.rctx.agBitmapTextureManager.getTextureBase(it).base },
                        AGTextureUnitInfo.DEFAULT.withLinear(true)
                    )
                }
            }
        }
    }

    private val identity = MMatrix3D()
    private val identityInv = identity.clone().invert()

    override fun render(ctx: RenderContext3D) {
        val ag = ctx.ag

        // @TODO: We should have a managed object for index and vertex buffers like Bitmap -> Texture
        // @TODO:   that handles this automatically. So they are released and allocated automatically on the GPU
        ctx.dynamicIndexBufferPool.alloc { indexBuffer ->
            ctx.useDynamicVertexData(mesh.vertexBuffers) { vertexData ->
                indexBuffer.upload(mesh.indexBuffer)
                //tempMat2.invert()
                //tempMat3.multiply(ctx.cameraMatInv, this.localTransform.matrix)
                //tempMat3.multiply(ctx.cameraMatInv, Matrix3D().invert(this.localTransform.matrix))
                //tempMat3.multiply(this.localTransform.matrix, ctx.cameraMat)
                val meshMaterial = mesh.material
                //val program = Shaders3D.PROGRAM_COLOR_3D ?: mesh.program ?: ctx.shaders.getProgram3D(
                val program = mesh.program ?: ctx.shaders.getProgram3D(
                    ctx.lights.size.clamp(0, 4),
                    mesh.maxWeights,
                    meshMaterial,
                    mesh.hasTexture
                )

                ctx.rctx[DefaultShaders.ProjViewUB].push {
                    it[u_ProjMat] = ctx.projCameraMat.immutable
                    it[u_ViewMat] = transform.globalMatrix.immutable
                    //this[u_ModMat] = tempMat2.multiply(tempMat1.apply { prepareExtraModelMatrix(this) }, modelMat)
                }
                ctx.rctx[Shaders3D.K3DPropsUB].push {
                    it[u_NormMat] = Matrix4.IDENTITY
                    it[u_ModMat] = prepareExtraModelMatrix() * modelMat.immutable
                }

                if (meshMaterial != null) {
                    setMaterialLight(ctx, Shaders3D.ambient, meshMaterial.ambient)
                    setMaterialLight(ctx, Shaders3D.diffuse, meshMaterial.diffuse)
                    setMaterialLight(ctx, Shaders3D.emission, meshMaterial.emission)
                    setMaterialLight(ctx, Shaders3D.specular, meshMaterial.specular)
                }

                ctx.lights.fastForEachWithIndex { index, light: Light3D ->
                    val lightColor = light.color
                    ctx.rctx[Shaders3D.lights[index]].push {
                        it[u_SourcePos] = light.transform.translation.immutable
                        it[u_Color] =
                            light.colorVec.setTo(lightColor.rf, lightColor.gf, lightColor.bf, 1f).immutable
                        it[u_Attenuation] = light.attenuationVec.setTo(
                            light.constantAttenuation,
                            light.linearAttenuation,
                            light.quadraticAttenuation
                        ).immutable
                    }
                }

                //println("mesh.vertexCount=${mesh.vertexCount}")

                Shaders3D.apply {
                    ag.draw(
                        frameBuffer = ctx.rctx.currentFrameBuffer,
                        vertexData = vertexData,
                        indices = indexBuffer,
                        indexType = mesh.indexType,
                        drawType = mesh.drawType,
                        program = program,
                        vertexCount = mesh.vertexCount,
                        blending = AGBlending.NONE,
                        uniformBlocks = ctx.rctx.createCurrentUniformsRef(program),
                        textureUnits = ctx.rctx.textureUnits.clone(),
                        depthAndFrontFace = rs,
                        /*
                        uniforms = uniformValues.apply {
                            //this[u_NormMat] = tempMat3.multiply(tempMat2, localTransform.matrix).invert().transpose()
                            this[u_NormMat] = tempMat3.multiply(tempMat2, transform.globalMatrix)//.invert()

                            this[u_Shininess] = meshMaterial?.shininess ?: 0.5f
                            this[u_IndexOfRefraction] = meshMaterial?.indexOfRefraction ?: 1f

                            if (meshMaterial != null) {
                                setMaterialLight(ctx, ambient, meshMaterial.ambient)
                                setMaterialLight(ctx, diffuse, meshMaterial.diffuse)
                                setMaterialLight(ctx, emission, meshMaterial.emission)
                                setMaterialLight(ctx, specular, meshMaterial.specular)
                            }

                            val skeleton = this@ViewWithMesh3D.skeleton
                            val skin = mesh.skin
                            this[u_BindShapeMatrix] = identity
                            this[u_BindShapeMatrixInv] = identity
                            //println("skeleton: $skeleton, skin: $skin")
                            if (skeleton != null && skin != null) {
                                skin.bones.fastForEach { bone ->
                                    val jointsBySid = skeleton.jointsBySid
                                    val joint = jointsBySid[bone.name]
                                    if (joint != null) {
                                        skin.matrices[bone.index].multiply(
                                            joint.transform.globalMatrix,
                                            joint.poseMatrixInv
                                        )
                                    } else {
                                        error("Can't find joint with name '${bone.name}'")
                                    }

                                }
                                this[u_BindShapeMatrix] = skin.bindShapeMatrix
                                this[u_BindShapeMatrixInv] = skin.bindShapeMatrixInv

                                this[u_BoneMats] = skin.matrices
                            }

                            this[u_AmbientColor] = ctx.ambientColor


                        },
                        */
                    )
                }
            }
        }
    }
}
