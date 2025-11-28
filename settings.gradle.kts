pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "GeyserUpdater"
include("common", "paper", "fabric", "velocity", "viaproxy", "geyser-extension")
