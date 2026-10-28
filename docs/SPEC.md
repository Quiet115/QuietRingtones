# QuietRingtones — Especificación (MVP)

## Problema
Cambiar el tono de llamada, el sonido de notificación y el de alarma en Android obliga a navegar por Ajustes del sistema, que es lento, no permite usar tus propios audios con facilidad y no permite alternar rápido entre "modos" (trabajo, noche, casa). En iPhone, además, el sistema **no permite** que ninguna app cambie los tonos del sistema.

## Solución
App Android nativa que muestra qué sonidos están activos en el sistema, permite previsualizar y elegir cualquiera (del sistema o archivos propios importados) y aplicarlo como predeterminado en un toque. Los **perfiles** guardan un conjunto (tono + notificación + alarma) y lo aplican de una vez. Siempre se puede **restaurar** lo que había antes.

La versión iPhone es una fase posterior y distinta (ver "Límites de plataforma").

## Límites de plataforma (hechos que condicionan el alcance)
- **Android**: una app con el permiso especial *Modificar ajustes del sistema* (`WRITE_SETTINGS`, lo concede el usuario a mano) puede fijar el tono, la notificación y la alarma **predeterminados** del sistema. No puede cambiar el sonido que cada *otra* app asigna a sus propios canales de notificación (eso es siempre manual del usuario en Ajustes de esa app).
- **iPhone**: ninguna app de terceros puede cambiar el tono de llamada, el tono de mensaje ni el sonido de notificaciones de otras apps. Lo único posible: (a) generar un archivo `.m4r` (≤ 30 s) y guiar al usuario para instalarlo (GarageBand / Archivos / Finder), y (b) sonidos propios para las notificaciones de la propia app. Por tanto "cambiar lo activado en el sistema" **no es viable en iOS**; la expansión será un "creador/exportador de tonos".

## Historias de usuario
1. Como usuario quiero ver el tono de llamada, la notificación y la alarma activos ahora mismo (nombre) para saber qué tengo.
2. Como usuario quiero ver la lista de sonidos disponibles de cada tipo y escucharlos antes de elegir.
3. Como usuario quiero aplicar un sonido como predeterminado de un tipo con un toque.
4. Como usuario quiero que la app me guíe para conceder el permiso de modificar ajustes, y que me diga claramente si falta, para no fallar en silencio.
5. Como usuario quiero importar un audio mío (mp3/m4a/ogg/wav) y usarlo como tono, notificación o alarma.
6. Como usuario quiero guardar perfiles ("Trabajo", "Noche") con un conjunto de tres sonidos y aplicarlos de una vez.
7. Como usuario quiero ver mis sonidos de antes de usar la app como un perfil llamado «Estado original» (solo lectura, siempre disponible) para volver a ellos en un toque.
8. Como usuario quiero que, si un sonido guardado ya no existe (audio borrado), se me avise y se use uno de reserva en vez de dejar el sistema sin sonido.

## Decisiones de implementación
- **Pila**: Android nativo, Kotlin + Jetpack Compose. Todo lo importante es API del sistema; un framework multiplataforma no aporta ahora y añade capas.
- **Módulos** (pocos y profundos):
  - `core` (Kotlin puro, JVM, sin Android): tipos del dominio, reglas de perfiles, restauración, validación y reserva. Sin dependencias del sistema → se prueba en JVM sin emulador.
  - `SoundSystem` (interfaz del *puerto*): `current(kind)`, `available(kind)`, `apply(kind, sound)`, `canModify()`, `import(archivo, kind)`, `preview(sound)`. Implementación Android con `RingtoneManager`, `Settings.System`, `MediaStore`, `MediaPlayer`. Implementación falsa en memoria para pruebas.
  - `ProfileStore`: persistencia de perfiles y del "estado original" (DataStore o JSON local).
  - `app` (Compose): pantallas Sonidos, Perfiles, Permiso/Ajustes. Solo presentación.
- **Importación**: selector de archivos del sistema (SAF, sin permiso de almacenamiento) → copia a `MediaStore` en `Ringtones/`, `Notifications/` o `Alarms/` (Android 10+, sin permiso extra) → queda disponible como sonido del sistema.
- **Restauración**: la primera vez que la app cambia un sonido guarda el valor original por tipo; "Restaurar" lo reaplica.
- **Versiones**: minSdk 29 (Android 10; MediaStore con ámbito simplifica la importación), target 35.
- **Idioma de la UI**: español (base), cadenas en recursos para traducir.
- **Datos**: todo local; sin cuenta, sin servidor, sin red, sin analítica en el MVP.

## Decisiones de pruebas
- Seam principal: la interfaz `SoundSystem` (un solo seam). Las pruebas ejercitan el comportamiento de perfiles/restauración/reserva a través de `core` + `SoundSystem` falso, en JVM.
- La implementación Android de `SoundSystem` se verifica con una prueba instrumentada mínima y a mano en un dispositivo real (no hay emulador en este equipo); no se prueba con mocks de `RingtoneManager`.
- Suite completa una vez al final; typecheck/compilación frecuente.

## Fuera de alcance (MVP)
- Sonido por contacto, por app o por canal de notificación de otras apps.
- Perfiles automáticos por horario/ubicación.
- Recortar o editar audio; descarga de catálogos de tonos; cuentas, nube, pagos, anuncios.
- Doble SIM con tonos distintos.
- **iPhone**: cualquier cosa; se planifica como fase 2 (exportador `.m4r` + guía) y no se promete cambio de tonos del sistema.
- Publicación en Play Store (el MVP termina en APK firmado de depuración instalable).

## Riesgos / supuestos anotados
- Fabricantes (Samsung, Xiaomi) pueden tener ajustes de sonido propios que ignoren parcialmente el valor predeterminado de Android; hay que probar en dispositivo real.
- Sin emulador ni dispositivo conectado en este equipo, la verificación en vivo depende del usuario (instalar el APK).
