pluginManagement {
    plugins {
        kotlin("jvm") version "2.2.10"
        kotlin("plugin.spring") version "2.2.10"
        id("org.springframework.boot") version "3.4.2"
        id("io.spring.dependency-management") version "1.1.7"
        id("org.liquibase.gradle") version "2.2.2"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "backend"
