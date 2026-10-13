package mx.tec.sonrisas.servidor

import mx.tec.sonrisas.data.fake.ArchivoDemo
import mx.tec.sonrisas.data.fake.CONTRASENA_DEMO
import mx.tec.sonrisas.data.fake.DatosDePrueba
import mx.tec.sonrisas.domain.RelojSistema
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Usuario

/** llena una base vacia: con los datos de prueba de la demo o, en produccion, solo con la cuenta de la fundacion */
object Semilla {

    // los mismos archivos que la app genera en modo sin servidor, guardados en resources/demo
    private val archivosDemo = listOf(
        Triple("cancion", "Cancion de los dias de la semana.pdf", "application/pdf"),
        Triple("colores", "Actividad de colores para casa.png", "image/png"),
        Triple("rutina", "Rutina de sueno en casa.pdf", "application/pdf"),
    )

    suspend fun preparar(db: Db, demo: Boolean, reiniciar: Boolean, claveFundacion: String?) = db.escribir { c ->
        val con = Conexion(c)
        if (reiniciar) {
            (Tablas.todas.map { it.nombre } + Tablas.tablasExtra).forEach { con.ejecutar("DELETE FROM $it") }
        }
        val vacia = con.consultar("SELECT COUNT(*) FROM usuarios") { it.getInt(1) }.first() == 0
        when {
            !vacia -> "La base ya tiene datos."
            demo -> {
                con.sembrarDemo()
                "Datos de prueba cargados. Todas las cuentas usan la contraseña $CONTRASENA_DEMO."
            }
            else -> {
                if (claveFundacion == null || claveFundacion.length < 12) {
                    error("Base vacía sin datos de prueba: define CLAVE_FUNDACION (12 caracteres o más) para crear la cuenta de la fundación.")
                }
                val fundacion = Usuario(nuevoId(), "fundacion", "Coordinación de la fundación", Rol.FUNDACION)
                with(con) { Tablas.usuarios.guardar(fundacion) }
                con.ejecutar("INSERT INTO claves (usuario_id, hash) VALUES (?, ?)", fundacion.id, Claves.hash(claveFundacion))
                "Se creó la cuenta \"fundacion\"."
            }
        }
    }

    private fun Conexion.sembrarDemo() {
        val archivos = archivosDemo.map { (clave, nombre, tipo) ->
            val bytes = Semilla::class.java.getResourceAsStream("/demo/$clave.${nombre.substringAfterLast('.')}")!!.use { it.readBytes() }
            ArchivoDemo(clave, nombre, tipo, "demo-$clave", bytes.size.toLong()) to bytes
        }
        val d = DatosDePrueba.generar(RelojSistema, archivos.map { it.first })
        archivos.forEach { (a, bytes) ->
            val sala = d.material.firstOrNull { it.ruta == a.ruta }?.salaId ?: return@forEach
            ejecutar("INSERT INTO archivos (id, sala_id, tipo, bytes) VALUES (?, ?, ?, ?)", a.ruta, sala, a.tipo, bytes)
        }
        Tablas.estancias.guardar(d.estancias)
        Tablas.salas.guardar(d.salas)
        Tablas.usuarios.guardar(d.usuarios)
        Tablas.tutores.guardar(d.tutores)
        Tablas.ninos.guardar(d.ninos)
        Tablas.asistencias.guardar(d.asistencias)
        Tablas.eventos.guardar(d.eventos)
        Tablas.incidentes.guardar(d.incidentes)
        Tablas.fotos.guardar(d.fotos)
        Tablas.avisos.guardar(d.avisos)
        Tablas.bloques.guardar(d.bloques)
        Tablas.especiales.guardar(d.especiales)
        Tablas.registros.guardar(d.registros)
        Tablas.objetos.guardar(d.objetos)
        Tablas.material.guardar(d.material)
        Tablas.conversaciones.guardar(d.conversaciones)
        Tablas.mensajes.guardar(d.mensajes)
        // todas las cuentas de prueba tienen la misma contrasena, asi que basta un hash
        val hash = Claves.hash(CONTRASENA_DEMO)
        d.usuarios.forEach { ejecutar("INSERT INTO claves (usuario_id, hash) VALUES (?, ?)", it.id, hash) }
        d.avisosLeidos.forEach { (u, ids) -> ids.forEach { ejecutar("INSERT INTO avisos_leidos (usuario_id, aviso_id) VALUES (?, ?)", u, it) } }
        d.objetosVistos.forEach { (u, ids) -> ids.forEach { ejecutar("INSERT INTO objetos_vistos (usuario_id, objeto_id) VALUES (?, ?)", u, it) } }
        d.lecturas.forEach { (u, convs) ->
            convs.forEach { (conv, hasta) -> ejecutar("INSERT INTO lecturas (usuario_id, conversacion_id, hasta) VALUES (?, ?, ?)", u, conv, orden(hasta)) }
        }
        // el contador guarda el ultimo folio usado
        ejecutar("INSERT INTO contadores (nombre, valor) VALUES ('folio', ?)", d.siguienteFolio - 1)
    }
}
