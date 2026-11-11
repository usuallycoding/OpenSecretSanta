package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.server.data.download.ApkDownloads
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ApkDownloadsTest {
    private val folder: Path = Files.createTempDirectory("santa-downloads")

    /** Makes a file; [modified] orders files that the version code does not (a bigger number is more recent). */
    private fun file(name: String, modified: Long = 1_000) {
        val path = folder.resolve(name)
        Files.write(path, byteArrayOf(1, 2, 3))
        path.toFile().setLastModified(modified * 1_000)
    }

    private fun latest(directory: Path = folder) = ApkDownloads(directory).latest()?.fileName?.toString()

    @Test
    fun theHighestVersionCodeWins() {
        file("opensecretsanta-1.1.4-100.apk")
        file("opensecretsanta-1.1.5-9000.apk")

        assertEquals("opensecretsanta-1.1.5-9000.apk", latest())
    }

    @Test
    fun codesAreComparedAsNumbersNotText() {
        file("opensecretsanta-1.1.4-900.apk")
        file("opensecretsanta-1.1.5-10000.apk")

        assertEquals("opensecretsanta-1.1.5-10000.apk", latest())
    }

    @Test
    fun theCodeBeatsTheModificationTime() {
        file("opensecretsanta-1.1.5-9000.apk", modified = 1)
        file("opensecretsanta-1.1.4-100.apk", modified = 99)

        assertEquals("opensecretsanta-1.1.5-9000.apk", latest())
    }

    @Test
    fun otherNamesComeAfterNamesWithACode() {
        file("opensecretsanta-1.1.4-100.apk", modified = 1)
        file("santa.apk", modified = 99)

        assertEquals("opensecretsanta-1.1.4-100.apk", latest())
    }

    @Test
    fun otherNamesGoByModificationTime() {
        file("old.apk", modified = 1)
        file("new.apk", modified = 99)

        assertEquals("new.apk", latest())
    }

    @Test
    fun aVersionNameWithADashIsStillRead() {
        file("opensecretsanta-1.2.0-beta-500.apk")
        file("opensecretsanta-1.1.5-9000.apk")

        assertEquals("opensecretsanta-1.1.5-9000.apk", latest())
    }

    @Test
    fun aFileBeingCopiedAndOtherFilesAreNotOffered() {
        file("opensecretsanta-1.1.6-9100.apk.part")
        file("notes.txt")
        file("opensecretsanta-1.1.5-9000.apk")
        Files.createDirectory(folder.resolve("opensecretsanta-9.9.9-99999.apk"))

        assertEquals("opensecretsanta-1.1.5-9000.apk", latest())
    }

    @Test
    fun aFinishedCopyReplacesTheOlderApp() {
        file("opensecretsanta-1.1.5-9000.apk")
        file("opensecretsanta-1.1.6-9100.apk.part")
        assertEquals("opensecretsanta-1.1.5-9000.apk", latest())

        Files.move(folder.resolve("opensecretsanta-1.1.6-9100.apk.part"), folder.resolve("opensecretsanta-1.1.6-9100.apk"))

        assertEquals("opensecretsanta-1.1.6-9100.apk", latest())
    }

    @Test
    fun aCodeTooBigForANumberCountsAsAnotherName() {
        file("opensecretsanta-1.1.5-99999999999999999999.apk", modified = 1)
        file("opensecretsanta-1.1.4-100.apk", modified = 99)

        assertEquals("opensecretsanta-1.1.4-100.apk", latest())
    }

    @Test
    fun anEmptyFolderHasNoApp() {
        assertNull(latest())
    }

    @Test
    fun aFolderWithNoApkHasNoApp() {
        file("notes.txt")

        assertNull(latest())
    }

    @Test
    fun aMissingFolderHasNoApp() {
        assertNull(latest(folder.resolve("nothing-here")))
    }

    @Test
    fun aFileInPlaceOfTheFolderHasNoApp() {
        file("opensecretsanta-1.1.5-9000.apk")

        assertNull(latest(folder.resolve("opensecretsanta-1.1.5-9000.apk")))
    }

    @Test
    fun appsInSubfoldersAreNotLookedFor() {
        Files.createDirectory(folder.resolve("older"))
        Files.write(folder.resolve("older").resolve("opensecretsanta-1.0.0-1.apk"), byteArrayOf(1))

        assertNull(latest())
    }
}
