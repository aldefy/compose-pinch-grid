import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.binary.compatibility.validator)
    id("maven-publish")
    id("signing")
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "PinchGrid"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
        }

        val jvmTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

android {
    namespace = "io.github.aldefy.pinchgrid"
    compileSdk = 35

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// ========== Maven Publishing + Signing ==========
val libraryVersion = "1.0.0-alpha01"
val libraryGroup = "io.github.aldefy"
val libraryArtifact = "pinch-grid"

group = libraryGroup
version = libraryVersion

publishing {
    publications.withType<MavenPublication>().configureEach {
        val publicationName = name
        val javadocJar = tasks.register("${publicationName}JavadocJar", Jar::class) {
            archiveClassifier.set("javadoc")
            archiveAppendix.set(publicationName)
        }
        artifact(javadocJar)
        pom {
            name.set("Compose Pinch Grid")
            description.set("Compose Multiplatform pinch-to-resize grid (ComposePinchGrid) — Google Photos style column count switching with smooth gestures, haptic feedback, and crossfade transitions.")
            url.set("https://github.com/aldefy/compose-pinch-grid")
            inceptionYear.set("2026")

            licenses {
                license {
                    name.set("Apache License 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    distribution.set("repo")
                }
            }

            developers {
                developer {
                    id.set("aldefy")
                    name.set("Adit Lal")
                    email.set("aditlal@gmail.com")
                    url.set("https://github.com/aldefy")
                }
            }

            scm {
                connection.set("scm:git:git://github.com/aldefy/compose-pinch-grid.git")
                developerConnection.set("scm:git:ssh://github.com/aldefy/compose-pinch-grid.git")
                url.set("https://github.com/aldefy/compose-pinch-grid")
            }

            issueManagement {
                system.set("GitHub Issues")
                url.set("https://github.com/aldefy/compose-pinch-grid/issues")
            }
        }
    }

    repositories {
        maven {
            name = "sonatype"
            val releasesUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (libraryVersion.endsWith("SNAPSHOT")) snapshotsUrl else releasesUrl

            credentials {
                username = findProperty("ossrhUsername")?.toString()
                    ?: System.getenv("OSSRH_USERNAME")
                    ?: ""
                password = findProperty("ossrhPassword")?.toString()
                    ?: System.getenv("OSSRH_PASSWORD")
                    ?: ""
            }
        }
        maven {
            name = "localStaging"
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

tasks.withType<Sign>().configureEach {
    onlyIf { project.hasProperty("signing.gnupg.keyName") }
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}
