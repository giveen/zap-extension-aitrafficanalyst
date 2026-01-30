import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.language.jvm.tasks.ProcessResources
import org.zaproxy.gradle.addon.AddOnStatus

plugins {
    `java-library`
    id("org.zaproxy.add-on") version "0.13.1"
    id("com.diffplug.spotless")
    id("org.zaproxy.common")
}

// Post-process generated ZapAddOn.xml to ensure required version tags are present
tasks.named("generateZapAddOnManifest") {
    doLast {
        val manifestFile = file("${project.buildDir}/resources/main/ZapAddOn.xml")
        if (manifestFile.exists()) {
            var content = manifestFile.readText()

            // Inject <not-before-version> to fix ZAP loading error
            if (!content.contains("not-before-version")) {
                content = content.replace("</author>", "</author>\n    <not-before-version>2.15.0</not-before-version>")
            }

            // Inject <semver>
            if (!content.contains("semver")) {
                val versionMatch = Regex("<version>([^<]+)</version>").find(content)
                val version = versionMatch?.groupValues?.get(1)
                if (version != null) {
                    content =
                        content.replaceFirst(
                            "<version>$version</version>",
                            "<version>$version</version>\n    <semver>$version</semver>",
                        )
                }
            }

            manifestFile.writeText(content)
            println("Manually patched ZapAddOn.xml with version constraints.")
        }
    }
}

// The add-on Gradle plugin generates ZapAddOn.xml; exclude the template copy to avoid duplicates.
tasks.named<ProcessResources>("processResources") {
    exclude("ZapAddOn.xml")
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// (Manifest post-processing will be appended at the end of the file)

description = "A template for a 3rd party ZAP Java add-on."

zapAddOn {
    addOnId.set("aitrafficanalyst")
    addOnName.set("AI Traffic Analyst")
    zapVersion.set("2.17.0")
    addOnStatus.set(AddOnStatus.ALPHA)

    releaseLink.set("https://github.com/jeremy/aitrafficanalyst/compare/v@PREVIOUS_VERSION@...v@CURRENT_VERSION@")
    unreleasedLink.set("https://github.com/jeremy/aitrafficanalyst/compare/v@CURRENT_VERSION@...HEAD")

    manifest {
        author.set("giveen") // Updated User
        url.set("https://github.com/giveen/aitrafficanalyst") // Updated URL
        description.set("AI-driven traffic analysis using local LLMs.")
        extensions {
            register("org.zaproxy.zap.extension.aitrafficanalyst.ExtensionAiAnalyst")
        }
    }
}

java {
    val javaVersion = JavaVersion.VERSION_17
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

spotless {
    kotlinGradle {
        ktlint()
    }
}

dependencies {
    // Core ZAP dependencies are provided by the plugin.
    // Keep commonlib as compileOnly for existing example rules.
    compileOnly("org.zaproxy.addon:commonlib:1.36.0")

    // Add these for our AI logic:
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("org.commonmark:commonmark:0.21.0")
    implementation("org.jsoup:jsoup:1.16.2")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}
