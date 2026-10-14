package mx.tec.sonrisas.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

// fechas y horas viajan como texto iso ("2026-10-14", "08:30", "2026-10-14T08:30", "2026-10")
private fun <T : Any> iso(nombre: String, leer: (String) -> T) = object : KSerializer<T> {
    override val descriptor = PrimitiveSerialDescriptor("mx.tec.sonrisas.$nombre", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: T) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): T = leer(decoder.decodeString())
}

object FechaSerializer : KSerializer<LocalDate> by iso("LocalDate", LocalDate::parse)
object FechaHoraSerializer : KSerializer<LocalDateTime> by iso("LocalDateTime", LocalDateTime::parse)
object HoraSerializer : KSerializer<LocalTime> by iso("LocalTime", LocalTime::parse)
object MesSerializer : KSerializer<YearMonth> by iso("YearMonth", YearMonth::parse)
