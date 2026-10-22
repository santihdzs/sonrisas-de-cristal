package mx.tec.sonrisas.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.tec.sonrisas.R
import mx.tec.sonrisas.data.ArchivoEntrada
import java.io.File
import java.io.InputStream
import java.util.UUID

/** nombre, tipo y tamano que reporta el proveedor; el contenido se lee hasta guardarlo */
fun Context.archivoDesde(uri: Uri): ArchivoEntrada {
    val resolver = applicationContext.contentResolver
    var nombre = "archivo"
    var tamano: Long? = null
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { c.getString(it) }?.let { nombre = it }
            c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 && !c.isNull(it) }?.let { tamano = c.getLong(it) }
        }
    }
    val tipo = resolver.getType(uri) ?: "application/octet-stream"
    return ArchivoEntrada(nombre, tipo, tamano) { requireNotNull(resolver.openInputStream(uri)) { "No se pudo leer el archivo." } }
}

private fun Context.uriCompartida(ruta: String): Uri = FileProvider.getUriForFile(this, "$packageName.archivos", File(ruta))

/** abre el archivo con la app que el telefono tenga para ese tipo; devuelve false si no hay ninguna */
fun Context.abrirArchivo(ruta: String, tipo: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uriCompartida(ruta), tipo)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return try {
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

fun Context.compartirArchivo(ruta: String, tipo: String, titulo: String) {
    val uri = uriCompartida(ruta)
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = tipo
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, titulo)
        clipData = ClipData.newRawUri(titulo, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(envio, titulo))
}

fun tamanoTexto(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format(LocaleMx, "%.1f MB", bytes / (1024.0 * 1024.0))
}

private const val LADO_MAXIMO = 1024

// se decodifica reducida para no cargar fotos de 12 MP completas en memoria
private fun decodificar(abrir: () -> InputStream?): Bitmap? {
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    abrir()?.use { BitmapFactory.decodeStream(it, null, limites) }
    if (limites.outWidth <= 0) return null
    var muestra = 1
    while (maxOf(limites.outWidth, limites.outHeight) / (muestra * 2) >= LADO_MAXIMO) muestra *= 2
    return abrir()?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestra }) }
}

/** donde la camara deja la foto: cache privada de la app, nunca la galeria. devuelve el archivo y la uri para la camara */
fun Context.destinoCamara(): Pair<File, Uri> {
    val archivo = File(cacheDir, "camara/${UUID.randomUUID()}.jpg").also { it.parentFile?.mkdirs() }
    return archivo to uriCompartida(archivo.path)
}

/** reduce la foto de la camara y la endereza segun su exif, para que no salga de lado ni pese varios MB */
suspend fun prepararFotoCamara(archivo: File): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val giro = when (ExifInterface(archivo.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val original = decodificar { archivo.inputStream() } ?: return@runCatching false
        val derecha = if (giro == 0f) original
        else Bitmap.createBitmap(original, 0, 0, original.width, original.height, Matrix().apply { postRotate(giro) }, true)
        archivo.outputStream().use { derecha.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        true
    }.getOrDefault(false)
}

/**
 * foto de la galeria por el mismo camino que la de la camara: copia privada, se decodifica y se vuelve a guardar como jpeg.
 * asi webp o heic llegan como jpeg y el exif original (gps incluido) no sale del telefono. null si no se pudo leer
 */
suspend fun Context.fotoDeGaleria(uri: Uri): File? {
    val archivo = destinoCamara().first
    val copiada = withContext(Dispatchers.IO) {
        runCatching { contentResolver.openInputStream(uri)?.use { entrada -> archivo.outputStream().use { entrada.copyTo(it) } } != null }.getOrDefault(false)
    }
    if (copiada && prepararFotoCamara(archivo)) return archivo
    archivo.delete()
    return null
}

/** muestra una imagen guardada en la app (ruta) o recien elegida (content://) */
@Composable
fun ImagenArchivo(
    fuente: String,
    descripcion: String?,
    modifier: Modifier = Modifier,
    escala: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val imagen by produceState<ImageBitmap?>(null, fuente) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                if (fuente.startsWith("content:")) decodificar { context.contentResolver.openInputStream(Uri.parse(fuente)) }
                else decodificar { File(fuente).takeIf { it.exists() }?.inputStream() }
            }.getOrNull()?.asImageBitmap()
        }
    }
    val img = imagen
    if (img != null) {
        Image(img, descripcion, modifier.clip(MaterialTheme.shapes.medium), contentScale = escala)
    } else {
        Box(modifier.clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainer), contentAlignment = Alignment.Center) {
            Icono(R.drawable.ic_image, descripcion, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
