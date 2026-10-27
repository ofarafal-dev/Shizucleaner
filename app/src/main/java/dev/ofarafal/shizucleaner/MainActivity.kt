package dev.ofarafal.shizucleaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dev.ofarafal.shizucleaner.ui.AppTheme
import dev.ofarafal.shizucleaner.ui.MainScreen
import dev.ofarafal.shizucleaner.ui.MainViewModel

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme { MainScreen(vm) }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }
}
