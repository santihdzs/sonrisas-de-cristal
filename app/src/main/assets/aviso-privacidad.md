# Aviso de privacidad integral

Versión 1.0, vigente desde el 22 de octubre de 2026.

Este aviso da cumplimiento a los artículos 14, 15, 16 y 17 de la Ley Federal de Protección de Datos Personales en Posesión de los Particulares (LFPDPPP, DOF 20 de marzo de 2025) y describe exactamente lo que hace la app Sonrisas de Cristal.

## 1. Quién es responsable de tus datos

La Fundación Estancias Infantiles Sonrisas de Cristal (la Fundación), con domicilio en Monterrey, Nuevo León, es responsable del tratamiento de los datos que se registran en la app. Cada estancia tiene una Dirección que atiende las solicitudes de privacidad de sus familias y de su personal (artículo 29 de la LFPDPPP).

## 2. Qué datos recopilamos

De niñas y niños:
- Identificación: nombre, apellidos, fecha de nacimiento y sala.
- Salud (datos sensibles, artículo 2, fracción VI): alergias, padecimientos y medicamentos; lesiones, síntomas o medicamentos reportados al llegar; incidentes como fiebre o reacción alérgica.
- Su día en la estancia: hora de llegada y salida, comidas, siestas, baño, estado de ánimo y notas de la educadora.
- Fotografías de actividades, solo si la familia lo autorizó y para el uso que autorizó.
- Autorización de imagen y estado del expediente (activo o en baja).

De madres, padres, tutores y personas autorizadas para recoger:
- Nombre, parentesco y teléfono.
- Quién dejó y quién recogió al niño cada día.
- Mensajes enviados a la estancia por el chat de la app.

Del personal (Dirección y educadoras):
- Nombre, usuario, rol, estancia y sala asignada.

De todas las cuentas:
- Usuario y contraseña. La contraseña nunca se guarda: solo se guarda una huella irreversible (PBKDF2 con 210 000 iteraciones).
- Registro de actividad: qué acción se hizo, quién y cuándo, con la hora del servidor.
- Si activaste el desbloqueo con huella digital, solo se guarda que está activado. Tu huella nunca sale del teléfono ni llega a nuestros servidores.

No recopilamos ubicación. A las fotografías se les borran los metadatos (EXIF, incluida la ubicación GPS) antes de guardarlas.

## 3. Para qué usamos tus datos

Finalidades necesarias para el servicio de cuidado infantil:
1. Registrar la asistencia y entregar a cada niño solo a personas autorizadas.
2. Llevar la bitácora diaria y avisar a la familia de incidentes y de temas de salud.
3. Permitir la comunicación entre la familia y la estancia.
4. Compartir con cada familia las fotografías en las que aparece su hijo o hija.
5. Generar reportes de asistencia y ocupación para la Dirección y la Fundación. A la Fundación solo le llegan totales, nunca datos de un niño en particular.
6. Proteger las cuentas: detectar accesos indebidos y llevar un registro de actividad.

No usamos tus datos con fines comerciales ni de mercadotecnia, ni para tomar decisiones automatizadas.

## 4. Consentimiento

Los datos de salud de los niños son sensibles. Por eso la madre, el padre o el tutor con patria potestad (artículos 23 y 425 del Código Civil Federal) da su consentimiento expreso y por escrito al inscribir al niño (artículo 8 de la LFPDPPP). La autorización para usar fotografías se registra por separado en el expediente, y la app no permite etiquetar en una foto a un niño sin esa autorización. Tratamos los datos de niñas y niños atendiendo a su interés superior y a su derecho a la intimidad (artículos 2 y 76 de la Ley General de los Derechos de Niñas, Niños y Adolescentes).

## 5. Dónde se guardan y cómo se protegen

- Los datos viven en la base de datos PostgreSQL de la Fundación, dentro de un contenedor aislado.
- Entre la app y el servidor todo viaja cifrado con HTTPS (TLS). Las respuestas del servidor incluyen cabeceras de seguridad (HSTS, CSP, nosniff) y no se guardan en caché.
- En tu teléfono, la sesión se guarda cifrada con AES-256-GCM, con una llave del Android Keystore. Al cerrar sesión se borran los archivos descargados.
- Cada persona ve solo lo que le corresponde: una familia solo ve a sus propios hijos; una educadora, solo su sala; la Dirección, solo su estancia. La Fundación no ve expedientes, fotos ni mensajes.
- La cuenta se bloquea ante intentos repetidos de acceso y hay límites de peticiones contra abusos (artículo 18 de la LFPDPPP).

## 6. Con quién compartimos tus datos

No transferimos tus datos a terceros. Solo los compartiríamos sin tu consentimiento en los casos del artículo 36 de la LFPDPPP; por ejemplo, ante un requerimiento de una autoridad competente o para la atención médica urgente de un niño. El proveedor del túnel cifrado que publica el servidor solo transporta el tráfico; no lo usa para nada más.

## 7. Cuánto tiempo los conservamos

- Expediente del niño, con sus datos de salud, asistencias, bitácora, incidentes y fotografías: mientras el niño esté inscrito. Al darlo de baja, el expediente se borra automáticamente a los 30 días. En ese plazo la baja se puede revertir.
- Fotografías: hasta que la educadora, la Dirección o la familia las borren. Al borrar una foto también se borra la imagen guardada. Si se elimina el expediente de un niño, se borran las fotos donde solo aparece él. De las fotos de grupo se le quita la etiqueta, pero la imagen se conserva para las familias de los demás niños; si quieres que se borre, pídelo a la Dirección o a la educadora.
- Cuentas de familia: se eliminan junto con el último expediente al que están ligadas, con su contraseña, sus sesiones, sus conversaciones directas y los mensajes que escribió en el chat de la sala. Los mensajes que escribió el personal en el chat de la sala se conservan.
- Sesiones: vencen a los 30 días o al cerrar sesión; las vencidas se borran automáticamente.
- Avisos y objetos perdidos: se conservan mientras opere la estancia.
- Registro de actividad: mientras opere la estancia, por seguridad. Las entradas ligadas a un niño se borran junto con su expediente; las de inicio y cierre de sesión conservan el nombre de la cuenta.

## 8. Tus derechos ARCO

Tú, o el padre, madre o tutor en el caso de un menor (artículo 21 de la LFPDPPP; artículo 89 del Reglamento), puedes:
- **Acceso:** pedir saber qué datos tenemos. Una familia ya puede ver en la app todo lo que se registró sobre sus hijos.
- **Rectificación:** pedir que se corrija un dato. La Dirección corrige el expediente desde la app.
- **Cancelación:** pedir que se borren tus datos. La Dirección da de baja el expediente y el borrado definitivo se hace como se describe en la sección 7.
- **Oposición:** oponerte a una finalidad. Por ejemplo, retirar la autorización de fotografías, que la Dirección cambia en el expediente.

Para hacerlo, pide la solicitud por escrito a la Dirección de tu estancia, con tu nombre, el dato y lo que pides (artículo 28). Te respondemos en un máximo de 20 días hábiles y, si procede, lo aplicamos en los 15 días hábiles siguientes (artículo 31). El trámite es gratuito (artículo 34). Si no estás de acuerdo con la respuesta, puedes acudir a la Secretaría Anticorrupción y Buen Gobierno (artículos 40 y 42).

Puedes retirar tu consentimiento en cualquier momento (artículo 7), aunque sin efectos retroactivos.

## 9. Cambios a este aviso

Si este aviso cambia, lo publicaremos en la app (pantalla Acerca de) y lo anunciaremos en Avisos (artículo 15, fracción VI).
