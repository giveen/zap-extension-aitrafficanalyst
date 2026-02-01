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

// Post-process generated ZapAddOn.xml to ensure required version tags are present.
// NOTE: The add-on Gradle plugin writes the generated manifest under build/zapAddOn/ZapAddOn.xml.
tasks.named("generateZapAddOnManifest") {
    doLast {
        val manifestFile = layout.buildDirectory.file("zapAddOn/ZapAddOn.xml").get().asFile
        if (manifestFile.exists()) {
            var content = manifestFile.readText()

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

            // Inject hard dependency on the official LLM add-on.
            if (!content.contains("<dependencies>")) {
                val depsBlock = "    <dependencies>\n        <addon>llm</addon>\n    </dependencies>\n"
                content =
                    when {
                        content.contains("</url>") -> content.replaceFirst("</url>", "</url>\n$depsBlock")
                        content.contains("</author>") ->
                            content.replaceFirst("</author>", "</author>\n$depsBlock")
                        else -> content
                    }
            }

            manifestFile.writeText(content)
            println("Manually patched ZapAddOn.xml (semver + dependencies).")
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
    // Minimum supported ZAP version.
    zapVersion.set("2.15.0")
    addOnStatus.set(AddOnStatus.ALPHA)

    releaseLink.set("https://github.com/giveen/zap-extension-aitrafficanalyst/compare/v@PREVIOUS_VERSION@...v@CURRENT_VERSION@")
    unreleasedLink.set("https://github.com/giveen/zap-extension-aitrafficanalyst/compare/v@CURRENT_VERSION@...HEAD")

    manifest {
        author.set("giveen") // Updated User
        url.set("https://github.com/giveen/aitrafficanalyst") // Updated URL
        description.set("AI-driven traffic analysis using the ZAP LLM add-on.")
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
    implementation("org.commonmark:commonmark:0.21.0")
    implementation("org.jsoup:jsoup:1.16.2")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}
