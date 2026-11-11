package com.usuallycoding.opensecretsanta.server.data.download

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * The Android apps in a folder, of which the newest is what `/download_latest` serves. An app is a readable file whose name ends
 * in `.apk`: a name like `opensecretsanta-1.1.5-28900000.apk` (what `scripts/distribute-android.sh` writes) is ordered by its
 * version code, as a number, and any other name comes after all of those, the most recently modified first. A file still being
 * copied has another name (`....apk.part`) and so is never offered. Names always come from the folder, never from a request.
 */
class ApkDownloads(private val directory: Path) {

    /** The newest app, or null when there is none or the folder is missing or cannot be read. */
    fun latest(): Path? = try {
        Files.newDirectoryStream(directory).use { entries ->
            entries.filter { Files.isRegularFile(it) && Files.isReadable(it) && it.fileName.toString().endsWith(APK) }
                .map { Candidate(it, versionCode(it), Files.getLastModifiedTime(it).toMillis()) }
                .maxWithOrNull(NEWEST)
                ?.path
        }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }

    private class Candidate(val path: Path, val code: Long?, val modifiedMillis: Long)

    private fun versionCode(file: Path): Long? = NAME.matchEntire(file.fileName.toString())?.groupValues?.get(1)?.toLongOrNull()

    private companion object {
        const val APK = ".apk"
        val NAME = Regex("opensecretsanta-.+-(\\d+)\\.apk")

        /** Coded names beat the others, by code; the others go by modification time; names settle a tie so the choice never wobbles. */
        val NEWEST: Comparator<Candidate> = Comparator { a, b ->
            val ordering = when {
                a.code != null && b.code != null -> a.code.compareTo(b.code)
                a.code != null -> 1
                b.code != null -> -1
                else -> a.modifiedMillis.compareTo(b.modifiedMillis)
            }
            if (ordering != 0) ordering else a.path.fileName.toString().compareTo(b.path.fileName.toString())
        }
    }
}
