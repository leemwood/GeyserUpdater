plugins {
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":common"))
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("com.google.gson", "cn.lemwood.geyserupdater.libs.gson")
    relocate("org.yaml.snakeyaml", "cn.lemwood.geyserupdater.libs.snakeyaml")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
