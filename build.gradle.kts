import io.izzel.taboolib.gradle.*

plugins {
    `java-library`
    `maven-publish`
    id("io.izzel.taboolib") version "2.0.39"
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
}

taboolib {
    env {
        install(Bukkit, BukkitHook, BukkitUtil, BukkitUI, CommandHelper)
        install(Database, MinecraftChat, Basic, I18n, Kether)
    }
    version {
        taboolib = "6.2.4-c90a237"
        // The generated descriptor unconditionally declares Folia support.
        skipPlatformFile = true
    }
    // Bundle both sides of TabooLib's private SLF4J API before database initialization.
    relocate("org.slf4j.", "org.slf4j_2_0_8.")
}

repositories {
    clear()
    mavenCentral()
    maven("https://repo.tabooproject.org/repository/releases/") {
        content {
            includeGroup("io.izzel.taboolib")
            includeGroupByRegex("org\\.tabooproject.*")
        }
    }
    maven("https://repo.papermc.io/repository/maven-public/") {
        content {
            includeGroup("io.papermc.paper")
            includeGroup("net.md-5")
        }
    }
    maven("https://libraries.minecraft.net/") {
        content { includeGroup("com.mojang") }
    }
    maven("https://maven.xbaimiao.com/repository/maven-public/") {
        content { includeGroup("public") }
    }
    maven("https://r.irepo.space/maven/") {
        content { includeGroup("pers.neige.neigeitems") }
    }
}

dependencies {
    add("taboo", "org.slf4j:slf4j-api:2.0.8") { isTransitive = false }
    add("taboo", "org.slf4j:slf4j-jdk14:2.0.8") { isTransitive = false }

    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("public:papi:1.0.0")
    compileOnly("public:vault:1.0.0")
    compileOnly("public:points:1.0.0")
    compileOnly("public:Zaphkiel:1.0.0")
    compileOnly("pers.neige.neigeitems:NeigeItems:1.15.113")
    compileOnly("public:MMOItems:6.9.4")
    compileOnly(fileTree("libs"))

    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.101.0")
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.2")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    // TabooLib relocates classes but does not rewrite service descriptor contents.
    // The relocated service descriptor is supplied by this project's resources.
    exclude("META-INF/services/org.slf4j.spi.SLF4JServiceProvider")
    exclude("META-INF/versions/**/module-info.class")
}

tasks.test {
    dependsOn(tasks.jar, "taboolibMainTask")
    inputs.file(tasks.jar.flatMap { it.archiveFile }).withPropertyName("pluginArtifact")
    systemProperty("shoppro.artifact", tasks.jar.get().archiveFile.get().asFile.absolutePath)
    useJUnitPlatform()
}

