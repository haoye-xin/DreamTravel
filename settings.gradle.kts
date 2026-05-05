pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // 高德地图 SDK Maven 仓库
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

rootProject.name = "DreamTravel"
include(":app")
