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
    implementation("com.android.tools.build:gradle:7.3.0")
    implementation("com.android.tools.build:gradle-api:7.3.0")
    gradleApi()
    implementation(kotlin("gradle-plugin", "1.7.10"))
    implementation(kotlin("stdlib"))

    implementation("org.ow2.asm:asm-tree:9.2")
    implementation("org.ow2.asm:asm-util:9.2")
    implementation("org.ow2.asm:asm-commons:9.2")

    implementation("org.javassist:javassist:3.29.2-GA")
}