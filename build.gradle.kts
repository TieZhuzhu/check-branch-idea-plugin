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
            """
            <p>easy-multi-project-check-branch helps you inspect and switch branches for multiple Git repositories from one IntelliJ IDEA tool window.</p>
            <ul>
              <li>Discovers Git repositories already recognized by the current IDEA project without scanning arbitrary disk folders</li>
              <li>Refreshes repository status asynchronously, including current branch, target branch availability, blocked states, and tracked local changes</li>
              <li>Supports batch switching and single-repository switching; repositories already on the target branch are skipped automatically</li>
              <li>Prefers remote target branches, falls back to local branches when remote checkout is unavailable, and can fall back to configured main-branch candidates</li>
              <li>Protects tracked local changes with IDEA Shelf before switching and asks users to restore shelved changes manually afterward</li>
              <li>Shows success, fallback, skipped, failed, and manual-restore-required result cards, with full failure reasons available on hover</li>
            </ul>
            <p>中文说明：easy-multi-project-check-branch 可以在 IntelliJ IDEA 右侧工具窗口中集中查看当前工作区内的 Git 仓库，并将选中的仓库批量切换到指定分支。</p>
            <ul>
              <li>自动发现当前 IDEA 项目中已识别的 Git 仓库，不扫描任意磁盘目录</li>
              <li>异步刷新当前分支、目标分支可用性、阻塞状态和未提交变更状态</li>
              <li>支持批量切换与单仓库精确切换，已在目标分支的仓库会自动跳过</li>
              <li>目标分支远端存在时优先切换远端引用，远端不可用时尝试本地分支，并支持主分支回退</li>
              <li>切换前优先使用 IDEA Shelf 搁置已跟踪变更，切换后提示用户手动恢复</li>
              <li>结果卡片展示成功、回退、跳过、失败和待恢复状态，悬浮可查看完整失败原因</li>
            </ul>
            """.trimIndent()
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
