# Sistema de diseño — Sonrisas de Cristal (rediseño de UI)

Este archivo es la **fuente de verdad** del estilo visual. Si en esta carpeta existe un
`DESIGN.md` generado por Stitch y dice algo distinto, **manda este archivo**.

Cada pantalla con mockup tiene un `.png` (cómo debe verse) y un `.html` (apoyo para medidas,
textos y colores). El HTML es web con Tailwind: **no se copia el código, se traduce a Jetpack
Compose** con los componentes de la app.

---

## 1. Tono

Herramienta profesional, cálida y tranquila para una estancia infantil.
**No infantil:** sin mascotas, sin caricaturas, sin emojis, sin fotos de niños en la UI.
Los niños se representan con un avatar de iniciales.

## 2. Colores

| Uso | Color |
|---|---|
| Primario (teal) | `#2E7D74` |
| Teal oscuro (degradado sutil del encabezado) | `#1F5C55` |
| Teal suave (seleccionado, píldora de la barra inferior) | `#E6F3F1` |
| Acento terracota (**solo** botón flotante y detalles puntuales) | `#D97A56` |
| Fondo de pantalla | `#F3F5F7` |
| Tarjetas | `#FFFFFF` |
| Texto principal | `#1B1F23` |
| Texto secundario | `#6B7280` |
| Rojo apagado (errores y acciones destructivas como borrar) | `#B94A3E` |
| Rojo vivo | **Solo** alertas reales y el contador de notificaciones |

Si `AppColors` ya define colores distintos, se actualizan **en un solo lugar** (el tema),
nunca pantalla por pantalla.

### Estados (píldoras suaves, texto oscuro del mismo tono)
- Presente: verde suave
- Salió: azul-gris suave
- Salud / advertencia: amarillo suave con icono de advertencia
- Sin llegar: gris claro

### Acentos por tipo de evento (del logo)
Naranja = alimento · Azul = siesta · Verde = pañal / presente · Amarillo = ánimo ·
Gris-azulado = llegada / salida.

### Color de cada sala (tonos suaves, no saturados)
Lactantes = azul · Maternales = rosa · Preescolar = verde.
Los nombres de las salas vienen de los datos; **no escribir nombres de sala fijos en la UI**.

## 3. Tipografía
Inter si ya está como recurso del proyecto; si no, la fuente del sistema (no agregar descargas).
- Títulos de pantalla: negrita, ~28-30 sp
- Nombres y números importantes: negrita / semibold
- Texto secundario: regular, gris
- Etiquetas de sección: MAYÚSCULAS pequeñas, semibold, gris, con un poco de espaciado entre letras

## 4. Formas y sombras
- Radios: 12 (controles pequeños) · 16 · 20 · 24 (tarjetas) · 28 (tarjeta teal y barra inferior) · 32 (encabezado de ancho completo)
- Chips, píldoras y botones: totalmente redondeados.
- **Tarjetas sin borde**, con sombra suave y difusa (elevación 2 dp aprox.). La barra inferior flotante, más marcada (8-12 dp).
- Nada de contornos finos en tarjetas, campos ni chips.

## 5. Espaciado
Márgenes laterales 16 dp. Entre tarjetas 12-16 dp. Base de 4/8 dp.
Zonas táctiles mínimas de 48 dp. Texto con buen contraste sobre el teal (blanco pleno).
Las listas con scroll llevan **espacio inferior suficiente** para que el botón flotante y la
barra inferior nunca tapen contenido.

---

## 6. Patrones de pantalla

**A. Pestaña con encabezado teal de ancho completo** (Mi sala de la educadora)
Bloque teal pegado a los bordes y a la parte de arriba, esquinas inferiores de 32 dp, detrás
de la barra de estado. Dentro, en blanco: título, botón de perfil (círculo translúcido), chip
con la sala, fecha, texto grande "9 de 14 presentes" y barra de progreso delgada.
**Sin dona ni leyenda**: los conteos van en los chips de filtro.
Después: chips de filtro con contador, banner de salud, etiqueta "NIÑOS DE LA SALA" y
cuadrícula de 2 columnas.

**B. Pestaña con título arriba y tarjeta teal flotante** (Dirección: Inicio, Niños, Avisos)
Barra de estado y título sobre el fondo claro. Debajo, tarjeta teal **con margen en los cuatro
lados** (esquinas de 28 dp en las cuatro) con el resumen. Nada de teal pegado arriba.

