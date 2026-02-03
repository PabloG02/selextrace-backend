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
	implementation(libs.spring.boot.starter.data.mongodb)
	implementation(libs.spring.boot.starter.webmvc)
	testImplementation(libs.spring.boot.starter.data.mongodb.test)
	testImplementation(libs.spring.boot.starter.webmvc.test)
	testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
	useJUnitPlatform()
}
