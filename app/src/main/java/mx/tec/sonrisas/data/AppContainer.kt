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

    /** direccion del servidor; vacia = solo los datos de prueba */
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
    private val camara = File(context.cacheDir, "camara")

    // copias locales de lo que se baja del servidor
    private val materialRemoto = File(context.cacheDir, "material")
    private val objetosRemotos = File(context.cacheDir, "objetos")
    private val fotosRemotas = File(context.cacheDir, "fotos")

    private val sesionRemota by lazy {
        RemotoSesionRepository(context, servidor, scope, listOf(materialRemoto, objetosRemotos, fotosRemotas))
    }

    private val conServidor by lazy {
        val remoto = Remoto(sesionRemota.api, Cambios())
        Repositorios(
            RemotoEstanciasRepository(remoto), RemotoNinosRepository(remoto), RemotoJornadaRepository(remoto),
            RemotoIncidentesRepository(remoto), RemotoFotosRepository(remoto, fotosRemotas), RemotoAvisosRepository(remoto),
            RemotoHorariosRepository(remoto), RemotoActividadRepository(remoto), RemotoCuentasRepository(remoto),
            RemotoObjetosRepository(remoto, objetosRemotos), RemotoMensajesRepository(remoto), RemotoMaterialRepository(remoto, materialRemoto),
        )
    }

    // DEMO: datos de prueba en memoria (data/fake), generados con la fecha del telefono. sirven con o sin servidor
    private val almacen by lazy { AlmacenMemoria(DatosDePrueba.generar(reloj, ArchivosDePrueba.crear(archivos))) }
    private val sesionDemo by lazy { FakeSesionRepository(almacen, reloj, scope) }
    private val demo by lazy {
        Repositorios(
            FakeEstanciasRepository(almacen, sesionDemo, reloj), FakeNinosRepository(almacen, sesionDemo, reloj),
            FakeJornadaRepository(almacen, sesionDemo, reloj), FakeIncidentesRepository(almacen, sesionDemo, reloj),
            FakeFotosRepository(almacen, sesionDemo, archivos, reloj), FakeAvisosRepository(almacen, sesionDemo, reloj),
            FakeHorariosRepository(almacen, sesionDemo, reloj), FakeActividadRepository(almacen, sesionDemo),
            FakeCuentasRepository(almacen, sesionDemo, reloj), FakeObjetosRepository(almacen, sesionDemo, archivos, reloj),
            FakeMensajesRepository(almacen, sesionDemo, reloj), FakeMaterialRepository(almacen, sesionDemo, archivos, reloj),
        )
    }

    // DEMO: al iniciar sesion decide si se usan los datos del servidor o los de prueba
    private val sesion by lazy { SesionConDemo(if (sinServidor) null else sesionRemota, sesionDemo, scope) }

    // los viewmodels de cada sesion se crean despues de entrar, asi que reciben los repositorios correctos
    private val repos: Repositorios get() = if (sesion.enDemo) demo else conServidor

    val sesionRepository: SesionRepository get() = sesion
    val estanciasRepository: EstanciasRepository get() = repos.estancias
    val ninosRepository: NinosRepository get() = repos.ninos
    val jornadaRepository: JornadaRepository get() = repos.jornada
    val incidentesRepository: IncidentesRepository get() = repos.incidentes
    val fotosRepository: FotosRepository get() = repos.fotos
    val avisosRepository: AvisosRepository get() = repos.avisos
    val horariosRepository: HorariosRepository get() = repos.horarios
    val actividadRepository: ActividadRepository get() = repos.actividad
    val cuentasRepository: CuentasRepository get() = repos.cuentas
    val objetosRepository: ObjetosPerdidosRepository get() = repos.objetos
    val mensajesRepository: MensajesRepository get() = repos.mensajes
    val materialRepository: MaterialRepository get() = repos.material

    fun iniciar() {
        scope.launch {
            sesionRepository
            // en los datos de prueba, al abrir la app se borran los expedientes cuya baja ya vencio (RF-18); con servidor lo hace el servidor
            demo.ninos.purgarVencidos()
            // los pdf de bitacoras de la sesion anterior ya se imprimieron o compartieron
            carpetaBitacoras.deleteRecursively()
            // fotos de la camara que no se guardaron; las recientes se dejan por si la camara sigue abierta
            camara.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - 86_400_000 }?.forEach { it.delete() }
        }
    }
}

private class Repositorios(
    val estancias: EstanciasRepository,
    val ninos: NinosRepository,
    val jornada: JornadaRepository,
    val incidentes: IncidentesRepository,
    val fotos: FotosRepository,
    val avisos: AvisosRepository,
    val horarios: HorariosRepository,
    val actividad: ActividadRepository,
    val cuentas: CuentasRepository,
    val objetos: ObjetosPerdidosRepository,
    val mensajes: MensajesRepository,
    val material: MaterialRepository,
)
