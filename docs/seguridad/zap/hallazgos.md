# Hallazgos del escaneo DAST con OWASP ZAP

- Herramienta: `ghcr.io/zaproxy/zaproxy:stable`, `zap-api-scan.py` con la especificación `openapi.json` (61 rutas, generadas de las rutas del backend; se quitó `/auth/logout` para que el escaneo no cerrara su propia sesión).
- Objetivo: el backend en la red de docker compose (`http://backend:8080`), el mismo contenedor que publica el túnel https.
- Sesión: escaneo **autenticado** con el token de la cuenta `direccion` de la demo, después de `reiniciar-demo`. El token se pasó con las variables `ZAP_AUTH_HEADER=Authorization` y `ZAP_AUTH_HEADER_VALUE="Bearer …"` y no quedó en ningún archivo (se revisó con grep).
- Fecha: 22 de octubre de 2026.

Nota de método: el primer intento autenticado usó el `replacer` dentro de `-z`. ZAP separa esas opciones por espacios, así que el valor quedó como `Bearer` sin token y todas las rutas protegidas respondieron 401. Ese intento se descartó. Con las variables de entorno, el backend ya no registró ningún 401: la sesión funcionó en todo el escaneo.

## Resultados (escaneo autenticado, reportes `reporte-zap.*`)

| Riesgo (confianza) | Alerta | Instancias | Endpoints |
|---|---|---|---|
| Alto | ninguna | 0 | |
| Medio | ninguna | 0 | |
| Bajo | ninguna | 0 | (la falta de Cross-Origin-Resource-Policy se corrigió, ver abajo) |
| Informativo (alta) | A Client Error response code was returned by the server | 4385 | todas las rutas |
| Informativo (media) | Non-Storable Content | 5 | `GET /asistencias`, `GET /avisos`, `GET /avisos/leidos`, `POST /asistencias/llegada`, `POST /avisos/leidos` |

Resumen de ZAP: `FAIL-NEW: 0, WARN-NEW: 0, PASS: 120`.

Respuestas que registró el backend durante el escaneo autenticado:

| Código | Cantidad | Significado |
|---|---|---|
| 429 | 3867 | límite general de la API: 1000 peticiones por minuto por IP |
| 400 | 595 | datos inválidos rechazados |
| 403 | 150 | la dirección intentó algo que su rol no permite (por ejemplo, crear cuentas de dirección o ver bloqueos) |
| 204 | 54 | escrituras válidas |
| 404 | 51 | rutas o ids que no existen |
| 200 | 26 | lecturas válidas |
| 405 | 6 | método no permitido |
| 401 | 0 | la sesión siguió válida en todo el escaneo |
| 500 | 0 | ningún error interno |

## Justificación o mitigación de cada alerta

**Cross-Origin-Resource-Policy faltante (bajo): corregido.**
En el escaneo sin sesión ZAP marcó que faltaba esta cabecera en `GET /` (reportes `antes-reporte-zap.*`). Sin ella, una página web de otro origen podría incrustar respuestas de la API, como fotos o archivos. Se agregó `Cross-Origin-Resource-Policy: same-origin` a `CabecerasSeguras` en `backend/src/main/kotlin/mx/tec/sonrisas/servidor/Seguridad.kt`. La app de Android no se ve afectada porque esta política solo la aplican los navegadores. Después de reconstruir, la alerta desapareció (de `WARN-NEW: 1, PASS: 119` a `WARN-NEW: 0, PASS: 120`) y la cabecera también sale por el túnel.

**Respuestas de error del cliente (informativo): esperado.**
Son los 400, 403, 404, 405 y 429 de la tabla anterior. Muestran que la API rechaza los datos inválidos, los ids inventados y las acciones fuera del rol, y que no contesta nada en las rutas de metadatos de nube que prueba ZAP (`/latest/meta-data/`, `/computeMetadata/v1/`). Ninguna respuesta fue 500 ni dejó ver detalles internos.

**Contenido no almacenable (informativo): intencional.**
Todas las respuestas llevan `Cache-Control: no-store` (P1), para que ni proxies ni el teléfono guarden en caché datos de niños.

## Evidencia de que los controles funcionan (429)

- **Límite general:** 3867 de las peticiones de ZAP recibieron 429. Una herramienta automática que manda miles de peticiones por minuto desde una sola IP queda frenada, que es justo el objetivo del control contra inundaciones.
- **Límite de login:** los intentos de ZAP sobre `/auth/login` también recibieron 429.
- **Bloqueo por varias IPs:** no se disparó (`P4: cuenta … bloqueada` no aparece en el log). Todo el tráfico de ZAP viene de una sola IP, y el bloqueo de 3 días necesita fallos desde 3 IPs distintas.
- **Costo:** por esos 429, ZAP no pudo probar a fondo una parte de las combinaciones. La cobertura completa de inyección, IDOR y privilegios está en las pruebas automatizadas (`SeguridadTest` S08 a S14 y `SEGBloqueoCuentaTest`). Para un escaneo más profundo se puede levantar el límite solo en un entorno de pruebas, nunca en el despliegue.

## Efecto en los datos de la demo

El escaneo hizo 54 escrituras válidas como `direccion`, por ejemplo marcar avisos como leídos. Si la demo debe verse limpia, hay que volver a correr `reiniciar-demo`.
