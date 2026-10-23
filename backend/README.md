# Servidor de Sonrisas de Cristal

Ktor y PostgreSQL. Aplica las mismas reglas de privacidad que la app (qué niño, sala o conversación ve cada quien) usando el código de `shared/`, que también compila la app.

## Correrlo en local

Con Docker (servidor y PostgreSQL), desde la raíz del repo. La primera vez crea el archivo `.env` con la contraseña de la base; está en `.gitignore` y nunca se sube:

```sh
cp .env.example .env          # en PowerShell: Copy-Item .env.example .env
# edita .env y pon una POSTGRES_PASSWORD larga (letras, números y guiones)
docker compose up -d --build
```

El puerto solo queda abierto para esta computadora (`127.0.0.1:8080`) y su emulador. Para entrar desde otro dispositivo, levanta también el túnel HTTPS de Cloudflare:

```sh
docker compose --profile tunel up -d
docker compose logs tunel     # busca la dirección https://….trycloudflare.com
```

Esa dirección cambia cada vez que se reinicia el túnel.

Si cambias `POSTGRES_PASSWORD` después de haber levantado la base, borra el volumen para que PostgreSQL se cree con la nueva contraseña (también borra los datos): `docker compose down -v`.

Sin Docker (usa H2, una base en el archivo `backend/datos/`):

```sh
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew -p backend run
```

En los dos casos queda en `http://localhost:8080`. La primera vez carga los datos de prueba de la demo (cuentas listadas en el README principal, contraseña `demo`). Los cambios se guardan en la base: sobreviven a reiniciar el servidor y la app.

### Volver a cargar los datos de prueba

Los datos de prueba se generan con la fecha del día. Antes de presentar conviene recargarlos para que "hoy" tenga llegadas, comidas y siestas:

```sh
docker compose run --rm --no-deps backend /app/bin/backend reiniciar-demo
# o sin docker:
./gradlew -p backend run --args=reiniciar-demo
```

Borra todo, vuelve a cargar los datos y termina; el servidor que ya está corriendo no necesita reiniciarse. Las sesiones abiertas en los teléfonos se cierran. Con `DATOS_DEMO=false` el comando se niega a correr, para que nunca borre una base real.

### Conectar la app

En la pantalla de inicio de sesión toca "Servidor":

- Emulador de Android Studio: `http://10.0.2.2:8080`.
- Teléfono o tablet: la dirección `https://….trycloudflare.com` del túnel. La versión release solo acepta `https://`.
- Red local por `http://` (solo versión debug): pon `HOST_IP=0.0.0.0` en `.env` y usa la IP de la laptop, por ejemplo `http://192.168.1.20:8080`. Abre el puerto sin cifrar a toda la red, así que úsalo solo para pruebas.

Solo en la versión debug, si el servidor falla las cuentas de prueba siguen entrando con datos de prueba en memoria. La versión release no tiene ese respaldo: sin servidor no se puede iniciar sesión. Si la base se creó antes del cambio de contraseña a `demo`, corre `reiniciar-demo` una vez.

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
| `PROXY_CONFIABLE` | `true` detrás de Render, del túnel u otro proxy | `false` (`true` en `docker-compose.yml`) |
| `POSTGRES_PASSWORD` | Contraseña de PostgreSQL, en `.env` | `docker compose` no arranca |
| `HOST_IP` | Interfaz donde se publica el puerto 8080, en `.env` | `127.0.0.1` (solo esta computadora) |

## Seguridad

