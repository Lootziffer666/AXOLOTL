package app.axolotl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiEndpointPolicyTest {
    @Test
    fun `accepts an HTTPS chat completions endpoint`() {
        assertNull(AiEndpointPolicy.validate("https://api.example.com/v1/chat/completions"))
    }

    @Test
    fun `rejects cleartext embedded credentials and unrelated paths`() {
        assertEquals("Only HTTPS endpoints are allowed", AiEndpointPolicy.validate("http://api.example.com/v1/chat/completions"))
        assertEquals(
            "Credentials must not be embedded in the endpoint",
            AiEndpointPolicy.validate("https://user:pass@example.com/v1/chat/completions"),
        )
        assertEquals(
            "Endpoint must target chat/completions",
            AiEndpointPolicy.validate("https://api.example.com/v1/models"),
        )
    }
}
