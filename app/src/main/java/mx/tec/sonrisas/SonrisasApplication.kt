package mx.tec.sonrisas

import android.app.Application
import mx.tec.sonrisas.data.AppContainer

class SonrisasApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
        container.iniciar()
    }
}
