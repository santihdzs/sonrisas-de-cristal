# Sonrisas de Cristal

App Android para la Fundación Estancias Infantiles Sonrisas de Cristal (Monterrey). Sustituye la bitácora en papel y los grupos de WhatsApp de cada estancia con una sola app para cuatro roles: educadora, familia, dirección y fundación. Proyecto del curso TC2007B.

## Stack y arquitectura

- **App:** Kotlin, Jetpack Compose, Material 3. `minSdk` 26, `targetSdk` 37.
- **Servidor:** Ktor con PostgreSQL. Si no hay `DATABASE_URL`, usa H2.
- `shared/`: modelos, reglas puras (validaciones, reporte de asistencia, `Visibilidad`: quién ve a qué niño), cuerpos del API y generador de datos de prueba. Lo compilan la app y el servidor.
- `app/.../data/`: interfaces de repositorios. `AppContainer` elige entre `data/remoto/` (servidor) y `data/fake/` (datos en memoria, solo en debug).
- `app/.../bitacora/`: bitácora imprimible en PDF (`DisenoBitacora` sin Android, `PdfBitacora` con `PdfDocument`).
- `app/.../ui/`: pantallas por rol, cada una con un `UiState` (cargando, éxito o error).
- `backend/`: servidor. Detalle del API, variables y despliegue en Render + Neon en [backend/README.md](backend/README.md).

Toda regla de privacidad vive en la capa de datos, nunca en las pantallas. Con servidor, quien la aplica es el servidor.

## Enlaces

