package korlibs.korge3d.format.gltf2

import korlibs.crypto.encoding.*
import korlibs.datastructure.*
import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.dynamic.*
import korlibs.io.file.*
import korlibs.io.file.std.*
import korlibs.io.lang.*
import korlibs.io.serialization.json.*
import korlibs.io.serialization.json.Json
import korlibs.io.stream.*
import korlibs.korge3d.material.*
import korlibs.logger.*
import korlibs.math.geom.*
import korlibs.memory.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

data class GLTF2ReadOptions(
    //val ignoreUnknownKeys: Boolean = true,
    val ignoreUnknownKeys: Boolean = false,
) {
    companion object {
        val DEFAULT = GLTF2ReadOptions()
    }
}

suspend fun VfsFile.readGLB(options: GLTF2ReadOptions = GLTF2ReadOptions.DEFAULT): GLTF2 = GLTF2.readGLB(this, options = options)
suspend fun VfsFile.readGLTF2(options: GLTF2ReadOptions = GLTF2ReadOptions.DEFAULT): GLTF2 {
    if (this.extensionLC == "glb") return readGLB(options)
    return GLTF2.readGLTF(this.readString(), null, this, options)
}

@Serializable
data class GLTF2Json(
    val asset: Asset = Asset(),
    val extensionsUsed: List<String> = emptyList(),
    val extensionsRequired: List<String> = emptyList(),
    val scene: Int = -1,
    val images: List<Image> = emptyList(),
    val textures: List<Texture> = emptyList(),
    val scenes: List<Scene> = emptyList(),
    val nodes: List<Node> = emptyList(),
    val meshes: List<Mesh> = emptyList(),
    val skins: List<Skin> = emptyList(),
    val animations: List<Animation> = emptyList(),
    val buffers: List<Buffer> = emptyList(),
    val bufferViews: List<BufferView> = emptyList(),
    val accessors: List<Accessor> = emptyList(),
    val materials: List<Material> = emptyList(),
    val samplers: List<Sampler> = emptyList(),
) {
    @Serializable
    data class Asset(
        val version: String = "2.0",
        val generator: String? = null,
        val copyright: String? = null,
    )
    @Serializable
    data class Scene(
        val name: String? = null,
        val nodes: IntArray = intArrayOf(),
    )
    @Serializable
    data class Node(
        val name: String? = null,
        val skin: Int = -1,
        val mesh: Int = -1,
        val children: IntArray? = null,
        val scale: FloatArray? = null,
        val translation: FloatArray? = null,
        val rotation: FloatArray? = null,
        val matrix: FloatArray? = null,
    )
    @Serializable
    data class Mesh(
        val name: String? = null,
        val primitives: List<Primitive> = emptyList(),
        val weights: FloatArray? = null,
    ) {
        @Serializable
        data class Primitive(
            val attributes: Map<String, Int> = emptyMap(),
            val indices: Int = -1,
            val material: Int = -1,
            val mode: Int = 4,
            val targets: List<Map<String, Int>> = emptyList(),
        )
    }
    @Serializable
    data class Skin(
        val name: String? = null,
        val inverseBindMatrices: Int = -1,
        val joints: IntArray? = null,
        val skeleton: Int = -1,
    )
    @Serializable
    data class Animation(
        val name: String? = null,
        val channels: List<Channel> = emptyList(),
        val samplers: List<Sampler> = emptyList(),
    ) {
        @Serializable
        data class Channel(
            val sampler: Int = -1,
            val target: Target? = null,
        ) {
            @Serializable
            data class Target(
                val node: Int = -1,
                val path: String? = null,
            )
        }
        @Serializable
        data class Sampler(
            val input: Int = -1,
            val interpolation: String = "LINEAR",
            val output: Int = -1,
        )
    }
    @Serializable
    data class Buffer(
        val name: String? = null,
        val uri: String? = null,
        val byteLength: Int = 0,
    )
    @Serializable
    data class BufferView(
        val name: String? = null,
        val buffer: Int = -1,
        val byteLength: Int = 0,
        val byteOffset: Int = 0,
        val target: Int = -1,
        val byteStride: Int = 0,
    )
    @Serializable
    data class Accessor(
        val name: String? = null,
        val bufferView: Int = 0,
        val byteOffset: Int = 0,
        val componentType: Int = 0,
        val count: Int = 0,
        val min: FloatArray = FloatArray(0),
        val max: FloatArray = FloatArray(0),
        val type: String = "SCALAR",
    )
    @Serializable
    data class Material(
        val name: String? = null,
        val alphaMode: String? = null, // OPAQUE
        val doubleSided: Boolean = true,
        val extensions: Map<String, JsonObject> = emptyMap(),
        val emissiveFactor: FloatArray? = null,
        val emissiveTexture: Texture? = null,
        val pbrMetallicRoughness: PBRMetallicRoughness? = null,
        val normalTexture: Texture? = null,
        val occlusionTexture: Texture? = null,
    ) {
        @Serializable
        data class PBRMetallicRoughness(
            val baseColorTexture: Texture? = null,
            val baseColorFactor: FloatArray? = null,
            val metallicFactor: Float = 0f,
            val roughnessFactor: Float = 0f,
            val metallicRoughnessTexture: Texture? = null,
        )
        @Serializable
        data class Texture(
            val scale: Int = -1,
            val index: Int = -1,
            val texCoord: Int = -1,
        )
    }
    @Serializable
    data class Texture(
        val sampler: Int = -1,
        val source: Int = -1,
    )
    @Serializable
    data class Image(
        val name: String? = null,
        val uri: String? = null,
        val bufferView: Int = -1,
        val mimeType: String? = null,
    )
    @Serializable
    data class Sampler(
        val magFilter: Int = -1,
        val minFilter: Int = -1,
        val wrapS: Int = -1,
        val wrapT: Int = -1,
    )
}

