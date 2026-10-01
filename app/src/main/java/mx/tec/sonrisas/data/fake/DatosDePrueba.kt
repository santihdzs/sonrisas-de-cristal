package mx.tec.sonrisas.data.fake

import mx.tec.sonrisas.domain.AccionRegistro
import mx.tec.sonrisas.domain.ActividadEspecial
import mx.tec.sonrisas.domain.Asistencia
import mx.tec.sonrisas.domain.Aviso
import mx.tec.sonrisas.domain.BloqueHorario
import mx.tec.sonrisas.domain.ColorEstrella
import mx.tec.sonrisas.domain.ControlEsfinteres
import mx.tec.sonrisas.domain.DIAS_PERIODO_BAJA
import mx.tec.sonrisas.domain.EstadoAnimo
import mx.tec.sonrisas.domain.EstadoExpediente
import mx.tec.sonrisas.domain.Estancia
import mx.tec.sonrisas.domain.EventoBitacora
import mx.tec.sonrisas.domain.Foto
import mx.tec.sonrisas.domain.Incidente
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.PersonaAutorizada
import mx.tec.sonrisas.domain.Porcion
import mx.tec.sonrisas.domain.RegistroActividad
import mx.tec.sonrisas.domain.Reloj
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.TemaFoto
import mx.tec.sonrisas.domain.TiempoComida
import mx.tec.sonrisas.domain.TipoAviso
import mx.tec.sonrisas.domain.TipoEvacuacion
import mx.tec.sonrisas.domain.TipoIncidente
import mx.tec.sonrisas.domain.TipoSala
import mx.tec.sonrisas.domain.Tutor
import mx.tec.sonrisas.domain.UsoFoto
import mx.tec.sonrisas.domain.Usuario
import mx.tec.sonrisas.domain.esDiaHabil
import mx.tec.sonrisas.domain.folioIncidente
import mx.tec.sonrisas.domain.lunesDeLaSemana
import mx.tec.sonrisas.domain.puedeAparecerEn
import mx.tec.sonrisas.domain.siguienteFeriado
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

const val CONTRASENA_DEMO = "demo1234"

data class DatosDePrueba(
    val estancias: List<Estancia>,
    val salas: List<Sala>,
    val usuarios: List<Usuario>,
    val tutores: List<Tutor>,
    val ninos: List<Nino>,
    val asistencias: List<Asistencia>,
    val eventos: List<EventoBitacora>,
    val incidentes: List<Incidente>,
    val fotos: List<Foto>,
    val avisos: List<Aviso>,
    val bloques: List<BloqueHorario>,
    val especiales: List<ActividadEspecial>,
    val registros: List<RegistroActividad>,
    val avisosLeidos: Map<String, Set<String>>,
    val siguienteFolio: Int,
) {
    companion object {
        fun generar(reloj: Reloj, semilla: Int = 2026): DatosDePrueba = Generador(reloj, semilla).generar()
    }
}

private class Generador(reloj: Reloj, semilla: Int) {
    private val r = Random(semilla)
    private val ahora: LocalDateTime = reloj.ahora()
    private val hoy: LocalDate = ahora.toLocalDate()
    private val mx = Locale.forLanguageTag("es-MX")

    private fun id(): String = UUID(r.nextLong(), r.nextLong()).toString()
    private fun <T> List<T>.azar(): T = this[r.nextInt(size)]
    private fun prob(p: Double) = r.nextDouble() < p

    private val estancias = mutableListOf<Estancia>()
    private val salas = mutableListOf<Sala>()
    private val usuarios = mutableListOf<Usuario>()
    private val tutores = mutableListOf<Tutor>()
    private val ninos = mutableListOf<Nino>()
    private val asistencias = mutableListOf<Asistencia>()
    private val eventos = mutableListOf<EventoBitacora>()
    private val incidentes = mutableListOf<Incidente>()
    private val fotos = mutableListOf<Foto>()
    private val avisos = mutableListOf<Aviso>()
    private val bloques = mutableListOf<BloqueHorario>()
    private val especiales = mutableListOf<ActividadEspecial>()
    private val registros = mutableListOf<RegistroActividad>()

