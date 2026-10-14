package mx.tec.sonrisas.servidor

import kotlin.test.Test
import kotlin.test.assertEquals

class DbTest {
    @Test
    fun convierteLaUrlDeNeonAJdbc() {
        val (jdbc, usuario, clave) = Db.jdbcDe("postgresql://ana:p%40ss@ep-uno.neon.tech/neondb?sslmode=require")
        assertEquals("jdbc:postgresql://ep-uno.neon.tech/neondb?sslmode=require", jdbc)
        assertEquals("ana", usuario)
        assertEquals("p@ss", clave)
    }

    @Test
    fun conservaElPuertoYSinUrlUsaH2() {
        assertEquals("jdbc:postgresql://db:5432/sonrisas", Db.jdbcDe("postgres://sonrisas:sonrisas@db:5432/sonrisas").first)
        assertEquals(true, Db.jdbcDe(null).first.startsWith("jdbc:h2:file:"))
    }
}
