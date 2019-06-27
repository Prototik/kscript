import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.3.40"
    id("com.github.johnrengelman.shadow") version "5.0.0"
    distribution
}

val jcabi_aether_version: String by ext
val maven_version: String by ext
val slf4j_version: String by ext
val kotlin_argparser_version: String by ext

dependencies {
    compileOnly(kotlin("stdlib"))

    compile("com.jcabi:jcabi-aether:$jcabi_aether_version") {
        exclude("org.hibernate", "hibernate-validator")
        exclude("org.slf4j", "slf4j-api")
        exclude("org.slf4j", "jcl-over-slf4j")
        exclude("org.apache.commons", "commons-lang3")
        exclude("cglib", "cglib")
        exclude("org.kuali.maven.wagons", "maven-s3-wagon")
    }
    // compile("com.jcabi:jcabi-aether:0.10.1:sources") //can be used for debugging, but somehow adds logging to dependency resolvement?
    compile("org.apache.maven:maven-core:$maven_version")
    compile("org.slf4j:slf4j-nop:$slf4j_version")
    compile("com.xenomachina:kotlin-argparser:$kotlin_argparser_version")

    testCompile(kotlin("test-junit"))
    testCompile(kotlin("script-runtime"))
}

repositories {
    jcenter()
}

val shadowJar by tasks.getting(ShadowJar::class)

distributions {
    named("main") {
        contents {
            into("bin") {
                from(shadowJar) {
                    rename(".*\\.jar", "kscript.jar")
                }
                from(file("src/kscript"))

                fileMode = "755".toInt(radix = 8)
            }
        }
    }
}
