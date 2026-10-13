package mx.tec.sonrisas.servidor

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import mx.tec.sonrisas.data.JsonApi
import mx.tec.sonrisas.domain.ActividadEspecial
import mx.tec.sonrisas.domain.Asistencia
import mx.tec.sonrisas.domain.Aviso
import mx.tec.sonrisas.domain.BloqueHorario
import mx.tec.sonrisas.domain.Conversacion
import mx.tec.sonrisas.domain.Estancia
import mx.tec.sonrisas.domain.EventoBitacora
import mx.tec.sonrisas.domain.Foto
import mx.tec.sonrisas.domain.Incidente
import mx.tec.sonrisas.domain.MaterialClase
import mx.tec.sonrisas.domain.Mensaje
import mx.tec.sonrisas.domain.Nino
import mx.tec.sonrisas.domain.ObjetoPerdido
import mx.tec.sonrisas.domain.RegistroActividad
import mx.tec.sonrisas.domain.Sala
import mx.tec.sonrisas.domain.Tutor
import mx.tec.sonrisas.domain.Usuario
import java.net.URI
import java.net.URLDecoder
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Types
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * cada coleccion es una tabla con el id, unas columnas para filtrar y el modelo completo en json.
 * fila() devuelve el id seguido de los valores de las columnas, en el mismo orden.
 */
class Tabla<T>(
    val nombre: String,
    val serializer: KSerializer<T>,
    val columnas: List<String> = emptyList(),
    val fila: (T) -> List<Any?>,
)

/** hora con ancho fijo para que ordenar y comparar como texto sea correcto */
private val formatoOrden = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSS")
fun orden(cuando: LocalDateTime): String = cuando.format(formatoOrden)

object Tablas {
    val estancias = Tabla("estancias", Estancia.serializer()) { listOf(it.id) }
    val salas = Tabla("salas", Sala.serializer(), listOf("estancia_id")) { listOf(it.id, it.estanciaId) }
    val usuarios = Tabla("usuarios", Usuario.serializer(), listOf("usuario", "estancia_id")) { listOf(it.id, it.usuario.lowercase(), it.estanciaId) }
    val tutores = Tabla("tutores", Tutor.serializer()) { listOf(it.id) }
    val ninos = Tabla("ninos", Nino.serializer(), listOf("estancia_id", "sala_id")) { listOf(it.id, it.estanciaId, it.salaId) }
    val asistencias = Tabla("asistencias", Asistencia.serializer(), listOf("nino_id", "fecha")) { listOf(it.id, it.ninoId, it.fecha) }
    val eventos = Tabla("eventos", EventoBitacora.serializer(), listOf("nino_id", "fecha")) { listOf(it.id, it.ninoId, it.cuando.toLocalDate()) }
    val incidentes = Tabla("incidentes", Incidente.serializer(), listOf("nino_id", "fecha")) { listOf(it.id, it.ninoId, it.cuando.toLocalDate()) }
    val fotos = Tabla("fotos", Foto.serializer(), listOf("sala_id")) { listOf(it.id, it.salaId) }
    val avisos = Tabla("avisos", Aviso.serializer(), listOf("estancia_id")) { listOf(it.id, it.estanciaId) }
    val bloques = Tabla("bloques", BloqueHorario.serializer(), listOf("sala_id")) { listOf(it.id, it.salaId) }
    val especiales = Tabla("especiales", ActividadEspecial.serializer(), listOf("sala_id")) { listOf(it.id, it.salaId) }
    val registros = Tabla("registros", RegistroActividad.serializer(), listOf("estancia_id", "nino_id", "cuando")) {
        listOf(it.id, it.estanciaId, it.ninoId, orden(it.cuando))
    }
    val objetos = Tabla("objetos", ObjetoPerdido.serializer(), listOf("sala_id")) { listOf(it.id, it.salaId) }
    val material = Tabla("material", MaterialClase.serializer(), listOf("sala_id")) { listOf(it.id, it.salaId) }
    val conversaciones = Tabla("conversaciones", Conversacion.serializer(), listOf("sala_id")) { listOf(it.id, it.salaId) }
    val mensajes = Tabla("mensajes", Mensaje.serializer(), listOf("conversacion_id", "autor_id", "enviado")) {
        listOf(it.id, it.conversacionId, it.autorId, orden(it.enviado))
    }

