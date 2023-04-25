package korlibs.korge.gltf

import korlibs.graphics.*
import korlibs.korge.render.*
import korlibs.korge.view.*

// @TODO: This should be a View3D
class GLTF2View(var gltf: GLTF2) : Container() {
    init {
        addChild(GLTF2MeshView(GLTF2MeshPrimitive(gltf, gltf.meshes.first().primitives.first())))
    }
    override fun renderInternal(ctx: RenderContext) {
        //GLTF2Mesh()
    }
}

class GLTF2MeshView(var mesh: GLTF2MeshPrimitive) : View() {
    override fun renderInternal(ctx: RenderContext) {
        GLTF2Renderer.drawMesh(ctx, mesh)
    }
}