- [Aviso de privacidad](app/src/main/assets/aviso-privacidad.md)
- [Reporte final (PDF)](docs/reporte-final.pdf)
- [Presentación 1](docs/presentaciones/presentacion-1.pdf) · [Presentación 2](docs/presentaciones/presentacion-2.pdf)
- [APK de release firmado](entrega/sonrisas-de-cristal-v3.0.0.apk)
- [Reporte OWASP ZAP](docs/seguridad/zap/hallazgos.md) · [Verificación del despliegue](docs/seguridad/verificacion-despliegue.txt) · [Resultados de pruebas](docs/pruebas/resultados.txt)
- [Video de demostración](https://drive.google.com/file/d/12scqGtx0UNwZK7dzs1meVS2RLC9BJlBB/view?usp=sharing)

## Backend

Requiere Docker. Un solo comando, desde la raíz del repo:

```sh
./levantar.sh
```

Crea `.env` a partir de `.env.example` (ya trae valores listos para la demo), levanta PostgreSQL, el backend con los datos de demostración y el túnel HTTPS, e imprime la dirección `https://….trycloudflare.com` que se le da a la app. En Windows (PowerShell):

```powershell
Copy-Item .env.example .env
docker compose --profile tunel up -d --build
docker compose logs tunel     # dirección https para la app
```

Detener sin borrar datos: `docker compose --profile tunel down`. En producción cambia `POSTGRES_PASSWORD` en `.env` por una aleatoria (`openssl rand -hex 24`) y pon `DATOS_DEMO=false`.

Variables de `.env.example`:

| Variable | Uso |
| --- | --- |
| `POSTGRES_PASSWORD` | Obligatoria. Contraseña de PostgreSQL. `.env` está en `.gitignore`. |
| `DATOS_DEMO` | `true` carga las cuentas de la demo; en producción, `false`. |
| `CLAVE_FUNDACION` | Solo con `DATOS_DEMO=false`: contraseña inicial de `fundacion` (12 caracteres o más). |
| `PROXY_CONFIABLE` | `true` detrás del túnel o de Render: la IP real llega en `X-Forwarded-For`. |
| `HOST_IP` | `127.0.0.1` (solo esta computadora y su emulador). `0.0.0.0` abre HTTP sin cifrar a la red, solo para pruebas. |

**Seed idempotente.**
- Al arrancar con la base vacía, carga los datos de prueba (`DATOS_DEMO=true`) o crea solo la cuenta `fundacion`. Si la base ya tiene datos, no toca nada.
- Para regenerar la demo con la fecha de hoy:

  ```sh
  docker compose run --rm --no-deps backend /app/bin/backend reiniciar-demo
  ```

  Con `DATOS_DEMO=false` el comando se niega a correr.

**Sin Docker (H2 en `backend/datos/`):**

```sh
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew -p backend run
./gradlew -p backend run --args=reiniciar-demo
```

En ambos casos el servidor queda en `http://localhost:8080`. El emulador entra por `http://10.0.2.2:8080`.

## Exponerlo

El perfil `tunel` de `docker-compose.yml` levanta Cloudflare Tunnel con la imagen fijada `cloudflare/cloudflared:2026.9.3`. Va por `--protocol http2` (TCP 443), porque muchas redes bloquean QUIC. Da HTTPS público sin abrir puertos:

```sh
docker compose --profile tunel up -d
docker compose logs tunel     # dirección https://….trycloudflare.com (cambia en cada reinicio)
```

## Compilar e instalar el APK

Se necesita el JDK de Android Studio: Gradle 9.8 no corre con Java 11.

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleRelease                                        # release firmado
./gradlew :app:assembleRelease -Pservidor=https://<tu-servidor>       # con servidor por defecto
./gradlew :app:assembleDebug                                          # debug
```

**Firma del release.** `keystore.properties` va en la raíz del repo y está en `.gitignore`. Contiene:

```properties
storeFile=/ruta/fuera/del/repo/sonrisas.keystore
storePassword=...
keyAlias=...
keyPassword=...
```

El `.keystore` vive fuera del repo. Sin `keystore.properties`, el release sale sin firmar.

El APK de la entrega está en `entrega/sonrisas-de-cristal-v3.0.0.apk`. Para instalarlo: `adb install entrega/sonrisas-de-cristal-v3.0.0.apk`, o copiarlo al teléfono y abrirlo.

**Elegir el servidor.**
- En debug: opción "Servidor" del inicio de sesión.
- En release: mantener presionado el logo.
- Release solo acepta `https://`.

**Datos de prueba sin servidor.**
- **Release no tiene datos de prueba de respaldo.** Sin servidor no se puede iniciar sesión.
- **Solo debug** conserva el respaldo: si el servidor no contesta o no conoce la cuenta, las cuentas de prueba entran con datos en memoria generados con la fecha del teléfono.

## Cuentas de prueba

Todas usan la contraseña `demo`.

| Rol | Usuarios |
| --- | --- |
| Fundación | `fundacion` |
| Dirección | `direccion` (San Bernabé), `direccion.vv` (Valle Verde) |
| Educadora | `educadora`, `educadora.lactantes`, `educadora.preescolar`, `educadora.vv.lactantes`, `educadora.vv.maternales`, `educadora.vv.preescolar` |
| Familia | `familia`, `familia.papa`, `familia.paola` |

## Pruebas

```sh
./gradlew -p backend test                 # servidor real contra H2 en memoria
PRUEBAS_DATABASE_URL=postgres://usuario:clave@host:5432/base ./gradlew -p backend test   # contra PostgreSQL real
./gradlew :app:testDebugUnitTest          # pruebas unitarias de la app
```

- **Backend:** cubre los RF por requisito (`RF01…RF15`), 27 ataques en `SeguridadTest` (S01–S27), archivos, bloqueo de cuentas, reloj del servidor y eliminación ARCO de punta a punta.
- **Reportes:**
  - [docs/pruebas/](docs/pruebas/): `resultados.txt` y el reporte HTML del backend.
  - [docs/seguridad/zap/](docs/seguridad/zap/): escaneo OWASP ZAP antes y después, con `hallazgos.md`.

## Seguridad

- **Contraseñas:** PBKDF2-HMAC-SHA256 con 210 000 iteraciones y sal por cuenta.
- **Sesiones:**
  - Tokens opacos de 32 bytes; la base guarda solo su SHA-256.
  - Duran 30 días. Retirar el acceso o cerrar sesión los invalida de inmediato.
  - En el teléfono, el token se cifra con AES-256-GCM y la llave vive en el Android Keystore.
- **Fuerza bruta:**
  - 10 intentos de login por minuto por IP.
  - 5 fallos bloquean la cuenta 15 minutos; fallos desde 3 IPs, 3 días.
  - Mismo tiempo de respuesta exista o no la cuenta.
  - Límite global de 1000 peticiones por minuto por IP.
- **Autorización en el servidor:**
  - Cada familia ve solo a sus hijos; la educadora, solo su sala; la fundación, solo datos consolidados.
  - El autor de cada registro sale de la sesión, nunca de la app.
- **Archivos:**
  - Hasta 10 MB.
  - Las fotos se validan por sus bytes y se recodifican sin EXIF ni GPS.
  - Cada archivo solo lo descarga quien puede ver la foto o la sala. Una subida sin foto, solo el personal.
- **Transporte:** HTTPS por túnel o Render, HSTS y cabeceras de seguridad en toda respuesta. Release no permite HTTP sin cifrar.
- **Datos personales:** aviso de privacidad dentro de la app. Borrar un expediente elimina fotos, imágenes, bitácora, incidentes, registros de actividad y la cuenta de la familia que se queda sin hijos inscritos.
- **Docker:** contenedor de solo lectura, sin capabilities, `no-new-privileges` y puerto solo en `127.0.0.1`.

## Funciones por rol

- **Educadora** (Sala, Fotos, Mensajes, Avisos):
  - Lista de su sala; llegada y salida con personas autorizadas.
  - Bitácora del día y registro grupal; incidentes.
  - Fotos con cámara y autorización de imagen.
  - Material, objetos perdidos, impresión de la bitácora y chat.
- **Familia** (Hoy, Semana, Fotos, Mensajes, Avisos):
  - El día y la semana de cada hijo; fotos donde aparece; incidentes.
  - Horario, material y objetos perdidos.
  - Bitácora para imprimir o compartir; chat con las educadoras.
- **Dirección** (Inicio, Niños, Mensajes, Avisos, Más):
  - Inscripción, expediente, bajas y eliminación.
  - Horarios, reporte de asistencia y registro de actividad.
  - Salas, personal y cuentas de familia.
- **Fundación:**
  - Resumen consolidado y reporte de asistencia sin nombres de niños.
  - Estancias, cuentas de dirección y desbloqueo de cuentas.

## Requerimientos

| Clave | Requerimiento | Dónde se cubre |
| --- | --- | --- |
| RF-01 | Dos tipos de perfil | Login con navegación distinta por rol (educadora, familia, dirección, fundación) |
| RF-02 | Lista de niños de la sala | "Mi sala"; solo se devuelven niños de la sala de la educadora |
| RF-03 | Entrada y salida | Hora, quién entrega, quién recoge y personal que entrega |
| RF-04 | Bitácora del día | Ficha del día con acciones rápidas |
| RF-05 | Registro para varios niños | Registro grupal |
| RF-06 | Vista del familiar | Pestaña "Hoy" |
| RF-07 | Cada familia ve solo a sus hijos | `Visibilidad`; aplica a fotos, chats, material, objetos perdidos e impresiones |
| RF-08 | Avisos del centro | Avisos con indicador de no leídos |
| RF-09 | Fotografía de la actividad | Fotos etiquetadas, visibles solo para las familias de esos niños |
| RF-10 | Borrar una foto | La familia borra la foto y su imagen para todos |
| RF-11 | Funciona sin internet | Pendiente, fuera de alcance: requiere guardar en el teléfono y sincronizar |
| RF-12 | Resumen semanal | Pestaña "Semana", compartir resumen e imprimir bitácora |
| RF-13 | Desbloqueo con huella | Bloqueo al ir a segundo plano o tras 5 minutos |
| RF-14 | Quién hizo qué | Registro de actividad: capturas, cuentas, salas, objetos, material y bitácoras exportadas |
| RF-15 | Inscripción y expediente | Tutores, personas autorizadas, tutores compartidos entre hermanos y cuentas de familia |
| RF-16 | Autorización de imagen | Dos permisos por niño, validados también en el servidor |
| RF-17 | Varias estancias | Todo se filtra por estancia; la fundación solo ve datos consolidados |
| RF-18 | Baja y eliminación | Baja temporal de 30 días y después eliminación completa en el servidor (al arrancar y cada 6 h). Se borran expediente, asistencia, bitácora, incidentes, fotos con su imagen (de una foto grupal solo se quita la etiqueta), registros de actividad del niño y la cuenta, chats y sesiones de la familia que se queda sin hijos inscritos. También se purgan sesiones vencidas y archivos huérfanos |
| RF-19 | Incidentes | Registro con folio; aparecen en "Hoy" y en la bitácora impresa |
| RF-20 | Reporte de asistencia | Por sala y por mes, para dirección y fundación |
| RF-21 | Horario de actividades | Horario por sala y actividades especiales |

## Decisiones clave

- **Sin registro público.** La fundación crea las cuentas de dirección; la dirección crea las de educadoras y familias. La contraseña temporal se muestra una sola vez.
- **Retirar acceso no borra la cuenta del personal.** Queda inactiva y conserva su nombre en la bitácora; su sesión se cierra en el momento.
- **Hermanos.** Un tutor es un solo registro para todos sus hijos y una sola cuenta de familia.
- **Chats.** Participa solo quien tiene acceso a la app. La pertenencia se recalcula siempre y los mensajes no se copian al registro de actividad.
- **Archivos en la base.** Material, fotos e imágenes de objetos se guardan en PostgreSQL. La app los baja a su caché privada y los borra al cerrar sesión.
- **Sesión con tokens opacos y no JWT,** para poder invalidarlos en el momento.
- **Actualización por polling:** cada 15 s, y el chat cada 3 s. No hay notificaciones push.
- **Bitácora fiel al formato oficial:** carta, texto mínimo de 7 pt y una hoja de observaciones cuando el texto no cabe.
