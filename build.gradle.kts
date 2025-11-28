plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    id("fabric-loom") version "1.7-SNAPSHOT" apply false
}

// Disable jar task for root project as it's just a container
tasks.jar {
    enabled = false
}

allprojects {
    group = "cn.lemwood"
    version = "1.0.0alpha-1"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://jitpack.io")
        maven("https://repo.velocitypowered.com/snapshots/")
        maven("https://maven.fabricmc.net/")
        maven("https://repo.viaversion.com")
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
    
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    dependencies {
        compileOnly("org.jetbrains:annotations:24.0.0")
    }
}

tasks.register("collectJars") {
    group = "build"
    description = "Collects all platform jars into a dist directory"
    dependsOn(subprojects.map { it.tasks.getByName("build") })

    doLast {
        val distDir = file("dist")
        distDir.deleteRecursively()
        distDir.mkdirs()

        subprojects.forEach { subproject ->
            if (subproject.name == "common") return@forEach

            val libsDir = subproject.buildDir.resolve("libs")
            if (libsDir.exists()) {
                libsDir.listFiles()?.forEach { jarFile ->
                    // Filter out sources jars or raw shadow jars if any, 
                    // we usually want the one without classifier or specific ones.
                    // Given our config:
                    // Paper: classifier ""
                    // Velocity: classifier ""
                    // ViaProxy: classifier ""
                    // Fabric: classifier "" (remapJar)
                    
                    // Copy only if it ends with .jar and doesn't have -dev, -sources, -shadow (unless that's the main one)
                    // Our main jars are named "project-version.jar".
                    if (jarFile.name.endsWith(".jar") && 
                        !jarFile.name.contains("-sources") && 
                        !jarFile.name.contains("-dev") &&
                        !jarFile.name.contains("-plain")) {
                        
                        println("Copying ${jarFile.name} to dist")
                        jarFile.copyTo(distDir.resolve("${subproject.name}-${subproject.version}.jar"), overwrite = true)
                    }
                }
            }
        }
        println("All jars collected in ${distDir.absolutePath}")
    }
}
