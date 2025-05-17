import korlibs.event.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.korge3d.*
import korlibs.io.file.std.*
import korlibs.math.geom.*

/*
class MainSkybox : Scene() {
    override suspend fun SContainer.sceneMain() {
        scene3D {
            val stage3D = this
            skyBox(resourcesVfs["skybox"].readCubeMap("jpg"))

            val angleSpeed = 1.degrees

            onMouseDrag {
                stage3D.camera.yawRight(angleSpeed * it.deltaDx * 0.1, 1f)
                stage3D.camera.pitchDown(angleSpeed * it.deltaDy * 0.1, 1f)
            }

            keys {
                downFrame(Key.UP) { stage3D.camera.pitchDown(angleSpeed * it.speed, 1f) }
                downFrame(Key.DOWN) { stage3D.camera.pitchUp(angleSpeed * it.speed, 1f) }
                downFrame(Key.RIGHT) { stage3D.camera.yawRight(angleSpeed * it.speed, 1f) }
                downFrame(Key.LEFT) { stage3D.camera.yawLeft(angleSpeed * it.speed, 1f) }
            }
        }
    }

    private val KeyEvent.speed: Float get() = if (shift) 5.0 else 1.0
}
*/
