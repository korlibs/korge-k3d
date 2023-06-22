package korlibs.korge3d

import korlibs.datastructure.FastArrayList
import korlibs.datastructure.Pool
import korlibs.graphics.*
import korlibs.korge.render.AgBitmapTextureManager
import korlibs.korge.render.AgBufferManager
import korlibs.korge.render.RenderContext
import korlibs.korge3d.material.*
import korlibs.math.geom.*


class RenderContext3D() {
    lateinit var ag: AG
    lateinit var rctx: RenderContext
    val shaders = Shaders3D()
    val bindMat4 = MMatrix3D()
    val bones = Array(128) { MMatrix3D() }
    val tempMat = MMatrix3D()
    val projMat: MMatrix3D = MMatrix3D()
    val lights = arrayListOf<Light3D>()
    val projCameraMat: MMatrix3D = MMatrix3D()
    val cameraMat: MMatrix3D = MMatrix3D()
    val cameraMatInv: MMatrix3D = MMatrix3D()
    val dynamicVertexBufferPool = Pool { AGBuffer() }
    val dynamicVertexDataPool = Pool { AGVertexData() }
    val dynamicIndexBufferPool = Pool { AGBuffer() }
    val ambientColor: MVector4 = MVector4()
    var occlusionStrength: Float = 1f
    val textureManager by lazy { AgBitmapTextureManager(ag) }
    val bufferManager by lazy { AgBufferManager(ag) }
    var depthAndFrontFace = AGDepthAndFrontFace.DEFAULT
        .withDepthFunc(depthFunc = AGCompareMode.LESS)
        .withDepth(0f, 1f)

    val tempAgVertexData = FastArrayList<AGVertexData>()

    inline fun useDynamicVertexData(vertexBuffers: FastArrayList<BufferWithVertexLayout>, block: (AGVertexArrayObject) -> Unit) {
        dynamicVertexDataPool.allocMultiple(vertexBuffers.size, tempAgVertexData) { vertexData ->
            for (n in 0 until vertexBuffers.size) {
                val bufferWithVertexLayout = vertexBuffers[n]
                vertexData[n].buffer.upload(bufferWithVertexLayout.buffer)
                vertexData[n].layout = bufferWithVertexLayout.layout
            }
            block(AGVertexArrayObject(vertexData))
        }
    }
}