    private val nombresNina = listOf(
        "Valentina", "Regina", "Camila", "Ximena", "María José", "Renata", "Victoria", "Isabella",
        "Natalia", "Romina", "Daniela", "Fernanda", "Paula", "Emilia", "Julieta", "Aitana", "Mía",
        "Luciana", "Ana Sofía", "Abril", "Alexa", "Zoe", "Andrea", "Frida", "Elena", "Constanza",
    )
    private val nombresNino = listOf(
        "Santiago", "Sebastián", "Leonardo", "Emiliano", "Diego", "Daniel", "Miguel Ángel", "Alejandro",
        "Iker", "Gael", "Tadeo", "Maximiliano", "Rodrigo", "Luis Ángel", "Emilio", "Julián", "Bruno",
        "Thiago", "Adrián", "José Pablo", "Joaquín", "Carlos", "Ángel", "Dante", "Matías", "Ian",
    )
    private val apellidos = listOf(
        "García", "Hernández", "Martínez", "López", "González", "Rodríguez", "Pérez", "Sánchez",
        "Flores", "Garza", "Villarreal", "Cantú", "Leal", "Elizondo", "Cavazos", "Guerra", "Morales",
        "Castillo", "Ortiz", "Rivera", "Moreno", "Chapa", "Zambrano", "Tamez", "Quiroga", "Saldaña",
        "Montemayor", "Benavides", "Medina", "Aguilar", "Reyna", "Esquivel",
    )
    private val nombresMama = listOf(
        "Mariana", "Ana Karen", "Daniela", "Alejandra", "Brenda", "Karla", "Jessica", "Diana", "Paola",
        "Adriana", "Lorena", "Yesenia", "Gabriela", "Mónica", "Cristina", "Liliana", "Nancy", "Érika",
    )
    private val nombresPapa = listOf(
        "Luis", "Carlos", "José", "Juan Carlos", "Ricardo", "Eduardo", "Óscar", "Fernando", "Roberto",
        "Héctor", "Alberto", "Raúl", "Javier", "Arturo", "Iván", "Marco Antonio", "Víctor",
    )
    private val otrosAutorizados = listOf(
        "Abuela" to listOf("Martha", "Rosa", "Teresa", "Leticia", "Norma", "Guadalupe"),
        "Abuelo" to listOf("Ramiro", "Gerardo", "Rogelio", "Humberto"),
        "Tía" to listOf("Cynthia", "Sandra", "Perla", "Karina"),
        "Tío" to listOf("Marco", "Efraín", "Osvaldo"),
    )

    private fun telefono() = "81 %04d %04d".format(r.nextInt(1000, 9999), r.nextInt(1000, 9999))
    private fun apellido() = apellidos.azar()

    private val educadoraDeSala = mutableMapOf<String, Usuario>()
    private val directoraDeEstancia = mutableMapOf<String, Usuario>()

    fun generar(): DatosDePrueba {
        crearEstructura()
        crearNinos()
        crearHorarios()
        crearHistorial()
        crearHoy()
        crearIncidentesPasados()
        crearFotos()
        crearAvisos()
        crearRegistrosRecientes()
        val numerados = incidentes.sortedBy { it.cuando }.mapIndexed { i, inc -> inc.copy(folio = folioIncidente(i + 1)) }
        val familia = usuarios.first { it.usuario == "familia" }
        val avisosSb = avisos.filter { it.estanciaId == estancias[0].id }.sortedBy { it.publicadoEl }
        return DatosDePrueba(
            estancias = estancias,
            salas = salas,
            usuarios = usuarios,
            tutores = tutores,
            ninos = ninos,
            asistencias = asistencias,
            eventos = eventos,
            incidentes = numerados,
            fotos = fotos,
            avisos = avisos,
            bloques = bloques,
            especiales = especiales,
            registros = registros.sortedByDescending { it.cuando },
            // la familia demo ya leyo los dos avisos mas viejos
            avisosLeidos = mapOf(familia.id to avisosSb.take(2).map { it.id }.toSet()) +
                // cada directora ya leyo lo que ella misma publico
                directoraDeEstancia.values.associate { d -> d.id to avisos.filter { it.autorId == d.id }.map { it.id }.toSet() },
            siguienteFolio = numerados.size + 1,
        )
    }

    private lateinit var sanBernabe: Estancia
    private lateinit var valleVerde: Estancia

