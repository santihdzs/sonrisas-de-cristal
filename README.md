# Sonrisas de Cristal

App Android para la Fundación Estancias Infantiles Sonrisas de Cristal (Monterrey). Sustituye la bitácora en papel y los grupos de WhatsApp de cada estancia con una sola app para cuatro perfiles: educadora, familia, dirección y fundación.

Proyecto del curso TC2007B. Kotlin, Jetpack Compose y Material 3.

## Cómo correrla

```sh
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest
```

Se necesita el JDK que trae Android Studio (Gradle 9.8 no corre con Java 11). Las pruebas unitarias cubren el diseño de la bitácora imprimible.

La app arranca con datos de prueba en memoria generados a partir de la fecha del teléfono. Todas las cuentas de prueba usan la contraseña `demo1234`.

## Arquitectura

- `domain/`: modelos y reglas puras (resúmenes del día y de la semana, validaciones, reporte de asistencia).
- `data/`: interfaces de repositorios y `AppContainer`. `data/fake/` tiene las implementaciones en memoria y los datos de prueba.
- `bitacora/`: la bitácora imprimible. `ContenidoBitacora` arma el texto de cada fila, `DisenoBitacora` calcula las páginas sin depender de Android y `PdfBitacora` las dibuja con `PdfDocument`.
- `ui/`: pantallas por rol, `AppViewModelProvider` y navegación. Cada pantalla recibe un `UiState` (cargando, éxito o error).

Toda regla de privacidad (qué niño, sala o conversación puede ver cada quien) vive en la capa de datos, no en las pantallas.

## Funciones por rol

**Educadora** (pestañas Sala, Fotos, Mensajes, Avisos)
- Lista de su sala, llegada y salida con personas autorizadas, bitácora del día y registro grupal.
- Incidentes, fotos con autorización de imagen, avisos.
- Desde la sala: material de clase, objetos perdidos e impresión de la bitácora de toda la sala.
- Chat de la sala y mensajes directos con las familias de su sala.

**Familia** (pestañas Hoy, Semana, Fotos, Mensajes, Avisos)
- El día y la semana de cada hijo, fotos donde aparece, incidentes, horario de la sala.
- En "Hoy", una tarjeta de la sala del hijo elegido con material de clase y objetos perdidos (con indicador de nuevos).
- Imprimir o compartir la bitácora de sus hijos.
- Chat de la sala y mensajes directos con las educadoras.

**Dirección** (pestañas Inicio, Niños, Mensajes, Avisos, Más)
- Inscripción y expediente, bajas, horarios, reporte de asistencia, registro de actividad.
- Registro de salas y de personal, asignación de sala y retiro de acceso.
- Al inscribir: elegir un tutor ya registrado para hermanos y dar acceso a la app a cada tutor.
- Desde cada sala en Inicio: material, objetos perdidos e impresión de la bitácora.

**Fundación**
- Resumen consolidado de todas las estancias y reporte de asistencia sin nombres de niños.
- Registro y edición de estancias y creación de la cuenta de dirección de cada una.

## Requerimientos

| Clave | Requerimiento | Dónde se cubre |
| --- | --- | --- |
| RF-01 | Dos tipos de perfil | Login con navegación distinta por rol (educadora, familia, dirección, fundación) |
| RF-02 | Lista de niños de la sala | "Mi sala"; la capa de datos solo devuelve niños de la sala de la educadora |
| RF-03 | Entrada y salida | Hojas de llegada y salida con hora, quién entrega, quién recoge y personal que entrega |
| RF-04 | Bitácora del día | Ficha del día con acciones rápidas |
| RF-05 | Registro para varios niños | Registro grupal |
| RF-06 | Vista del familiar | Pestaña "Hoy" |
| RF-07 | Cada familia ve solo a sus hijos | `Visibilidad` en la capa de datos; aplica a fotos, chats, material, objetos perdidos e impresiones |
| RF-08 | Avisos del centro | Avisos con indicador de no leídos |
| RF-09 | Fotografía de la actividad | Fotos etiquetadas visibles solo para las familias de esos niños |
| RF-10 | Borrar una foto | La familia borra la foto para todos |
| RF-11 | Funciona sin internet | Pendiente: requiere el backend (ver "Backend") |
| RF-12 | Resumen semanal | Pestaña "Semana", compartir resumen e imprimir bitácora |
| RF-13 | Desbloqueo con huella | Bloqueo al ir a segundo plano o tras 5 minutos |
| RF-14 | Quién hizo qué | Registro de actividad: capturas, cuentas, salas, objetos perdidos, material y bitácoras exportadas |
| RF-15 | Inscripción y expediente | Inscripción con tutores, personas autorizadas, tutores compartidos entre hermanos y cuentas de familia |
| RF-16 | Autorización de imagen | Dos permisos por niño, validados también en la capa de datos |
| RF-17 | Varias estancias | Todo se filtra por estancia; la fundación solo ve datos consolidados |
| RF-18 | Baja y eliminación | Baja temporal con periodo de gracia y purga al abrir la app |
| RF-19 | Incidentes | Registro con folio; aparecen en "Hoy" y en la bitácora impresa |
| RF-20 | Reporte de asistencia | Por sala y por mes, para dirección y fundación |
| RF-21 | Horario de actividades | Horario por sala y actividades especiales |

