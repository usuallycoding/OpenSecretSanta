package com.usuallycoding.opensecretsanta.ui

/** Every sentence the app shows on its own, in one place so a later translation has one place to start. */
object Strings {
    const val APP_NAME = "Open Secret Santa"

    const val USERNAME = "Username"
    const val PASSWORD = "Password"
    const val SIGN_IN = "Sign in"
    const val SIGN_OUT = "Sign out"
    const val SIGNING_IN = "Signing in..."

    const val SESSION_ENDED = "You have been signed out. Please sign in again."
    const val WRONG_CREDENTIALS = "Wrong username or password."
    const val SERVER_UNREACHABLE = "The server could not be reached. Check your connection and try again."
    const val SERVER_FAILED = "Something went wrong on the server. Please try again."

    fun signInPaused(minutes: Int?): String =
        if (minutes == null) {
            "Too many attempts. Sign-in is paused for a while, please try again later."
        } else {
            "Too many attempts. Sign-in is paused for about $minutes ${if (minutes == 1) "minute" else "minutes"}."
        }

    const val CHANGE_PASSWORD_TITLE = "Choose a new password"
    const val CHANGE_PASSWORD_INTRO = "Your password is temporary. Choose your own before you continue."
    const val CURRENT_PASSWORD = "Current password"
    const val NEW_PASSWORD = "New password"
    const val REPEAT_PASSWORD = "Repeat new password"
    const val CHANGE_PASSWORD = "Change password"
    const val CHANGING_PASSWORD = "Changing..."
    const val PASSWORDS_DO_NOT_MATCH = "The two passwords do not match."

    const val STATE_PENDING = "Pending"
    const val STATE_ACTIVE = "Active"
    const val STATE_ARCHIVED = "Archived"
    const val STATE_PENDING_NOTE = "The organiser has not drawn the names yet."
    const val STATE_ACTIVE_NOTE = "The names have been drawn."
    const val STATE_ARCHIVED_NOTE = "This exchange is over."

    fun participantCount(count: Int) = if (count == 1) "1 participant" else "$count participants"

    const val GROUPS_EMPTY_TITLE = "You are not in any group yet."
    const val GROUPS_EMPTY_BODY = "An organiser adds people to a group by username. Once you are added, it shows up here."
    const val GROUP_GONE = "This group is no longer available."
    const val GROUP_PARTICIPANTS = "Participants"
    const val YOU = "you"

    const val PROFILE_WHY = "A Santa uses your name and address to post your gift."
    const val PROFILE_INCOMPLETE = "Add your name and address so a gift can reach you."
    const val SAVE = "Save"
    const val SAVING = "Saving..."
    const val SAVED = "Saved."

    const val WISHLISTS = "Wishlists"
    const val MY_WISHLIST = "My wishlist"
    const val RECEIVER_WISHLIST = "Your receiver's wishlist"
    const val RECEIVER_WISHLIST_LATER = "Your receiver's wishlist appears here after the names are drawn."

    const val ADD_ITEM = "Add item"
    const val OK = "OK"
    const val CANCEL = "Cancel"
    const val DELETE = "Delete"
    const val ADD_ITEM_TITLE = "Add an item"
    const val EDIT_ITEM_TITLE = "Edit item"
    const val FIELD_TITLE = "Title"
    const val FIELD_NOTE = "Note (optional)"
    const val FIELD_URL = "Link (optional)"
    const val DELETE_ITEM_TITLE = "Delete this item?"
    fun deleteItemBody(title: String) = "\"$title\" will be removed from your list."
    const val MOVE_UP = "Move up"
    const val MOVE_DOWN = "Move down"
    const val WISHLIST_EMPTY = "You have not added anything to this list yet."
    const val WISHLIST_CLOSED = "This list can no longer be changed."
    fun wishlistFull(max: Int) = "Your list is full ($max items). Delete one to add another."
    const val RECEIVER_WISHLIST_EMPTY = "Your receiver has not added anything yet."
    const val NOTICE_CHANGED_ELSEWHERE = "Your list was changed elsewhere, so it has been reloaded."
    const val NOTICE_ITEM_GONE = "That item no longer exists. The list has been reloaded."
    const val NOTICE_LOCKED = "This list can no longer be changed. It has been reloaded."
    const val NOTICE_LIST_FULL = "Your list is full, so the item was not added."

    const val TRY_AGAIN = "Try again"
    const val REFRESH = "Refresh"

    const val LOCAL_NETWORK_TITLE = "Allow access to your local network"
    const val LOCAL_NETWORK_WHY = "The server is on a private network (this computer or your home or office network). Android needs your permission before this app can connect to it."
    const val LOCAL_NETWORK_CONTINUE = "Continue"
    const val LOCAL_NETWORK_DENIED = "The server cannot be reached without access to your local network."
    const val LOCAL_NETWORK_TRY_AGAIN = "Try again"
    const val LOCAL_NETWORK_SETTINGS_HINT = "Android will not ask again. Allow \"Nearby devices\" or local network access for this app in its settings."
    const val LOCAL_NETWORK_OPEN_SETTINGS = "Open settings"

    const val TITLE_GROUPS = "Groups"
    const val TITLE_GROUP = "Group"
    const val TITLE_PROFILE = "Profile"
    const val TITLE_ADMIN = "Admin"
    const val BACK = "Back"
    const val MENU = "Menu"
    const val SWITCH_TO_ADMIN = "Switch to admin view"
    const val SWITCH_TO_PARTICIPANT = "Switch to participant view"

    const val PARTICIPANT_VIEW = "Participant"
    const val ADMIN_VIEW = "Admin"

    fun signedInAs(username: String) = "Signed in as $username"
    fun currentView(view: String) = "$view view"
    const val PARTICIPANT_PLACEHOLDER = "Your groups, wishlist and questions will appear here."
    const val ADMIN_PLACEHOLDER = "Group management will appear here."
}
