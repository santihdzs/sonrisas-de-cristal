# Sonrisas de Cristal — instrucciones para Claude Code

App Android (Kotlin, Jetpack Compose, Material 3) para la Fundación Estancias Infantiles
Sonrisas de Cristal. Cuatro roles: Fundación, Dirección, Educadora y Familiar.
Responde siempre en español. Quien te usa no programa mucho: explica en qué archivo
cambiaste cada cosa y por qué.

## Rediseño de UI (trabajo en curso)

Estamos cambiando **solo el aspecto** de la app. El estilo y los mockups están en
`docs/diseno/`:

- `docs/diseno/DISENO.md` → reglas de estilo. **Léelo completo antes de tocar la UI.**
- `docs/diseno/<pantalla>/screen.png` y `code.html` → cómo debe verse cada pantalla.
  El HTML es Tailwind (web): úsalo como referencia de medidas y textos, no lo copies.
  Traduce todo a Compose.
- Si hay un `DESIGN.md` de Stitch, ignóralo donde contradiga `DISENO.md`.

### Reglas del rediseño
1. **Solo capa de UI** (`ui/`: tema, componentes y pantallas). No cambies lógica, repositorios,
   modelos, API, autenticación, almacenamiento del token ni rutas de navegación.
2. **No toques `shared/` ni `backend/`.**
3. **Conserva todas las funciones.** Si un mockup no muestra algo que la pantalla actual sí
   hace, mantenlo y adáptalo al estilo; pregunta si dudas.
4. **LoginScreen:** en debug se mantiene el botón "Servidor". En release se mantiene el
   mantener presionado el logo para configurar el servidor. No lo quites.
5. **Componentes primero:** crea o actualiza el tema y los componentes compartidos antes de
   rediseñar pantallas, y reutilízalos. No dupliques estilos en cada pantalla.
6. **No agregues dependencias** sin preguntarme.
7. **No inventes funciones**, pestañas ni botones. Los mockups traen contenido de ejemplo
   (chips de respuesta rápida, "En línea", horarios de respuesta, avisos inventados): si la app no
   lo tiene, no lo agregues y avísame.
8. **Personaje 3D:** no lo implementes sin que yo lo confirme (usa avatar de iniciales).
   El Chat ya está implementado: solo se cambia su aspecto.
9. Los textos de la UI van en español. Los datos de prueba no se cambian.

### Forma de trabajar
- Una fase o una pantalla a la vez. **Detente al terminar cada una** y espera mi revisión.
- Compila antes de decir que terminaste. Si no puedes compilar, dilo.
- Un commit por pantalla, con mensaje claro. **No hagas push** ni toques `main`.
  La rama de trabajo es `rediseno-ui`.
- Al terminar cada pantalla di: archivos cambiados, a qué mockup se parece y qué quedó
  distinto o pendiente.

### Orden de trabajo
0. Reconocimiento (sin cambiar nada)
1. Tema y componentes compartidos
2. Una pantalla piloto: Mi sala
3. Pantallas con mockup, por rol: Educadora → Familiar → Dirección
4. **Todas las demás pantallas** (la app ya está terminada: se rediseñan todas, con o sin
   mockup, basándote en los patrones y en las pantallas con mockup)
5. Opcional: personaje 3D

### Pendientes del equipo (no resolver por tu cuenta)
- Pantalla de Fotos: el texto "En esta demostración elige una imagen de ejemplo" debe
  quitarse o resolverse con la cámara. Pregúntame antes de tocarlo.
- Nombres de salas distintos entre datos de prueba (Lactantes / Maternales / Preescolar y
  Orugas / Mariposas): no los escribas fijos en la UI.
- Cómo compilar: ver `README.md` (necesita el JDK de Android Studio).