- Contraseñas con PBKDF2-HMAC-SHA256 (210 000 iteraciones, sal por cuenta).
- Sesiones con tokens aleatorios de 32 bytes. La base solo guarda su SHA-256, así que una copia de la base no sirve para entrar. Duran 30 días; cerrar sesión borra el token.
- Cada petición vuelve a leer la cuenta: si la dirección retira el acceso, la siguiente petición de ese teléfono recibe 401 y la app regresa al inicio de sesión.
- Máximo 10 intentos de inicio de sesión por minuto por IP, y 5 fallos seguidos bloquean esa cuenta 15 minutos desde cualquier IP (también nombres que no existen, para no delatar cuáles existen). Fallos desde 3 IPs distintas la bloquean 3 días; la fundación la puede desbloquear. Un usuario que no existe tarda lo mismo en rechazarse que una contraseña incorrecta.
- Toda la API tiene un límite de 1000 peticiones por minuto por IP. Detrás de un proxy se usa la última IP de `X-Forwarded-For`, la que agrega el proxy, porque las anteriores las puede inventar el cliente.
- El autor de cada registro sale de la sesión, nunca de lo que manda la app.
- Archivos de hasta 10 MB; el límite se cuenta al recibir aunque el tamaño declarado mienta. La imagen de una foto solo la descarga quien ve esa foto; material y objetos, quien ve la sala; una subida que aún no es foto, solo el personal de la sala.
- Borrar un expediente elimina también sus fotos con la imagen, su registro de actividad y la cuenta de la familia que se queda sin hijos. La purga (al arrancar y cada 6 h) borra además sesiones vencidas y archivos sin uso de más de 24 h.
- Los errores internos responden un mensaje genérico, sin detalles. Una fecha mal escrita responde 400, no 500.
- Cabeceras de seguridad en todas las respuestas: `Strict-Transport-Security`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Content-Security-Policy`, `Referrer-Policy` y `Cache-Control: no-store`.
- Un expediente solo se borra definitivamente si ya está en baja, aunque la petición no venga de la app.
- Familias y educadoras no reciben los nombres de usuario (logins) del personal.
- Las contraseñas temporales son de tres palabras y cuatro dígitos (unos 31 bits de entropía).
- En Docker: contraseña de la base obligatoria en `.env`, puerto solo en `127.0.0.1`, contenedor de solo lectura, sin capabilities y con `no-new-privileges`.

## Pruebas

```sh
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew -p backend test
```

En Windows (PowerShell):

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat -p backend test
```

El reporte queda en `backend/build/reports/tests/test/index.html`. Para correrlas contra PostgreSQL real, define `PRUEBAS_DATABASE_URL=postgres://usuario:clave@host:puerto/base`; cada prueba usa su propio esquema y lo borra al final.

Corren el servidor real contra H2 en memoria con los datos de prueba y revisan:
- que la familia solo vea a sus hijos;
- que la educadora no vea niños de otra sala;
- que la fundación no reciba nombres de niños;
- que retirar el acceso cierre la sesión abierta;
- el límite de archivos y la descarga de material de otra sala;
- que la imagen de una foto solo la descargue quien puede ver esa foto;
- el límite de intentos de inicio de sesión.

`SeguridadTest.kt` agrega 27 ataques con ids S01–S27: tokens falsos, alterados o vencidos, inyección SQL, IDOR, privilegios, límites de peticiones y bloqueo por cuenta, validación de entrada, cabeceras y contraseñas temporales.

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
| Fotos | `GET /fotos?sala=` o `?nino=`, `/fotos/{id}`; `POST /fotos/imagen?sala=` (la imagen va como cuerpo, devuelve su id), `/fotos`, `/fotos/{id}/consulta`; `DELETE /fotos/{id}` |
| Avisos | `GET /avisos`, `/avisos/leidos`; `POST /avisos`, `/avisos/leidos` |
| Objetos perdidos | `GET /objetos?sala=`, `/objetos/vistos`; `POST /objetos?sala=&descripcion=&donde=&fecha=` (la foto va como cuerpo), `/objetos/vistos`, `/objetos/{id}/reclamo`, `/entrega`, `/rechazo` |
| Material | `GET /material?sala=`; `POST /material?sala=&titulo=&descripcion=&nombre=` (el archivo va como cuerpo); `PUT`, `DELETE /material/{id}` |
| Archivos | `GET /archivos/{id}` |
| Mensajes | `GET /conversaciones`, `/conversaciones/{id}`, `/conversaciones/{id}/mensajes`, `/mensajes/no-leidos`, `/contactos`; `POST /conversaciones/{id}/mensajes`, `/conversaciones/{id}/leida`, `/directas` |
| Horarios | `GET /salas/{id}/bloques`, `/salas/{id}/especiales`; `PUT /bloques`, `/especiales`; `DELETE /bloques/{id}`, `/especiales/{id}` |
| Cuentas | `GET /cuentas`, `/bloqueos` (solo fundación); `POST /cuentas/direccion`, `/cuentas/educadora`, `/cuentas/familia`, `/cuentas/{id}/sala`, `/cuentas/{id}/retiro`, `/cuentas/{id}/desbloqueo` (solo fundación) |
| Actividad | `GET /registros` (los 2000 más recientes) |
