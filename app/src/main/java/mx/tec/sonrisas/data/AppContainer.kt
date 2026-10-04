package mx.tec.sonrisas.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.fake.AlmacenMemoria
import mx.tec.sonrisas.data.fake.DatosDePrueba
import mx.tec.sonrisas.data.fake.FakeActividadRepository
import mx.tec.sonrisas.data.fake.FakeAvisosRepository
import mx.tec.sonrisas.data.fake.FakeCuentasRepository
import mx.tec.sonrisas.data.fake.FakeEstanciasRepository
import mx.tec.sonrisas.data.fake.FakeFotosRepository
import mx.tec.sonrisas.data.fake.FakeHorariosRepository
import mx.tec.sonrisas.data.fake.FakeIncidentesRepository
import mx.tec.sonrisas.data.fake.FakeJornadaRepository
import mx.tec.sonrisas.data.fake.FakeNinosRepository
import mx.tec.sonrisas.data.fake.FakeObjetosRepository
import mx.tec.sonrisas.data.fake.FakeSesionRepository
import mx.tec.sonrisas.domain.Reloj
import mx.tec.sonrisas.domain.RelojSistema

// para conectar el backend basta con cambiar las implementaciones de este archivo
class AppContainer(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val reloj: Reloj = RelojSistema

    private val almacen by lazy { AlmacenMemoria(DatosDePrueba.generar(reloj)) }

    val archivos = AlmacenArchivos(context.filesDir)

    private val sesionFake by lazy { FakeSesionRepository(almacen, reloj, scope) }

    val sesionRepository: SesionRepository by lazy { sesionFake }
    val estanciasRepository: EstanciasRepository by lazy { FakeEstanciasRepository(almacen, sesionFake, reloj) }
    val ninosRepository: NinosRepository by lazy { FakeNinosRepository(almacen, sesionFake, reloj) }
    val jornadaRepository: JornadaRepository by lazy { FakeJornadaRepository(almacen, sesionFake, reloj) }
    val incidentesRepository: IncidentesRepository by lazy { FakeIncidentesRepository(almacen, sesionFake, reloj) }
    val fotosRepository: FotosRepository by lazy { FakeFotosRepository(almacen, sesionFake, reloj) }
    val avisosRepository: AvisosRepository by lazy { FakeAvisosRepository(almacen, sesionFake, reloj) }
    val horariosRepository: HorariosRepository by lazy { FakeHorariosRepository(almacen, sesionFake, reloj) }
    val actividadRepository: ActividadRepository by lazy { FakeActividadRepository(almacen, sesionFake) }
    val cuentasRepository: CuentasRepository by lazy { FakeCuentasRepository(almacen, sesionFake, reloj) }
    val objetosRepository: ObjetosPerdidosRepository by lazy { FakeObjetosRepository(almacen, sesionFake, archivos, reloj) }

    fun iniciar() {
        scope.launch {
            sesionRepository
            // al abrir la app se borran los expedientes cuya baja ya vencio (RF-18)
            ninosRepository.purgarVencidos()
        }
    }
}
