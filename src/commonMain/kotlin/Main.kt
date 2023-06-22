import korlibs.image.color.*
import korlibs.korge.*
import korlibs.korge.scene.*
import korlibs.render.*

suspend fun main() = Korge(backgroundColor = Colors["#3f3f3f"], quality = GameWindow.Quality.QUALITY) {
    views.injector
        .mapPrototype { MainStage3d() }
        .mapPrototype { PhysicsScene() }
        .mapPrototype { CratesScene() }
        .mapPrototype { MonkeyScene() }
        .mapPrototype { SkinningScene() }

    sceneContainer().changeTo({ MainStage3d() })
}
