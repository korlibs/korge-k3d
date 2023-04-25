import korlibs.io.file.std.*
import korlibs.korge.*
import korlibs.korge.gltf.*
import korlibs.korge.scene.*
import korlibs.korge.view.*

suspend fun main() = Korge {
    sceneContainer().changeTo({ MainStage3d() })
}

class MainStage3d : Scene() {
    override suspend fun SContainer.sceneMain() {
        val gltf = resourcesVfs["Box.glb"].readGLTF2()
        println(gltf)
        addChild(GLTF2View(gltf))
    }
}
