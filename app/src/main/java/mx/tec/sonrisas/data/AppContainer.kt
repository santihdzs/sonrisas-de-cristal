package mx.tec.sonrisas.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mx.tec.sonrisas.BuildConfig
import mx.tec.sonrisas.data.fake.AlmacenMemoria
import mx.tec.sonrisas.data.fake.ArchivosDePrueba
import mx.tec.sonrisas.data.fake.DatosDePrueba
import mx.tec.sonrisas.data.fake.FakeActividadRepository
import mx.tec.sonrisas.data.fake.FakeAvisosRepository
import mx.tec.sonrisas.data.fake.FakeCuentasRepository
import mx.tec.sonrisas.data.fake.FakeEstanciasRepository
import mx.tec.sonrisas.data.fake.FakeFotosRepository
import mx.tec.sonrisas.data.fake.FakeHorariosRepository
import mx.tec.sonrisas.data.fake.FakeIncidentesRepository
import mx.tec.sonrisas.data.fake.FakeJornadaRepository
import mx.tec.sonrisas.data.fake.FakeMaterialRepository
import mx.tec.sonrisas.data.fake.FakeMensajesRepository
import mx.tec.sonrisas.data.fake.FakeNinosRepository
import mx.tec.sonrisas.data.fake.FakeObjetosRepository
import mx.tec.sonrisas.data.fake.FakeSesionRepository
import mx.tec.sonrisas.data.remoto.Cambios
import mx.tec.sonrisas.data.remoto.Remoto
import mx.tec.sonrisas.data.remoto.RemotoActividadRepository
import mx.tec.sonrisas.data.remoto.RemotoAvisosRepository
import mx.tec.sonrisas.data.remoto.RemotoCuentasRepository
import mx.tec.sonrisas.data.remoto.RemotoEstanciasRepository
import mx.tec.sonrisas.data.remoto.RemotoFotosRepository
import mx.tec.sonrisas.data.remoto.RemotoHorariosRepository
import mx.tec.sonrisas.data.remoto.RemotoIncidentesRepository
import mx.tec.sonrisas.data.remoto.RemotoJornadaRepository
import mx.tec.sonrisas.data.remoto.RemotoMaterialRepository
import mx.tec.sonrisas.data.remoto.RemotoMensajesRepository
import mx.tec.sonrisas.data.remoto.RemotoNinosRepository
import mx.tec.sonrisas.data.remoto.RemotoObjetosRepository
import mx.tec.sonrisas.data.remoto.RemotoSesionRepository
import mx.tec.sonrisas.domain.Reloj
import mx.tec.sonrisas.domain.RelojSistema
import java.io.File

// elige entre el servidor y los datos de prueba en memoria; las pantallas no notan la diferencia
class AppContainer(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val preferencias = context.getSharedPreferences("servidor", Context.MODE_PRIVATE)

    /** direccion del servidor; vacia = datos de prueba sin servidor (respaldo para la demo) */
    val servidor: String = preferencias.getString("url", null) ?: BuildConfig.SERVIDOR

    private val sinServidor = servidor.isBlank()

    /** se aplica al reiniciar la app */
    fun cambiarServidor(url: String) {
        preferencias.edit().putString("url", url.trim()).commit()
    }

    val reloj: Reloj = RelojSistema

    val archivos = AlmacenArchivos(context.filesDir)

    // los pdf de la bitacora son temporales: se regeneran en cada impresion
    val carpetaBitacoras = File(context.cacheDir, "bitacoras")

    // copias locales de lo que se baja del servidor
    private val materialRemoto = File(context.cacheDir, "material")
    private val objetosRemotos = File(context.cacheDir, "objetos")

    private val almacen by lazy { AlmacenMemoria(DatosDePrueba.generar(reloj, ArchivosDePrueba.crear(archivos))) }

    private val sesionFake by lazy { FakeSesionRepository(almacen, reloj, scope) }

    private val sesionRemota by lazy { RemotoSesionRepository(context, servidor, scope, listOf(materialRemoto, objetosRemotos)) }

    private val remoto by lazy { Remoto(sesionRemota.api, Cambios()) }

    val sesionRepository: SesionRepository by lazy { if (sinServidor) sesionFake else sesionRemota }
    val estanciasRepository: EstanciasRepository by lazy {
        if (sinServidor) FakeEstanciasRepository(almacen, sesionFake, reloj) else RemotoEstanciasRepository(remoto)
    }
    val ninosRepository: NinosRepository by lazy { if (sinServidor) FakeNinosRepository(almacen, sesionFake, reloj) else RemotoNinosRepository(remoto) }
    val jornadaRepository: JornadaRepository by lazy {
        if (sinServidor) FakeJornadaRepository(almacen, sesionFake, reloj) else RemotoJornadaRepository(remoto)
    }
    val incidentesRepository: IncidentesRepository by lazy {
        if (sinServidor) FakeIncidentesRepository(almacen, sesionFake, reloj) else RemotoIncidentesRepository(remoto)
    }
    val fotosRepository: FotosRepository by lazy { if (sinServidor) FakeFotosRepository(almacen, sesionFake, reloj) else RemotoFotosRepository(remoto) }
    val avisosRepository: AvisosRepository by lazy { if (sinServidor) FakeAvisosRepository(almacen, sesionFake, reloj) else RemotoAvisosRepository(remoto) }
    val horariosRepository: HorariosRepository by lazy {
        if (sinServidor) FakeHorariosRepository(almacen, sesionFake, reloj) else RemotoHorariosRepository(remoto)
    }
    val actividadRepository: ActividadRepository by lazy { if (sinServidor) FakeActividadRepository(almacen, sesionFake) else RemotoActividadRepository(remoto) }
    val cuentasRepository: CuentasRepository by lazy {
        if (sinServidor) FakeCuentasRepository(almacen, sesionFake, reloj) else RemotoCuentasRepository(remoto)
    }
    val objetosRepository: ObjetosPerdidosRepository by lazy {
        if (sinServidor) FakeObjetosRepository(almacen, sesionFake, archivos, reloj) else RemotoObjetosRepository(remoto, objetosRemotos)
    }
    val mensajesRepository: MensajesRepository by lazy {
        if (sinServidor) FakeMensajesRepository(almacen, sesionFake, reloj) else RemotoMensajesRepository(remoto)
    }
    val materialRepository: MaterialRepository by lazy {
        if (sinServidor) FakeMaterialRepository(almacen, sesionFake, archivos, reloj) else RemotoMaterialRepository(remoto, materialRemoto)
    }

    fun iniciar() {
        scope.launch {
            sesionRepository
            // sin servidor, al abrir la app se borran los expedientes cuya baja ya vencio (RF-18); con servidor lo hace el servidor
            ninosRepository.purgarVencidos()
            // los pdf de bitacoras de la sesion anterior ya se imprimieron o compartieron
            carpetaBitacoras.deleteRecursively()
        }
    }
}