    private fun crearEstructura() {
        sanBernabe = Estancia(id(), "Estancia San Bernabé", "San Bernabé, Monterrey")
        valleVerde = Estancia(id(), "Estancia Valle Verde", "Valle Verde, Monterrey")
        estancias += listOf(sanBernabe, valleVerde)

        salas += Sala(id(), sanBernabe.id, "Lactantes", TipoSala.LACTANTES, ColorEstrella.AZUL)
        salas += Sala(id(), sanBernabe.id, "Maternales", TipoSala.MATERNALES, ColorEstrella.ROJO)
        salas += Sala(id(), sanBernabe.id, "Preescolar", TipoSala.PREESCOLAR, ColorEstrella.VERDE)
        salas += Sala(id(), valleVerde.id, "Lactantes", TipoSala.LACTANTES, ColorEstrella.NARANJA)
        salas += Sala(id(), valleVerde.id, "Maternales", TipoSala.MATERNALES, ColorEstrella.AMARILLO)
        salas += Sala(id(), valleVerde.id, "Preescolar", TipoSala.PREESCOLAR, ColorEstrella.AZUL)

        fun personal(usuario: String, nombre: String, rol: Rol, estancia: Estancia?, sala: Sala?) =
            Usuario(id(), usuario, CONTRASENA_DEMO, nombre, rol, estancia?.id, sala?.id).also { usuarios += it }

        val (sbLac, sbMat, sbPre, vvLac, vvMat, vvPre) = salas
        educadoraDeSala[sbMat.id] = personal("educadora", "Mariana López Hernández", Rol.EDUCADORA, sanBernabe, sbMat)
        educadoraDeSala[sbLac.id] = personal("educadora.lactantes", "Rosa Elena Garza Villarreal", Rol.EDUCADORA, sanBernabe, sbLac)
        educadoraDeSala[sbPre.id] = personal("educadora.preescolar", "Patricia Cantú Morales", Rol.EDUCADORA, sanBernabe, sbPre)
        educadoraDeSala[vvLac.id] = personal("educadora.vv.lactantes", "Brenda Salinas Ochoa", Rol.EDUCADORA, valleVerde, vvLac)
        educadoraDeSala[vvMat.id] = personal("educadora.vv.maternales", "Yolanda Ibarra Treviño", Rol.EDUCADORA, valleVerde, vvMat)
        educadoraDeSala[vvPre.id] = personal("educadora.vv.preescolar", "Carmen Solís Rangel", Rol.EDUCADORA, valleVerde, vvPre)
        directoraDeEstancia[sanBernabe.id] = personal("direccion", "Claudia Martínez Leal", Rol.DIRECCION, sanBernabe, null)
        directoraDeEstancia[valleVerde.id] = personal("direccion.vv", "Verónica Salazar Treviño", Rol.DIRECCION, valleVerde, null)
        personal("fundacion", "Guadalupe Villarreal Ríos", Rol.FUNDACION, null, null)
    }

    private operator fun <T> List<T>.component6(): T = this[5]

    private fun nacimientoPara(tipo: TipoSala): LocalDate {
        val meses = when (tipo) {
            TipoSala.LACTANTES -> r.nextInt(6, 18)
            TipoSala.MATERNALES -> r.nextInt(18, 36)
            TipoSala.PREESCOLAR -> r.nextInt(36, 58)
        }
        return hoy.minusMonths(meses.toLong()).minusDays(r.nextLong(0, 28))
    }

    private fun tutorComoPersona(t: Tutor) = PersonaAutorizada(t.id, t.nombre, t.parentesco, t.telefono)

    private fun crearFamilia(apellidoPapa: String, apellidoMama: String, conPapa: Boolean): List<Tutor> {
        val mama = Tutor(id(), "${nombresMama.azar()} $apellidoMama ${apellido()}", "Mamá", telefono())
        val lista = mutableListOf(mama)
        if (conPapa) lista += Tutor(id(), "${nombresPapa.azar()} $apellidoPapa ${apellido()}", "Papá", telefono())
        tutores += lista
        return lista
    }

    private fun extras(apellidoFamilia: String): List<PersonaAutorizada> {
        val n = listOf(0, 1, 1, 2).azar()
        return List(n) {
            val (parentesco, nombres) = otrosAutorizados.azar()
            PersonaAutorizada(id(), "${nombres.azar()} $apellidoFamilia ${apellido()}", parentesco, telefono())
        }.distinctBy { it.parentesco }
    }

    private val usados = mutableSetOf<String>()

    private fun nombreLibre(nina: Boolean): String {
        val lista = if (nina) nombresNina else nombresNino
        repeat(20) {
            val n = lista.azar()
            if (usados.add(n)) return n
        }
        return lista.azar()
    }

    private lateinit var sofia: Nino
    private lateinit var mateo: Nino
    private val ninosBajaVencida = mutableListOf<String>()

