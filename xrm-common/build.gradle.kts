plugins {
    java
    `maven-publish`
}

import java.time.LocalDate
import java.time.format.DateTimeFormatter

group = "com.xuanruimu"
version = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.M.d"))

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.purpurmc.org/snapshots")
}

dependencies {
    compileOnly("org.purpurmc.purpur:purpur-api:26.2.build.+")
    compileOnly("net.kyori:adventure-api:5.2.0")
    compileOnly("net.kyori:adventure-text-minimessage:5.2.0")
    implementation("com.google.inject:guice:7.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testImplementation("org.junit.platform:junit-platform-launcher:6.1.3")
    testImplementation("org.junit.platform:junit-platform-console-standalone:6.1.3")
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.23.0")
    testImplementation("org.purpurmc.purpur:purpur-api:26.2.build.+")
    testImplementation("net.kyori:adventure-api:5.2.0")
    testImplementation("net.kyori:adventure-text-minimessage:5.2.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.forkOptions.jvmArgs?.addAll(listOf("-Dfile.encoding=UTF-8"))
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing", "-Xlint:-this-escape", "-Xlint:-removal", "-Xlint:-deprecation", "-Werror"))
}

tasks.jar {
    archiveBaseName.set("xrm-common")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")

    manifest {
        attributes(
            "Implementation-Title" to "xrmm-common",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "玄锐暮"
        )
    }

    from(configurations.runtimeClasspath.get().map {
        if (it.isDirectory) it else zipTree(it)
    }) {
        exclude("module-info.class")
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<ProcessResources>().configureEach {
    filteringCharset = "UTF-8"
}

tasks.test {
    enabled = providers.gradleProperty("runTests").map { it.toBoolean() }.getOrElse(false)
    useJUnitPlatform()
    reports {
        junitXml.required.set(true)
        html.required.set(true)
    }
    val utf8Args = listOf("-Dfile.encoding=UTF-8", "-Dsun.jnu.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-Dsun.stderr.encoding=UTF-8", "-Dnet.bytebuddy.experimental=true", "-Xshare:off")
    jvmArgs = utf8Args
    systemProperty("file.encoding", "UTF-8")
    defaultCharacterEncoding = "UTF-8"
    maxParallelForks = 1
}

tasks.register<JavaExec>("runTests") {
    mainClass.set("org.junit.platform.console.ConsoleLauncher")
    classpath = sourceSets["test"].runtimeClasspath
    jvmArgs("-Dfile.encoding=UTF-8", "-Dsun.jnu.encoding=UTF-8", "-Dnet.bytebuddy.experimental=true", "-Djdk.attach.allowAttachSelf=true")
    val testRuntimeClasspath = sourceSets["test"].runtimeClasspath.asPath
    if (project.hasProperty("testClass")) {
        args("execute", "--classpath", testRuntimeClasspath, "--select-class", project.property("testClass").toString(), "--details=verbose", "--details-theme=ascii")
    } else {
        args("execute", "--classpath", testRuntimeClasspath, "--scan-classpath", "--details=verbose", "--details-theme=ascii")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.xuanruimu"
            artifactId = "xrm-common"
            version = project.version.toString()
            from(components["java"])
        }
    }
}