// https://github.com/KhronosGroup/glTF/blob/main/specification/2.0/Specification.adoc
// https://github.com/KhronosGroup/glTF-Sample-Models/tree/master/2.0
data class GLTF2(
    val asset: GAsset,
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
    val skins: List<GSkin>,
    val animations: List<GAnimation>,
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
        val mesh: Int?,
        val skin: Int?,
    ) : GElement
    data class GScene(val name: String, val nodes: List<GNode>) : GElement
    data class GAsset(
        val version: String
    )

    data class GBuffer(val byteLength: Int, val uri: String? = null, val data: Buffer) {
        override fun toString(): String = "GBuffer($byteLength, uri=${uri?.substr(0, 128)}, data=${data})"
    }
    data class GBufferView(
        val gbuffer: GBuffer,
        val slice: Buffer,
        val offsetInBuffer: Int,
        val length: Int,
        val byteStride: Int,
        val target: Int,
    )
    data class GTexture(
        val source: Int
    ) : GElement
    data class GSkin(
        val inverseBindMatrices: Int,
        val joints: IntArray,
        val skeleton: Int,
    ) : GElement
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
    data class GMesh(
        val name: String,
        val primitives: List<GPrimitive>,
    ) : Extra by Extra.Mixin()
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

    data class GAnimationChannelTarget(val node: Int, val path: String)
    data class GAnimationChannel(val sampler: Int, val target: GAnimationChannelTarget)
    data class GAnimationSampler(val input: Int, val interpolation: String, val output: Int)
    data class GAnimation(val name: String?, val channels: List<GAnimationChannel>, val samplers: List<GAnimationSampler>)

    companion object {
        val logger = Logger("GLTF2")

        suspend fun readGLB(file: VfsFile, options: GLTF2ReadOptions = GLTF2ReadOptions.DEFAULT): GLTF2 = readGLB(file.readBytes(), file, options)
        suspend fun readGLB(data: ByteArray, file: VfsFile? = null, options: GLTF2ReadOptions = GLTF2ReadOptions.DEFAULT): GLTF2 {
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

            return readGLTF(json, bin, file, options)
        }

        fun resolveUri(parent: VfsFile?, uri: String): VfsFile? {
            val base64Prefix = "data:application/gltf-buffer;base64,"
            if (uri.startsWith(base64Prefix)) {
                return uri.removePrefix(base64Prefix).fromBase64().asMemoryVfsFile("buffer.bin")
            }
            return parent?.get(uri)
        }

        // @TODO: Use kotlinx-serialization
        suspend fun readGLTF(jsonString: String, bin: ByteArray? = null, file: VfsFile? = null, options: GLTF2ReadOptions = GLTF2ReadOptions.DEFAULT): GLTF2 {


            println("options=$options")

            //println("jsonString=$jsonString")

            val json2 = try {
                kotlinx.serialization.json.Json { this.ignoreUnknownKeys = options.ignoreUnknownKeys }
                    .decodeFromString<GLTF2Json>(jsonString)
            } catch (e: Throwable) {
                println("ERROR parsing: $jsonString")
                throw e
            }

            println(json2)

            //println(jsonString)
            val json = Json.parseFastDyn(jsonString)
            val buffers = json["buffers"].list.map {
                val byteLength = it["byteLength"].int
                val uri = it["uri"].toStringOrNull()
                val data = uri?.let { resolveUri(file?.parent, it)?.readBytes() } ?: bin
                GBuffer(byteLength, uri, Buffer(data ?: byteArrayOf()))
            }
            val bufferViews = json["bufferViews"].list.map {
                val bufferIndex = it["buffer"].int
                val byteLength = it["byteLength"].int
                val byteOffset = it["byteOffset"].int
                val byteStride = it["byteStride"].int
                val target = it["target"].int
                val buffer = buffers[bufferIndex]
                val slice = buffer.data.sliceWithSize(byteOffset, byteLength)
                GBufferView(buffer, slice, byteOffset, byteLength, byteStride, target)
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
                val skin = it["skin"].toIntOrNull()
                val mesh = it["mesh"].toIntOrNull()
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
                GNode(name, matrix, children, cameraIndex, mesh = mesh, skin = skin)
            }
            val scenes = json["scenes"].list.map {
                val name = it["name"].str
                val nodes = it["nodes"].list.map { nodes[it.int] }
                GScene(name, nodes)
            }
            val asset = json["asset"].let {
                val version = it["version"].str
                GAsset(version)
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
            val animations = json["animations"].list.map {
                val name = it["name"].toStringOrNull()
                val channels = it["channels"].list.map {
                    val sampler = it["sampler"].int
                    val target = it["target"].let {
                        val node = it["node"].int
                        val path = it["path"].str
                        GAnimationChannelTarget(node, path)
                    }
                    GAnimationChannel(sampler, target)
                }
                val samplers = it["samplers"].list.map {
                    val input = it["input"].int
                    val interpolation = it["interpolation"].str
                    val output = it["output"].int
                    GAnimationSampler(input, interpolation, output)
                }
                GAnimation(name, channels, samplers)
            }
            val skins = json["skins"].list.map {
                val inverseBindMatrices = it["inverseBindMatrices"].int
                val joints = it["joints"].intArray
                val skeleton = it["skeleton"].int
                GSkin(inverseBindMatrices, joints, skeleton)
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
                        resolveUri(file?.parent, it.uri)?.readBitmap()
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

            if (asset.version != "2.0") {
                println("!! WARNING: Unsupported glTF2 version ${asset.version}")
            }
            println("asset=$asset")
            println("buffers[${buffers.size}]=$buffers")
            println("bufferViews[${bufferViews.size}]=$bufferViews")
            println("scenes[${scenes.size}]=$scenes")
            println("cameras[${cameras.size}]=$cameras")
            println("accessors[${accessors.size}]=$accessors")
            println("meshes[${meshes.size}]=$meshes")
            println("materials[${materials.size}]=$materials")
            println("materials3D[${materials3D.size}]=$materials3D")
            println("skins[${skins.size}]=$skins")
            println("animations[${animations.size}]=$animations")
            return GLTF2(
                asset,
                buffers,
                bufferViews,
                scenes,
                cameras,
                accessors,
                meshes,
                materials,
                materials3D,
                nodes,
                skins,
                animations
            )
        }
    }
}