Puntos pendientes del catálogo que se resolvieron:
- Varios tutores por niño y hermanos con una sola cuenta de familia.
- Alta, baja y asignación del personal (pantalla "Personal").
- Mensajes de la familia hacia la sala (chat de sala y mensajes directos).

## Bitácora imprimible

- Semanal por niño en carta horizontal, diaria en carta vertical y semanal de toda la sala.
- Sigue el orden del formato oficial de filtro diario: datos generales, registro de ingreso, registro de estadía (alimentación, descanso, funciones excretoras, estado de ánimo, accidentes, salud) y registro de egreso, con columnas de lunes a viernes.
- Encabezado con fundación, estancia, sala, niño, periodo y fecha de generación; pie con número de página.
- Los días sin asistencia dicen "No asistió", los días por venir "Pendiente" y los campos vacíos "Sin registro".
- Se dibuja sobre una cuadrícula fija en puntos con la tipografía sans serif del sistema, así que los mismos datos siempre dan el mismo PDF.
- "Imprimir" usa `PrintManager`; "Compartir PDF" usa un `FileProvider`. Cada exportación queda en el registro de actividad.

## Decisiones

- **Sin registro público.** Las cuentas solo las crean la fundación (dirección de cada estancia) y la dirección (educadoras y familias). Así nadie entra a una estancia sin que alguien de la estancia lo autorice.
- **Contraseña temporal.** Al crear una cuenta se muestran usuario y contraseña una sola vez, con "Copiar". El cambio obligatorio de contraseña en el primer acceso queda para cuando exista el backend.
- **Retirar acceso no borra la cuenta.** La cuenta queda inactiva: no puede iniciar sesión, sale de los chats y su nombre se conserva en la bitácora y en el registro de actividad. Si tenía la sesión abierta, se cierra.
- **Una sala por educadora.** Una sala puede tener varias educadoras, pero cada educadora pertenece a una sala.
- **Máximo cinco salas por estancia.** El color de estrella identifica a la sala y no se repite dentro de la estancia; hay cinco colores.
- **Hermanos.** Al inscribir se puede elegir un tutor ya registrado en la estancia; el tutor es el mismo registro para ambos niños y su cuenta de familia ve a los dos. Si se editan sus datos, cambian para todos sus hijos.
- **Chats.** Solo participan personas con acceso a la app. La dirección está en el chat de cada sala de su estancia, pero no inicia mensajes directos (el requerimiento solo define familia y educadora). La pertenencia se recalcula siempre: si un niño cambia de sala o alguien pierde el acceso, la conversación deja de mostrarse. Los mensajes no se copian al registro de actividad para no exponer su contenido.
- **Objetos perdidos.** La familia ve que un objeto está reclamado, pero solo ve el nombre de quien lo reclamó si fue ella. Los entregados quedan en el filtro "Entregados".
- **Material de clase.** PDF, imágenes y documentos de Word, Excel y PowerPoint, de hasta 10 MB. La educadora edita o borra lo que ella subió; la dirección puede corregir cualquier material de su estancia.
- **Archivos.** Las fotos de objetos y el material se copian al almacenamiento privado de la app. Como los datos de prueba viven en memoria, al abrir la app se borran los archivos subidos en una ejecución anterior y los PDF temporales de la bitácora.
- **Bitácora en papel.** La semana de un niño ocupa dos hojas, igual que el formato oficial; al imprimir una sala, cada niño empieza en su propia hoja. El texto nunca baja de 7 pt: una celda que no cabe en cuatro renglones deja "sigue en Observaciones, nota N" y el texto completo pasa a una hoja de observaciones al final.
- **Accidentes y salud.** Golpes, caídas, mordidas, rasguños y "otro" van en "Accidentes"; reacción alérgica, fiebre y malestar van en "Salud".
- **Salida.** La asistencia ahora guarda qué persona del personal entregó al niño, porque el formato oficial lo pide.
- **Letra al 200 %.** Las etiquetas de la barra inferior se limitan a 130 % para que quepan las cinco pestañas; el resto de la pantalla sí escala.
- **Tableta.** El contenido tiene un ancho máximo de 840 dp para que no se estire de lado a lado en horizontal.

## Backend

Todavía no hay backend: la carpeta `backend/` solo tiene un README con el stack planeado (Ktor y PostgreSQL) y carpetas vacías, sin proyecto ni contrato de API. Por eso la app sigue con repositorios en memoria.

Para conectarlo basta con implementar las interfaces de `data/Repositorios.kt` y `data/SesionRepository.kt` (por ejemplo con Retrofit) y cambiarlas en `AppContainer`. Las pantallas y los ViewModels no cambian. Los archivos llegan a la capa de datos como `ArchivoEntrada`, así que un repositorio remoto puede subirlos tal cual. El funcionamiento sin conexión (RF-11) depende de ese backend.
