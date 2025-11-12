import java.util.Locale

plugins {
    java
    id("com.gradleup.shadow") version "9.2.2"
    id("org.jooq.jooq-codegen-gradle") version "3.20.8"
    // id("io.freefair.lombok") version "8.13.1"
}

group = "me.bounser"
version = "1.9.3"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://maven.respark.dev/releases") {
        name = "respark-releases"
    }
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
        name = "placeholderapi"
    }
    maven("https://m2.dv8tion.net/releases") {
        name = "dv8tion"
    }
    maven("https://nexus.scarsz.me/content/groups/public/") {
        name = "Scarsz-Nexus"
    }
    maven("https://repo.codemc.io/repository/maven-snapshots/") {
        name = "codemc-snapshots"
    }
    maven("https://repo.codemc.io/repository/maven-public/") {
        name = "codemc-repo"
    }
    maven("https://repo.xenondevs.xyz/releases/") {
        name = "xenondevs"
    }
    maven("https://jitpack.io") {
        name = "jitpack"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")

    jooqCodegen("org.jooq:jooq-codegen:3.20.8")
    jooqCodegen("com.h2database:h2:2.4.240")

    implementation("xyz.xenondevs.invui:invui:1.43")
    implementation("jfree:jfreechart:1.0.13")
    implementation("net.dv8tion:JDA:5.0.0-beta.18")
    implementation("net.wesjd:anvilgui:1.10.4-SNAPSHOT")
    implementation("redis.clients:jedis:5.1.2")
    implementation("de.tr7zw:item-nbt-api:2.13.1")
    implementation("io.javalin:javalin:6.6.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.22")
    implementation("net.kyori:adventure-platform-bukkit:4.3.3")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("org.bstats:bstats-bukkit:3.1.0")
    implementation("com.mysql:mysql-connector-j:9.5.0")
    implementation("com.h2database:h2:2.4.240")
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.jooq:jooq:3.20.8")

    compileOnly("me.leoko.advancedgui:AdvancedGUI:2.2.8")
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("net.milkbowl.vault:VaultAPI:1.7")
    compileOnly("com.discordsrv:discordsrv:1.28.0")
    compileOnly("commons-io:commons-io:2.14.0")
    compileOnly("com.github.retrooper:packetevents-spigot:2.10.1")
}

val assetsUrl = uri("https://github.com/Owen1212055/mc-assets/archive/refs/heads/main.zip")
val assetsZip = layout.buildDirectory.file("mc-assets.zip")
val assetsExtracted = layout.buildDirectory.dir("extracted-assets")
val assetsProcessed = layout.buildDirectory.dir("processed-assets")

val targetJavaVersion = 21
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}

val downloadAssets by tasks.registering {
    outputs.file(assetsZip)
    doLast {
        val zip = assetsZip.get().asFile
        if (!zip.exists()) {
            zip.outputStream().use { output ->
                assetsUrl.toURL().openStream().use { input -> input.copyTo(output) }
            }
        }

    }
}

val extractAssets by tasks.registering(Copy::class) {
    dependsOn(downloadAssets)
    from({ zipTree(assetsZip).matching { include("**/mc-assets-main/item-assets/*.png") } })
    into(assetsExtracted)
    eachFile {
        this.relativePath = RelativePath(true, this.relativePath.segments.last())
    }
}

val renameAssets by tasks.registering {
    dependsOn(extractAssets)
    outputs.dir(assetsProcessed)

    doLast {
        val inputDir = assetsExtracted.get().asFile
        val outputDir = assetsProcessed.get().asFile
        outputDir.mkdirs()

        inputDir.walkTopDown().forEach { file ->
            if (file.isFile) {
                val relative = file.relativeTo(inputDir).path.lowercase(Locale.ROOT)
                val targetFile = File(outputDir, relative)
                targetFile.parentFile.mkdirs()
                file.copyTo(targetFile, overwrite = true)
            }
        }
    }
}

tasks.processResources {
    dependsOn(renameAssets)
    from(assetsProcessed) {
        into("images/items")
    }
}

tasks.compileJava {
    dependsOn("jooqCodegen")
    options.encoding = "UTF-8"

    if (targetJavaVersion >= 17 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
}

val script = projectDir.resolve("src/main/resources/db/schema.sql")
jooq {
    configuration {
        jdbc {
            driver = "org.h2.Driver"
            url = "jdbc:h2:mem:jooq_codegen;DB_CLOSE_DELAY=-1;MODE=MySQL;INIT=RUNSCRIPT FROM '${script.absolutePath.replace("\\", "\\\\")}'"
            user = "sa"
            password = ""
        }
        generator {
            database {
                name = "org.jooq.meta.h2.H2Database"
                properties {
                    property {
                        key = "scripts"
                        value = script.toString()
                    }
                }
            }
            generate {
                isDaos = true
                isPojos = true
                isRecords = true
                isLinks = true
            }
            target {
                packageName = "me.biquaternions.nascraft.schema"
                directory = "src/main/generated"
            }
        }
    }
}

sourceSets {
    main {
        java {
            srcDir("src/main/generated")
        }
    }
}

tasks.build {
    dependsOn(renameAssets)
    dependsOn("shadowJar")
}

tasks.shadowJar {
    archiveClassifier.set("")

    mapOf(
        "org.bstats" to "bstats",
        "net.wesjd.anvilgui" to "anvilgui",
        "de.tr7zw.changeme.nbtapi" to "nbtapi",
        "io.javalin" to "web.libs.javalin",
        "kotlin" to "web.libs.kotlin",
        "org.jooq" to "jooq",
        "com.zaxxer.hikari" to "hikari",
    ).forEach { (key, value) ->
        relocate(key, "me.bounser.$value")
    }

}

tasks.jar {
    archiveClassifier.set("dev")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("*plugin.yml") {
        expand(props)
    }
}
