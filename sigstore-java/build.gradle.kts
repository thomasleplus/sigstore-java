import com.google.protobuf.gradle.id

plugins {
    id("build-logic.java-published-library")
    id("com.diffplug.spotless") version "6.11.0"
    id("org.jsonschema2dataclass") version "4.5.0"
    id("com.google.protobuf") version "0.9.1"
}

description = "A Java client for signing and verifying using Sigstore"

dependencies {
    compileOnly("org.immutables:gson:2.9.2")
    compileOnly("org.immutables:value-annotations:2.9.2")
    annotationProcessor("org.immutables:value:2.9.2")

    implementation(platform("com.google.cloud:libraries-bom:26.1.4"))
    implementation("com.google.http-client:google-http-client-apache-v2")
    implementation("com.google.http-client:google-http-client-gson")

    implementation("io.github.erdtman:java-json-canonicalization:1.1")

    // grpc deps
    implementation(platform("io.grpc:grpc-bom:1.50.2"))
    implementation("io.grpc:grpc-protobuf")
    implementation("io.grpc:grpc-stub")
    runtimeOnly("io.grpc:grpc-netty-shaded")
    compileOnly("org.apache.tomcat:annotations-api:6.0.53") // java 9+ only

    implementation("commons-codec:commons-codec:1.15")
    implementation("com.google.code.gson:gson:2.10")
    implementation("org.bouncycastle:bcutil-jdk18on:1.72")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.72")

    implementation(platform("com.google.oauth-client:google-oauth-client-bom:1.34.1"))
    implementation("com.google.oauth-client:google-oauth-client")
    implementation("com.google.oauth-client:google-oauth-client-jetty")
    implementation("com.google.oauth-client:google-oauth-client-java6")

    testImplementation(project(":sigstore-testkit"))
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    testImplementation("org.mockito:mockito-core:4.8.1")
    testImplementation("no.nav.security:mock-oauth2-server:0.5.6")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.10.0")
    testImplementation("net.sourceforge.htmlunit:htmlunit:2.67.0")
    testImplementation("org.eclipse.jetty:jetty-server:11.0.12")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.21.9"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.50.2"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").configureEach {
            plugins {
                id("grpc")
            }
            builtins {
                named("java") {
                    // Adds @javax.annotation.Generated annotation to the generated code
                    option("annotate_code")
                }
            }
        }
    }
}

spotless {
    kotlinGradle {
        target("*.gradle.kts") // default target for kotlinGradle
        ktlint()
    }
    format("misc") {
        target("*.md", ".gitignore", "**/*.yaml")

        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()
    }
    java {
        googleJavaFormat("1.6")
        licenseHeaderFile("$rootDir/config/licenseHeader")
        targetExclude("build/**/*.java", "src/*/java/dev/sigstore/encryption/certificates/transparency/*.java")
    }
    format("conscrypt", com.diffplug.gradle.spotless.JavaExtension::class.java) {
        googleJavaFormat("1.6")
        licenseHeaderFile("$rootDir/config/conscryptLicenseHeader")
        target("src/*/java/dev/sigstore/encryption/certificates/transparency/*.java")
    }
}

jsonSchema2Pojo {
    source.setFrom(files("${sourceSets.main.get().output.resourcesDir}/rekor/model"))
    targetDirectoryPrefix.set(file("$buildDir/generated/sources/rekor-model/"))
    targetPackage.set("dev.sigstore.rekor")
    generateBuilders.set(true)
    annotationStyle.set("gson")
}

// TODO: keep until these code gen plugins explicitly declare dependencies
tasks.named("sourcesJar") {
    dependsOn("generateJsonSchema2DataClass0")
}

tasks.test {
    useJUnitPlatform {
    }
}
