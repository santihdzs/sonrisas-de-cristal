import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.serialization") version "2.4.20"
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
    // modelos y reglas compartidos con la app
    sourceSets.main { kotlin.srcDir("../shared/src/main/kotlin") }
}

application {
    mainClass.set("mx.tec.sonrisas.servidor.MainKt")
    // "hoy" y los datos de prueba dependen de la zona horaria de las estancias
    applicationDefaultJvmArgs = listOf("-Duser.timezone=America/Monterrey")
}

dependencies {
    implementation(platform("io.ktor:ktor-bom:3.6.0"))
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-rate-limit")
    implementation("io.ktor:ktor-server-forwarded-header")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.postgresql:postgresql:42.7.14")
    implementation("com.zaxxer:HikariCP:7.1.0")
    implementation("com.h2database:h2:2.5.252")
    implementation("ch.qos.logback:logback-classic:1.6.5")

    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Duser.timezone=America/Monterrey")
}
