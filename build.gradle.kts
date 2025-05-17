import korlibs.korge.gradle.*

plugins {
    alias(libs.plugins.korge)
}

korge {
    id = "org.korge.samples.mymodule"

// To enable all targets at once

    //targetAll()

// To enable targets based on properties/environment variables
    //targetDefault()

// To selectively enable targets

    targetJvm()
    targetJs()
    //targetIos()
    targetAndroid()
    serializationJson()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    add("commonMainApi", project(":deps"))
}
