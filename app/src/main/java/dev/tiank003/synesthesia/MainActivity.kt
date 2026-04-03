package dev.tiank003.synesthesia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.tiank003.synesthesia.ui.navigation.NavGraph
import dev.tiank003.synesthesia.ui.theme.SynesthesiaTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SynesthesiaTheme {
                NavGraph()
            }
        }
    }
}