    private fun crearNinos() {
        val (sbLac, sbMat, sbPre, vvLac, vvMat, vvPre) = salas

        // familia demo: laura es tutora de sofia y mateo, jorge es el segundo tutor de ambos
        val laura = Tutor(id(), "Laura Treviño Garza", "Mamá", "81 2345 6789")
        val jorge = Tutor(id(), "Jorge Ramírez Salinas", "Papá", "81 8765 4321")
        tutores += listOf(laura, jorge)
        val abuelaCarmen = PersonaAutorizada(id(), "Carmen Garza Leal", "Abuela", "81 1122 3344")
        val autorizadosDemo = listOf(tutorComoPersona(laura), tutorComoPersona(jorge), abuelaCarmen)
        usuarios += Usuario(id(), "familia", CONTRASENA_DEMO, laura.nombre, Rol.FAMILIAR, sanBernabe.id, tutorId = laura.id)
        usuarios += Usuario(id(), "familia.papa", CONTRASENA_DEMO, jorge.nombre, Rol.FAMILIAR, sanBernabe.id, tutorId = jorge.id)
        usados += listOf("Sofía", "Mateo")

        sofia = Nino(
            id(), "Sofía", "Ramírez Treviño", hoy.minusMonths(29).minusDays(12), sanBernabe.id, sbMat.id,
            listOf(laura.id, jorge.id), autorizadosDemo, alergias = "Cacahuate",
        )
        mateo = Nino(
            id(), "Mateo", "Ramírez Treviño", hoy.minusMonths(11).minusDays(3), sanBernabe.id, sbLac.id,
            listOf(laura.id, jorge.id), autorizadosDemo,
        )

        fun generarSala(sala: Sala, cantidad: Int): MutableList<Nino> = MutableList(cantidad) {
            val nina = prob(0.5)
            val apPapa = apellido()
            val apMama = apellido()
            val familia = crearFamilia(apPapa, apMama, prob(0.75))
            Nino(
                id(), nombreLibre(nina), "$apPapa $apMama", nacimientoPara(sala.tipo), sala.estanciaId, sala.id,
                familia.map { it.id }, familia.map { tutorComoPersona(it) } + extras(apPapa),
            )
        }

        val lactantesSb = generarSala(sbLac, 7).apply { add(2, mateo) }
        val maternalesSb = generarSala(sbMat, 13).apply { add(0, sofia) }
        val preescolarSb = generarSala(sbPre, 13)

        // informacion medica y permisos de imagen repartidos a proposito
        lactantesSb[4] = lactantesSb[4].copy(padecimientos = "Intolerancia a la lactosa")
        lactantesSb[6] = lactantesSb[6].copy(permisoImagenRedes = false)
        maternalesSb[5] = maternalesSb[5].copy(padecimientos = "Asma", medicamentos = "Salbutamol en inhalador, 2 disparos si hay tos o silbido")
        maternalesSb[8] = maternalesSb[8].copy(permisoImagenInterna = false, permisoImagenRedes = false)
        maternalesSb[3] = maternalesSb[3].copy(permisoImagenRedes = false)
        maternalesSb[10] = maternalesSb[10].copy(permisoImagenRedes = false)
        preescolarSb[2] = preescolarSb[2].copy(alergias = "Penicilina")
        preescolarSb[7] = preescolarSb[7].copy(permisoImagenInterna = false, permisoImagenRedes = false)
        // una baja reciente que todavia se puede reactivar
        val fechaBaja = hoy.minusDays(6)
        preescolarSb[12] = preescolarSb[12].copy(estado = EstadoExpediente.BajaTemporal(fechaBaja, fechaBaja.plusDays(DIAS_PERIODO_BAJA)))
        // y una vencida, que se purga al abrir la app para demostrar RF-18
        val bajaVieja = hoy.minusDays(DIAS_PERIODO_BAJA + 12)
        val vencido = generarSala(sbPre, 1).first()
            .copy(estado = EstadoExpediente.BajaTemporal(bajaVieja, bajaVieja.plusDays(DIAS_PERIODO_BAJA)))
        ninosBajaVencida += vencido.id

        ninos += lactantesSb + maternalesSb + preescolarSb + vencido
        ninos += generarSala(vvLac, 6) + generarSala(vvMat, 8) + generarSala(vvPre, 7)
    }

    private fun crearHorarios() {
        fun bloquesDe(sala: Sala, lista: List<Pair<String, String>>) {
            lista.forEachIndexed { i, (hora, actividad) ->
                val inicio = LocalTime.parse(hora)
                val fin = if (i < lista.lastIndex) LocalTime.parse(lista[i + 1].first) else LocalTime.of(16, 30)
                bloques += BloqueHorario(id(), sala.id, inicio, fin, actividad)
            }
        }
        salas.forEach { sala ->
            when (sala.tipo) {
                TipoSala.LACTANTES -> bloquesDe(sala, listOf(
                    "07:15" to "Recepción", "08:00" to "Biberón y desayuno", "09:00" to "Estimulación temprana",
                    "09:45" to "Colación", "10:15" to "Siesta de la mañana", "11:30" to "Juego sensorial",
                    "12:15" to "Comida", "13:00" to "Siesta de la tarde", "14:45" to "Biberón y cambio de pañal",
                    "15:30" to "Salida",
                ))
                TipoSala.MATERNALES -> bloquesDe(sala, listOf(
                    "07:15" to "Recepción", "08:30" to "Desayuno", "09:15" to "Motricidad y juego dirigido",
                    "10:00" to "Colación", "10:30" to "Juego en el patio", "11:15" to "Cuento y música",
                    "12:30" to "Comida", "13:15" to "Siesta", "15:00" to "Actividad libre", "15:30" to "Salida",
                ))
                TipoSala.PREESCOLAR -> bloquesDe(sala, listOf(
                    "07:15" to "Recepción", "08:30" to "Desayuno", "09:15" to "Actividades pedagógicas",
                    "10:00" to "Colación", "10:30" to "Educación física", "11:15" to "Lectura y lenguaje",
                    "12:30" to "Comida", "13:15" to "Descanso", "14:00" to "Arte y música", "15:30" to "Salida",
                ))
            }
        }
        val maternalesSb = salas[1]
        especiales += ActividadEspecial(id(), maternalesSb.id, hoy, LocalTime.of(11, 15), LocalTime.of(12, 15), "Visita del cuentacuentos")
    }

