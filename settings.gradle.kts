pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // ✅ AGREGAR ESTE (si no está)
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Medicare"
include(":app")