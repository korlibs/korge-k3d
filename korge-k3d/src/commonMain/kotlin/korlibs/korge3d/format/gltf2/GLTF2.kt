package korlibs.korge3d.format.gltf2

import korlibs.datastructure.*
import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.dynamic.*
import korlibs.io.file.*
import korlibs.io.lang.*
import korlibs.io.serialization.json.*
import korlibs.io.stream.*
import korlibs.korge3d.material.*
import korlibs.logger.*
import korlibs.math.geom.*
import korlibs.memory.*

suspend fun VfsFile.readGLB(): GLTF2 = GLTF2.readGLB(this)
suspend fun VfsFile.readGLTF2(): GLTF2 {
    if (this.extensionLC == "glb") return readGLB()
    return GLTF2.readGLTF(this.readString(), null, this)
}

// https://github.com/KhronosGroup/glTF/blob/main/specification/2.0/Specification.adoc
// https://github.com/KhronosGroup/glTF-Sample-Models/tree/master/2.0
data class GLTF2(
    val buffers: List<GBuffer>,
    // @TODO: We could map bufferViews to Buffer + offset
    val bufferViews: List<GBufferView>,
    val scenes: List<GScene>,
    val cameras: List<GCamera>,
    val accessors: List<GAccessor>,
    val meshes: List<GMesh>,
    val materials: List<GMaterial>,
    val materials3D: List<PBRMaterial3D>,
    val nodes: List<GNode>,
) {

    interface GElement
    open class GBaseElement : GElement, Extra by Extra.Mixin()

    interface GCamera : GElement
    enum class GAttributeKind {
        POSITION, NORMAL, TANGENT, COLOR, TEXCOORD, JOINTS, WEIGHTS;

        companion object {
            val VALUES = values()

            operator fun get(name: String): GAttributeKind {
                return when (name) {
                    "POSITION"-> POSITION
                    "NORMAL"-> NORMAL
                    "TANGENT"-> TANGENT
                    "COLOR"-> COLOR
                    "TEXCOORD"-> TEXCOORD
                    "JOINTS"-> JOINTS
                    "WEIGHTS"-> WEIGHTS
                    else -> TODO("Unsupported '$name'")
                }
            }
        }
    }
    inline class GAttribute private constructor(val data: Int) {
        val kind: GAttributeKind get() = GAttributeKind.VALUES[(data and 0xFF)]
        val index: Int get() = (data ushr 8)

        override fun toString(): String = "GAttribute($kind, $index)"

        constructor(kind: GAttributeKind, index: Int) : this(kind.ordinal or (index shl 8))
        companion object {
            val POSITION = GAttribute(GAttributeKind.POSITION, 0)
            val NORMAL = GAttribute(GAttributeKind.NORMAL, 0)
            val TANGENT = GAttribute(GAttributeKind.TANGENT, 0)
            val TEXCOORD_0 = GAttribute(GAttributeKind.TEXCOORD, 0)
            val TEXCOORD_1 = GAttribute(GAttributeKind.TEXCOORD, 1)

            operator fun invoke(name: String): GAttribute {
                val parts = name.split('_')
                val kind = GAttributeKind[parts[0]]
                return GAttribute(kind, parts.getOrElse(1) { "0" }.toInt())
            }
        }
    }
    data class GPerspectiveCamera(
        val name: String, val aspectRatio: Float, val yfov: Float, val zfar: Float, val znear: Float
    ) : GCamera
    data class GOrthoCamera(
        val name: String, val xmag: Float, val ymag: Float, val zfar: Float, val znear: Float
    ) : GCamera
    data class GNode(
        val name: String,
        val matrix: Matrix4,
        val children: List<Int>,
        val cameraIndex: Int?,
        val mesh: Int?
    ) : GElement
    data class GScene(val name: String, val nodes: List<GNode>) : GElement

    data class GBuffer(val byteLength: Int, val uri: String? = null, val data: Buffer) {
        override fun toString(): String = "GBuffer($byteLength, uri=$uri, data=${data})"
    }
    data class GBufferView(val gbuffer: GBuffer, val slice: Buffer, val offsetInBuffer: Int, val length: Int)
    data class GTexture(val source: Int) : GElement
    data class GImage(
        val uri: String?,
        val bufferView: Int?,
        val mimeType: String?,
    ) : GElement
    data class GAccessor(
        val bufferView: Int,
        val byteOffset: Int,
        val type: String,
        val count: Int,
        val max: FloatArray,
        val min: FloatArray,
        val componentTType: VarKind,
        val ncomponent: Int
    ) : GElement {
        fun asIndexType(): AGIndexType = when (componentTType) {
            VarKind.TBOOL, VarKind.TBYTE, VarKind.TUNSIGNED_BYTE -> AGIndexType.UBYTE
            VarKind.TSHORT, VarKind.TUNSIGNED_SHORT -> AGIndexType.USHORT
            VarKind.TINT, VarKind.TFLOAT -> AGIndexType.UINT
        }

        fun bufferView(gltf: GLTF2): GBufferView = gltf.bufferViews[bufferView]
    }

    data class GPrimitive(
        val attributes: Map<GAttribute, Int>,
        val indices: Int,
        val material: Int,
        val drawType: AGDrawType
    )
    data class GMesh(val name: String, val primitives: List<GPrimitive>) : Extra by Extra.Mixin()
    data class GMaterialTexture(
        val scale: Int, val index: Int, val texCoord: Int
    ) : Extra by Extra.Mixin()

    data class GMaterial(
        val name: String,
        val normalTexture: GMaterialTexture?,
        val emissiveFactor: FloatArray,
        val gMetallicRoughness: GMetallicRoughness
    ) : GBaseElement() {

    }

    data class GMetallicRoughness(
        val baseColorFactor: FloatArray,
        val metallicFactor: Float,
        val roughnessFactor: Float,
        val baseColorTexture: GMaterialTexture?,
        val metallicRoughnessTexture: GMaterialTexture?,
        val occlusionTexture: GMaterialTexture?,
    ) : GElement

    companion object {
        val logger = Logger("GLTF2")

        suspend fun readGLB(file: VfsFile): GLTF2 = readGLB(file.readBytes(), file)
        suspend fun readGLB(data: ByteArray, file: VfsFile? = null): GLTF2 {
            val s = data.openSync()
            if (s.readString(4) != "glTF") error("Not a glTF binary")
            val version = s.readS32LE()
            if (version != 2) error("Not a glTF version 2.0")
            val fileSize = s.readS32LE()
            var json: String = "{}"
            var bin: ByteArray? = null
            while (s.position < fileSize) {
                val chunkSize = s.readS32LE()
                val chunkName = s.readStringz(4)
                val chunkData = s.readStream(chunkSize)
                when (chunkName) {
                    "JSON" -> json = chunkData.toByteArray().toString(Charsets.UTF8)
                    "BIN" -> bin = chunkData.toByteArray()
                }
                logger.trace { "CHUNK[$chunkName] = $chunkSize" }
            }

            return readGLTF(json, bin, file)
        }

        // @TODO: Use kotlinx-serialization
        suspend fun readGLTF(jsonString: String, bin: ByteArray? = null, file: VfsFile? = null): GLTF2 {
            //println(jsonString)
            val json = Json.parseFastDyn(jsonString)
            val buffers = json["buffers"].list.map {
                val byteLength = it["byteLength"].int
                val uri = it["uri"].toStringOrNull()
                val data = uri?.let { file?.parent?.get(it)?.readBytes() } ?: bin
                GBuffer(byteLength, uri, Buffer(data ?: byteArrayOf()))
            }
            val bufferViews = json["bufferViews"].list.map {
                val bufferIndex = it["buffer"].int
                val byteLength = it["byteLength"].int
                val byteOffset = it["byteOffset"].int
                val buffer = buffers[bufferIndex]
                val slice = buffer.data.sliceWithSize(byteOffset, byteLength)
                GBufferView(buffer, slice, byteOffset, byteLength)

            }
            val cameras: List<GCamera> = json["cameras"].list.map {
                val name = it["name"].str
                val type = it["type"].str
                when (type) {
                    "perspective" -> {
                        val perspective = it["perspective"]
                        GPerspectiveCamera(
                            name,
                            perspective["aspectRatio"].float,
                            perspective["yfov"].float,
                            perspective["zfar"].float,
                            perspective["znear"].float,
                        )
                    }
                    "orthographic" -> {
                        val orthographic = it["orthographic"]
                        GOrthoCamera(
                            name,
                            orthographic["xmag"].float,
                            orthographic["ymag"].float,
                            orthographic["zfar"].float,
                            orthographic["znear"].float,
                        )
                    }
                    else -> TODO("Unsupported camera type '$type'")
                }
            }
            val nodes: List<GNode> = json["nodes"].list.map {
                val name = it["name"].str
                val children = it["children"].list.map { it.int }
                val cameraIndex = it["camera"].toIntOrNull()
                var matrix: Matrix4 = Matrix4.IDENTITY
                if (it.contains("matrix")) { // column-major order
                    matrix = Matrix4.fromColumns(it["matrix"].floatArray)
                }
                if (it.contains("translation")) {
                    val v = it["translation"].floatArray
                    matrix *= Matrix4.translation(v[0], v[1], v[2])
                }
                if (it.contains("rotation")) {
                    val v = it["rotation"].floatArray
                    matrix *= Quaternion(v[0], v[1], v[2], v[3]).toMatrix()
                }
                if (it.contains("scale")) {
                    val v = it["scale"].floatArray
                    matrix *= Matrix4.scale(v[0], v[1], v[2])
                }
                GNode(name, matrix, children, cameraIndex, it["mesh"].toIntOrNull())
            }
            val scenes = json["scenes"].list.map {
                val name = it["name"].str
                val nodes = it["nodes"].list.map { nodes[it.int] }
                GScene(name, nodes)
            }
            val accessors: List<GAccessor> = json["accessors"].list.map {
                val bufferView = it["bufferView"].int
                val byteOffset = it["byteOffset"].int
                val componentType = it["componentType"].int
                val type = it["type"].str
                val count = it["count"].int
                val max = it["max"].floatArray
                val min = it["min"].floatArray
                if (it["sparse"].isNotNull) error("Unsupported sparse accessors: $it")
                val ncomponent = when (type) {
                    "SCALAR" -> 1
                    "VEC2" -> 2
                    "VEC3" -> 3
                    "VEC4" -> 4
                    "MAT2" -> 4
                    "MAT3" -> 9
                    "MAT4" -> 16
                    else -> TODO("Unsupported type=$type")
                }
                val componentTType = when (componentType) {
                    0x1400 -> VarKind.TBYTE
                    0x1401 -> VarKind.TUNSIGNED_BYTE
                    0x1402 -> VarKind.TSHORT
                    0x1403 -> VarKind.TUNSIGNED_SHORT
                    0x1404 -> VarKind.TINT
                    0x1405 -> VarKind.TINT // TODO: TUNSIGNED_INT
                    0x1406 -> VarKind.TFLOAT
                    else -> TODO("Unsupported componentType=$componentType")
                }

                GAccessor(
                    bufferView, byteOffset, type, count, max, min, componentTType, ncomponent
                )
            }
            val meshes = json["meshes"].list.map {
                val name = it["name"].str
                println("mesh=$it")
                val primitives = it["primitives"].list.map {
                    val indices = it["indices"].int
                    val material = it["material"].int
                    val mode = it["mode"].toIntOrNull() ?: 4
                    val attributes = it["attributes"].map.map {
                        GAttribute(it.key.str) to it.value.int
                    }.toMap()
                    val drawType = when (mode) {
                        0 -> AGDrawType.POINTS
                        1 -> AGDrawType.LINES
                        2 -> AGDrawType.LINE_LOOP
                        3 -> AGDrawType.LINE_STRIP
                        4 -> AGDrawType.TRIANGLES
                        5 -> AGDrawType.TRIANGLE_STRIP
                        6 -> AGDrawType.TRIANGLE_FAN
                        else -> TODO("Unsupported draw mode=$mode")
                    }
                    GPrimitive(attributes, indices, material, drawType)
                }
                GMesh(name, primitives)
            }
            val images = json["images"].list.map {
                val uri = it["uri"].toStringOrNull()
                val bufferView = it["bufferView"].toIntOrNull()
                val mimeType = it["mimeType"].toStringOrNull()
                GImage(uri, bufferView, mimeType)
            }
            val textures = json["textures"].list.map {
                val source = it["source"].int
                GTexture(source)
            }
            fun Dyn.toGMaterialTexture(): GMaterialTexture? {
                if (this.isNull) return null
                val scale = this["scale"].int
                val index = this["index"].int
                val texCoord = this["texCoord"].int // index
                return GMaterialTexture(scale, index, texCoord)
            }

            val materials = json["materials"].list.map {
                val name = it["name"].str
                val normalTexture = it["normalTexture"].toGMaterialTexture()
                val occlusionTexture = it["occlusionTexture"].toGMaterialTexture()
                val pbrMetallicRoughness = it["pbrMetallicRoughness"]
                val emissiveFactor = it["emissiveFactor"].floatArray
                val baseColorFactor = pbrMetallicRoughness["baseColorFactor"].floatArray
                val metallicFactor = pbrMetallicRoughness["metallicFactor"].float
                val roughnessFactor = pbrMetallicRoughness["roughnessFactor"].float
                val baseColorTexture = pbrMetallicRoughness["baseColorTexture"].toGMaterialTexture()
                val metallicRoughnessTexture = pbrMetallicRoughness["metallicRoughnessTexture"].toGMaterialTexture()

                GMaterial(name, normalTexture, emissiveFactor, GMetallicRoughness(
                    baseColorFactor,
                    metallicFactor,
                    roughnessFactor,
                    baseColorTexture,
                    metallicRoughnessTexture,
                    occlusionTexture
                ))
            }

            val bitmaps: List<Bitmap> = images.map {
                (when {
                    it.bufferView != null -> {
                        val bytes = bufferViews[it.bufferView].slice.i8.getArray()
                        bytes.openAsync().readBitmap()
                    }
                    it.uri != null -> {
                        file?.parent?.get(it.uri)?.readBitmap()
                    }
                    else -> null
                } ?: Bitmaps.white.base).also { img ->
                    println("file=$file, it=$it, img=$img")
                }
            }

            suspend fun readTexture(tex: GMaterialTexture): Bitmap {
                return bitmaps[textures[tex.index].source]
            }

            val materials3D: List<PBRMaterial3D> = materials.map { gmat ->
                val gMetallicRoughness = gmat.gMetallicRoughness
                val baseColorFactor = gMetallicRoughness.baseColorFactor
                PBRMaterial3D(
                    diffuse = gMetallicRoughness.baseColorTexture
                        ?.let { PBRMaterial3D.LightTexture(readTexture(it)) }
                        //?.let { null }
                        ?: PBRMaterial3D.LightColor(RGBA.float(
                            baseColorFactor.getOrElse(0) { 1f },
                            baseColorFactor.getOrElse(1) { 1f },
                            baseColorFactor.getOrElse(2) { 1f },
                            baseColorFactor.getOrElse(3) { 1f }
                        )),
                    occlusionTexture = gMetallicRoughness.occlusionTexture?.let { readTexture(it) },
                )
            }

            println("buffers[${buffers.size}]=$buffers")
            println("bufferViews[${bufferViews.size}]=$bufferViews")
            println("scenes[${scenes.size}]=$scenes")
            println("cameras[${cameras.size}]=$cameras")
            println("accessors[${accessors.size}]=$accessors")
            println("meshes[${meshes.size}]=$meshes")
            println("materials[${materials.size}]=$materials")
            println("materials3D[${materials3D.size}]=$materials3D")
            return GLTF2(
                buffers,
                bufferViews,
                scenes,
                cameras,
                accessors,
                meshes,
                materials,
                materials3D,
                nodes
            )
        }
    }
}
