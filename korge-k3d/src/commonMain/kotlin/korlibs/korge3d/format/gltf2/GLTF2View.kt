package korlibs.korge3d.format.gltf2

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge3d.*
import korlibs.korge3d.material.*
import korlibs.math.geom.*
import korlibs.memory.*

fun Container3D.gltf2View(gltf: GLTF2) = GLTF2View(gltf).addTo(this)

class GLTF2View(var gltf: GLTF2) : Container3D() {
    init {
        for (scene in gltf.scenes) {
            for (node in scene.nodes) {
                addChild(GLTF2ViewNode(gltf, node))
            }
        }
    }
}

class GLTF2ViewNode(val gltf: GLTF2, val node: GLTF2.GNode) : Container3D() {
    init {
        node.mesh?.let { addChild(GLTF2ViewMesh(gltf, gltf.meshes[it])) }
        transform.setMatrix(node.matrix.mutable)
        for (child in node.children) {
            addChild(GLTF2ViewNode(gltf, gltf.nodes[child]))
        }
    }
}

class GLTF2ViewMesh(val gltf: GLTF2, val mesh: GLTF2.GMesh) : Container3D() {
    init {
        for (primitive in mesh.primitives) {
            addChild(GLTF2ViewPrimitive(gltf, primitive))
            //println("primitive=$primitive")
        }
    }
}

class GLTF2ViewPrimitive(val gltf: GLTF2, val primitive: GLTF2.GPrimitive) : ViewWithMaterial3D() {
    val drawType: AGDrawType get() = primitive.drawType
    val vertexData: AGVertexArrayObject = AGVertexArrayObject(*primitive.attributes.map { attr ->
        val att = when (attr.key) {
            GLTF2.GAttribute.POSITION -> Shaders3D.a_pos
            GLTF2.GAttribute.NORMAL -> Shaders3D.a_norm
            GLTF2.GAttribute.TANGENT -> a_Tangent
            GLTF2.GAttribute.TEXCOORD_0 -> Shaders3D.a_tex
            else -> TODO("${attr.key}")
        }
        val accessor = gltf.accessors[attr.value]
        val bufferView = gltf.bufferViews[accessor.bufferView]
        val buffer = bufferView.slice.slice(accessor.byteOffset)

        when (att){
            Shaders3D.a_pos, Shaders3D.a_norm -> {
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
    val indexAccessor = gltf.accessors[primitive.indices]
    val indexType: AGIndexType = indexAccessor.asIndexType()
    val indexDataOffset = 0
    val indexSlice = indexAccessor.bufferView(gltf).slice.slice(indexAccessor.byteOffset)
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

