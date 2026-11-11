package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.server.data.download.ApkDownloads
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.http.content.LocalFileContent
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

private val ANDROID_APP = ContentType("application", "vnd.android.package-archive")

/**
 * The newest Android app as a file download, for anyone. The file is the one [ApkDownloads] picks from the folder, never one a
 * request names, and the route is this exact path, so the folder cannot be listed or reached into.
 */
fun Route.downloadRoutes(downloads: ApkDownloads) {
    get(ApiRoutes.DOWNLOAD_LATEST) {
        val file = blocking { downloads.latest() } ?: throw DomainException.NotFound("There is no Android app to download yet.")
        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, file.fileName.toString()).toString(),
        )
        call.respond(LocalFileContent(file.toFile(), ANDROID_APP))
    }
}
