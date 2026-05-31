package app.infrastructure.config

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.Path

/**
 * Wrapper around Java Path and File classes to work with sourcerer's files.
 */
object FileHelper {
    private val dirName = "data"
    private val jarPath = getJarPath()
    private val settingsPath = jarPath.resolve(dirName)

    private val specificExts = listOf(".min.js")

    fun getPath(name: String, vararg parts: String): Path {
        val path = settingsPath.resolve(Paths.get("", *parts))
        if (Files.notExists(path)) {
            Files.createDirectories(path)
        }
        return path.resolve(name)
    }

    fun getFile(name: String, vararg parts: String): File {
        return getPath(name, *parts).toFile()
    }

    fun notExists(name: String, vararg parts: String): Boolean {
        return Files.notExists(getPath(name, *parts))
    }

    fun getFileExtension(path: String): String {
        val fileName = Paths.get(path).fileName.toString().toLowerCase()
        for (ext in specificExts) {
            if (fileName.endsWith(ext)) {
                return ext
            }
        }
        return fileName.substringAfterLast(delimiter = '.', missingDelimiterValue = "")
    }

    fun getJarPath(): Path {
        val fullPathURI = FileHelper::class.java.protectionDomain
            .codeSource.location.toURI()
        val fullPath = Paths.get(fullPathURI)
        val root = fullPath.root
        return root.resolve(fullPath.subpath(0, fullPath.nameCount - 1))
    }

    fun String.toPath(): Path {
        val substitutePath = if (this.startsWith("~" + File.separator)) {
            System.getProperty("user.home") + this.substring(1)
        } else { this }
        return Paths.get(substitutePath).toAbsolutePath().normalize()
    }
}
