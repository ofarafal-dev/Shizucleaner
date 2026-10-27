package dev.ofarafal.shizucleaner.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import dev.ofarafal.shizucleaner.BuildConfig
import dev.ofarafal.shizucleaner.IShellService
import dev.ofarafal.shizucleaner.service.ShellService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

enum class ShizukuState { NOT_RUNNING, UNSUPPORTED, NO_PERMISSION, CONNECTING, READY }

data class ShellResult(val exitCode: Int, val output: String) {
    val ok: Boolean get() = exitCode == 0
}

class ShizukuClient(context: Context) {

    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(ShizukuState.NOT_RUNNING)
    val state: StateFlow<ShizukuState> = _state

    @Volatile
    private var service: IShellService? = null

    private val args = Shizuku.UserServiceArgs(
        ComponentName(appContext.packageName, ShellService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("shell")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = IShellService.Stub.asInterface(binder)
            _state.value = ShizukuState.READY
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            _state.value = ShizukuState.CONNECTING
        }
    }

    private val binderReceived = Shizuku.OnBinderReceivedListener { refresh() }
    private val binderDead = Shizuku.OnBinderDeadListener {
        service = null
        _state.value = ShizukuState.NOT_RUNNING
    }
    private val permissionResult = Shizuku.OnRequestPermissionResultListener { _, result ->
        if (result == PackageManager.PERMISSION_GRANTED) {
            refresh()
        } else {
            _state.value = ShizukuState.NO_PERMISSION
        }
    }

    fun start() {
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permissionResult)
        refresh()
    }

    fun stop() {
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
        Shizuku.removeRequestPermissionResultListener(permissionResult)
        if (service != null) {
            runCatching { Shizuku.unbindUserService(args, connection, true) }
            service = null
        }
    }

    fun refresh() {
        when {
            !Shizuku.pingBinder() -> _state.value = ShizukuState.NOT_RUNNING
            Shizuku.isPreV11() -> _state.value = ShizukuState.UNSUPPORTED
            Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED ->
                _state.value = ShizukuState.NO_PERMISSION
            service != null -> _state.value = ShizukuState.READY
            else -> {
                _state.value = ShizukuState.CONNECTING
                Shizuku.bindUserService(args, connection)
            }
        }
    }

    fun requestPermission() {
        if (Shizuku.pingBinder() && !Shizuku.isPreV11()) Shizuku.requestPermission(REQUEST_CODE)
    }

    suspend fun exec(vararg cmd: String): ShellResult = withContext(Dispatchers.IO) {
        val s = service ?: throw IllegalStateException("Usługa Shizuku nie jest połączona")
        val raw = s.exec(arrayOf(*cmd))
        val nl = raw.indexOf('\n')
        val head = if (nl >= 0) raw.substring(0, nl) else raw
        val code = head.removePrefix("EXIT:").toIntOrNull() ?: -1
        ShellResult(code, if (nl >= 0) raw.substring(nl + 1) else "")
    }

    /** Skan plików w serwisie shell (kinds: appdirs, large, dups, apks, emptydirs). Zwraca surowe linie. */
    suspend fun scan(kind: String, vararg args: String): String = withContext(Dispatchers.IO) {
        val s = service ?: throw IllegalStateException("Usługa Shizuku nie jest połączona")
        s.scan(kind, arrayOf(*args))
    }

    /** Kasowanie ścieżek z nadpisaniem (passes 0..3). Zwraca surowe linie OK/ERR. */
    suspend fun delete(paths: List<String>, passes: Int): String = withContext(Dispatchers.IO) {
        val s = service ?: throw IllegalStateException("Usługa Shizuku nie jest połączona")
        s.delete(paths.toTypedArray(), passes)
    }

    /** Kopia APK (źródła z /data/app) do pamięci współdzielonej. Zwraca surowe "OK\t..." albo "E\t...". */
    suspend fun backup(srcs: List<String>, pkg: String): String = withContext(Dispatchers.IO) {
        val s = service ?: throw IllegalStateException("Usługa Shizuku nie jest połączona")
        s.backup(srcs.toTypedArray(), pkg)
    }

    private companion object {
        const val REQUEST_CODE = 1001
    }
}
