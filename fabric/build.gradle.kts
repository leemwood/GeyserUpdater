plugins {
    id("fabric-loom")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":common"))
    "shadow"(project(":common"))
    minecraft("com.mojang:minecraft:1.21")
    mappings("net.fabricmc:yarn:1.21+build.1:v2")
    modImplementation("net.fabricmc:fabric-loader:0.15.11")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.100.0+1.21")
}

tasks.shadowJar {
    archiveClassifier.set("dev-shadow") // Loom remapJar will consume this?
    // Loom handles remapping.
    // We want the final jar to include 'common' and 'gson' etc.
    // So we need to shadow 'common' into the mod jar.
    configurations = listOf(project.configurations.getByName("shadow"))
    
    relocate("com.google.gson", "cn.lemwood.geyserupdater.libs.gson")
    relocate("org.yaml.snakeyaml", "cn.lemwood.geyserupdater.libs.snakeyaml")
}

tasks.remapJar {
    inputFile.set(tasks.shadowJar.get().archiveFile)
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.remapJar)
}
