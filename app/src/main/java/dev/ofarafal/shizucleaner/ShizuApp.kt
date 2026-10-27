package dev.ofarafal.shizucleaner

import android.app.Application
import dev.ofarafal.shizucleaner.data.AppSettings
import dev.ofarafal.shizucleaner.data.AppsRepository
import dev.ofarafal.shizucleaner.data.CleanerRepository
import dev.ofarafal.shizucleaner.data.PackageDb
import dev.ofarafal.shizucleaner.shizuku.ShizukuClient

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

    override fun onCreate() {
        super.onCreate()
        PackageDb.load(this)
        settings = AppSettings(this)
        shizuku = ShizukuClient(this).also { it.start() }
        cleaner = CleanerRepository(this, shizuku)
        apps = AppsRepository(this, shizuku, cleaner)
    }
}
