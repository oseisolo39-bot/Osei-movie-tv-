plugins {
    id("com.android.application") version "8.5.2" apply false
    id("com.android.library") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

tasks.register("cleanHprof") {
    doLast {
        val rootDirFile = rootDir
        val hprofFiles = rootDirFile.listFiles { file -> file.extension == "hprof" }
        hprofFiles?.forEach { file ->
            println("Deleting: ${file.name} (size: ${file.length()} bytes)")
            val deleted = file.delete()
            println("Deleted successfully: $deleted")
        }
    }
}

