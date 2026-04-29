pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolution {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        // 高德地图 Maven 仓库
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

rootProject.name = "DreamTravel"
include(":app")
