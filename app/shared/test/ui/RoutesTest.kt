package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.navigation.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoutesTest {
    private val routes: List<SerialDescriptor> = listOf(
        serializer<GroupRoute>().descriptor,
        serializer<GifterRoute>().descriptor,
        serializer<ReceiverRoute>().descriptor,
        serializer<GifterWishlistRoute>().descriptor,
        serializer<GifterQuestionsRoute>().descriptor,
        serializer<ReceiverWishlistRoute>().descriptor,
        serializer<ReceiverQuestionsRoute>().descriptor,
        serializer<InboxRoute>().descriptor,
        serializer<SendLetterRoute>().descriptor,
        serializer<AnnouncementsRoute>().descriptor,
        serializer<AdminUserRoute>().descriptor,
        serializer<AdminGroupRoute>().descriptor,
        serializer<AdminGroupEditRoute>().descriptor,
    )

    // Navigation on the web and iOS cannot carry an enum or any other class in a route: it fails when the screens are built,
    // right after sign-in, which no unit test of a route would show
    @Test
    fun everyArgumentOfARouteIsAPlainValue() {
        for (route in routes) {
            for (i in 0 until route.elementsCount) {
                assertTrue(route.getElementDescriptor(i).kind is PrimitiveKind, "${route.serialName}.${route.getElementName(i)}")
            }
        }
    }

    @Test
    fun everyPaneRouteTakesJustTheGroup() {
        for (pane in GroupPane.entries) {
            val route = pane.route(1)
            val descriptor = when (route) {
                is AnnouncementsRoute -> serializer<AnnouncementsRoute>().descriptor
                is GifterWishlistRoute -> serializer<GifterWishlistRoute>().descriptor
                is GifterQuestionsRoute -> serializer<GifterQuestionsRoute>().descriptor
                is InboxRoute -> serializer<InboxRoute>().descriptor
                is ReceiverWishlistRoute -> serializer<ReceiverWishlistRoute>().descriptor
                is ReceiverQuestionsRoute -> serializer<ReceiverQuestionsRoute>().descriptor
                is SendLetterRoute -> serializer<SendLetterRoute>().descriptor
                else -> error("${pane.name} has a route that is not a pane's")
            }
            assertEquals(1, descriptor.elementsCount, pane.name)
            assertEquals("groupId", descriptor.getElementName(0), pane.name)
        }
    }
}
