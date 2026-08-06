package app.axolotl.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.util.regex.Pattern

data class DetectedContext(
    val primaryType: ContextType,
    val matchedText: String,
    val actionSuggestions: List<QuickActionOption>
)

enum class ContextType {
    URL, PHONE, EMAIL, IBAN, ADDRESS, ERROR_TRACE, GENERAL_TEXT
}

data class QuickActionOption(
    val label: String,
    val iconName: String, // "search", "call", "map", "email", "copy", "ai", "share"
    val execute: (Context) -> Unit
)

object QuickActionHelper {

    private val URL_PATTERN = Pattern.compile("https?://[\\w-]+(\\.[\\w-]+)+[/#?]?.*", Pattern.CASE_INSENSITIVE)
    private val PHONE_PATTERN = Pattern.compile("(\\+?[0-9]{1,4}[\\s-]?)?(\\(?\\d{2,5}\\)?[\\s-]?)?\\d{3,5}[\\s-]?\\d{3,5}")
    private val EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}")
    private val IBAN_PATTERN = Pattern.compile("[A-Z]{2}\\d{2}[a-zA-Z0-9]{11,30}")

    fun analyzeText(rawText: String): DetectedContext {
        val text = rawText.trim()

        if (URL_PATTERN.matcher(text).find()) {
            val matcher = URL_PATTERN.matcher(text)
            val url = if (matcher.find()) matcher.group() else text
            return DetectedContext(
                primaryType = ContextType.URL,
                matchedText = url,
                actionSuggestions = listOf(
                    QuickActionOption("Open Link in Browser", "web") { ctx ->
                        try {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                        } catch (e: Exception) {
                            Toast.makeText(ctx, "Could not open URL", Toast.LENGTH_SHORT).show()
                        }
                    },
                    QuickActionOption("Share Link", "share") { ctx ->
                        HandoffHelper.shareText(ctx, url)
                    },
                    QuickActionOption("Copy Link", "copy") { ctx ->
                        HandoffHelper.copyToClipboard(ctx, "URL", url)
                    }
                )
            )
        }

        if (EMAIL_PATTERN.matcher(text).find()) {
            val matcher = EMAIL_PATTERN.matcher(text)
            val email = if (matcher.find()) matcher.group() else text
            return DetectedContext(
                primaryType = ContextType.EMAIL,
                matchedText = email,
                actionSuggestions = listOf(
                    QuickActionOption("Send Email", "email") { ctx ->
                        try {
                            ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                        } catch (e: Exception) {
                            Toast.makeText(ctx, "No email client found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    QuickActionOption("Copy Email", "copy") { ctx ->
                        HandoffHelper.copyToClipboard(ctx, "Email", email)
                    }
                )
            )
        }

        if (IBAN_PATTERN.matcher(text.replace(" ", "")).find()) {
            val iban = text.replace(" ", "")
            return DetectedContext(
                primaryType = ContextType.IBAN,
                matchedText = iban,
                actionSuggestions = listOf(
                    QuickActionOption("Copy Validated IBAN", "copy") { ctx ->
                        HandoffHelper.copyToClipboard(ctx, "IBAN", iban)
                    },
                    QuickActionOption("Share IBAN Details", "share") { ctx ->
                        HandoffHelper.shareText(ctx, "IBAN: $iban")
                    }
                )
            )
        }

        if (PHONE_PATTERN.matcher(text).matches() && text.length in 7..20) {
            return DetectedContext(
                primaryType = ContextType.PHONE,
                matchedText = text,
                actionSuggestions = listOf(
                    QuickActionOption("Dial Phone Number", "call") { ctx ->
                        try {
                            ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$text")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                        } catch (e: Exception) {
                            Toast.makeText(ctx, "Could not dial", Toast.LENGTH_SHORT).show()
                        }
                    },
                    QuickActionOption("Copy Number", "copy") { ctx ->
                        HandoffHelper.copyToClipboard(ctx, "Phone", text)
                    }
                )
            )
        }

        if (text.contains("Exception") || text.contains("Error") || text.contains("at ") || text.contains("NullPointer")) {
            return DetectedContext(
                primaryType = ContextType.ERROR_TRACE,
                matchedText = text,
                actionSuggestions = listOf(
                    QuickActionOption("Search Fix on Google", "search") { ctx ->
                        HandoffHelper.searchWeb(ctx, text.take(150))
                    },
                    QuickActionOption("Build Agent Repair Prompt", "ai") { ctx ->
                        val prompt = "Analyze and fix this Android error:\n\n```\n$text\n```"
                        HandoffHelper.sendToAI(ctx, prompt)
                    },
                    QuickActionOption("Copy Error Log", "copy") { ctx ->
                        HandoffHelper.copyToClipboard(ctx, "Error Log", text)
                    }
                )
            )
        }

        if (text.lines().size <= 3 && (text.contains("Str.") || text.contains("Street") || text.contains("Road") || text.contains("Avenue") || text.matches(Regex(".*\\d{5}.*")))) {
            return DetectedContext(
                primaryType = ContextType.ADDRESS,
                matchedText = text,
                actionSuggestions = listOf(
                    QuickActionOption("Open in Google Maps", "map") { ctx ->
                        HandoffHelper.openMaps(ctx, text)
                    },
                    QuickActionOption("Copy Address", "copy") { ctx ->
                        HandoffHelper.copyToClipboard(ctx, "Address", text)
                    }
                )
            )
        }

        return DetectedContext(
            primaryType = ContextType.GENERAL_TEXT,
            matchedText = text,
            actionSuggestions = listOf(
                QuickActionOption("Search Web", "search") { ctx ->
                    HandoffHelper.searchWeb(ctx, text)
                },
                QuickActionOption("Send to AI Agent", "ai") { ctx ->
                    HandoffHelper.sendToAI(ctx, text)
                },
                QuickActionOption("Copy Text", "copy") { ctx ->
                    HandoffHelper.copyToClipboard(ctx, "Text", text)
                },
                QuickActionOption("Share Text", "share") { ctx ->
                    HandoffHelper.shareText(ctx, text)
                }
            )
        )
    }
}
