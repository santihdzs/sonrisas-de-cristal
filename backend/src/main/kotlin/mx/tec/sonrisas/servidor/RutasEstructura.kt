package mx.tec.sonrisas.servidor

import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import kotlin.random.asKotlinRandom
import mx.tec.sonrisas.data.AccesoDenegado
import mx.tec.sonrisas.data.Id
import mx.tec.sonrisas.data.NuevaCuenta
import mx.tec.sonrisas.data.ResumenEstancia
import mx.tec.sonrisas.domain.AccionRegistro
import mx.tec.sonrisas.domain.Credenciales
import mx.tec.sonrisas.domain.Estancia
import mx.tec.sonrisas.domain.ReporteAsistencia
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.Usuario
import mx.tec.sonrisas.domain.Validaciones
import mx.tec.sonrisas.domain.contrasenaTemporal
import mx.tec.sonrisas.domain.usuarioSugerido
import java.time.YearMonth

// EstanciasRepository, CuentasRepository y ActividadRepository

private fun Ctx.estanciasVisibles(): List<Estancia> = when (u.rol) {
    Rol.FUNDACION -> Tablas.estancias.todos()
    else -> listOfNotNull(u.estanciaId?.let { Tablas.estancias.porId(it) })
}

/** la fundacion ve a todo el personal; los demas, solo el de su estancia */
private fun Ctx.usuariosDelAmbito(): List<Usuario> =
    if (u.rol == Rol.FUNDACION) Tablas.usuarios.todos()
    else u.estanciaId?.let { Tablas.usuarios.donde("estancia_id = ?", it) }.orEmpty()

private fun Ctx.crearCuenta(nombre: String, rol: Rol, estanciaId: String, detalle: String, salaId: String? = null, tutorId: String? = null): Credenciales {
    if (nombre.isBlank()) throw IllegalArgumentException("Escribe el nombre.")
    val ocupados = consultar("SELECT usuario FROM usuarios") { it.getString(1) }
    val usuario = usuarioSugerido(nombre, ocupados)
    val contrasena = contrasenaTemporal(azarSeguro.asKotlinRandom())
    val nuevo = Usuario(nuevoId(), usuario, nombre.trim(), rol, estanciaId, salaId, tutorId)
    Tablas.usuarios.guardar(nuevo)
    ejecutar("INSERT INTO claves (usuario_id, hash) VALUES (?, ?)", nuevo.id, Claves.hash(contrasena))
    registrar(AccionRegistro.CUENTA, detalle, estanciaId = estanciaId)
    return Credenciales(nuevo.nombre, usuario, contrasena, rol)
}

private fun Ctx.educadoraPropia(usuarioId: String): Usuario =
    Tablas.usuarios.porId(usuarioId)?.takeIf { it.rol == Rol.EDUCADORA && it.estanciaId == u.estanciaId } ?: throw AccesoDenegado()

