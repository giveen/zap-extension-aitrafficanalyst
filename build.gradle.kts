import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Jar
import org.zaproxy.gradle.addon.AddOnStatus

plugins {
    `java-library`
    id("org.zaproxy.add-on") version "0.13.1"
    id("com.diffplug.spotless")
    id("org.zaproxy.common")
}

// Post-process generated ZapAddOn.xml to inject a <semver> tag (not emitted by the plugin
// version in use). The <dependencies> block is declared properly via the zapAddOn manifest
// DSL below and needs no manual patching.
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

            manifestFile.writeText(content)
            println("Manually patched ZapAddOn.xml (semver).")
        }
    }
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// (Manifest post-processing will be appended at the end of the file)

description = "AI-driven traffic analysis using the ZAP LLM add-on."

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
        // Hard dependency on the official ZAP LLM add-on, which supplies all provider
        // configuration and LLM communication. Matches the pattern used by the official
        // openapi add-on's own LLM integration (ExtensionOpenApiLlm).
        dependencies {
            addOns {
                register("llm")
            }
        }
    }
}

java {
    val javaVersion = JavaVersion.VERSION_17
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

tasks.test {
    useJUnitPlatform()
}

spotless {
    kotlinGradle {
        ktlint()
    }
}

dependencies {
    // Core ZAP dependencies are provided by the plugin.
    // Keep commonlib as compileOnly for existing example rules.
    compileOnly("org.zaproxy.addon:commonlib:1.40.0")
    // ZAP core bundles json-lib (declared `api` in zaproxy/zap.gradle.kts), so it's always on
    // the runtime classpath; compileOnly avoids bundling a second copy in our own jar.
    compileOnly("net.sf.json-lib:json-lib:2.4:jdk15")

    // Add these for our AI logic:
    implementation("org.commonmark:commonmark:0.21.0")
    implementation("org.jsoup:jsoup:1.16.2")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    // json-lib is compileOnly for main (provided by ZAP core at runtime), but tests run outside
    // ZAP, so it needs to be present on the test runtime classpath explicitly.
    testRuntimeOnly("net.sf.json-lib:json-lib:2.4:jdk15")
}
