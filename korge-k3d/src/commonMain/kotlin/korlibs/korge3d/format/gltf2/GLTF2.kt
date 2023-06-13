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
import korlibs.korge3d.*
import korlibs.korge3d.material.*
import korlibs.korge3d.util.*
import korlibs.logger.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*
import korlibs.memory.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

suspend fun VfsFile.readGLB(options: GLTF2.ReadOptions = GLTF2.ReadOptions.DEFAULT): GLTF2 = GLTF2.readGLB(this, options = options)
suspend fun VfsFile.readGLTF2(options: GLTF2.ReadOptions = GLTF2.ReadOptions.DEFAULT): GLTF2 {
    if (this.extensionLC == "glb") return readGLB(options)
    return GLTF2.readGLTF(this.readString(), null, this, options)
}

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
                doubleSided = gmat.doubleSided,
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
        /** The glTF version in the form of `<major>.<minor>` that this asset targets. */
        val version: String = "2.0",
        /** Tool that generated this glTF model.  Useful for debugging. */
        val generator: String? = null,
        /** A copyright message suitable for display to credit the content creator. */
        val copyright: String? = null,
        /** The minimum glTF version in the form of `<major>.<minor>` that this asset targets. This property **MUST NOT** be greater than the asset version. */
        val minVersion: String? = null,
    ) : Base()
    @Serializable
    data class Scene(
        val name: String? = null,
        /** The indices of each root node */
        val nodes: IntArray = intArrayOf(),
    ) : Base()
    @Serializable
    data class Node(
        val name: String? = null,
        /** The index of the camera referenced by this node. */
        val camera: Int = -1,
        /** The index of the skin referenced by this node. When a skin is referenced by a node within a scene, all joints used by the skin **MUST** belong to the same scene. When defined, `mesh` **MUST** also be defined. */
        val skin: Int = -1,
        /** The index of the mesh in this node. */
        val mesh: Int = -1,
        /** The indices of this node's children. */
        val children: IntArray? = null,
        /** The node's non-uniform scale, given as the scaling factors along the x, y, and z axes. */
        val scale: FloatArray? = null,
        /** The node's translation along the x, y, and z axes. */
        val translation: FloatArray? = null,
        /** The node's unit quaternion rotation in the order (x, y, z, w), where w is the scalar. */
        val rotation: FloatArray? = null,
        /** A floating-point 4x4 transformation matrix stored in column-major order. */
        val matrix: FloatArray? = null,
        /** The weights of the instantiated morph target. The number of array elements **MUST** match the number of morph targets of the referenced mesh. When defined, `mesh` **MUST** also be defined. */
        val weights: IntArray? = null,
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
        /** An array of primitives, each defining geometry to be rendered. */
        val primitives: List<Primitive> = emptyList(),
        /** Array of weights to be applied to the morph targets. The number of array elements **MUST** match the number of morph targets. */
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
        val isJoints0: Boolean get() = str == "JOINTS_0"
        val isWeights0: Boolean get() = str == "WEIGHTS_0"
    }
    @Serializable
    data class Primitive(
        /** A plain JSON object, where each key corresponds to a mesh attribute semantic and each value is the index of the accessor containing attribute's data. */
        val attributes: Map<PrimitiveAttribute, Int> = emptyMap(),
        /** The index of the accessor that contains the vertex indices.  When this is undefined, the primitive defines non-indexed geometry.  When defined, the accessor **MUST** have `SCALAR` type and an unsigned integer component type. */
        val indices: Int = -1,
        /** The index of the material to apply to this primitive when rendering. */
        val material: Int = -1,
        /** The topology type of primitives to render. */
        val mode: Int = 4,
        /** An array of morph targets. Morph targets: one per morphing weight. Typically, 4 as max for standard. A plain JSON object specifying attributes displacements in a morph target, where each key corresponds to one of the three supported attribute semantic (`POSITION`, `NORMAL`, or `TANGENT`) and each value is the index of the accessor containing the attribute displacements' data. */
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
        /** The index of the accessor containing the floating-point 4x4 inverse-bind matrices. */
        val inverseBindMatrices: Int = -1,
        /** Indices of skeleton nodes, used as joints in this skin. */
        val joints: IntArray? = null,
        /** he index of the node used as a skeleton root. */
        val skeleton: Int = -1,
    ) : Base() {
        fun inverseBindMatricesAccessor(gltf: GLTF2): Accessor = gltf.accessors[inverseBindMatrices]
        fun skeletonNode(gltf: GLTF2): Node = gltf.nodes[skeleton]
    }
    @Serializable
    data class Animation(
        val name: String? = null,
        /** An array of animation channels. An animation channel combines an animation sampler with a target property being animated. Different channels of the same animation **MUST NOT** have the same targets. */
        val channels: List<Channel> = emptyList(),
        /** An array of animation samplers. An animation sampler combines timestamps with a sequence of output values and defines an interpolation algorithm. */
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
                val path: TargetPath? = null,
            ) : Base()

            @Serializable(with = TargetPathSerializer::class)
            enum class TargetPath(val key: String) {
                TRANSLATION("translation"),
                ROTATION("rotation"),
                SCALE("scale"),
                WEIGHTS("weights"),
                UNKNOWN("unknown"),
                ;
                companion object {
                    val BY_KEY = values().associateBy { it.key }
                }
            }

            @Serializer(forClass = TargetPath::class)
            object TargetPathSerializer : KSerializer<TargetPath> {
                override val descriptor: SerialDescriptor
                    get() = PrimitiveSerialDescriptor("TargetPath", PrimitiveKind.STRING)
                override fun serialize(encoder: Encoder, value: TargetPath) {
                    encoder.encodeString(value.key)
                }
                override fun deserialize(decoder: Decoder): TargetPath =
                    TargetPath.BY_KEY[decoder.decodeString()] ?: TargetPath.UNKNOWN
            }
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
                return if (times.size > 0) times[times.size - 1, 0] else 0f
            }

            @Transient
            var inputAccessor: GLTF2AccessorVector? = null
            @Transient
            var outputAccessor: GLTF2AccessorVector? = null

            fun inputAccessor(gltf: GLTF2): Accessor = gltf.accessors[input]
            fun outputAccessor(gltf: GLTF2): Accessor = gltf.accessors[output]

            fun times(gltf: GLTF2): GLTF2AccessorVector {
                if (inputAccessor == null) {
                    val accessor = inputAccessor(gltf)
                    inputAccessor = GLTF2AccessorVector(accessor, accessor.bufferSlice(gltf))
                }
                return inputAccessor!!
            }
            fun outputBuffer(gltf: GLTF2): GLTF2AccessorVector {
                if (outputAccessor == null) {
                    val accessor = outputAccessor(gltf)
                    outputAccessor = GLTF2AccessorVector(accessor, accessor.bufferSlice(gltf))
                }
                return outputAccessor!!
            }

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
                val lowIndex = genericBinarySearchLeft(0, times.size) { times[it, 0].compareTo(time) }
                val highIndex = if (lowIndex >= times.size - 1) lowIndex else lowIndex + 1
                out.requestedTime = time
                out.lowIndex = lowIndex
                out.highIndex = highIndex
                out.lowTime = times[lowIndex, 0]
                out.highTime = times[highIndex, 0]
                out.interpolation = interpolation
                return out
            }
            fun doLookup(gltf: GLTF2, time: Float, count: Int = 1): GLTF2AccessorVector {
                val vec = GLTF2AccessorVector(outputAccessor(gltf), count)
                doLookup(gltf, time, vec, count)
                return vec
            }
            fun doLookup(gltf: GLTF2, time: Float, out: GLTF2AccessorVector, count: Int = 1, outIndex: Int = 0) {
                val lookup = lookup(gltf, time)
                val output = outputBuffer(gltf)
                for (n in 0 until count) {
                    out.setInterpolated(
                        outIndex + n,
                        output,
                        lookup.lowIndex * count + n,
                        output,
                        lookup.highIndex * count + n,
                        lookup.ratioClamped
                    )
                }
                //println("lookup.ratioClamped=${lookup.ratioClamped}, lookup.lowIndex=${lookup.lowIndex}, lookup.highIndex=${lookup.highIndex}, out=$out : ${out.accessor}")
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
    enum class AccessorType(
        val ncomponent: Int
    ) {
        SCALAR(1), VEC2(2), VEC3(3), VEC4(4), MAT2(4), MAT3(9), MAT4(16);
    }
    @Serializable
    data class Accessor(
        val name: String? = null,
        /** The index of the buffer view. When undefined, the accessor **MUST** be initialized with zeros; `sparse` property or extensions **MAY** override zeros with actual values. */
        val bufferView: Int = 0,
        /** The offset relative to the start of the buffer view in bytes. */
        val byteOffset: Int = 0,
        /**
         * The datatype of the accessor's components.
         * UNSIGNED_INT type **MUST NOT** be used for any accessor that is not referenced by `mesh.primitive.indices`.
         * `type` parameter of `vertexAttribPointer()`.  The corresponding typed arrays are `Int8Array`, `Uint8Array`, `Int16Array`, `Uint16Array`, `Uint32Array`, and `Float32Array`. */
        val componentType: Int = 0,
        /** Specifies whether integer data values are normalized before usage." */
        val normalized: Boolean = false,
        /** The number of elements referenced by this accessor, not to be confused with the number of bytes or number of components. */
        val count: Int = 1,
        /** Minimum value of each component in this accessor. */
        val min: FloatArray = FloatArray(0),
        /** Maximum value of each component in this accessor. */
        val max: FloatArray = FloatArray(0),
        /** Specifies if the accessor's elements are scalars, vectors, or matrices. */
        val type: AccessorType = AccessorType.SCALAR,
        /** Sparse storage of elements that deviate from their initialization value. */
        //val sparse: Any?,
    ) : Base() {
        val componentTType: VarKind get() = when (componentType) {
            /* 5120 */ 0x1400 -> VarKind.TBYTE // signed byte --- f = max(c / 127.0, -1.0)   --- c = round(f * 127.0)
            /* 5121 */ 0x1401 -> VarKind.TUNSIGNED_BYTE // unsigned byte --- f = c / 255.0  --- c = round(f * 255.0)
            /* 5122 */ 0x1402 -> VarKind.TSHORT // signed short --- f = max(c / 32767.0, -1.0) --- c = round(f * 32767.0)
            /* 5123 */ 0x1403 -> VarKind.TUNSIGNED_SHORT // unsigned short --- f = c / 65535.0 --- c = round(f * 65535.0)
            /* 5124 */ 0x1404 -> VarKind.TINT
            /* 5125 */ 0x1405 -> VarKind.TINT // TODO: TUNSIGNED_INT
            /* 5126 */ 0x1406 -> VarKind.TFLOAT
            else -> TODO("Unsupported componentType=$componentType")
        }
        val ncomponent get() = type.ncomponent
        val bytesPerEntry get() = componentTType.bytesSize * ncomponent
        val requireNormalization: Boolean get() = when (componentTType) {
            VarKind.TBOOL -> false
            VarKind.TBYTE -> true
            VarKind.TUNSIGNED_BYTE -> true
            VarKind.TSHORT -> true
            VarKind.TUNSIGNED_SHORT -> true
            VarKind.TINT -> false
            VarKind.TFLOAT -> false
        }

        fun VarType.Companion.BOOL(count: Int) =
            when (count) { 0 -> VarType.TVOID; 1 -> VarType.Bool1; 2 -> VarType.Bool2; 3 -> VarType.Bool3; 4 -> VarType.Bool4; else -> invalidOp; }
        fun VarType.Companion.MAT(count: Int) =
            when (count) { 0 -> VarType.TVOID; 1 -> VarType.Float1; 2 -> VarType.Mat2; 3 -> VarType.Mat3; 4 -> VarType.Mat4; else -> invalidOp; }

        fun VarType.Companion.gen(kind: VarKind, ncomponent: Int, type: AccessorType): VarType {
            return when (type) {
                AccessorType.MAT2 -> VarType.MAT(2)
                AccessorType.MAT3 -> VarType.MAT(3)
                AccessorType.MAT4 -> VarType.MAT(4)
                else -> when (kind) {
                    VarKind.TBOOL -> VarType.BOOL(ncomponent)
                    VarKind.TBYTE -> VarType.BYTE(ncomponent)
                    VarKind.TUNSIGNED_BYTE -> VarType.UBYTE(ncomponent)
                    VarKind.TSHORT -> VarType.SHORT(ncomponent)
                    VarKind.TUNSIGNED_SHORT -> VarType.USHORT(ncomponent)
                    VarKind.TINT -> VarType.INT(ncomponent)
                    VarKind.TFLOAT -> VarType.FLOAT(ncomponent)
                }
            }
        }

        val varType: VarType = VarType.gen(componentTType, ncomponent, type)
        fun asIndexType(): AGIndexType = when (componentTType) {
            VarKind.TBOOL, VarKind.TBYTE, VarKind.TUNSIGNED_BYTE -> AGIndexType.UBYTE
            VarKind.TSHORT, VarKind.TUNSIGNED_SHORT -> AGIndexType.USHORT
            VarKind.TINT, VarKind.TFLOAT -> AGIndexType.UINT
        }
        fun bufferView(gltf: GLTF2): BufferView = gltf.bufferViews[bufferView]
        fun bufferSlice(gltf: GLTF2): korlibs.memory.Buffer = bufferView(gltf).slice(gltf).slice(byteOffset)
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
        val orthographic: Orthographic? = null,
    ) : Base() {
        @Serializable
        data class Orthographic(
            val xmag: Float,
            val ymag: Float,
            val zfar: Float,
            val znear: Float,
        ) : Base() {
            //fun toCamera(): Camera3D {
            //    return Camera3D.Orthographic(yfov.radians, znear, zfar)
            //}
        }

        @Serializable
        data class Perspective(
            val aspectRatio: Float = 1.5f,
            val yfov: Float = 0.660593f,
            val zfar: Float = 100f,
            val znear: Float = 0.01f,
        ) : Base() {
            fun toCamera(): Camera3D {
                return Camera3D.Perspective(yfov.radians, znear, zfar)
            }
        }
    }

    companion object {
        @Transient
        val EMPTY_BUFFER = korlibs.memory.Buffer(0)

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

inline class GLTF2AccessorVectorMAT4(val vec: GLTF2AccessorVector) {
    val size: Int get() = vec.size
    operator fun get(index: Int): Matrix4 = Matrix4.fromColumns(FloatArray(16) { vec[index, it] })
    operator fun set(index: Int, value: Matrix4) {
        val values = value.copyToColumns()
        for (n in 0 until 16) vec[index, n] = values[n]
    }
    fun toList(): List<Matrix4> = (0 until size).map { this[it] }

    override fun toString(): String = "${toList()}"
}

data class GLTF2AccessorVector(val accessor: GLTF2.Accessor, val buffer: Buffer) {
    constructor(accessor: GLTF2.Accessor, size: Int = 1) : this(accessor, Buffer(accessor.bytesPerEntry * size))
    val dims: Int get() = accessor.ncomponent
    val bytesPerEntry = accessor.bytesPerEntry
    val size: Int get() = buffer.sizeInBytes / bytesPerEntry
    val sizeComponents: Int get() = buffer.sizeInBytes / accessor.componentTType.bytesSize
    fun toVector3(): Vector3 = Vector3.func { if (it < sizeComponents) getLinear(it) else 0f }
    fun toVector4(): Vector4 = Vector4.func { if (it < sizeComponents) getLinear(it) else 0f }

    fun getLinear(index: Int): Float {
        try {
            return when (accessor.componentTType) {
                VarKind.TBYTE -> kotlin.math.max(buffer.i8[index].toFloat() / 127f, -1f)
                VarKind.TBOOL, VarKind.TUNSIGNED_BYTE -> buffer.i8[index].toFloat() / 255f
                VarKind.TSHORT -> kotlin.math.max(buffer.i16[index].toFloat() / 32767f, -1f)
                VarKind.TUNSIGNED_SHORT -> buffer.u16[index] / 65535f
                VarKind.TINT -> buffer.i32[index].toFloat()
                VarKind.TFLOAT -> buffer.f32[index]
            }
        } catch (e: IndexOutOfBoundsException) {
            println("!! ERROR accessing $index of buffer.sizeInBytes=${buffer.sizeInBytes}, dims=$dims, bytesPerEntry=$bytesPerEntry, size=$size, accessor=$accessor")
            throw e
        }
    }

    fun setLinear(index: Int, value: Float) {
        when (accessor.componentTType) {
            VarKind.TBYTE -> buffer.i8[index] = kotlin.math.round(value * 127.0).toInt().toByte()
            VarKind.TBOOL, VarKind.TUNSIGNED_BYTE -> buffer.i8[index] = kotlin.math.round(value * 255.0).toInt().toByte()
            VarKind.TSHORT -> buffer.i16[index] = kotlin.math.round(value * 32767f).toInt().toShort()
            VarKind.TUNSIGNED_SHORT -> buffer.i16[index] = kotlin.math.round(value * 65535f).toInt().toShort()
            VarKind.TINT -> buffer.i32[index] = value.toInt()
            VarKind.TFLOAT -> buffer.f32[index] = value
        }
    }

    operator fun get(index: Int, dim: Int): Float = getLinear(index * dims + dim)
    operator fun set(index: Int, dim: Int, value: Float) {
        setLinear(index * dims + dim, value)
    }

    fun setInterpolated(index: Int, a: GLTF2AccessorVector, aIndex: Int, b: GLTF2AccessorVector, bIndex: Int, ratio: Float) {
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
                    append(this@GLTF2AccessorVector[n, dim])
                }
                append("]")
            }
            append("]")
        }
    }
}