fun Route.rutasEstructura(db: Db) {
    get("/estancias") { lee(db) { estanciasVisibles() } }

    get("/salas") {
        lee(db) { if (u.rol == Rol.FUNDACION) Tablas.salas.todos() else salasEstancia() }
    }

    get("/educadoras") {
        lee(db) {
            if (u.rol == Rol.FAMILIAR) emptyList()
            else usuariosDelAmbito().filter { it.rol == Rol.EDUCADORA && it.activo }
        }
    }

    get("/personal") {
        lee(db) { usuariosDelAmbito().filter { it.rol == Rol.EDUCADORA || it.rol == Rol.DIRECCION } }
    }

    get("/resumen") {
        lee(db) {
            if (u.rol != Rol.FUNDACION && u.rol != Rol.DIRECCION) return@lee emptyList<ResumenEstancia>()
            val mes = YearMonth.from(hoy)
            estanciasVisibles().map { e ->
                val activos = Tablas.ninos.donde("estancia_id = ?", e.id).filter { it.activo }
                val asistencias = Tablas.asistencias.en("nino_id", activos.map { it.id }, "AND fecha >= ?", mes.atDay(1))
                val reporte = ReporteAsistencia.de(mes, hoy, activos, emptyList(), asistencias)
                ResumenEstancia(e, activos.size, asistencias.count { it.fecha == hoy && !it.salio }, reporte.promedio)
            }
        }
    }

    put("/estancias") {
        val estancia = call.receive<Estancia>()
        escribe(db) {
            if (u.rol != Rol.FUNDACION) throw AccesoDenegado()
            if (Validaciones.estancia(estancia.nombre, estancia.colonia, estancia.telefono).isNotEmpty()) {
                throw IllegalArgumentException("Revisa los datos de la estancia.")
            }
            val limpia = estancia.copy(nombre = estancia.nombre.trim(), colonia = estancia.colonia.trim(), telefono = estancia.telefono.trim())
            val existe = Tablas.estancias.porId(limpia.id) != null
            Tablas.estancias.guardar(limpia)
            registrar(
                AccionRegistro.ESTRUCTURA,
                if (existe) "Editó los datos de ${limpia.nombre}" else "Registró la estancia ${limpia.nombre}",
                estanciaId = limpia.id,
            )
            limpia
        }
    }

    put("/salas") {
        val sala = call.receive<Sala>()
        escribe(db) {
            val estanciaId = direccion()
            val existente = Tablas.salas.porId(sala.id)
            if (existente != null && existente.estanciaId != estanciaId) throw AccesoDenegado()
            val otras = salasEstancia().filter { it.id != sala.id }
            if (Validaciones.sala(sala.nombre, sala.estrella, otras).isNotEmpty()) throw IllegalArgumentException("Revisa los datos de la sala.")
            val limpia = sala.copy(estanciaId = estanciaId, nombre = sala.nombre.trim())
            Tablas.salas.guardar(limpia)
            registrar(AccionRegistro.ESTRUCTURA, if (existente != null) "Editó la sala ${limpia.nombre}" else "Registró la sala ${limpia.nombre}")
            limpia
        }
    }

    get("/cuentas") {
        lee(db) {
            when (u.rol) {
                Rol.FUNDACION -> Tablas.usuarios.todos().filter { it.rol == Rol.DIRECCION }
                Rol.DIRECCION -> usuariosDelAmbito().filter { it.rol != Rol.FUNDACION }
                else -> emptyList()
            }.sortedBy { it.nombre }
        }
    }

    post("/cuentas/direccion") {
        val p = call.receive<NuevaCuenta>()
        escribe(db) {
            if (u.rol != Rol.FUNDACION) throw AccesoDenegado()
            val estancia = p.estanciaId?.let { Tablas.estancias.porId(it) } ?: throw AccesoDenegado()
            crearCuenta(p.nombre, Rol.DIRECCION, estancia.id, "Creó la cuenta de dirección de ${p.nombre.trim()} en ${estancia.nombre}")
        }
    }

    post("/cuentas/educadora") {
        val p = call.receive<NuevaCuenta>()
        escribe(db) {
            val estanciaId = direccion()
            val sala = salaDeMiEstancia(p.salaId ?: throw AccesoDenegado())
            crearCuenta(p.nombre, Rol.EDUCADORA, estanciaId, "Registró a la educadora ${p.nombre.trim()} en la sala ${sala.nombre}", salaId = sala.id)
        }
    }

    post("/cuentas/familia") {
        val tutorId = call.receive<Id>().id
        escribe(db) {
            val estanciaId = direccion()
            val hijos = Tablas.ninos.donde("estancia_id = ?", estanciaId).filter { tutorId in it.tutorIds }
            val tutor = Tablas.tutores.porId(tutorId)
            if (tutor == null || hijos.isEmpty()) throw AccesoDenegado()
            if (usuariosDelAmbito().any { it.tutorId == tutorId && it.activo }) throw IllegalStateException("Este tutor ya tiene acceso a la app.")
            val nombres = hijos.joinToString(", ") { it.nombre }
            crearCuenta(
                tutor.nombre, Rol.FAMILIAR, estanciaId,
                "Dio acceso a la app a ${tutor.nombre} (${tutor.parentesco.lowercase()} de $nombres)", tutorId = tutor.id,
            )
        }
    }

    post("/cuentas/{id}/sala") {
        val usuarioId = call.parameters["id"]!!
        val salaId = call.receive<Id>().id
        escribe(db) {
            direccion()
            val sala = salaDeMiEstancia(salaId)
            val e = educadoraPropia(usuarioId)
            if (!e.activo) throw AccesoDenegado()
            Tablas.usuarios.guardar(e.copy(salaId = sala.id))
            registrar(AccionRegistro.CUENTA, "Asignó a ${e.nombre} a la sala ${sala.nombre}")
        }
    }

    post("/cuentas/{id}/retiro") {
        val usuarioId = call.parameters["id"]!!
        escribe(db) {
            direccion()
            val e = educadoraPropia(usuarioId)
            Tablas.usuarios.guardar(e.copy(activo = false))
            // si tenia la sesion abierta en algun telefono, se cierra
            Sesiones.cerrarTodas(this, e.id)
            ejecutar("DELETE FROM huellas WHERE usuario_id = ?", e.id)
            registrar(AccionRegistro.CUENTA, "Retiró el acceso a la app de ${e.nombre}")
        }
    }

    get("/registros") {
        lee(db) {
            if (u.rol != Rol.DIRECCION) emptyList()
            // shortcut: solo los 2000 mas recientes; si la pantalla necesita mas historia, paginar
            else Tablas.registros.donde("estancia_id = ? ORDER BY cuando DESC LIMIT 2000", u.estanciaId)
        }
    }
}
