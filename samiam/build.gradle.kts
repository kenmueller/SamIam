import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.panteleyev.jpackage.ImageType
import org.panteleyev.jpackage.JPackageTask

plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.panteleyev.jpackageplugin") version "1.5.2"

}
val main = "edu.ucla.belief.ui.UI"
application { mainClass.set(main) }
tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveClassifier.set("all")
    isZip64 = true
}
group = parent!!.group
version = parent!!.version
val shadowJarName = "${project.name}-${version}-all.jar"

task("packageNetworkSamples", Copy::class) {
    group = "distribution"
    from("${rootDir}/network_samples").into("$buildDir/package/network_samples")
}
task("packageShadowJar", Copy::class) {
    group = "distribution"
    dependsOn("shadowJar")
    from("${buildDir}/libs/$shadowJarName").into("$buildDir/package")
}
task("packageInflibJavadoc", Copy::class) {
    group = "distribution"
    dependsOn(":inflib:javadoc")
    from("${rootDir}/inflib/build/docs/javadoc").into("$buildDir/package/inflib_javadoc")
}
task("packageSamIamJavadoc", Copy::class) {
    group = "distribution"
    dependsOn(":samiam:javadoc")
    from("${rootDir}/samiam/build/docs/javadoc").into("$buildDir/package/samiam_javadoc")
}
task("packageHtmlHelp", Copy::class) {
    group = "distribution"
    from("${rootDir}/htmlhelp").into("$buildDir/package/htmlhelp")
}
tasks.jpackage {
    dependsOn(
        "packageNetworkSamples",
        "packageShadowJar",
        "packageSamIamJavadoc",
        "packageInflibJavadoc",
        "packageHtmlHelp"
    )
    group = "distribution"

    input = "build/package"
    destination = "$buildDir/dist"

    appName = project.name
    vendor = "ucla"

    mainJar = shadowJarName
    mainClass = main
    icon = "${projectDir}/src/main/resources/images/SamIamAppIcon.png"

    javaOptions = listOf("-Dfile.encoding=UTF-8")

    windows {
        type = ImageType.MSI
        winMenu = true
        winDirChooser = true
        appName = project.name
        winShortcutPrompt = true
        icon = "${projectDir}/src/main/resources/images/SamIamAppIcon-White.ico"
    }
    linux {
        type = ImageType.DEFAULT
        linuxShortcut = true
        linuxAppCategory = "SamIam"
    }
    mac {
        type = ImageType.DMG
    }
}

tasks.create("jpackageAppImage", JPackageTask::class) {
    group = "distribution"
    dependsOn("shadowJar")
    input = "build/libs"
    destination = "$buildDir/dist"
    appName = "samiam-$version"
    vendor = "ucla"
    mainJar = shadowJarName
    mainClass = main
    javaOptions = listOf("-Dfile.encoding=UTF-8")
    linux {
        appName = "${project.name}-$version.AppImage"
        type = ImageType.APP_IMAGE
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":inflib"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}
tasks.javadoc {
    options {
        this as StandardJavadocDocletOptions
        tags(
            "from",
            "changed",
            "decision",
            "precondition",
            "postcondition",
            "pq",
            "param-missing", //parameters in the javadoc that aren't present in the code
        )
        addBooleanOption("Xdoclint:none", true)
        addStringOption("Xmaxwarns", "1")
    }
}
tasks.test {
    useJUnitPlatform()
    testLogging {
        events(PASSED, FAILED, SKIPPED)
        showStandardStreams = true
    }
}