/*
data class GLTF2Vector(val dims: Int, val floats: Float32Buffer) {
    constructor(dims: Int, size: Int = 1) : this(dims, Float32Buffer(size * dims))
    fun checkDims(dims: Int) {
        assert(this.dims == dims) { "Expected ${this.dims} dimensions, but found $dims" }
    }
    fun toVector3(): Vector3 = Vector3.func { if (dims >= it) this[0, it] else 0f }
    fun toVector4(): Vector4 = Vector4.func { if (dims >= it) this[0, it] else 0f }

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

 */

interface GLTF2Holder {
    val gltf: GLTF2
    val GLTF2.Node.childrenNodes: List<GLTF2.Node> get() = this.childrenNodes(gltf) ?: emptyList()
    val GLTF2.Scene.childrenNodes: List<GLTF2.Node> get() = this.childrenNodes(gltf) ?: emptyList()
}

fun GLTF2.Node.childrenNodes(gltf: GLTF2): List<GLTF2.Node>? = this.children?.map { gltf.nodes[it] }
fun GLTF2.Scene.childrenNodes(gltf: GLTF2): List<GLTF2.Node>? = this.nodes?.map { gltf.nodes[it] }
fun GLTF2.Node.mesh(gltf: GLTF2): GLTF2.Mesh = gltf.meshes[this.mesh]