    val todas = listOf(
        estancias, salas, usuarios, tutores, ninos, asistencias, eventos, incidentes, fotos, avisos,
        bloques, especiales, registros, objetos, material, conversaciones, mensajes,
    )

    // tablas que no guardan un modelo del dominio
    val esquemaExtra = listOf(
        "CREATE UNIQUE INDEX IF NOT EXISTS usuarios_usuario_unico ON usuarios (usuario)",
        "CREATE TABLE IF NOT EXISTS claves (usuario_id VARCHAR PRIMARY KEY, hash VARCHAR NOT NULL)",
        "CREATE TABLE IF NOT EXISTS huellas (usuario_id VARCHAR PRIMARY KEY)",
        "CREATE TABLE IF NOT EXISTS sesiones (token_hash VARCHAR PRIMARY KEY, usuario_id VARCHAR NOT NULL, expira BIGINT NOT NULL)",
        "CREATE INDEX IF NOT EXISTS sesiones_usuario ON sesiones (usuario_id)",
        "CREATE TABLE IF NOT EXISTS avisos_leidos (usuario_id VARCHAR NOT NULL, aviso_id VARCHAR NOT NULL, PRIMARY KEY (usuario_id, aviso_id))",
        "CREATE TABLE IF NOT EXISTS objetos_vistos (usuario_id VARCHAR NOT NULL, objeto_id VARCHAR NOT NULL, PRIMARY KEY (usuario_id, objeto_id))",
        "CREATE TABLE IF NOT EXISTS lecturas (usuario_id VARCHAR NOT NULL, conversacion_id VARCHAR NOT NULL, hasta VARCHAR NOT NULL, PRIMARY KEY (usuario_id, conversacion_id))",
        "CREATE TABLE IF NOT EXISTS archivos (id VARCHAR PRIMARY KEY, sala_id VARCHAR NOT NULL, tipo VARCHAR NOT NULL, bytes BYTEA NOT NULL)",
        "CREATE TABLE IF NOT EXISTS contadores (nombre VARCHAR PRIMARY KEY, valor INT NOT NULL)",
    )
    val tablasExtra = listOf("claves", "huellas", "sesiones", "avisos_leidos", "objetos_vistos", "lecturas", "archivos", "contadores")
}

open class Conexion(val con: Connection) {

    fun ejecutar(sql: String, vararg args: Any?): Int = con.prepareStatement(sql).use { ps ->
        ps.poner(args)
        ps.executeUpdate()
    }

    fun <R> consultar(sql: String, vararg args: Any?, fila: (ResultSet) -> R): List<R> = con.prepareStatement(sql).use { ps ->
        ps.poner(args)
        ps.executeQuery().use { rs -> buildList { while (rs.next()) add(fila(rs)) } }
    }

    fun <T> Tabla<T>.donde(condicion: String, vararg args: Any?): List<T> =
        consultar("SELECT datos FROM $nombre WHERE $condicion", *args) { JsonApi.decodeFromString(serializer, it.getString(1)) }

    fun <T> Tabla<T>.todos(): List<T> = donde("1 = 1")

    fun <T> Tabla<T>.porId(id: String): T? = donde("id = ?", id).firstOrNull()

    /** columna IN (valores) mas un filtro opcional; con la lista vacia no consulta */
    fun <T> Tabla<T>.en(columna: String, valores: Collection<String>, extra: String = "", vararg args: Any?): List<T> {
        if (valores.isEmpty()) return emptyList()
        val lugares = valores.joinToString(",") { "?" }
        return donde("$columna IN ($lugares) $extra", *valores.toTypedArray(), *args)
    }

    fun <T> Tabla<T>.guardar(vararg objetos: T) = guardar(objetos.toList())

