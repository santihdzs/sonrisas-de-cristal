# Servidor de Sonrisas de Cristal

Ktor y PostgreSQL. Aplica las mismas reglas de privacidad que la app (qué niño, sala o conversación ve cada quien) usando el código de `shared/`, que también compila la app.

## Correrlo en local

Con Docker (servidor y PostgreSQL), desde la raíz del repo:

```sh
docker compose up -d --build
```

Sin Docker (usa H2, una base en el archivo `backend/datos/`):

```sh
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew -p backend run
```

En los dos casos queda en `http://localhost:8080`. La primera vez carga los datos de prueba de la demo (las mismas cuentas de `DEMO.md`, contraseña `demo1234`). Los cambios se guardan en la base: sobreviven a reiniciar el servidor y la app.

### Volver a cargar los datos de prueba

Los datos de prueba se generan con la fecha del día. Antes de presentar conviene recargarlos para que "hoy" tenga llegadas, comidas y siestas:

```sh
docker compose run --rm --no-deps backend /app/bin/backend reiniciar-demo
# o sin docker:
./gradlew -p backend run --args=reiniciar-demo
```

Borra todo, vuelve a cargar los datos y termina; el servidor que ya está corriendo no necesita reiniciarse. Las sesiones abiertas en los teléfonos se cierran. Con `DATOS_DEMO=false` el comando se niega a correr, para que nunca borre una base real.

### Conectar el teléfono

En la pantalla de inicio de sesión toca "Servidor" y escribe la dirección de la laptop en la red local, por ejemplo `http://192.168.1.20:8080` (en macOS: `ipconfig getifaddr en0`). El teléfono y la laptop deben estar en la misma red. En el emulador de Android Studio la laptop es `http://10.0.2.2:8080`. Si el servidor falla, activa "Datos de prueba sin servidor" en la misma opción y la app usa los datos en memoria.

La versión debug acepta `http://` para la demo en red local; la versión release solo acepta `https://`.

## Publicarlo gratis

La opción recomendada es Render (servidor) con Neon (PostgreSQL). Las dos tienen plan gratuito.