    private val notasPool = listOf(
        "Hoy compartió sus juguetes con sus compañeros.",
        "Estuvo muy participativo en la actividad de música.",
        "Se le terminaron los pañales, por favor manda más mañana.",
        "Comió muy bien las verduras.",
        "Le costó un poco despedirse en la mañana, después se integró bien.",
        "Pintó un dibujo muy bonito para la familia, va en su mochila.",
        "Por favor manda un cambio de ropa extra.",
        "Hoy aprendió una canción nueva sobre los colores.",
        "Estuvo un poco cansado después de la comida.",
        "Se lavó las manos solito antes de comer.",
    )

    private fun porcion(): Porcion {
        val x = r.nextDouble()
        return when {
            x < 0.10 -> Porcion.NADA
            x < 0.30 -> Porcion.POCO
            x < 0.85 -> Porcion.TODO
            else -> Porcion.PIDIO_MAS
        }
    }

    private fun control(tipo: TipoSala): ControlEsfinteres = when (tipo) {
        TipoSala.LACTANTES -> ControlEsfinteres.NO_APLICA
        TipoSala.MATERNALES -> if (prob(0.6)) ControlEsfinteres.AVISO else ControlEsfinteres.NO_AVISO
        TipoSala.PREESCOLAR -> if (prob(0.92)) ControlEsfinteres.AVISO else ControlEsfinteres.NO_AVISO
    }

    private fun horaAzar(desde: LocalTime, minutosRango: Int): LocalTime = desde.plusMinutes(r.nextLong(0, minutosRango + 1L))

    private fun salaDe(n: Nino) = salas.first { it.id == n.salaId }

    /** genera asistencia y bitacora de un dia; con limite solo deja lo que ya paso */
    private fun generarDia(
        n: Nino,
        fecha: LocalDate,
        limite: LocalTime? = null,
        entradaFija: LocalTime? = null,
        salidaFija: LocalTime? = null,
        entregaFija: String? = null,
        recogeFija: String? = null,
    ) {
        val sala = salaDe(n)
        val educadora = educadoraDeSala.getValue(sala.id)
        val entrada = entradaFija ?: horaAzar(LocalTime.of(7, 15), 90)
        if (limite != null && entrada.isAfter(limite)) return
        val salida = (salidaFija ?: horaAzar(LocalTime.of(14, 30), 120)).takeIf { limite == null || !it.isAfter(limite) }
        val ya = { h: LocalTime -> limite == null || !h.isAfter(limite) }
        val dentro = { h: LocalTime -> !h.isBefore(entrada) && (salida == null || h.isBefore(salida)) && ya(h) }

        asistencias += Asistencia(
            id = id(),
            ninoId = n.id,
            fecha = fecha,
            horaEntrada = entrada,
            entregadoPorId = entregaFija ?: n.personasAutorizadas.azar().id,
            recibidoPorId = educadora.id,
            mochilaCompleta = !prob(0.04),
            horaSalida = salida,
            recogidoPorId = salida?.let { recogeFija ?: n.personasAutorizadas.azar().id },
            salioLimpio = salida?.let { true },
            pertenenciasCompletas = salida?.let { true },
            buenEstadoSalida = salida?.let { true },
        )
        val quien = educadora.id
        fun en(h: LocalTime) = LocalDateTime.of(fecha, h)

        listOf(
            TiempoComida.DESAYUNO to horaAzar(LocalTime.of(8, 35), 20),
            TiempoComida.COLACION to horaAzar(LocalTime.of(10, 2), 10),
            TiempoComida.COMIDA to horaAzar(LocalTime.of(12, 40), 15),
        ).forEach { (tiempo, h) ->
            if (dentro(h)) eventos += EventoBitacora.Alimento(id(), n.id, en(h), quien, tiempo, porcion())
        }

        val siestas = if (sala.tipo == TipoSala.LACTANTES) {
            listOf(horaAzar(LocalTime.of(10, 15), 15) to r.nextLong(40, 75), horaAzar(LocalTime.of(13, 0), 20) to r.nextLong(60, 100))
        } else {
            listOf(horaAzar(LocalTime.of(13, 15), 15) to r.nextLong(55, 105))
        }
        siestas.forEach { (inicio, duracion) ->
            if (dentro(inicio)) {
                val fin = inicio.plusMinutes(duracion).takeIf { ya(it) && (salida == null || it.isBefore(salida)) }
                eventos += EventoBitacora.Siesta(id(), n.id, en(fin ?: inicio), quien, inicio, fin)
            }
        }

        val veces = r.nextInt(2, 5)
        repeat(veces) { i ->
            val h = entrada.plusMinutes(40L + i * 110L + r.nextLong(0, 30))
            if (dentro(h)) {
                val tipo = if (i == 1 && prob(0.55)) TipoEvacuacion.POPO else TipoEvacuacion.PIPI
                eventos += EventoBitacora.Evacuacion(id(), n.id, en(h), quien, tipo, control(sala.tipo))
            }
        }

        val hAnimo = horaAzar(LocalTime.of(11, 40), 20)
        if (dentro(hAnimo)) {
            val x = r.nextDouble()
            val estado = when {
                x < 0.76 -> EstadoAnimo.FELIZ
                x < 0.91 -> EstadoAnimo.TRISTE
                else -> EstadoAnimo.ENOJADO
            }
            eventos += EventoBitacora.Animo(
                id(), n.id, en(hAnimo), quien, estado,
                lloro = estado != EstadoAnimo.FELIZ && prob(0.5),
                peleo = prob(0.05),
                participo = prob(0.88),
            )
        }
        val hNota = horaAzar(LocalTime.of(14, 0), 20)
        if (dentro(hNota) && prob(0.25)) {
            eventos += EventoBitacora.Nota(id(), n.id, en(hNota), quien, notasPool.azar())
        }
    }

