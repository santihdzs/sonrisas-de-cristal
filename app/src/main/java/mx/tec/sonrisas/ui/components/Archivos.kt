package mx.tec.sonrisas.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
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
