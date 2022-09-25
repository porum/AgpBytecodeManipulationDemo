plugins {
    kotlin("jvm") version "1.7.10"
}

repositories {
    google()
    mavenCentral()
}

configurations.configureEach {
    resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
}

dependencies {
    implementation(kotlin("stdlib"))

    // gradle api, agp, kgp
    gradleApi()
    implementation("com.android.tools.build:gradle:7.3.0")
    implementation("com.android.tools.build:gradle-api:7.3.0")
    implementation(kotlin("gradle-plugin", "1.7.10"))

    // asm
    implementation("org.ow2.asm:asm-tree:9.2")
    implementation("org.ow2.asm:asm-util:9.2")
    implementation("org.ow2.asm:asm-commons:9.2")

    // javassist
    implementation("org.javassist:javassist:3.29.2-GA")

    // aspectj
    implementation("org.aspectj:aspectjtools:1.9.7")
}