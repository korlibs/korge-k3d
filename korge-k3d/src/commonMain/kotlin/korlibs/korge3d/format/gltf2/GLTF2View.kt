package korlibs.korge3d.format.gltf2

import korlibs.datastructure.iterators.*
import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge3d.*
import korlibs.korge3d.material.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.memory.*
import korlibs.time.*

fun Container3D.gltf2View(gltf: GLTF2, autoAnimate: Boolean = true) = GLTF2View(gltf, autoAnimate).addTo(this)

// https://github.com/javagl/glTF-Tutorials/blob/master/gltfTutorial/gltfTutorial_019_SimpleSkin.md
class GLTF2ViewSkin(
    val gltf: GLTF2,
    val skin: GLTF2.Skin,
    val view: GLTF2View,
) {
    val inverseBindMatrices = GLTF2AccessorVectorMAT4(gltf.accessors[skin.inverseBindMatrices].accessor(gltf))

    fun getJoints(): Array<Matrix4> {
        val rootJoint = view.nodeToViews[gltf.nodes[skin.joints[0]]]!!
        return (0 until skin.joints.size)
            .map { n ->
                val jointId = skin.joints[n]
                val viewNode = view.nodeToViews[gltf.nodes[jointId]]!!
                //viewNode.transform.matrix.immutable * inverseBindMatrices[n]
                //val transform = viewNode.transform

                //viewNode.transform.globalMatrix

                //(viewNode.transform.viewMatrixUncached.immutable * inverseBindMatrices[n])
                (viewNode.transform.concatMatrixUpTo(rootJoint, including = true) * inverseBindMatrices[n])
                    .also {
                        if (it != Matrix4.IDENTITY) {
                            //println("n=$n, MAT=$it")
                        }
                    }
                //Matrix4.IDENTITY
            }
            .toTypedArray()
    }

    fun putUniforms(ctx: RenderContext3D) {
        ctx.rctx[Shaders3D.Bones4UB].push {
            it[u_BoneMats] = getJoints()
        }
    }

    fun writeFrom(skin0: GLTF2ViewSkin, skin1: GLTF2ViewSkin? = null, ratio: Float = 0f) {
        val targetSkin = this
        val ratio = ratio.clamp01()
        for (jointId in targetSkin.skin.joints) {
            val targetNode = targetSkin.view.nodeToViews[targetSkin.gltf.nodes[jointId]]!!
            val targetNodeName = targetNode.name ?: continue
            val skin0Node = skin0.view.findByName(targetNodeName).firstOrNull()
            val skin1Node = skin1?.view?.findByName(targetNodeName)?.firstOrNull()
            if (skin0Node != null || skin1Node != null) {
                if (skin0Node != null && skin1Node != null) {
                    targetNode.transform.setToInterpolated(
                        skin0Node.transform, skin1Node.transform, ratio,
                        doTranslation = false,
                        doRotation = true,
                        doScale = false
                    )
                }
            }
            //println("viewNode=$viewNodeName, otherViewNode=${otherViewNode.name}")
        }
    }
}

class GLTF2SceneView(override var gltf: GLTF2, val scene: GLTF2.Scene, val rootView: GLTF2View, autoAnimate: Boolean = true) : Container3D(), GLTF2Holder {
    fun addNode(node: GLTF2.Node): GLTF2ViewNode {
        val view = GLTF2ViewNode(gltf, node, rootView)
        addChild(view)
        return view
    }

    val rootNodes = scene.childrenNodes.map { addNode(it) }
}


class GLTF2View(override var gltf: GLTF2, autoAnimate: Boolean = true) : Container3D(), GLTF2Holder {
//class GLTF2View(override var gltf: GLTF2, autoAnimate: Boolean = false) : Container3D(), GLTF2Holder {
    val nodeToViews = LinkedHashMap<GLTF2.Node, GLTF2ViewNode>()
    private val skinsToView =  LinkedHashMap<GLTF2.Skin, GLTF2ViewSkin>()

    val viewSkins by lazy { gltf.skins.map { getViewSkin(it) } }

    fun getViewSkin(skin: GLTF2.Skin): GLTF2ViewSkin {
        return skinsToView.getOrPut(skin) {
            GLTF2ViewSkin(gltf, skin, this)
        }
    }

    init {
        for (skin in gltf.skins) {
            getViewSkin(skin)
        }
    }
    val sceneViews = gltf.scenes.map { GLTF2SceneView(gltf, it, this, autoAnimate).addTo(this) }
    init {
        if (autoAnimate) {
            addUpdater(first = false) {
                //println("updater!")
                updateAnimationDelta(it)
            }
        }
    }

    var currentTime = 0.seconds
    fun updateAnimationDelta(dt: TimeSpan) {
        currentTime += dt
        updateAnimation(currentTime)
    }

