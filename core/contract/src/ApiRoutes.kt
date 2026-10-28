package com.usuallycoding.opensecretsanta.contract

object ApiRoutes {
    const val HEALTH = "/health"

    const val LOGIN = "/auth/login"

    const val ME = "/me"
    const val ME_PASSWORD = "/me/password"

    const val ADMIN_USERS = "/admin/users"
    const val ADMIN_USER = "/admin/users/{username}"
    const val ADMIN_USER_RESET_PASSWORD = "/admin/users/{username}/reset-password"

    const val ADMIN_GROUPS = "/admin/groups"
    const val ADMIN_GROUP = "/admin/groups/{id}"
    const val ADMIN_GROUP_PARTICIPANTS = "/admin/groups/{id}/participants"
    const val ADMIN_GROUP_PENDING = "/admin/groups/{id}/pending"
    const val ADMIN_GROUP_ACTIVATE = "/admin/groups/{id}/activate"
    const val ADMIN_GROUP_ARCHIVE = "/admin/groups/{id}/archive"
    const val ADMIN_GROUP_ASSIGNMENTS = "/admin/groups/{id}/assignments"
    const val ADMIN_GROUP_PARTICIPANT_WISHLIST = "/admin/groups/{id}/participants/{username}/wishlist"
    const val ADMIN_GROUP_QUESTIONS = "/admin/groups/{id}/questions"

    const val GROUPS = "/groups"
    const val GROUP = "/groups/{id}"

    const val GROUP_RECEIVER = "/groups/{id}/receiver"
    const val GROUP_SANTA = "/groups/{id}/santa"
    const val GROUP_RECEIVER_QUESTIONS = "/groups/{id}/receiver/questions"
    const val GROUP_SANTA_QUESTIONS = "/groups/{id}/santa/questions"
    const val GROUP_SANTA_QUESTION_ANSWER = "/groups/{id}/santa/questions/{number}/answer"

    const val GROUP_WISHLIST = "/groups/{id}/wishlist"
    const val GROUP_WISHLIST_ITEMS = "/groups/{id}/wishlist/items"
    const val GROUP_WISHLIST_ITEM = "/groups/{id}/wishlist/items/{itemId}"
    const val GROUP_WISHLIST_ORDER = "/groups/{id}/wishlist/order"
    const val GROUP_RECEIVER_WISHLIST = "/groups/{id}/receiver/wishlist"

    fun groupWishlist(id: Long) = "$GROUPS/$id/wishlist"
    fun groupWishlistItems(id: Long) = "$GROUPS/$id/wishlist/items"
    fun groupWishlistItem(id: Long, itemId: Long) = "$GROUPS/$id/wishlist/items/$itemId"
    fun groupWishlistOrder(id: Long) = "$GROUPS/$id/wishlist/order"
    fun groupReceiverWishlist(id: Long) = "$GROUPS/$id/receiver/wishlist"

    fun groupReceiver(id: Long) = "$GROUPS/$id/receiver"
    fun groupSanta(id: Long) = "$GROUPS/$id/santa"
    fun groupReceiverQuestions(id: Long) = "$GROUPS/$id/receiver/questions"
    fun groupSantaQuestions(id: Long) = "$GROUPS/$id/santa/questions"
    fun groupSantaQuestionAnswer(id: Long, number: Int) = "$GROUPS/$id/santa/questions/$number/answer"

    fun adminGroup(id: Long) = "$ADMIN_GROUPS/$id"
    fun adminGroupParticipants(id: Long) = "$ADMIN_GROUPS/$id/participants"
    fun adminGroupPending(id: Long) = "$ADMIN_GROUPS/$id/pending"
    fun adminGroupActivate(id: Long) = "$ADMIN_GROUPS/$id/activate"
    fun adminGroupArchive(id: Long) = "$ADMIN_GROUPS/$id/archive"
    fun adminGroupAssignments(id: Long) = "$ADMIN_GROUPS/$id/assignments"
    fun adminGroupParticipantWishlist(id: Long, username: String) = "$ADMIN_GROUPS/$id/participants/$username/wishlist"
    fun adminGroupQuestions(id: Long) = "$ADMIN_GROUPS/$id/questions"
    fun group(id: Long) = "$GROUPS/$id"

    fun adminUser(username: String) = "$ADMIN_USERS/$username"
    fun adminUserResetPassword(username: String) = "$ADMIN_USERS/$username/reset-password"
}
