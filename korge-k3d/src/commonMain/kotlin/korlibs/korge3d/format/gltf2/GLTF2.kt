package korlibs.korge3d.format.gltf2

import korlibs.crypto.encoding.*
import korlibs.datastructure.*
import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.file.*
import korlibs.io.file.std.*
import korlibs.io.lang.*
import korlibs.io.stream.*
import korlibs.korge3d.material.*
import korlibs.korge3d.util.*
import korlibs.logger.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*
import korlibs.memory.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

suspend fun VfsFile.readGLB(options: GLTF2.ReadOptions = GLTF2.ReadOptions.DEFAULT): GLTF2 = GLTF2.readGLB(this, options = options)
suspend fun VfsFile.readGLTF2(options: GLTF2.ReadOptions = GLTF2.ReadOptions.DEFAULT): GLTF2 {
    if (this.extensionLC == "glb") return readGLB(options)
    return GLTF2.readGLTF(this.readString(), null, this, options)
}

interface GLTF2Holder {
    val gltf: GLTF2
    val GLTF2.Node.childrenNodes: List<GLTF2.Node> get() = this.childrenNodes(gltf) ?: emptyList()
    val GLTF2.Scene.childrenNodes: List<GLTF2.Node> get() = this.childrenNodes(gltf) ?: emptyList()
}

fun GLTF2.Node.childrenNodes(gltf: GLTF2): List<GLTF2.Node>? = this.children?.map { gltf.nodes[it] }
fun GLTF2.Scene.childrenNodes(gltf: GLTF2): List<GLTF2.Node>? = this.nodes?.map { gltf.nodes[it] }
fun GLTF2.Node.mesh(gltf: GLTF2): GLTF2.Mesh = gltf.meshes[this.mesh]

