import java.nio.file.Files

plugins {
    kotlin("multiplatform") version "2.1.20"
    id("org.jetbrains.dokka") version "2.0.0"
}

repositories {
    mavenCentral()
}

kotlin {
    linuxX64("native") {
        binaries {
            executable {
                entryPoint = "main"
            }
            all {
                linkerOpts("--as-needed")
            }
        }

        compilations.named("main") {
            cinterops {
                val libparted by creating
                val syslog by creating
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.7.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            }
        }
    }
}

tasks.register("removeKexeExtension") {
    group = "build"
    description = "Remove the .kexe extension"

    doLast {
        val kexeFiles = fileTree(layout.buildDirectory.dir("bin/native/")) {
            include("**/*.kexe")
        }.files

        kexeFiles.forEach { file ->
            val oldPath = file.toPath()

            if (Files.isSymbolicLink(oldPath)) {
                return@forEach
            }

            val newPath = file.toPath().resolveSibling(file.name.removeSuffix(".kexe"))
            file.renameTo(newPath.toFile())

            if (Files.exists(oldPath) || Files.isSymbolicLink(oldPath)) {
                Files.delete(oldPath)
            }

            // Create symlink: arkinstall.kexe -> arkinstall so gradle is happy :)
            Files.createSymbolicLink(oldPath, newPath.fileName)
        }
    }
}

tasks.register("stripReleaseExecutableNative") {
    group = "build"
    description = "Strip symbols from the release executable"

    dependsOn("linkReleaseExecutableNative")

    doLast {
        val outputFile = layout.buildDirectory.file("bin/native/releaseExecutable/arkinstall.kexe").get().asFile
        if (!outputFile.exists()) error("Failed to find path to release executable")
        println("Stripping symbols from ${outputFile.absolutePath}")

        exec {
            commandLine("strip", "--strip-unneeded", outputFile.absolutePath)
        }

    }
}

tasks.named("linkDebugExecutableNative") {
    finalizedBy("removeKexeExtension")
}

tasks.named("linkReleaseExecutableNative") {
    finalizedBy("removeKexeExtension", "stripReleaseExecutableNative")
}