import korlibs.korge.*
import korlibs.korge.scene.*

suspend fun main() = Korge {
    views.injector
        .mapPrototype { MainStage3d() }
        .mapPrototype { CratesScene() }
        .mapPrototype { MonkeyScene() }
        .mapPrototype { SkinningScene() }


    sceneContainer().changeTo({ MainStage3d() })
}