// https://github.com/KhronosGroup/glTF/blob/main/specification/2.0/Specification.adoc
// https://github.com/KhronosGroup/glTF-Sample-Models/tree/master/2.0
// https://github.com/syoyo/tinygltf
@Serializable
data class GLTF2(
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
    val cameras: List<Camera> = emptyList(),
) : Extra by Extra.Mixin(), GLTF2Holder {
    override val gltf: GLTF2 get() = this

    val materials3D: List<PBRMaterial3D> by lazy {
        materials.map { gmat ->
            val gMetallicRoughness = gmat.pbrMetallicRoughness
            val baseColorFactor = gMetallicRoughness.baseColorFactor ?: FloatArray(0)
            PBRMaterial3D(
                diffuse = gMetallicRoughness.baseColorTexture
                    ?.let { PBRMaterial3D.LightTexture(it.getTexture(this)) }
                //?.let { null }
                    ?: PBRMaterial3D.LightColor(
                        RGBA.float(
                        baseColorFactor.getOrElse(0) { 1f },
                        baseColorFactor.getOrElse(1) { 1f },
                        baseColorFactor.getOrElse(2) { 1f },
                        baseColorFactor.getOrElse(3) { 1f }
                    )),
                occlusionTexture = gmat.occlusionTexture?.let { it.getTexture(gltf) },
            )
        }
    }

    private suspend fun ensureLoad(file: VfsFile?, bin: ByteArray?) {
        ensureLoadBuffers(file, bin)
        ensureLoadImages(file)
    }

    private suspend fun ensureLoadImages(file: VfsFile?) {
        //println("$file")
        for (image in images) {
            if (image.bitmap == null) {
                //println("ensureLoadImages: $image")
                val buffer = image.uri?.let { resolveUri(file, it) }
                    ?: (if (image.bufferView >= 0) bufferViews[image.bufferView].slice(this).i8.getArray().asMemoryVfsFile() else null)
                image.bitmap = buffer?.readBitmap() ?: Bitmaps.transparent.bmp
            }
        }
    }
    private suspend fun ensureLoadBuffers(file: VfsFile?, bin: ByteArray?) {
        for (buffer in buffers) {
            if (buffer.optBuffer == null) {
                val bytes = buffer.uri?.let { resolveUri(file, it) }?.readBytes()
                    ?: bin
                    ?: error("Couldn't load buffer : $buffer")
                buffer.optBuffer = Buffer(bytes)
            }
        }
    }

    data class ReadOptions(
        //val ignoreUnknownKeys: Boolean = true,
        val ignoreUnknownKeys: Boolean = false,
    ) {
        companion object {
            val DEFAULT = ReadOptions()
        }
    }


    open class Base : Extra by Extra.Mixin()

    fun resolveUri(file: VfsFile?, uri: String): VfsFile? {
        if (uri.startsWith("data:")) {
            val dataPart = uri.substringBefore(',', "")
            val content = uri.substringAfter(',')
            return content.fromBase64().asMemoryVfsFile("buffer.bin")
        }
        return file?.parent?.get(uri)
    }

    @Serializable
    data class Asset(
        val version: String = "2.0",
        val generator: String? = null,
        val copyright: String? = null,
    ) : Base()
    @Serializable
    data class Scene(
        val name: String? = null,
        val nodes: IntArray = intArrayOf(),
    ) : Base()
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
    ) : Base() {
        val mmatrix by lazy {
            var out: Matrix4 = Matrix4.IDENTITY
            // column-major order
            if (matrix != null) out = Matrix4.fromColumns(matrix)
            if (translation != null) out *= Matrix4.translation(translation[0], translation[1], translation[2])
            if (rotation != null) out *= Quaternion(rotation[0], rotation[1], rotation[2], rotation[3]).toMatrix()
            if (scale != null) out *= Matrix4.scale(scale[0], scale[1], scale[2])
            out
        }
    }
    @Serializable
    data class Mesh(
        val name: String? = null,
        val primitives: List<Primitive> = emptyList(),
        /** Optional default weights for morphing */
        val weights: FloatArray? = null,
    ) : Base() {
        @Transient
        val weightsVector: Vector4 = if (weights != null) Vector4.func { weights.getOrElse(it) { 0f } } else Vector4.ZERO
    }
    @Serializable
    inline class PrimitiveAttribute(val str: String) {
        val isPosition: Boolean get() = str == "POSITION"
        val isNormal: Boolean get() = str == "NORMAL"
        val isTangent: Boolean get() = str == "TANGENT"
        val isTexcoord0: Boolean get() = str == "TEXCOORD_0"
    }
    @Serializable
    data class Primitive(
        val attributes: Map<PrimitiveAttribute, Int> = emptyMap(),
        val indices: Int = -1,
        val material: Int = -1,
        val mode: Int = 4,
        /** Morph targets: one per morphing weight. Typically, 4 as max for standard. */
        val targets: List<Map<PrimitiveAttribute, Int>> = emptyList(),
    ) : Base() {
        val drawType: AGDrawType get() = when (mode) {
            0 -> AGDrawType.POINTS
            1 -> AGDrawType.LINES
            2 -> AGDrawType.LINE_LOOP
            3 -> AGDrawType.LINE_STRIP
            4 -> AGDrawType.TRIANGLES
            5 -> AGDrawType.TRIANGLE_STRIP
            6 -> AGDrawType.TRIANGLE_FAN
            else -> TODO("Unsupported draw mode=$mode")
        }
    }
    @Serializable
    data class Skin(
        val name: String? = null,
        val inverseBindMatrices: Int = -1,
        val joints: IntArray? = null,
        val skeleton: Int = -1,
    ) : Base()
    @Serializable
    data class Animation(
        val name: String? = null,
        val channels: List<Channel> = emptyList(),
        val samplers: List<Sampler> = emptyList(),
    ) : Base() {
        @Serializable
        data class Channel(
            val sampler: Int = -1,
            val target: Target? = null,
        ) : Base() {
            @Serializable
            data class Target(
                val node: Int = -1,
                val path: String? = null,
            ) : Base()
        }
        @Serializable
        data class Sampler(
            /** bufferView index with timestamps */
            val input: Int = -1,
            val interpolation: String = "LINEAR",
            val output: Int = -1,
        ) : Base() {
            fun maxTime(gltf: GLTF2): Float {
                val times = times(gltf)
                return times[times.size - 1]
            }
            fun times(gltf: GLTF2): Float32Buffer = gltf.bufferViews[input].slice(gltf).f32
            fun outputBuffer(gltf: GLTF2, dims: Int): GLTF2Vector = GLTF2Vector(dims, gltf.bufferViews[output].slice(gltf).f32)

            data class Lookup(
                var requestedTime: Float = 0f,
                var lowIndex: Int = 0, var highIndex: Int = 0,
                var lowTime: Float = 0f, var highTime: Float = 0f,
                var interpolation: String = "LINEAR",
            ) {
                val ratio: Float get() = requestedTime.convertRange(lowTime, highTime, 0f, 1f)
                val ratioClamped: Float get() = ratio.clamp01()
            }
            fun lookup(gltf: GLTF2, time: Float, out: Lookup = Lookup()): Lookup {
                val times = times(gltf)
                val lowIndex = genericBinarySearchLeft(0, times.size) { times[it].compareTo(time) }
                val highIndex = if (lowIndex >= times.size - 1) lowIndex else lowIndex + 1
                out.requestedTime = time
                out.lowIndex = lowIndex
                out.highIndex = highIndex
                out.lowTime = times[lowIndex]
                out.highTime = times[highIndex]
                out.interpolation = interpolation
                return out
            }
            fun doLookup(gltf: GLTF2, time: Float, dims: Int): GLTF2Vector {
                val vec = GLTF2Vector(dims, 1)
                doLookup(gltf, time, vec)
                return vec
            }
            fun doLookup(gltf: GLTF2, time: Float, out: GLTF2Vector, outIndex: Int = 0) {
                val lookup = lookup(gltf, time)
                val output = outputBuffer(gltf, out.dims)
                //println("lookup.ratioClamped=${lookup.ratioClamped}, lookup.lowIndex=${lookup.lowIndex}, lookup.highIndex=${lookup.highIndex}")
                out.setInterpolated(outIndex, output, lookup.lowIndex, output, lookup.highIndex, lookup.ratioClamped)
            }
        }
    }
    @Serializable
    data class Buffer(
        val name: String? = null,
        val uri: String? = null,
        val byteLength: Int = 0,
    ) : Base() {
        @Transient
        var optBuffer: korlibs.memory.Buffer? = null
        val buffer: korlibs.memory.Buffer get() = optBuffer ?: error("Buffer not loaded!")

        override fun toString(): String = "Buffer($name, uri=${uri?.substr(0, 100)}, byteLength=$byteLength)"
    }
    @Serializable
    data class BufferView(
        val name: String? = null,
        val buffer: Int = -1,
        val byteLength: Int = 0,
        val byteOffset: Int = 0,
        val target: Int = -1,
        val byteStride: Int = 0,
    ) : Base() {
        fun slice(gltf: GLTF2): korlibs.memory.Buffer =
            gltf.buffers[buffer].buffer.sliceWithSize(byteOffset, byteLength)
    }
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
    ) : Base() {
        val componentTType get() = when (componentType) {
            0x1400 -> VarKind.TBYTE
            0x1401 -> VarKind.TUNSIGNED_BYTE
            0x1402 -> VarKind.TSHORT
            0x1403 -> VarKind.TUNSIGNED_SHORT
            0x1404 -> VarKind.TINT
            0x1405 -> VarKind.TINT // TODO: TUNSIGNED_INT
            0x1406 -> VarKind.TFLOAT
            else -> TODO("Unsupported componentType=$componentType")
        }
        val ncomponent get() = when (type) {
            "SCALAR" -> 1
            "VEC2" -> 2
            "VEC3" -> 3
            "VEC4" -> 4
            "MAT2" -> 4
            "MAT3" -> 9
            "MAT4" -> 16
            else -> TODO("Unsupported type=$type")
        }
        fun asIndexType(): AGIndexType = when (componentTType) {
            VarKind.TBOOL, VarKind.TBYTE, VarKind.TUNSIGNED_BYTE -> AGIndexType.UBYTE
            VarKind.TSHORT, VarKind.TUNSIGNED_SHORT -> AGIndexType.USHORT
            VarKind.TINT, VarKind.TFLOAT -> AGIndexType.UINT
        }
        fun bufferView(gltf: GLTF2): BufferView = gltf.bufferViews[bufferView]
    }
    @Serializable
    data class Material(
        val name: String? = null,
        val alphaMode: String? = null, // OPAQUE
        val doubleSided: Boolean = true,
        val extensions: Map<String, JsonObject> = emptyMap(),
        val emissiveFactor: FloatArray? = null,
        val emissiveTexture: TextureRef? = null,
        val pbrMetallicRoughness: PBRMetallicRoughness = PBRMetallicRoughness(),
        val normalTexture: TextureRef? = null,
        val occlusionTexture: TextureRef? = null,
    ) : Base() {
        @Serializable
        data class PBRMetallicRoughness(
            val baseColorTexture: TextureRef? = null,
            val baseColorFactor: FloatArray? = null,
            val metallicFactor: Float = 0f,
            val roughnessFactor: Float = 0f,
            val metallicRoughnessTexture: TextureRef? = null,
        ) : Base()
        @Serializable
        data class TextureRef(
            val scale: Int = -1,
            val index: Int = -1,
            val texCoord: Int = -1,
        ) : Base() {
            fun getTexture(gltf: GLTF2): Bitmap? =
                gltf.textures[index].getImage(gltf).bitmap
        }
    }
    @Serializable
    data class Texture(
        val sampler: Int = -1,
        val source: Int = -1,
    ) : Base() {
        fun getImage(gltf: GLTF2): Image {
            return gltf.images[source]
        }
    }

    @Serializable
    data class Image(
        val name: String? = null,
        val uri: String? = null,
        val bufferView: Int = -1,
        val mimeType: String? = null,
    ) : Base() {
        @Transient
        var bitmap: Bitmap? = null
    }
    @Serializable
    data class Sampler(
        val magFilter: Int = -1,
        val minFilter: Int = -1,
        val wrapS: Int = -1,
        val wrapT: Int = -1,
    ) : Base()
    @Serializable
    data class Camera(
        val name: String? = null,
        val type: String,
        val perspective: Perspective? = null,
    ) : Base() {
        @Serializable
        data class Perspective(
            val aspectRatio: Float = 1.5f,
            val yfov: Float = 0.660593f,
            val zfar: Float = 100f,
            val znear: Float = 0.01f,
        ) : Base()
    }

    companion object {
        val logger = Logger("GLTF2")

        suspend fun readGLB(file: VfsFile, options: ReadOptions = ReadOptions.DEFAULT): GLTF2 = readGLB(file.readBytes(), file, options)
        suspend fun readGLB(data: ByteArray, file: VfsFile? = null, options: ReadOptions = ReadOptions.DEFAULT): GLTF2 {
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

        // @TODO: Use kotlinx-serialization
        suspend fun readGLTF(jsonString: String, bin: ByteArray? = null, file: VfsFile? = null, options: ReadOptions = ReadOptions.DEFAULT): GLTF2 {
            return try {
                kotlinx.serialization.json.Json { this.ignoreUnknownKeys = options.ignoreUnknownKeys }
                    .decodeFromString<GLTF2>(jsonString)
                    .also { it.ensureLoad(file, bin) }
            } catch (e: Throwable) {
                println("options=$options")
                println("ERROR parsing: $jsonString")
                throw e
            }
        }
    }
}

