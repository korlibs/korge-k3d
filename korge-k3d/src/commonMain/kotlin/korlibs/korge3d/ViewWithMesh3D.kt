package korlibs.korge3d

import korlibs.graphics.*
import korlibs.korge3d.material.*
import korlibs.math.*
import korlibs.memory.*

inline fun Container3D.mesh(mesh: Mesh3D, callback: ViewWithMesh3D.() -> Unit = {}): ViewWithMesh3D {
    return ViewWithMesh3D(mesh).addTo(this, callback)
}

open class ViewWithMesh3D(
    var mesh: Mesh3D,
    var skeleton: Skeleton3D? = null
) : ViewWithMaterial3D() {

    override val material: PBRMaterial3D? get() = mesh.material

    override fun render(ctx: RenderContext3D) {
        val ag = ctx.ag

        // @TODO: We should have a managed object for index and vertex buffers like Bitmap -> Texture
        // @TODO:   that handles this automatically. So they are released and allocated automatically on the GPU
        ctx.dynamicIndexBufferPool.alloc { indexBuffer ->
            ctx.useDynamicVertexData(mesh.vertexBuffers) { vertexData ->
                indexBuffer.upload(mesh.indexBuffer)

                val meshMaterial = mesh.material

                val program = mesh.program ?: ctx.shaders.getProgram3D(
                    ctx.lights.size.clamp(0, 4),
                    mesh.maxWeights,
                    meshMaterial,
                    mesh.hasTexture,
                    0,
                    hasVertexColor = false
                )
                putUniforms(ctx)
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
                        blending = blendMode.factors,
                        uniformBlocks = ctx.rctx.createCurrentUniformsRef(program),
                        textureUnits = textureUnits,
                        depthAndFrontFace = ctx.depthAndFrontFace,
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
