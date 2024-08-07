import java.security.MessageDigest

plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"

    id("io.github.goooler.shadow") version "8.1.7"
}

group = "team.ktusers"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://mvn.bladehunt.net")
}

dependencies {
    testImplementation(kotlin("test"))
}
task<Zip>("compressResourcePack") {
    destinationDirectory.set(layout.projectDirectory.dir("resource_pack").dir("compressed"))
    archiveFileName = "resource_pack_${version}.zip"
    from(layout.projectDirectory.dir("resource_pack").dir("src"))
}

task("generateResourcePack") {
    dependsOn("compressResourcePack")

    doLast {
        val zipFile =
            layout.projectDirectory
                .file("resource_pack/compressed/resource_pack_${project.version}.zip")
                .asFile

        val shaFile =
            layout.projectDirectory
                .file("resource_pack/compressed/resource_pack_${project.version}.sha1")
                .asFile

        val sha1 = MessageDigest.getInstance("SHA-1")
        zipFile.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                sha1.update(buffer, 0, read)
            }
        }

        val sha1Hash = sha1.digest().joinToString("") { "%02x".format(it) }

        shaFile.writeText(sha1Hash)
    }
}

tasks.build { dependsOn("shadowJar") }

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}