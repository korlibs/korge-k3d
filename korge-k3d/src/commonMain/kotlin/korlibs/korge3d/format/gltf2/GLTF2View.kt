package korlibs.korge3d.format.gltf2

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge3d.*
import korlibs.korge3d.material.*
import korlibs.math.geom.*
import korlibs.memory.*

fun Container3D.gltf2View(gltf: GLTF2) = GLTF2View(gltf).addTo(this)

class GLTF2View(override var gltf: GLTF2) : Container3D(), GLTF2Holder {
    init {
        for (scene in gltf.scenes) {
            for (node in scene.childrenNodes) {
                addChild(GLTF2ViewNode(gltf, node))
            }
        }
    }
}

class GLTF2ViewNode(override val gltf: GLTF2, val node: GLTF2.Node) : Container3D(), GLTF2Holder {
    init {
        if (node.mesh >= 0) {
            addChild(GLTF2ViewMesh(gltf, gltf.meshes[node.mesh]))
        }
        transform.setMatrix(node.mmatrix.mutable)
        for (child in node.childrenNodes) {
            addChild(GLTF2ViewNode(gltf, child))
        }
    }
}

class GLTF2ViewMesh(val gltf: GLTF2, val mesh: GLTF2.Mesh) : Container3D() {
    init {
        for (primitive in mesh.primitives) {
            addChild(GLTF2ViewPrimitive(gltf, primitive))
            //println("primitive=$primitive")
        }
    }
}

class GLTF2ViewPrimitive(override val gltf: GLTF2, val primitive: GLTF2.Primitive) : ViewWithMaterial3D(), GLTF2Holder {
    val drawType: AGDrawType get() = primitive.drawType
    val vertexData: AGVertexArrayObject = AGVertexArrayObject(*primitive.attributes.map { attr ->
        val attrKey = attr.key
        val att = when {
            attrKey.isPosition -> Shaders3D.a_pos
            attrKey.isNormal -> Shaders3D.a_norm
            attrKey.isTangent -> a_Tangent
            attrKey.isTexcoord0 -> Shaders3D.a_tex
            else -> TODO("${attr.key}")
        }
        val accessor = gltf.accessors[attr.value]
        val bufferView = gltf.bufferViews[accessor.bufferView]
        val buffer = bufferView.slice(gltf).slice(accessor.byteOffset)

        when (att){
            Shaders3D.a_pos, Shaders3D.a_norm -> {
                accessor.componentType
                check(accessor.componentTType == VarKind.TFLOAT)
                check(accessor.ncomponent == 3)
            }
            a_Tangent -> {
                check(accessor.componentTType == VarKind.TFLOAT)
                check(accessor.ncomponent == 4)
            }
            Shaders3D.a_tex -> {
                check(accessor.componentTType == VarKind.TFLOAT)
                check(accessor.ncomponent == 2)
            }
            else -> TODO("Unsupported $att")
        }

        AGVertexData(VertexLayout(att), buffer = AGBuffer().also { it.upload(buffer) })
    }.toTypedArray())

    // @TODO:
    val indexAccessor: GLTF2.Accessor = gltf.accessors[primitive.indices]
    val indexType: AGIndexType = indexAccessor.asIndexType()
    val indexDataOffset = 0
    val indexSlice = indexAccessor.bufferView(gltf).slice(gltf).slice(indexAccessor.byteOffset)
    val indexData = AGBuffer().also { it.upload(indexSlice) }
    val vertexCount = indexSlice.sizeInBytes / indexType.bytesSize

    //val meshMaterial = Material3D(diffuse = Material3D.LightTexture(crateTex))
    //override val material = Material3D(diffuse = Material3D.LightColor(Colors.RED.withAd(0.5)))
    override val material = gltf.materials3D[primitive.material]

    override fun render(ctx: RenderContext3D) {
        //ctx[DefaultShaders.ProjViewUB].push {
        //    val width = 100f
        //    val height = 100f
        //    it[this.u_ProjMat] = Matrix4.perspective(45.degrees, width / height, 1f, 1000f)
        //    it[this.u_ViewMat] = Matrix4()
        //}

        //val program = ctx.shaders.getProgram3D(
        //val program = PROGRAM ?: ctx.shaders.getProgram3D(
        val program = ctx.shaders.getProgram3D(
            ctx.lights.size.clamp(0, 4),
            //mesh.maxWeights,
            0,
            material,
            material.hasTexture
            //mesh.hasTexture
        )
        putUniforms(ctx)

        //println("uniformBlocks=$uniformBlocks")

        //println("drawType=$drawType")
        //println("vertexData=$vertexData")

        ctx.ag.draw(
            AGBatch(
                frameBuffer = ctx.rctx.currentFrameBuffer.base,
                frameBufferInfo = ctx.rctx.currentFrameBuffer.info,
                drawType = drawType,
                indexType = indexType,
                indices = indexData,
                vertexData = vertexData,
                program = program,
                uniformBlocks = ctx.rctx.createCurrentUniformsRef(program),
                textureUnits = textureUnits,
                vertexCount = vertexCount,
                drawOffset = 0,
                depthAndFrontFace = DEFAULT_DEPTH_FUNC,
            )
        )
    }

    companion object {
        val a_Tangent: Attribute = Attribute("a_Tangent", VarType.Float4, normalized = false, precision = Precision.LOW, fixedLocation = 2)
    }
}

//fun createMeshPrimitive(primitive: GLTF2.GPrimitive) {
//    AGVertexArrayObject(AGVertexData())
//    primitive.attributes.map {
//        when (it.key) {
//            GLTF2.GAttribute.POSITION -> Shaders3D.a_pos
//            else -> TODO("${it.key}")
//        }
//    }
//}