1. En [neon.tech](https://neon.tech) crea un proyecto y copia la cadena de conexión (`postgresql://usuario:clave@ep-....neon.tech/neondb?sslmode=require`).
2. En [render.com](https://render.com) crea un "Web Service" desde este repositorio, con el tipo Docker:
   - Dockerfile Path: `backend/Dockerfile`
   - Docker Build Context Directory: `.` (la raíz del repo)
   - Plan: Free
3. Agrega estas variables de entorno en Render:
   - `DATABASE_URL`: la cadena de Neon.
   - `DATOS_DEMO`: `false`.
   - `CLAVE_FUNDACION`: la contraseña de la cuenta `fundacion`, de 12 caracteres o más. Solo se usa la primera vez, con la base vacía.
   - `PROXY_CONFIABLE`: `true` (Render pasa la IP real del teléfono en `X-Forwarded-For`; se usa para limitar los intentos de inicio de sesión).
4. Al terminar el despliegue, Render da una dirección `https://...onrender.com`. Compila la app con esa dirección como servidor por defecto:

   ```sh
   ./gradlew :app:assembleRelease -Pservidor=https://sonrisas.onrender.com
   ```

   o ponla en la opción "Servidor" del inicio de sesión.
5. Entra como `fundacion`, registra las estancias y crea las cuentas de dirección; desde ahí cada dirección registra a su personal y a las familias.

Límites del plan gratuito:
- Render apaga el servidor tras 15 minutos sin uso. La primera petición después tarda cerca de un minuto. Mientras alguien usa la app, el servidor sigue despierto.
- Neon da 0.5 GB. El material de clase y las fotos de objetos perdidos se guardan en la base, así que caben unos cincuenta PDF grandes. Si se llena, hay que pasar a un plan de pago o mover los archivos a un almacenamiento aparte.

También funciona en cualquier máquina con Docker (una computadora de la fundación o una VM): `DATOS_DEMO=false CLAVE_FUNDACION=... POSTGRES_PASSWORD=... docker compose up -d`. En ese caso hace falta ponerle HTTPS adelante (por ejemplo Caddy o un túnel de Cloudflare).

## Variables de entorno

| Variable | Para qué | Si no se define |
| --- | --- | --- |
| `PORT` | Puerto HTTP | `8080` |
| `DATABASE_URL` | `postgres://...` o `jdbc:...` | H2 en `./datos/` |
| `DATOS_DEMO` | `false` en producción: sin datos de prueba | `true` |
| `CLAVE_FUNDACION` | Contraseña inicial de `fundacion` cuando `DATOS_DEMO=false` | La base vacía no arranca |
| `PROXY_CONFIABLE` | `true` detrás de Render u otro proxy | `false` |
| `POSTGRES_PASSWORD` | Solo en `docker-compose.yml` | `sonrisas` |

## Seguridad

- Contraseñas con PBKDF2-HMAC-SHA256 (210 000 iteraciones, sal por cuenta).
- Sesiones con tokens aleatorios de 32 bytes. La base solo guarda su SHA-256, así que una copia de la base no sirve para entrar. Duran 30 días; cerrar sesión borra el token.
- Cada petición vuelve a leer la cuenta: si la dirección retira el acceso, la siguiente petición de ese teléfono recibe 401 y la app regresa al inicio de sesión.
- Máximo 10 intentos de inicio de sesión por minuto por IP. Un usuario que no existe tarda lo mismo en rechazarse que una contraseña incorrecta.
- El autor de cada registro sale de la sesión, nunca de lo que manda la app.
- Archivos de hasta 10 MB; el límite se cuenta al recibir aunque el tamaño declarado mienta. Solo los descarga quien puede ver esa sala.
- Los errores internos responden un mensaje genérico, sin detalles.

## Pruebas

```sh
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew -p backend test
```

Corren el servidor real contra H2 en memoria con los datos de prueba y revisan:
- que la familia solo vea a sus hijos;
- que la educadora no vea niños de otra sala;
- que la fundación no reciba nombres de niños;
- que retirar el acceso cierre la sesión abierta;
- el límite de archivos y la descarga de material de otra sala;
- el límite de intentos de inicio de sesión.

## Cómo está hecho

- `Main.kt`: arranque, plugins, inicio de sesión y manejo de errores (403, 400, 409, 413).
- `Db.kt`: cada colección es una tabla con el id, unas columnas para filtrar (estancia, sala, niño, fecha) y el modelo completo en JSON. Las escrituras van en transacción y de una en una.
- `Rutas*.kt`: una ruta por método de los repositorios de la app (`app/.../data/Repositorios.kt`), con las mismas reglas que los repositorios falsos.
- `Semilla.kt`: datos de prueba o cuenta inicial de la fundación.
- El esquema se crea al arrancar. Si se cambia una tabla existente hay que agregar migraciones (por ejemplo Flyway).

## API

Todas las rutas, salvo `GET /` y `POST /auth/login`, piden `Authorization: Bearer <token>`. Los cuerpos son JSON con los modelos de `shared/`.

| Área | Rutas |
| --- | --- |
| Sesión | `POST /auth/login`, `POST /auth/logout`, `GET /yo`, `PUT /yo/huella` |
| Estancias | `GET /estancias`, `/salas`, `/educadoras`, `/personal`, `/resumen`; `PUT /estancias`, `/salas` |
| Niños | `GET /ninos`, `/ninos/{id}`, `/ninos/{id}/tutores`, `/tutores`; `PUT /ninos`; `POST /ninos/{id}/baja`, `/ninos/{id}/reactivar`; `DELETE /ninos/{id}` |
| Jornada | `GET /asistencias`, `/eventos` (`?fecha=` o `?nino=&desde=&hasta=`), `/reporte?mes=&estancia=&sala=`; `POST /asistencias/llegada`, `/asistencias/salida`, `/eventos`, `/eventos/{id}/fin`, `/exportaciones`; `DELETE /eventos/{id}` |
| Incidentes | `GET /incidentes` (igual que eventos), `/incidentes/{id}`; `POST /incidentes` |
| Fotos | `GET /fotos?sala=` o `?nino=`, `/fotos/{id}`; `POST /fotos`, `/fotos/{id}/consulta`; `DELETE /fotos/{id}` |
| Avisos | `GET /avisos`, `/avisos/leidos`; `POST /avisos`, `/avisos/leidos` |
| Objetos perdidos | `GET /objetos?sala=`, `/objetos/vistos`; `POST /objetos?sala=&descripcion=&donde=&fecha=` (la foto va como cuerpo), `/objetos/vistos`, `/objetos/{id}/reclamo`, `/entrega`, `/rechazo` |
| Material | `GET /material?sala=`; `POST /material?sala=&titulo=&descripcion=&nombre=` (el archivo va como cuerpo); `PUT`, `DELETE /material/{id}` |
| Archivos | `GET /archivos/{id}` |
| Mensajes | `GET /conversaciones`, `/conversaciones/{id}`, `/conversaciones/{id}/mensajes`, `/mensajes/no-leidos`, `/contactos`; `POST /conversaciones/{id}/mensajes`, `/conversaciones/{id}/leida`, `/directas` |
| Horarios | `GET /salas/{id}/bloques`, `/salas/{id}/especiales`; `PUT /bloques`, `/especiales`; `DELETE /bloques/{id}`, `/especiales/{id}` |
| Cuentas | `GET /cuentas`; `POST /cuentas/direccion`, `/cuentas/educadora`, `/cuentas/familia`, `/cuentas/{id}/sala`, `/cuentas/{id}/retiro` |
| Actividad | `GET /registros` (los 2000 más recientes) |
