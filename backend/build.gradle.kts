import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
    kotlin("plugin.jpa") version "1.9.23"
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(22)) }
}

repositories { mavenCentral() }

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // ʹ�� Spring Boot �����汾�� MySQL Connector
    runtimeOnly("com.mysql:mysql-connector-j")
    // ��� kotlin/reflect/full/KClasses ClassNotFound����Ҫ��ʽ���� kotlin-reflect
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions { jvmTarget = "17"; freeCompilerArgs += "-Xjsr305=strict" }
}

tasks.withType<JavaCompile> {
    options.release.set(17)
}

tasks.withType<Test> { useJUnitPlatform() }
