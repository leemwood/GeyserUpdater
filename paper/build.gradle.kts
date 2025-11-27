plugins {
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":common"))
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("com.google.gson", "cn.lemwood.geyserupdater.libs.gson")
    relocate("org.yaml.snakeyaml", "cn.lemwood.geyserupdater.libs.snakeyaml")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
