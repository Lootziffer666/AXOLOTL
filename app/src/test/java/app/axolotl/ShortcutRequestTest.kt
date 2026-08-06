package app.axolotl

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShortcutRequestTest {
    @Test
    fun browsableLinkRequiresConfirmationAndBoundsMenu() {
        val request = ShortcutRequest.from(
            Intent(Intent.ACTION_VIEW, Uri.parse("borderline://open?menu=99"))
        )!!

        assertEquals(3, request.menuIndex)
        assertTrue(request.requiresConfirmation)
    }

    @Test
    fun internalShortcutDoesNotRequireConfirmation() {
        val request = ShortcutRequest.from(
            Intent(ShortcutRequest.ACTION_SHORTCUT).putExtra("menu_index", 2)
        )!!

        assertEquals(2, request.menuIndex)
        assertFalse(request.requiresConfirmation)
    }

    @Test
    fun unsupportedExternalHostIsRejected() {
        assertNull(
            ShortcutRequest.from(
                Intent(Intent.ACTION_VIEW, Uri.parse("borderline://appendix?text=secret"))
            )
        )
    }
}
