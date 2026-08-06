package app.axolotl

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import app.axolotl.data.DockSettings
import app.axolotl.ui.DockViewModel
import app.axolotl.ui.SettingsViewModel
import app.axolotl.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class AxolotlFrameScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun axolotl_frame_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme { AxolotlFrame(onOpenBorderline = {}) }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/axolotl-frame.png")
  }

  @Test
  fun clipboard_privacy_controls_screenshot() {
    val application = ApplicationProvider.getApplicationContext<Application>()

    composeTestRule.setContent {
      MyApplicationTheme {
        ProvisioningTab(
          settings = DockSettings(),
          settingsViewModel = SettingsViewModel(application),
          dockViewModel = DockViewModel(application)
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(
      filePath = "build/outputs/roborazzi/clipboard-privacy-controls.png"
    )
  }
}
