plugins {
    java
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
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://repo.purpurmc.org/snapshots")
}

dependencies {
    compileOnly("org.purpurmc.purpur:purpur-api:26.2.build.+")
    compileOnly("net.kyori:adventure-api:5.2.0")
    compileOnly("net.kyori:adventure-text-minimessage:5.2.0")
    implementation("com.zaxxer:HikariCP:7.1.0")
    implementation("com.mysql:mysql-connector-j:26.7.0")
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
    implementation("fr.mrmicky:fastboard:2.2.1")
    implementation("com.google.inject:guice:7.0.0")
    implementation("com.xuanruimu:xrm-common:+")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testImplementation("org.junit.platform:junit-platform-launcher:6.1.3")
    testImplementation("org.junit.platform:junit-platform-console-standalone:6.1.3")
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.23.0")
    testImplementation("org.purpurmc.purpur:purpur-api:26.2.build.+")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.forkOptions.jvmArgs?.addAll(listOf("-Dfile.encoding=UTF-8"))
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing", "-Xlint:-this-escape", "-Xlint:-removal", "-Xlint:-deprecation", "-Werror"))
}

tasks.jar {
    archiveBaseName.set("XRMdenglu")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")

    manifest {
        attributes(
            "Implementation-Title" to "燃烧之陨登录服",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "玄锐暮"
        )
    }

    // 打包依赖到JAR中
    from(configurations.runtimeClasspath.get().map {
        if (it.isDirectory) it else zipTree(it)
    }) {
        exclude("module-info.class")
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register<Copy>("deployPlugin") {
    from(tasks.jar)
    into("D:/xuanr/Desktop/燃烧之陨我的世界服务端/燃烧之陨登录服/plugins")
    doNotTrackState("中文路径导致增量构建哈希失败")
}

tasks.build {
    finalizedBy("deployPlugin")
}

tasks.withType<ProcessResources>().configureEach {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(
            "version" to project.version,
            "name" to "XRMdenglu",
            "description" to "暮澜纪元 登录服插件",
            "author" to "玄锐暮"
        )
    }
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

tasks.register("printTestClasspath") {
    doLast {
        println(sourceSets["test"].runtimeClasspath.asPath)
    }
}

tasks.register<JavaExec>("runTests") {
    mainClass.set("org.junit.platform.console.ConsoleLauncher")
    classpath = sourceSets["test"].runtimeClasspath
    jvmArgs("-Dfile.encoding=UTF-8", "-Dsun.jnu.encoding=UTF-8", "-Dnet.bytebuddy.experimental=true", "-Djdk.attach.allowAttachSelf=true")
    doFirst {
        val testRuntimeClasspath = sourceSets["test"].runtimeClasspath.asPath
        val testClass = providers.gradleProperty("testClass").orNull
        if (testClass != null) {
            args("execute", "--classpath", testRuntimeClasspath, "--select-class", testClass, "--details=verbose", "--details-theme=ascii")
        } else {
            args(
                "execute",
                "--classpath", testRuntimeClasspath,
                "--scan-classpath",
                "--include-classname=.*",
                "--exclude-engine", "junit-vintage",
                "--details=verbose",
                "--details-theme=ascii"
            )
        }
    }
}
