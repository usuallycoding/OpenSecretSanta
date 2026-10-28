package com.usuallycoding.opensecretsanta.domain.group

/**
 * What deleting a group takes with it besides the group's own details and participants: the draw, every wishlist
 * item, every question and every answer. A group with none of it can be deleted at once; one with any needs a second,
 * explicit confirmation.
 */
data class DeletionImpact(
    val wishlistItems: Int,
    val questions: Int,
    val answers: Int,
    val draw: Boolean,
) {
    val isEmpty: Boolean get() = wishlistItems == 0 && questions == 0 && answers == 0 && !draw
}
