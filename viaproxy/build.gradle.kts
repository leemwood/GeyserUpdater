plugins {
    id("com.github.johnrengelman.shadow")
}

repositories {
    maven("https://repo.raphimc.net/repository/maven-releases/")
    maven("https://repo.raphimc.net/repository/maven-snapshots/")
}

dependencies {
    implementation(project(":common"))
    compileOnly("net.raphimc:ViaProxy:3.4.2")
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("com.google.gson", "cn.lemwood.geyserupdater.libs.gson")
    relocate("org.yaml.snakeyaml", "cn.lemwood.geyserupdater.libs.snakeyaml")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
