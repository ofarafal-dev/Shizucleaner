package dev.ofarafal.shizucleaner.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandPolicyTest {

    private fun ok(vararg c: String) = CommandPolicy.isAllowed(arrayOf(*c))

    @Test
    fun allowsExpectedCommands() {
        assertTrue(ok("pm", "uninstall", "-k", "--user", "0", "com.facebook.katana"))
        assertTrue(ok("pm", "uninstall", "--user", "0", "com.some.app"))
        assertTrue(ok("pm", "disable-user", "--user", "0", "com.some.app"))
        assertTrue(ok("pm", "clear", "--user", "0", "com.some.app"))
        assertTrue(ok("pm", "enable", "--user", "0", "com.some.app"))
        assertTrue(ok("pm", "install-existing", "--user", "0", "com.some.app"))
        assertTrue(ok("pm", "trim-caches", "1000G"))
        assertTrue(ok("dumpsys", "diskstats"))
    }

    @Test
    fun rejectsUnknownCommandsAndInjection() {
        assertFalse(ok("rm", "-rf", "/sdcard"))
        assertFalse(ok("sh", "-c", "pm list packages"))
        assertFalse(ok("pm", "uninstall", "--user", "0", "com.some.app; rm -rf /"))
        assertFalse(ok("pm", "uninstall", "--user", "0", "com.some.app", "extra"))
        assertFalse(ok("pm", "uninstall", "--user", "all", "com.some.app"))
        assertFalse(ok("pm", "trim-caches", "1000G; id"))
        assertFalse(ok("pm", "trim-caches", "-1"))
        assertFalse(ok("dumpsys", "activity"))
        assertFalse(ok())
    }

    @Test
    fun destructiveCommandsRefuseCriticalPackages() {
        for (cmd in listOf("uninstall", "disable-user", "clear")) {
            val c = if (cmd == "uninstall") arrayOf("pm", cmd, "--user", "0", "com.android.systemui")
            else arrayOf("pm", cmd, "--user", "0", "com.android.systemui")
            assertFalse(cmd, CommandPolicy.isAllowed(c))
        }
        assertFalse(ok("pm", "uninstall", "-k", "--user", "0", "com.google.android.gms"))
        assertFalse(ok("pm", "clear", "--user", "0", "com.android.phone"))
        assertFalse(ok("pm", "uninstall", "-k", "--user", "0", "com.samsung.android.knox.containercore"))
    }

    @Test
    fun restoringCriticalPackagesIsAllowed() {
        assertTrue(ok("pm", "enable", "--user", "0", "com.android.systemui"))
        assertTrue(ok("pm", "install-existing", "--user", "0", "com.google.android.gms"))
    }

    @Test
    fun firewallCommandsAreValidated() {
        assertTrue(ok("cmd", "connectivity", "set-chain3-enabled", "true"))
        assertFalse(ok("cmd", "connectivity", "set-chain3-enabled", "false"))
        assertTrue(ok("cmd", "connectivity", "set-package-networking-enabled", "false", "com.some.app"))
        assertTrue(ok("cmd", "connectivity", "set-package-networking-enabled", "true", "com.some.app"))
        assertFalse(ok("cmd", "connectivity", "set-package-networking-enabled", "maybe", "com.some.app"))
        assertFalse(ok("cmd", "connectivity", "set-package-networking-enabled", "false", "com.some.app; id"))
        // blokowanie internetu pakietom krytycznym jest odrzucone, odblokowanie nie
        assertFalse(ok("cmd", "connectivity", "set-package-networking-enabled", "false", "com.android.phone"))
        assertTrue(ok("cmd", "connectivity", "set-package-networking-enabled", "true", "com.android.phone"))
    }

    @Test
    fun revokeOnlyKnownRuntimePermissions() {
        assertTrue(ok("pm", "revoke", "--user", "0", "com.some.app", "android.permission.CAMERA"))
        assertTrue(ok("pm", "revoke", "--user", "0", "com.some.app", "android.permission.ACCESS_BACKGROUND_LOCATION"))
        assertFalse(ok("pm", "revoke", "--user", "0", "com.some.app", "android.permission.INSTALL_PACKAGES"))
        assertFalse(ok("pm", "revoke", "--user", "0", "com.some.app", "android.permission.READ_LOGS"))
        assertFalse(ok("pm", "revoke", "--user", "0", "com.some.app", "com.custom.permission.X"))
        assertFalse(ok("pm", "revoke", "--user", "0", "com.android.phone", "android.permission.CAMERA"))
        assertFalse(ok("pm", "revoke", "com.some.app", "android.permission.CAMERA"))
    }

    @Test
    fun installOnlyFromOwnBackupFolder() {
        val dir = "/storage/emulated/0/ShizuCleaner/backups"
        assertTrue(ok("pm", "install", "-r", "$dir/com.some.app/base.apk"))
        assertFalse(ok("pm", "install", "-r", "$dir/../../Download/evil.apk"))
        assertFalse(ok("pm", "install", "-r", "$dir/com.some.app/../x.apk"))
        assertFalse(ok("pm", "install", "-r", "/storage/emulated/0/Download/evil.apk"))
        assertFalse(ok("pm", "install", "-r", "$dir/com.some.app/base.apk; id"))
        assertFalse(ok("pm", "install", "-r", "-d", "$dir/com.some.app/base.apk"))
        assertFalse(ok("pm", "install", "$dir/com.some.app/base.apk"))
    }
}
