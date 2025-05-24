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

tasks.register("stripKexeExtension") {
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

            // Create symlink: arkinstall.kexe -> arkinstall so gradle is happy
            Files.createSymbolicLink(oldPath, newPath.fileName)
        }
    }
}

tasks.named("linkDebugExecutableNative") {
    finalizedBy("stripKexeExtension")
}

tasks.named("linkReleaseExecutableNative") {
    finalizedBy("stripKexeExtension")
}