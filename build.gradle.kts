import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.16.0"
}

group = "com.augustlee.tool"
version = "1.0.0-SNAPSHOT"

val localVerificationIdePath = providers.gradleProperty("localVerificationIdePath")
    .orElse(providers.environmentVariable("LOCAL_VERIFICATION_IDE_PATH"))
val isGitHubActions = providers.environmentVariable("GITHUB_ACTIONS")
    .map { it.equals("true", ignoreCase = true) }
    .orElse(false)

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

        testFramework(TestFrameworkType.Plugin.Java)
    }

    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        name = "Easy Multi Project Check Branch"
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
            val idePath = localVerificationIdePath.orNull
            if (!idePath.isNullOrBlank()) {
                local(file(idePath))
            } else if (isGitHubActions.get()) {
                current()
            }
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
        gradleVersion = "9.0.0"
    }

    buildSearchableOptions {
        enabled = false
    }
}
