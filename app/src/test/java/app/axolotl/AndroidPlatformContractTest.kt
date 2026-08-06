package app.axolotl

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidPlatformContractTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val packageInfo = application.packageManager.getPackageInfo(
        application.packageName,
        PackageManager.GET_PERMISSIONS
    )

    @Test
    fun privilegedPackageAndLogPermissionsAreAbsent() {
        val permissions = packageInfo.requestedPermissions.orEmpty().toSet()
        assertFalse(Manifest.permission.READ_LOGS in permissions)
        assertFalse(Manifest.permission.QUERY_ALL_PACKAGES in permissions)
        assertTrue(Manifest.permission.POST_NOTIFICATIONS in permissions)
    }

    @Test
    fun overlayServiceIsPrivateAndDeclaresSpecialUseType() {
        val service = application.packageManager.getServiceInfo(
            ComponentName(application, DockOverlayService::class.java),
            PackageManager.GET_META_DATA
        )

        assertFalse(service.exported)
        assertTrue(service.foregroundServiceType and ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE != 0)
    }
}
