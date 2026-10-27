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
}
