plugins {
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":common"))
    compileOnly("org.geysermc.geyser:api:2.2.0-SNAPSHOT")
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("com.google.gson", "cn.lemwood.geyserupdater.libs.gson")
    relocate("org.yaml.snakeyaml", "cn.lemwood.geyserupdater.libs.snakeyaml")
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("extension.yml") {
        expand("version" to project.version)
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
