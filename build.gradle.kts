plugins {
    id("net.mcberry.stem") version "1.0.1"
    id("maven-publish")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "Fabric Maven"
        url = uri("https://maven.fabricmc.net/")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
    withJavadocJar()
}

val berry_version: String by project
val fabric_loader_version: String by project
val jetbrains_annotations_version: String by project
dependencies {
    compileOnly("net.mcberry:berry:${berry_version}")
    compileOnly("net.fabricmc:fabric-loader:${fabric_loader_version}")
    compileOnly("org.jetbrains:annotations:${jetbrains_annotations_version}")
}

val petunia_version: String by project
tasks.jar {
    manifest {
        attributes (
            "Petunia-Version" to petunia_version,
            "Berry-Base-Mod" to "net.mcberry.petunia.PetuniaInitializer",
            "Berry-Base-Mod-Name" to "petunia"
        )
    }
}

val minecraft_version: String by project
stem {
    minecraftVersion(minecraft_version)

    playerName("BerryDev")

    projectMod {
        setArchiveTask("jar")
    }

    externalMod {
        setDependency("net.mcberry:berry:${berry_version}")
    }

    transformArgs { jvmArgs, programArgs, classpath, mainClass, runDir ->
        jvmArgs += "-Dberry.indev=true"

        val side: String
        var finalMainClass = mainClass
        if (mainClass == "net.minecraft.bundler.Main") {
            jvmArgs += "-Dberry.side=SERVER"
            jvmArgs += "-DbundlerRepoDir=" + gradle.gradleUserHomeDir.resolve("caches").resolve("stem_mcdev").resolve("server_libraries")
            programArgs.add("--nogui")
            finalMainClass = "net.minecraft.server.Main"
            side = "server"
        } else {
            side = "client"
        }

        classpath += File(runDir, "mods/berry-${berry_version}.jar").absoluteFile
        jvmArgs += "-Dberry.main=$finalMainClass"

        "net.mcberry.berry.loader.BerryLoaderMain"
    }
}

group = "net.mcberry"
version = petunia_version
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