**C. Pantalla de detalle** (Horarios, detalle de foto, detalle de aviso, Avisar a la sala,
conversación de chat)
Barra superior clara con flecha de regreso y título. **Sin barra inferior.**

**D. Hoy (familiar)**
Sin bloque teal. Zona superior con un solo fondo continuo, selector de hijo en píldora flotante,
tarjetas blancas flotantes con "Ahora mismo", sala y "Ver horario de hoy", y una hoja blanca de
esquinas superiores redondeadas con Asistencia, Comidas y Siesta.
El personaje 3D del mockup **es opcional y se deja para el final** (ver sección 9).

**Toda pantalla tiene estado vacío** con icono y texto corto.

## 7. Barra inferior flotante
Píldora blanca desconectada de los bordes (28 dp, margen de 16 dp, sombra suave). Solo en las
pestañas principales. Elemento seleccionado: píldora teal suave detrás del icono, icono y
etiqueta teal. No seleccionados: gris medio. Contador de no leídos: círculo rojo pequeño.
**Las pestañas de cada rol no cambian, solo su aspecto** (la familia puede tener hasta 5 con Chat: las etiquetas deben caber sin cortarse). Respetar el área de gestos del sistema.

## 8. Componentes compartidos (en `ui/components`)
Crear o actualizar **antes** de tocar pantallas, y reutilizar siempre:
- Encabezado de ancho completo (patrón A)
- Fila de título + tarjeta teal flotante (patrón B)
- Barra superior de detalle con flecha (patrón C)
- Barra inferior flotante
- Tarjeta blanca base
- Etiqueta de sección (mayúsculas grises)
- Chip de filtro con contador y selector segmentado en píldora
- Píldora de estado (presente / salió / salud / sin llegar)
- Avatar de iniciales (cuadrado redondeado o círculo, tono según sala)
- Botón flotante extendido (terracota)
- Estado vacío
- Banner de aviso (amarillo suave)
- Diálogo de confirmación (acciones destructivas, ya existe `ConfirmDialog`)

## 9. Mockups y su estado

| Pantalla | Rol | Patrón | Nota |
|---|---|---|---|
| Mi sala | Educadora | A | Cuadrícula de 2 columnas, botón "Registro grupal" |
| Hoy | Familiar | D | Personaje 3D opcional, al final |
| Inicio | Dirección | B | Incluye "Inscribir niño" y "Publicar aviso" en la tarjeta teal |
| Niños | Dirección | B | Búsqueda, filtros por sala, Activos / Baja temporal, "Inscribir niño" |
| Avisos | Dirección | B | Etiquetas de categoría con color suave, "Publicar aviso" |
| Horarios | Dirección | C | Lápiz gris y bote en rojo apagado; borrar pide confirmación |
| Chat | Familiar | C | Ya implementado: solo cambiar el aspecto (conversación y lista de chats) |

**Los mockups traen contenido de ejemplo.** Textos como "En línea", "Responde de 7:00 a 17:00",
los chips de respuesta rápida del chat, los avisos de ejemplo o los nombres de educadoras son
inventados. **Solo se usan si la app ya tiene esa función o ese dato.** Si el mockup muestra algo
que la app no tiene, no se agrega: se avisa para decidirlo.

**Personaje 3D:** necesita un PNG con fondo transparente por hijo y puede chocar con el tono
"no infantil". Solo con aprobación. Mientras tanto, avatar de iniciales.

### Todas las pantallas se rediseñan
La app ya está terminada: **cada pantalla recibe el nuevo estilo**, tenga o no mockup.
Las que no tienen mockup (Login, Recuperar contraseña, Semana, Fotos / Galería, Detalle de
foto, Avisos de la familia, Detalle de aviso, Avisar a la sala, Perfil, lista de chats,
bitácora y registro grupal de la educadora, etc.) se resuelven con los patrones A-D y los
componentes compartidos, parecidas al mockup más cercano. Al terminar cada una, indicar a qué
mockup se parece y qué decisiones de diseño se tomaron, para revisarla.

---

## 10. Prohibido
- Tarjetas, campos o chips con contorno fino.
- Encabezado teal pegado arriba en las pantallas de patrón B.
- Dona o leyenda en el encabezado de Mi sala.
- Rojo vivo fuera de alertas y contador de notificaciones.
- Mascotas, caricaturas, emojis, fotos de niños.
- Añadir funciones, pestañas o botones que no estén en la app o en el mockup.
