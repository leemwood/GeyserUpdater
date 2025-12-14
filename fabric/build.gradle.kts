plugins {
    id("fabric-loom")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":common"))
    minecraft("com.mojang:minecraft:1.21")
    mappings("net.fabricmc:yarn:1.21+build.1:v2")
    modImplementation("net.fabricmc:fabric-loader:0.15.11")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.100.0+1.21")
}