    fun updateAnimation(time: TimeSpan) {
        for (animation in gltf.animations) {
            for (channel in animation.channels) {
                val target = channel.target ?: continue
                val node = gltf.nodes[target.node]
                val view = nodeToViews[node] ?: continue
                val sampler = animation.samplers[channel.sampler]
                val maxTime = sampler.maxTime(gltf)
                val currentTime = time.seconds.toFloat()
                val rtime = currentTime % maxTime

                //println("channel=$channel : ${sampler.times(gltf).toFloatArray().toList()}")

                when (target.path) {
                    GLTF2.Animation.Channel.TargetPath.WEIGHTS -> {
                        view.meshView?.primitiveViews?.fastForEach {
                            it.weights = sampler.doLookup(gltf, rtime, GLTF2.Animation.Sampler.LookupKind.NORMAL, it.nweights).toVector4()
                        }
                    }
                    GLTF2.Animation.Channel.TargetPath.ROTATION -> {
                        //println("sampler.doLookup(gltf, rtime).toVector4()=${sampler.doLookup(gltf, rtime).toVector4()}")
                        val vec4 = sampler.doLookup(gltf, rtime, GLTF2.Animation.Sampler.LookupKind.QUATERNION).toVector4()
                        //val quat = Quaternion(Vector4(vec4.w, vec4.x, vec4.y, vec4.z))
                        val quat = Quaternion(vec4)
                        view.rotation(quat.normalized())
                        //println("rot=$quat")
                    }
                    GLTF2.Animation.Channel.TargetPath.TRANSLATION -> {
                        val pos = sampler.doLookup(gltf, rtime, GLTF2.Animation.Sampler.LookupKind.NORMAL).toVector3()
                        view.position(pos)
                        //println("pos=$pos")
                    }
                    GLTF2.Animation.Channel.TargetPath.SCALE -> {
                        val scale = sampler.doLookup(gltf, rtime, GLTF2.Animation.Sampler.LookupKind.NORMAL).toVector3()
                        //println(sampler.outputAccessor)
                        //println(vec3)
                        view.scale(scale)
                        //println("scale=$scale")
                    }
                    else -> {
                        println("Unsupported animation target.path=${target.path}")
                    }
                }
            }
        }
    }

    fun skinForNode(node: GLTF2.Node): GLTF2ViewSkin? =
        if (node.skin >= 0) skinsToView[gltf.skins[node.skin]] else null
}

class GLTF2ViewNode(override val gltf: GLTF2, val node: GLTF2.Node, val view: GLTF2View) : Container3D(), GLTF2Holder {
    init {
        name = node.name
    }
    val skin: GLTF2ViewSkin? = view.skinForNode(node)
    val meshView = if (node.mesh >= 0) GLTF2ViewMesh(gltf, gltf.meshes[node.mesh], this).addTo(this) else null
    val childrenNodes = node.childrenNodes.map { GLTF2ViewNode(gltf, it, view).addTo(this) }
    init {
        transform.setMatrix(node.mmatrix.mutable)
        view.nodeToViews[node] = this
    }
}

class GLTF2ViewMesh(val gltf: GLTF2, val mesh: GLTF2.Mesh, val viewNode: GLTF2ViewNode) : Container3D() {
    val primitiveViews = mesh.primitives.map {
        GLTF2ViewPrimitive(gltf, it, mesh, this).addTo(this)
    }
}

class GLTF2ViewPrimitive(override val gltf: GLTF2, val primitive: GLTF2.Primitive, val mesh: GLTF2.Mesh, val viewMesh: GLTF2ViewMesh) : ViewWithMaterial3D(), GLTF2Holder {
    val nweights get() = primitive.targets.size
    var weights = mesh.weightsVector
    val drawType: AGDrawType get() = primitive.drawType