    private fun crearHistorial() {
        val dias = (45 downTo 1).map { hoy.minusDays(it.toLong()) }.filter { it.esDiaHabil() }
        dias.forEach { fecha ->
            ninos.forEach { n ->
                val estado = n.estado
                val inscrito = estado !is EstadoExpediente.BajaTemporal || fecha.isBefore(estado.fechaBaja)
                if (inscrito && prob(0.9)) generarDia(n, fecha)
            }
        }
    }

    private fun crearHoy() {
        val limite = ahora.toLocalTime()
        val laura = sofia.tutorIds[0]
        val jorge = sofia.tutorIds[1]
        salas.forEach { sala ->
            val activos = ninos.filter { it.salaId == sala.id && it.activo }
            val candidatos = activos.filter { it.id != sofia.id && it.id != mateo.id }.shuffled(r)
            // la sala demo siempre queda con dos sin llegar para que se vea "12 de 14"
            val faltan = candidatos.take(if (sala.id == sofia.salaId) 2 else r.nextInt(2, 4)).map { it.id }.toSet()
            activos.filter { it.id !in faltan }.forEach { n ->
                when (n.id) {
                    sofia.id -> generarDia(n, hoy, limite, LocalTime.of(8, 5), LocalTime.of(15, 10), laura, jorge)
                    mateo.id -> generarDia(n, hoy, limite, LocalTime.of(8, 5), LocalTime.of(15, 10), laura, jorge)
                    else -> generarDia(n, hoy, limite)
                }
            }
        }
        val horaIncidente = LocalTime.of(10, 40)
        if (!limite.isBefore(horaIncidente)) {
            incidentes += Incidente(
                id(), "", sofia.id, LocalDateTime.of(hoy, horaIncidente), TipoIncidente.GOLPE, "Rodilla derecha",
                "Golpe leve en la rodilla al jugar en el patio",
                "Se limpió la zona y se aplicó compresa fría. Se le dio seguimiento durante la mañana.",
                educadoraDeSala.getValue(sofia.salaId).id,
            )
        }
    }

    private fun diaHabilAtras(n: Int): LocalDate {
        var fecha = hoy
        var cuenta = 0
        while (cuenta < n) {
            fecha = fecha.minusDays(1)
            if (fecha.esDiaHabil()) cuenta++
        }
        return fecha
    }

