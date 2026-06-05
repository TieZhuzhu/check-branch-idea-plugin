import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.0.1"
}

group = "com.augustlee.tool"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.2")

        bundledPlugin("com.intellij.java")
        bundledPlugin("Git4Idea")

        instrumentationTools()
        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Plugin.Java)
    }

    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        name = "easy-multi-project-check-branch"
        description = provider {
            val readme = layout.projectDirectory.file("README.md").asFile.readText(Charsets.UTF_8)
            val startMarker = "<!-- Plugin description -->"
            val endMarker = "<!-- Plugin description end -->"
            readme.substringAfter(startMarker)
                .substringBefore(endMarker)
                .trim()
        }

        ideaVersion {
            sinceBuild = "242"
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
        options.encoding = "UTF-8"
    }

    test {
        useJUnitPlatform()
    }

    wrapper {
        gradleVersion = "8.13"
    }

    buildSearchableOptions {
        enabled = false
    }
}