    /** inserta o reemplaza por id; borrar e insertar funciona igual en postgres y h2 */
    fun <T> Tabla<T>.guardar(objetos: List<T>) {
        if (objetos.isEmpty()) return
        val sqlInsert = "INSERT INTO $nombre (id, ${columnas.joinToString("") { "$it, " }}datos) VALUES (${(0..columnas.size).joinToString("") { "?, " }}?)"
        con.prepareStatement("DELETE FROM $nombre WHERE id = ?").use { borrar ->
            con.prepareStatement(sqlInsert).use { insertar ->
                objetos.forEach { o ->
                    val valores = fila(o)
                    borrar.poner(arrayOf(valores.first()))
                    borrar.addBatch()
                    insertar.poner((valores + JsonApi.encodeToString(serializer, o)).toTypedArray())
                    insertar.addBatch()
                }
                borrar.executeBatch()
                insertar.executeBatch()
            }
        }
    }

    fun <T> Tabla<T>.borrar(id: String) = ejecutar("DELETE FROM $nombre WHERE id = ?", id)

    fun <T> Tabla<T>.borrarDonde(condicion: String, vararg args: Any?) = ejecutar("DELETE FROM $nombre WHERE $condicion", *args)

    private fun java.sql.PreparedStatement.poner(args: Array<out Any?>) {
        args.forEachIndexed { i, v ->
            when (v) {
                null -> setNull(i + 1, Types.VARCHAR)
                is String, is Int, is Long, is Boolean, is ByteArray -> setObject(i + 1, v)
                // fechas y horas se guardan como texto iso
                else -> setString(i + 1, v.toString())
            }
        }
    }
}

class Db(url: String, usuario: String? = null, clave: String? = null) {
    private val fuente = HikariDataSource(HikariConfig().apply {
        jdbcUrl = url
        usuario?.let { username = it }
        clave?.let { password = it }
        maximumPoolSize = 8
    })

    // shortcut: un solo escritor a la vez evita carreras (folios, reemplazos) y basta para una fundacion; con mucha carga, transacciones por fila
    private val candado = Mutex()

    init {
        fuente.connection.use { c ->
            val con = Conexion(c)
            // shortcut: el esquema se crea al arrancar; cuando cambie una tabla existente hay que agregar migraciones (flyway)
            Tablas.todas.forEach { t ->
                val columnas = t.columnas.joinToString("") { "$it VARCHAR, " }
                con.ejecutar("CREATE TABLE IF NOT EXISTS ${t.nombre} (id VARCHAR PRIMARY KEY, ${columnas}datos VARCHAR NOT NULL)")
                t.columnas.forEach { col -> con.ejecutar("CREATE INDEX IF NOT EXISTS ${t.nombre}_$col ON ${t.nombre} ($col)") }
            }
            Tablas.esquemaExtra.forEach { con.ejecutar(it) }
        }
    }

    suspend fun <R> leer(bloque: (Connection) -> R): R = withContext(Dispatchers.IO) { fuente.connection.use(bloque) }

    suspend fun <R> escribir(bloque: (Connection) -> R): R = candado.withLock {
        withContext(Dispatchers.IO) {
            fuente.connection.use { c ->
                c.autoCommit = false
                try {
                    bloque(c).also { c.commit() }
                } catch (e: Throwable) {
                    c.rollback()
                    throw e
                }
            }
        }
    }

    fun cerrar() = fuente.close()

    companion object {
        /** acepta jdbc:... o la url postgres://usuario:clave@host/base que dan neon, render o supabase */
        fun desde(url: String?): Db {
            if (url.isNullOrBlank()) return Db("jdbc:h2:file:./datos/sonrisas;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")
            if (url.startsWith("jdbc:")) return Db(url)
            val uri = URI(url)
            val (usuario, clave) = uri.rawUserInfo.orEmpty().split(":", limit = 2).map { URLDecoder.decode(it, Charsets.UTF_8) } + listOf("", "")
            val puerto = if (uri.port > 0) ":${uri.port}" else ""
            val consulta = uri.rawQuery?.let { "?$it" } ?: ""
            return Db("jdbc:postgresql://${uri.host}$puerto${uri.rawPath}$consulta", usuario.ifBlank { null }, clave.ifBlank { null })
        }
    }
}
