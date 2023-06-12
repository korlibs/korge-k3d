import korlibs.image.color.*
import korlibs.korge.*
import korlibs.korge.scene.*

suspend fun main() = Korge(backgroundColor = Colors["#3f3f3f"]) {
    views.injector
        .mapPrototype { MainStage3d() }
        .mapPrototype { PhysicsScene() }
        .mapPrototype { CratesScene() }
        .mapPrototype { MonkeyScene() }
        .mapPrototype { SkinningScene() }

    sceneContainer().changeTo({ MainStage3d() })
}