    private fun crearIncidentesPasados() {
        val maternales = ninos.filter { it.salaId == sofia.salaId && it.id != sofia.id && it.activo }
        val preescolar = ninos.filter { it.salaId == salas[2].id && it.activo }
        val mordido = maternales[2]
        val caido = preescolar[4]
        incidentes += Incidente(
            id(), "", mordido.id, LocalDateTime.of(diaHabilAtras(7), LocalTime.of(11, 5)), TipoIncidente.MORDIDA,
            "Brazo izquierdo", "Un compañero lo mordió durante el juego en el patio.",
            "Se lavó con agua y jabón y se aplicó frío. Se habló con ambos niños.",
            educadoraDeSala.getValue(mordido.salaId).id,
        )
        incidentes += Incidente(
            id(), "", caido.id, LocalDateTime.of(diaHabilAtras(16), LocalTime.of(10, 50)), TipoIncidente.CAIDA,
            "Frente", "Se tropezó al correr y se pegó en la frente con el piso.",
            "Se aplicó compresa fría y se observó durante una hora. No presentó mareo ni vómito.",
            educadoraDeSala.getValue(caido.salaId).id,
        )
    }

    private val descripcionesFoto = mapOf(
        TemaFoto.PINTURA to listOf("Pintamos con las manos usando colores primarios", "Hicimos un mural de otoño con hojas y pintura"),
        TemaFoto.JARDIN to listOf("Regamos las plantas del huerto", "Buscamos bichitos en el jardín"),
        TemaFoto.MUSICA to listOf("Tocamos panderos y maracas en la clase de música", "Cantamos y bailamos la canción de los animales"),
        TemaFoto.LECTURA to listOf("Leímos juntos el cuento de la oruga", "Hora del cuento con títeres"),
        TemaFoto.CUMPLEANOS to listOf("Celebramos los cumpleaños del mes"),
        TemaFoto.JUEGO to listOf("Armamos una torre gigante con bloques", "Circuito de motricidad en el patio"),
    )

    private fun crearFotos() {
        fun fotosDeSala(sala: Sala, cantidad: Int, fijos: Map<Int, String>) {
            val activos = ninos.filter { it.salaId == sala.id && it.activo }
            val dias = (1..14).map { diaHabilAtras(it) }.distinct().shuffled(r).take(cantidad).sortedDescending()
            dias.forEachIndexed { i, fecha ->
                val tema = TemaFoto.entries.azar()
                val uso = if (i % 3 == 1) UsoFoto.REDES else UsoFoto.INTERNA
                val posibles = activos.filter { it.puedeAparecerEn(uso) }
                val etiquetados = posibles.shuffled(r).take(r.nextInt(3, minOf(8, posibles.size) + 1)).map { it.id }.toMutableSet()
                // sofia o mateo aparecen en las fotos marcadas
                fijos[i]?.let { etiquetados += it } ?: run { etiquetados -= setOf(sofia.id, mateo.id) }
                fotos += Foto(
                    id(), sala.id, LocalDateTime.of(fecha, horaAzar(LocalTime.of(9, 30), 240)),
                    descripcionesFoto.getValue(tema).azar(), etiquetados.toList(), uso, tema,
                )
            }
        }
        fotosDeSala(salas[1], 7, mapOf(0 to sofia.id, 1 to sofia.id, 3 to sofia.id, 5 to sofia.id))
        fotosDeSala(salas[0], 3, mapOf(1 to mateo.id))
        fotosDeSala(salas[2], 2, emptyMap())
        salas.drop(3).forEach { fotosDeSala(it, 2, emptyMap()) }
        // foto de la baja vencida, que se borra junto con su expediente
        fotos += Foto(
            id(), salas[2].id, LocalDateTime.of(hoy.minusDays(DIAS_PERIODO_BAJA + 20), LocalTime.of(10, 0)),
            "Circuito de motricidad en el patio", ninosBajaVencida, UsoFoto.INTERNA, TemaFoto.JUEGO,
        )
    }

    private fun crearAvisos() {
        val directora = directoraDeEstancia.getValue(sanBernabe.id)
        val juntaDia = hoy.lunesDeLaSemana().plusWeeks(1).with(DayOfWeek.FRIDAY)
        val (feriado, nombreFeriado) = siguienteFeriado(hoy)
        val fmtFeriado = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", mx)
        fun aviso(tipo: TipoAviso, titulo: String, cuerpo: String, evento: LocalDateTime?, haceDias: Long, estancia: Estancia, autor: Usuario) {
            avisos += Aviso(id(), estancia.id, tipo, titulo, cuerpo, evento, LocalDateTime.of(hoy.minusDays(haceDias), LocalTime.of(9, 0).plusMinutes(r.nextLong(0, 90))), autor.id)
        }
        aviso(
            TipoAviso.GENERAL, "Colecta de ropa de invierno",
            "Recibimos chamarras, suéteres y cobijas en buen estado para las familias que más lo necesitan. Puedes dejarlas en la dirección de 8:00 a 16:00.",
            null, 12, sanBernabe, directora,
        )
        aviso(
            TipoAviso.CAMPANA, "Campaña de vacunación contra la influenza",
            "Personal del centro de salud aplicará la vacuna contra la influenza a niñas y niños de 6 meses en adelante. Trae la cartilla de vacunación de tu hija o hijo.",
            LocalDateTime.of(hoy.plusDays(8), LocalTime.of(9, 0)), 6, sanBernabe, directora,
        )
        aviso(
            TipoAviso.SUSPENSION, "Suspensión de labores por el $nombreFeriado",
            "La estancia permanecerá cerrada el ${feriado.format(fmtFeriado)} por ser día feriado oficial. Reanudamos actividades al día siguiente en el horario de siempre.",
            LocalDateTime.of(feriado, LocalTime.of(7, 15)), 3, sanBernabe, directora,
        )
        aviso(
            TipoAviso.JUNTA, "Junta de padres de familia",
            "Te esperamos en el salón de usos múltiples para revisar los avances del bimestre y el calendario de actividades. La junta dura una hora aproximadamente.",
            LocalDateTime.of(juntaDia, LocalTime.of(17, 30)), 1, sanBernabe, directora,
        )
        aviso(
            TipoAviso.GENERAL, "Nuevo horario de recepción",
            "A partir del próximo lunes la recepción en Valle Verde será de 7:30 a 9:00.",
            null, 2, valleVerde, directoraDeEstancia.getValue(valleVerde.id),
        )
    }

