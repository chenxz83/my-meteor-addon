plugins {
    alias(libs.plugins.fabric.loom)
}

val archivesBaseName = providers.gradleProperty("archives_base_name").get()
val mavenGroup = providers.gradleProperty("maven_group").get()

// Commit hash for update checking (set by CI via GITHUB_SHA, empty for local builds)
val commit = providers.environmentVariable("GITHUB_SHA").orElse(providers.gradleProperty("commit")).getOrElse("")

base {
    archivesName = archivesBaseName
    version = libs.versions.mod.version.get()
    group = mavenGroup
}

repositories {
    maven {
        name = "meteor-maven"
        url = uri("https://maven.meteordev.org/releases")
    }
    maven {
        name = "meteor-maven-snapshots"
        url = uri("https://maven.meteordev.org/snapshots")
    }
}

dependencies {
    // Fabric
    minecraft(libs.minecraft)
    compileOnly(libs.mixin)
    mappings("net.fabricmc:yarn:1.21.8+build.1:v2")
    implementation(libs.fabric.loader)

    // Meteor (modCompileOnly so Loom remaps it from intermediary to yarn)
    modCompileOnly(libs.meteor.client) {
        // Fabric API modules are bundled (jar-in-jar) inside meteor-client and are not
        // needed at compile time; excluding them avoids an access-widener namespace error.
        exclude(group = "net.fabricmc.fabric-api")
    }
}

loom {
    // meteor-client ships an intermediary-namespaced access widener that Loom 1.14
    // cannot remap; our addon only uses public APIs, so we skip transitive access wideners.
    enableTransitiveAccessWideners = false
    accessWidenerPath = file("src/main/resources/crystal-aura-plus.accesswidener")

}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().toInt()))
    }
}

fun toMinecraftCompat(version: String): String {
    val stable = Regex("""^(\d{2})\.([1-9]\d*)(?:\.(\d+))?$""")

    stable.matchEntire(version)?.let {
        val (year, drop, _) = it.destructured
        return "~$year.$drop"
    }

    val pre = Regex("""^(\d{2})\.([1-9]\d*)-pre[-.](\d+)$""")
    pre.matchEntire(version)?.let {
        return version.replace("-pre-", "-pre.")
    }

    val rc = Regex("""^(\d{2})\.([1-9]\d*)-rc[-.](\d+)$""")
    rc.matchEntire(version)?.let {
        return version.replace("-rc-", "-rc.")
    }

    return version
}

tasks {
    processResources {
        inputs.property("commit", commit)

        filesMatching("commit.txt") {
            expand("commit" to commit)
        }
    }

    jar {
        inputs.property("archivesName", archivesBaseName)

        from("LICENSE") {
            rename { "${it}_$archivesBaseName" }
        }
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(
            listOf(
                "-Xlint:deprecation",
                "-Xlint:unchecked"
            )
        )
    }
}
