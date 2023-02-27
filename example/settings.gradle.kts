pluginManagement { repositories {  mavenLocal(); mavenCentral(); google(); gradlePluginPortal()  }  }

plugins {
    //id("com.soywiz.kproject.settings") version "0.0.1-SNAPSHOT"
    id("com.soywiz.kproject.settings") version "0.1.2"
}


rootProject.name = "${rootDir.parentFile.name}-example"

//println("rootProject.name=${rootProject.name}")

kproject("./deps")
