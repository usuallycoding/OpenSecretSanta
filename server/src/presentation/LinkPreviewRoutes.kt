package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.LinkPreviewDto
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.wishlist.LinkPreview
import com.usuallycoding.opensecretsanta.domain.wishlist.LinkPreviewResolver
import com.usuallycoding.opensecretsanta.domain.wishlist.LinkPreviewStatus
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

internal fun Map<String, LinkPreview>.toDtoMap(urls: List<String>): Map<String, LinkPreviewDto> =
    buildMap {
        for (url in urls) {
            val preview = this@toDtoMap[url] ?: continue
            when (preview.status) {
                LinkPreviewStatus.PENDING -> put(url, LinkPreviewDto(status = LinkPreviewDto.STATUS_PENDING))
                LinkPreviewStatus.READY -> put(
                    url,
                    LinkPreviewDto(
                        status = LinkPreviewDto.STATUS_READY,
                        title = preview.title,
                        siteName = preview.siteName,
                        imagePath = if (preview.hasImage) ApiRoutes.linkPreviewImage(preview.id) else null,
                    ),
                )
                LinkPreviewStatus.FAILED -> Unit
            }
        }
    }

/** Serves server-cached link preview thumbnails to authenticated users. */
fun Route.linkPreviewRoutes(previews: LinkPreviewResolver) {
    get(ApiRoutes.LINK_PREVIEW_IMAGE) {
        val id = call.parameters["id"]?.toLongOrNull() ?: throw DomainException.NotFound("Link preview not found.")
        val image = blocking { previews.findImage(id) } ?: throw DomainException.NotFound("Link preview not found.")
        call.response.header(HttpHeaders.CacheControl, CacheControl.MaxAge(maxAgeSeconds = 86_400, visibility = CacheControl.Visibility.Private).toString())
        call.respondBytes(image.bytes, ContentType.parse(image.contentType))
    }
}
