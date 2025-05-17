import korlibs.datastructure.iterators.fastForEach
import korlibs.datastructure.values
import korlibs.io.file.std.resourcesVfs
import korlibs.korge.scene.Scene
import korlibs.korge.view.SContainer
import korlibs.korge.view.addUpdater
import korlibs.korge3d.*
import korlibs.korge3d.animation.Animator3D
import korlibs.korge3d.format.readColladaLibrary


class SkinningScene : Scene() {
    override suspend fun SContainer.sceneInit() {
        scene3D {
            //val library = resourcesVfs["model.dae"].readColladaLibrary()
            //val library = resourcesVfs["ball.dae"].readColladaLibrary()
            val library = resourcesVfs["skinning.dae"].readColladaLibrary()
            //val library = resourcesVfs["model_skinned_animated.dae"].readColladaLibrary()
            //val library = resourcesVfs["Fallera.dae"].readColladaLibrary()

            val mainSceneView = library.mainScene.instantiate()
            val cameras = mainSceneView.findByType<Camera3D>()

            val animators = library.animationDefs.values.map { Animator3D(it, mainSceneView) }
            addUpdater { animators.fastForEach { animator -> animator.update(it) } }
            val model = mainSceneView.findByType<ViewWithMesh3D>().first()
            //.rotation(-90.degrees, 90.degrees, 0.degrees)

            val camera1 = cameras.firstOrNull() ?: camera
            val camera2 = cameras.lastOrNull() ?: camera

            camera = camera1.clone()

            this += mainSceneView
            addUpdater {
                //val mainSceneView = mainSceneView
                //println(mainSceneView)

                //println("Camera: ${camera.transform}")
                //println("Model: ${model.transform}")
                //println("Skeleton: ${model.skeleton}")
            }
        }
    }

}