    private fun crearRegistrosRecientes() {
        val porId = usuarios.associateBy { it.id }
        val ninoPorId = ninos.associateBy { it.id }
        val desde = diaHabilAtras(2)
        fun registro(cuando: LocalDateTime, usuarioId: String, accion: AccionRegistro, detalle: String, ninoId: String? = null) {
            val u = porId.getValue(usuarioId)
            val estanciaId = u.estanciaId ?: ninoId?.let { ninoPorId[it]?.estanciaId }
            registros += RegistroActividad(id(), cuando, u.id, u.nombre, accion, detalle, ninoId, estanciaId)
        }
        val fechas = asistencias.map { it.fecha }.filter { !it.isBefore(desde) }.distinct()
        fechas.forEach { fecha ->
            educadoraDeSala.values.forEach { e ->
                registro(LocalDateTime.of(fecha, LocalTime.of(7, 5).plusMinutes(r.nextLong(0, 8))), e.id, AccionRegistro.SESION, "Inició sesión")
            }
        }
        asistencias.filter { !it.fecha.isBefore(desde) }.forEach { a ->
            val n = ninoPorId.getValue(a.ninoId)
            registro(LocalDateTime.of(a.fecha, a.horaEntrada), a.recibidoPorId, AccionRegistro.CAPTURA, describirLlegada(n, a), n.id)
            a.horaSalida?.let { registro(LocalDateTime.of(a.fecha, it), a.recibidoPorId, AccionRegistro.CAPTURA, describirSalida(n, a), n.id) }
        }
        eventos.filter { !it.cuando.toLocalDate().isBefore(desde) }.forEach { e ->
            val n = ninoPorId.getValue(e.ninoId)
            registro(e.cuando, e.registradoPorId, AccionRegistro.CAPTURA, describirEvento(n, e), n.id)
        }
        incidentes.filter { !it.cuando.toLocalDate().isBefore(desde) }.forEach { i ->
            val n = ninoPorId.getValue(i.ninoId)
            registro(i.cuando, i.registradoPorId, AccionRegistro.CAPTURA, describirIncidente(n, i), n.id)
        }
        avisos.forEach { a -> registro(a.publicadoEl, a.autorId, AccionRegistro.AVISO, "Publicó el aviso \"${a.titulo}\"") }
        fotos.filter { it.cuando.toLocalDate() >= hoy.minusDays(14) && it.ninoIds.none { id -> id in ninosBajaVencida } }.forEach { f ->
            val sala = salas.first { it.id == f.salaId }
            registro(f.cuando, educadoraDeSala.getValue(sala.id).id, AccionRegistro.CAPTURA, "Agregó una foto: ${f.descripcion}")
        }
        val familia = usuarios.first { it.usuario == "familia" }
        fotos.filter { sofia.id in it.ninoIds }.take(2).forEach { f ->
            registro(f.cuando.plusHours(5), familia.id, AccionRegistro.FOTO_CONSULTADA, "Consultó la foto \"${f.descripcion}\"", sofia.id)
        }
        val baja = ninos.first { (it.estado as? EstadoExpediente.BajaTemporal)?.let { b -> b.eliminarEl.isAfter(hoy) } == true }
        registro(
            LocalDateTime.of(hoy.minusDays(6), LocalTime.of(12, 20)), directoraDeEstancia.getValue(sanBernabe.id).id,
            AccionRegistro.EXPEDIENTE, "Dio de baja temporal a ${baja.nombreCompleto}", baja.id,
        )
    }
}
