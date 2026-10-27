package dev.ofarafal.shizucleaner

import android.app.Application
import dev.ofarafal.shizucleaner.data.AppSettings
import dev.ofarafal.shizucleaner.data.AppsRepository
import dev.ofarafal.shizucleaner.data.CleanerRepository
import dev.ofarafal.shizucleaner.data.FirewallRepository
import dev.ofarafal.shizucleaner.data.PackageDb
import dev.ofarafal.shizucleaner.data.PermissionsRepository
import dev.ofarafal.shizucleaner.shizuku.ShizukuClient
import dev.ofarafal.shizucleaner.shizuku.ShizukuState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Współdzielone obiekty: jedno połączenie z Shizuku dla wszystkich ekranów. */
class ShizuApp : Application() {

    lateinit var shizuku: ShizukuClient
        private set
    lateinit var settings: AppSettings
        private set
    lateinit var cleaner: CleanerRepository
        private set
    lateinit var apps: AppsRepository
        private set
    lateinit var firewall: FirewallRepository
        private set
    lateinit var permissions: PermissionsRepository
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        PackageDb.load(this)
        settings = AppSettings(this)
        shizuku = ShizukuClient(this).also { it.start() }
        cleaner = CleanerRepository(this, shizuku)
        apps = AppsRepository(this, shizuku, cleaner)
        firewall = FirewallRepository(this, shizuku)
        permissions = PermissionsRepository(this, shizuku)

        // Android czyści reguły zapory po restarcie: nakładamy je ponownie, gdy tylko Shizuku jest gotowe.
        scope.launch {
            shizuku.state.collect { state ->
                if (state == ShizukuState.READY) runCatching { firewall.reapplyAll() }
            }
        }
    }
}
