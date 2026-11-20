package com.usuallycoding.opensecretsanta.server.presentation

import com.usuallycoding.opensecretsanta.server.notification.NotificationDispatcher
import com.usuallycoding.opensecretsanta.contract.ApiRoutes
import com.usuallycoding.opensecretsanta.contract.ReorderWishlistRequest
import com.usuallycoding.opensecretsanta.contract.SharedWishlistItemDto
import com.usuallycoding.opensecretsanta.contract.WishlistItemDto
import com.usuallycoding.opensecretsanta.contract.WishlistItemRequest
import com.usuallycoding.opensecretsanta.domain.error.DomainException
import com.usuallycoding.opensecretsanta.domain.wishlist.AddWishlistItem
import com.usuallycoding.opensecretsanta.domain.wishlist.DeleteWishlistItem
import com.usuallycoding.opensecretsanta.domain.wishlist.GetMyWishlist
import com.usuallycoding.opensecretsanta.domain.wishlist.GetReceiverWishlist
import com.usuallycoding.opensecretsanta.domain.wishlist.LinkPreview
import com.usuallycoding.opensecretsanta.domain.wishlist.LinkPreviewResolver
import com.usuallycoding.opensecretsanta.domain.wishlist.ReorderWishlist
import com.usuallycoding.opensecretsanta.domain.wishlist.SharedWishlistItem
import com.usuallycoding.opensecretsanta.domain.wishlist.UpdateWishlistItem
import com.usuallycoding.opensecretsanta.domain.wishlist.WishlistItem
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import java.time.Instant

private fun iso(millis: Long): String = Instant.ofEpochMilli(millis).toString()

private fun WishlistItem.toDto(previews: Map<String, LinkPreview>) =
    WishlistItemDto(id, title, note, urls.firstOrNull(), iso(createdAtMillis), iso(updatedAtMillis), urls, previews.toDtoMap(urls))

private fun SharedWishlistItem.toDto(previews: Map<String, LinkPreview>) =
    SharedWishlistItemDto(title, note, urls.firstOrNull(), iso(createdAtMillis), iso(updatedAtMillis), urls, previews.toDtoMap(urls))

/** A non-numeric item id can never match an item, so it is reported like any other missing item. */
private fun ApplicationCall.itemId(): Long =
    parameters["itemId"]?.toLongOrNull() ?: throw DomainException.NotFound("Item not found.")

/** A participant's own wishlist for one group. */
fun Route.wishlistRoutes(
    getWishlist: GetMyWishlist,
    addItem: AddWishlistItem,
    updateItem: UpdateWishlistItem,
    deleteItem: DeleteWishlistItem,
    reorder: ReorderWishlist,
    notifications: NotificationDispatcher,
    previews: LinkPreviewResolver,
) {
    get(ApiRoutes.GROUP_WISHLIST) {
        val user = call.currentUser()
        val items = blocking { getWishlist(user, call.groupId()) }
        val previewMap = blocking { previews.lookupAndEnqueue(items.flatMap { it.urls }) }
        call.respond(items.map { it.toDto(previewMap) })
    }
    post(ApiRoutes.GROUP_WISHLIST_ITEMS) {
        val user = call.currentUser()
        val groupId = call.groupId()
        val request = call.receive<WishlistItemRequest>()
        val item = blocking { addItem(user, groupId, request.title, request.note, request.urls, request.url) }
        notifications.wishlistChanged(groupId, user.username)
        val previewMap = blocking { previews.lookupAndEnqueue(item.urls) }
        call.respond(HttpStatusCode.Created, item.toDto(previewMap))
    }
    put(ApiRoutes.GROUP_WISHLIST_ITEM) {
        val user = call.currentUser()
        val groupId = call.groupId()
        val itemId = call.itemId()
        val request = call.receive<WishlistItemRequest>()
        val item = blocking { updateItem(user, groupId, itemId, request.title, request.note, request.urls, request.url) }
        notifications.wishlistChanged(groupId, user.username)
        val previewMap = blocking { previews.lookupAndEnqueue(item.urls) }
        call.respond(item.toDto(previewMap))
    }
    delete(ApiRoutes.GROUP_WISHLIST_ITEM) {
        val user = call.currentUser()
        val groupId = call.groupId()
        val itemId = call.itemId()
        blocking { deleteItem(user, groupId, itemId) }
        notifications.wishlistChanged(groupId, user.username)
        call.respond(HttpStatusCode.NoContent)
    }
    put(ApiRoutes.GROUP_WISHLIST_ORDER) {
        val user = call.currentUser()
        val groupId = call.groupId()
        val request = call.receive<ReorderWishlistRequest>()
        val items = blocking { reorder(user, groupId, request.itemIds) }
        val previewMap = blocking { previews.lookupAndEnqueue(items.flatMap { it.urls }) }
        call.respond(items.map { it.toDto(previewMap) })
    }
}

/** What a Santa may read: their receiver's list for this group, without ids and without any record of the read. */
fun Route.receiverWishlistRoutes(getReceiverWishlist: GetReceiverWishlist, previews: LinkPreviewResolver) {
    get(ApiRoutes.GROUP_RECEIVER_WISHLIST) {
        val user = call.currentUser()
        val items = blocking { getReceiverWishlist(user, call.groupId()) }
        val previewMap = blocking { previews.lookupAndEnqueue(items.flatMap { it.urls }) }
        call.respond(items.map { it.toDto(previewMap) })
    }
}
