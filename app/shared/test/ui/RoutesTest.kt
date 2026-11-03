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
    fun aPageOpensOnTheTabItsRouteAsksFor() {
        assertEquals(PageTab.Wishlist, GifterRoute(1).tab)
        assertEquals(PageTab.Questions, GifterRoute(1, questions = true).tab)
        assertEquals(PageTab.Wishlist, ReceiverRoute(1).tab)
        assertEquals(PageTab.Questions, ReceiverRoute(1, questions = true).tab)
    }
}
