plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
}

group = "dev.loratech"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.release.set(21)
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.mysql:mysql-connector-j:8.3.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.3.3")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("com.google.gson", "dev.loratech.guard.libs.gson")
        relocate("com.github.benmanes.caffeine", "dev.loratech.guard.libs.caffeine")
        relocate("okhttp3", "dev.loratech.guard.libs.okhttp3")
        relocate("okio", "dev.loratech.guard.libs.okio")
        relocate("com.zaxxer.hikari", "dev.loratech.guard.libs.hikari")
        minimize()
    }

    build {
        dependsOn(shadowJar)
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    processResources {
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand("version" to version)
        }
    }
}
