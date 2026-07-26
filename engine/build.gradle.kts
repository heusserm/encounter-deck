plugins {
    kotlin("jvm")
    application
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

application {
    // A tiny runnable demo so we can eyeball generated cards from the terminal.
    mainClass.set("com.encounterdeck.engine.DemoKt")
}

tasks.test {
    useJUnitPlatform()
}