    fun genAGVertexData(prim: GLTF2.PrimitiveAttribute, index: Int, targetIndex: Int): AGVertexData {
        val accessor = gltf.accessors[index]
        val bufferView = gltf.bufferViews[accessor.bufferView]
        val buffer = bufferView.slice(gltf).sliceBuffer(accessor.byteOffset)
        val att = when {
            prim.isPosition -> Shaders3D.a_pos
            prim.isNormal -> Shaders3D.a_nor
            prim.isTangent -> if (accessor.ncomponent == 3) Shaders3D.a_tan3 else Shaders3D.a_tan
            prim.isColor0 -> Shaders3D.a_col
            prim.isTexcoord(0) -> Shaders3D.a_tex
            prim.isTexcoord(1) -> Shaders3D.a_tex1
            prim.isTexcoord(2) -> Shaders3D.a_tex2
            prim.isTexcoord(3) -> Shaders3D.a_tex3
            prim.isJoints(0) -> Shaders3D.a_joints[0]
            prim.isWeights(0) -> Shaders3D.a_weights[0]
            else -> TODO("${prim}")
        }
        val expectedComponents = when (att) {
            Shaders3D.a_pos, Shaders3D.a_nor -> 3
            Shaders3D.a_tan3 -> 3
            Shaders3D.a_tan -> 4
            Shaders3D.a_col -> 4
            Shaders3D.a_tex -> 2
            Shaders3D.a_tex1 -> 2
            Shaders3D.a_tex2 -> 2
            Shaders3D.a_tex3 -> 2
            Shaders3D.a_joints[0] -> 4
            Shaders3D.a_weights[0] -> 4
            else -> TODO("Unsupported $att")
        }
        check(accessor.ncomponent == expectedComponents) { "$prim in $accessor expected to have $expectedComponents components but had ${accessor.ncomponent}" }

        //when (att) {
        //    Shaders3D.a_pos, Shaders3D.a_nor -> check(accessor.componentTType == VarKind.TFLOAT)
        //    Shaders3D.a_tan, Shaders3D.a_tan3 -> check(accessor.componentTType == VarKind.TFLOAT)
        //    Shaders3D.a_tex -> check(accessor.componentTType == VarKind.TFLOAT)
        //    Shaders3D.a_joints[0] -> check(accessor.componentTType == VarKind.TFLOAT)
        //    Shaders3D.a_weights[0] -> check(accessor.componentTType == VarKind.TFLOAT)
        //    else -> TODO("Unsupported $att")
        //}

        val ratt = when (att) {
            Shaders3D.a_pos -> if (targetIndex < 0) Shaders3D.a_pos else Shaders3D.a_posTarget[targetIndex]
            Shaders3D.a_tan -> if (targetIndex < 0) Shaders3D.a_tan else Shaders3D.a_tanTarget[targetIndex]
            Shaders3D.a_nor -> if (targetIndex < 0) Shaders3D.a_nor else Shaders3D.a_norTarget[targetIndex]
            else -> att
        }

        /*
        val tryeNormalization = when (att) {
            Shaders3D.a_pos, Shaders3D.a_tan, Shaders3D.a_nor -> true
            else -> false
        }

        val normalized = tryeNormalization && accessor.requireNormalization

         */
        val normalized = accessor.normalized
        val stride = accessor.bufferView(gltf).byteStride

        accessor.attachDebugName = prim.str

        //println("genAGVertexData: $prim, componentTType=${accessor.componentTType}, normalized=$normalized")
        return AGVertexData(VertexLayout(ratt.copy(
            type = accessor.varType,
            normalized = normalized
        ), layoutSize = if (stride > 0) stride else null), buffer = AGBuffer().also { it.upload(buffer) })
    }

    val njoins = primitive.attributes.count { it.key.isJoints(0) } * 4

    val vertexData: AGVertexArrayObject = AGVertexArrayObject(
        *primitive.attributes.map { (prim, index) -> genAGVertexData(prim, index, -1) }.toTypedArray(),
        *primitive.targets.flatMapIndexed { targetIndex, map ->
            map.map { (prim, index) -> genAGVertexData(prim, index, targetIndex) }
        }.toTypedArray(),
    ).also {
        //gltf.accessors
        for ((index, accessor) in gltf.accessors.withIndex()) {
            //println("ACCESSOR[$index][${accessor.attachDebugName}]: $accessor : ${accessor.accessor(gltf)}")
        }
    }
    val hasVertexColor = GLTF2.PrimitiveAttribute.COLOR(0) in primitive.attributes

    // @TODO:
    val indexAccessor: GLTF2.Accessor? = primitive.indices.takeIf { it >= 0 }?.let { gltf.accessors[it] }
    val indexType: AGIndexType? = indexAccessor?.asIndexType()
    val indexDataOffset = 0
    val indexSlice = indexAccessor?.bufferSlice(gltf)
    val indexData = indexSlice?.let { slice -> AGBuffer().also { it.upload(slice) } }
    val vertexCount = when {
        indexSlice != null -> indexSlice.sizeInBytes / indexType!!.bytesSize
        else -> gltf.accessors[primitive.attributes[GLTF2.PrimitiveAttribute.POSITION]!!].count
    }

    //val meshMaterial = Material3D(diffuse = Material3D.LightTexture(crateTex))
    //override val material = Material3D(diffuse = Material3D.LightColor(Colors.RED.withAd(0.5)))
    override val material = gltf.materials3D.getOrElse(primitive.material) { PBRMaterial3D.DEFAULT }

    override fun putUniforms(ctx: RenderContext3D) {
        super.putUniforms(ctx)
        ctx.rctx[Shaders3D.WeightsUB].push {
            it[u_Weights] = this@GLTF2ViewPrimitive.weights
        }
        // @TODO: We could probably do this once per mesh/node?
        //println("viewMesh.viewNode.skin=${viewMesh.viewNode.skin}")
        viewMesh.viewNode.skin?.let { viewSkin ->
            viewSkin.putUniforms(ctx)
        }
    }

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
            nweights,
            material,
            material.hasTexture,
            njoins,
            hasVertexColor = hasVertexColor
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
                indexType = indexType ?: AGIndexType.NONE,
                indices = indexData,
                vertexData = vertexData,
                cullFace = if (material.doubleSided) AGCullFace.NONE else AGCullFace.BACK,
                program = program,
                uniformBlocks = ctx.rctx.createCurrentUniformsRef(program),
                textureUnits = textureUnits,
                vertexCount = vertexCount,
                drawOffset = 0,
                depthAndFrontFace = ctx.depthAndFrontFace,
            )
        )
    }

    companion object {
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

