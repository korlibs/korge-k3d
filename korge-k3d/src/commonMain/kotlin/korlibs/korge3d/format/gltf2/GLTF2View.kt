package korlibs.korge3d.format.gltf2

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge3d.*
import korlibs.memory.*

fun Container3D.gltf2View(gltf: GLTF2) = GLTF2View(gltf).addTo(this)

class GLTF2View(var gltf: GLTF2) : Container3D() {
    init {
        for (mesh in gltf.meshes) {
            for (primitive in mesh.primitives) {
                addChild(GLTF2ViewPrimitive(gltf, primitive))
            }
        }
    }
}

class GLTF2ViewPrimitive(val gltf: GLTF2, val primitive: GLTF2.GPrimitive) : View3D() {
    val drawType: AGDrawType get() = primitive.drawType
    val vertexData: AGVertexArrayObject = AGVertexArrayObject(*primitive.attributes.map { attr ->
        val att = when (attr.key) {
            GLTF2.GAttribute.POSITION -> a_Pos
            GLTF2.GAttribute.NORMAL -> a_Normal
            else -> TODO("${attr.key}")
        }
        val accessor = gltf.accessors[attr.value]
        val bufferView = gltf.bufferViews[accessor.bufferView]
        val buffer = bufferView.slice.slice(accessor.byteOffset)

        when (att){
            a_Pos, a_Normal -> {
                check(accessor.componentTType == VarKind.TFLOAT)
                check(accessor.ncomponent == 3)
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
    val vertexCount = indexSlice.sizeInBytes / indexType.sizeInBytes

    companion object {
        val a_Pos: Attribute = Attribute("a_Pos", VarType.Float3, normalized = false, precision = Precision.HIGH, fixedLocation = 0)
        val a_Normal: Attribute = Attribute("a_Normal", VarType.Float3, normalized = false, precision = Precision.HIGH, fixedLocation = 1)

        val PROGRAM = Program(
            VertexShaderDefault {
                SET(out, u_ProjMat * u_ViewMat * vec4(GLTF2ViewPrimitive.a_Pos, 1f.lit))
                //SET(out, vec4(a_Pos["xy"], 0f.lit, 1f.lit))
            },
            FragmentShader {
                SET(out, vec4(1f.lit, 0f.lit, 1f.lit, 1f.lit))
            }
        )
    }

    override fun render(ctx: RenderContext3D) {
        //ctx[DefaultShaders.ProjViewUB].push {
        //    val width = 100f
        //    val height = 100f
        //    it[this.u_ProjMat] = Matrix4.perspective(45.degrees, width / height, 1f, 1000f)
        //    it[this.u_ViewMat] = Matrix4()
        //}

        val program = PROGRAM
        putUniforms(ctx)
        val uniformBlocks = ctx.rctx.createCurrentUniformsRef(program)

        //println("uniformBlocks=$uniformBlocks")

        ctx.ag.draw(
            AGBatch(
                frameBuffer = ctx.rctx.currentFrameBuffer.base,
                frameBufferInfo = ctx.rctx.currentFrameBuffer.info,
                drawType = drawType,
                indexType = indexType,
                indices = indexData,
                vertexData = vertexData,
                program = PROGRAM,
                uniformBlocks = uniformBlocks,
                textureUnits = ctx.rctx.textureUnits.clone(),
                vertexCount = vertexCount,
                drawOffset = 0,
                depthAndFrontFace = DEFAULT_DEPTH_FUNC,
            )
        )
    }
}

// @TODO: starting with 4.0.0-rc2 this should be available
val AGIndexType.sizeInBytes: Int get() = when (this) {
    AGIndexType.UBYTE -> 1
    AGIndexType.USHORT -> 2
    AGIndexType.UINT -> 4
    else -> -4
}

fun createMeshPrimitive(primitive: GLTF2.GPrimitive) {
    AGVertexArrayObject(AGVertexData())
    primitive.attributes.map {
        when (it.key) {
            GLTF2.GAttribute.POSITION -> GLTF2ViewPrimitive.a_Pos
            else -> TODO("${it.key}")
        }
    }
}