data class GLTF2Vector(val dims: Int, val floats: Float32Buffer) {
    constructor(dims: Int, size: Int = 1) : this(dims, Float32Buffer(size * dims))
    fun checkDims(dims: Int) {
        assert(this.dims == dims) { "Expected ${this.dims} dimensions, but found $dims" }
    }
    fun toVector4(): Vector4 {
        return Vector4(
            if (dims >= 1) this[0, 0] else 0f,
            if (dims >= 2) this[0, 1] else 0f,
            if (dims >= 3) this[0, 2] else 0f,
            if (dims >= 4) this[0, 3] else 0f,
        )
    }

    val size: Int get() = floats.size / dims
    operator fun get(n: Int, dim: Int): Float = floats[n * dims + dim]
    operator fun set(n: Int, dim: Int, value: Float) { floats[n * dims + dim] = value }

    fun setInterpolated(index: Int, a: GLTF2Vector, aIndex: Int, b: GLTF2Vector, bIndex: Int, ratio: Float) {
        a.checkDims(dims)
        b.checkDims(dims)
        for (dim in 0 until dims) {
            //println("a[aIndex, dim], b[bIndex, dim]=${a[aIndex, dim]} : ${b[bIndex, dim]}")
            this[index, dim] = ratio.toRatio().interpolate(a[aIndex, dim], b[bIndex, dim])
        }
    }

    override fun toString(): String {
        return buildString {
            append("[")
            for (n in 0 until size) {
                if (n != 0) append(", ")
                append("[")
                for (dim in 0 until dims) {
                    if (dim != 0) append(", ")
                    append(this@GLTF2Vector[n, dim])
                }
                append("]")
            }
            append("]")
        }
    }
}
