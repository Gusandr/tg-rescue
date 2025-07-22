plugins {
    kotlin("jvm") version "2.1.20"
    id("app.cash.sqldelight") version "2.1.0"
    id("java")
    id("application")
}

group = "me.gusandr"
version = "1.0"


repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    google()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("app.cash.sqldelight:sqlite-driver:2.1.0")
    implementation("app.cash.sqldelight:jdbc-driver:2.1.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.4")
}

application {
    mainClass.set("me.gusandr.App")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "me.gusandr.App"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

sqldelight {
    databases {
        create("Users") {
            packageName.set("me.gusandr")
            dialect("app.cash.sqldelight:mysql-dialect:2.1.0")
            srcDirs("sqldelight")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

