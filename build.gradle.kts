plugins {
	java
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
}

group = "pablog"
version = "0.0.1-SNAPSHOT"
description = "AptaSuite rewrite for the web"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(libs.milib)
	implementation(libs.eclipse.collections)
	implementation(libs.eclipse.collections.api)
	implementation(libs.bloom.filter)
	implementation(libs.commons.math3)
	implementation(libs.fastutil)
	// Spring Boot dependencies
	implementation(libs.spring.boot.starter.data.jpa)
	implementation(libs.spring.boot.starter.security)
	implementation(libs.spring.boot.starter.webmvc)
	runtimeOnly(libs.h2)
	runtimeOnly(libs.postgresql)
	testImplementation(libs.spring.boot.starter.data.jpa.test)
	testImplementation(libs.spring.boot.starter.security.test)
	testImplementation(libs.spring.boot.starter.webmvc.test)
	testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
	useJUnitPlatform()
}
