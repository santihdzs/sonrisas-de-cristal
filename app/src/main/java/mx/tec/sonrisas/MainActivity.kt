package mx.tec.sonrisas

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import mx.tec.sonrisas.ui.AppViewModelProvider
import mx.tec.sonrisas.ui.navigation.SonrisasApp
import mx.tec.sonrisas.ui.state.SesionViewModel
import mx.tec.sonrisas.ui.theme.SonrisasTheme

// FragmentActivity porque BiometricPrompt la necesita
class MainActivity : FragmentActivity() {

    private val sesionViewModel: SesionViewModel by viewModels { AppViewModelProvider.Factory }

    // strong + credencial no existe antes de android 11, ahi se usa weak + credencial
    private val autenticadores: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        else BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // al mandar la app a segundo plano se bloquea (RF-13); girar la pantalla no cuenta
        lifecycle.addObserver(
            LifecycleEventObserver { _, evento ->
                if (evento == Lifecycle.Event.ON_STOP && !isChangingConfigurations) sesionViewModel.alIrASegundoPlano()
            },
        )
        setContent {
            SonrisasTheme {
                SonrisasApp(sesionViewModel, onDesbloquear = ::desbloquear)
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        sesionViewModel.registrarInteraccion()
    }

    private fun desbloquear() {
        when (BiometricManager.from(this).canAuthenticate(autenticadores)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Unit
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                sesionViewModel.avisarBloqueo(
                    "Este dispositivo no tiene huella ni bloqueo de pantalla configurados. Configúralos en Ajustes o toca \"Cambiar de usuario\" para entrar con tu contraseña.",
                )
                return
            }
            else -> {
                sesionViewModel.avisarBloqueo(
                    "Este dispositivo no permite desbloquear con huella. Toca \"Cambiar de usuario\" para entrar con tu contraseña.",
                )
                return
            }
        }
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    sesionViewModel.desbloquear()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    val cancelado = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    sesionViewModel.avisarBloqueo(
                        if (cancelado) null else "No pudimos verificar tu identidad ($errString). Inténtalo de nuevo o cambia de usuario.",
                    )
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear Sonrisas de Cristal")
            .setSubtitle("Usa tu huella o el bloqueo de pantalla")
            .setAllowedAuthenticators(autenticadores)
            .build()
        prompt.authenticate(info)
    }
}
