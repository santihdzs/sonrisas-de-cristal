package mx.tec.sonrisas

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import mx.tec.sonrisas.ui.AppViewModelProvider
import mx.tec.sonrisas.ui.navigation.SonrisasApp
import mx.tec.sonrisas.ui.state.SesionViewModel
import mx.tec.sonrisas.ui.theme.SonrisasTheme

class MainActivity : FragmentActivity() {

    private val sesionViewModel: SesionViewModel by viewModels { AppViewModelProvider.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SonrisasTheme {
                SonrisasApp(sesionViewModel)
            }
        }
    }
}
