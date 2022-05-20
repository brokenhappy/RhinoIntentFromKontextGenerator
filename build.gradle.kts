import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21"
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish") version "0.18.0"
}

pluginBundle {
    website = "https://github.com/brokenhappy/RhinoIntentFromKontextGenerator"
    vcsUrl = "https://github.com/brokenhappy/RhinoIntentFromKontextGenerator.git"
    tags = listOf("picoVoice", "Rhino", "generate", "yaml", "intent", "context")
}
gradlePlugin {
    plugins {
        create("RhinoKontext") {
            id = "com.woutwerkman.rhinok"
            displayName = "Rhino Context to Intent Code Generator"
            description = "Generates code from Rhino context yaml file and generates data classes so you dont have to work with untyped JSON :)"
            implementationClass = "com.woutwerkman.rhinok.RhinoKPlugin"
        }
    }
}

group = "com.woutwerkman"
version = "0.5.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.yaml:snakeyaml:1.30")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}