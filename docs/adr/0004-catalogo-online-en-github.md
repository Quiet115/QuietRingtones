# Catálogo online en un repositorio de GitHub, con vista previa en caché

Los sonidos nuevos (música clásica de dominio público o CC0) viven en `Quiet115/QuietRingtones-catalog` y no dentro del APK, para que la app pese poco (2,5 MB) y el catálogo pueda crecer sin publicar versiones. La app lee `catalog.json`, descarga un clip corto a una caché al pulsar reproducir y lo guarda de verdad al pulsar «Usar» (queda entre los audios del usuario, fijo arriba). La caché se puede vaciar desde Configuración → Catálogo sin perder lo ya añadido.

- **Los sonidos del teléfono no se mueven:** son del sistema (RingtoneManager) y siguen listándose igual; el catálogo online es una sección aparte debajo.
- **Integridad:** cada clip lleva su SHA-256 en el índice y se rechaza si no coincide; las rutas del índice solo pueden apuntar a `clips/*.ogg` del repositorio de catálogos.
- **Privacidad:** solo se contacta con `raw.githubusercontent.com`, sin datos del usuario, y solo al abrir una pestaña de sonidos con el catálogo activado (se puede desactivar). Sin conexión se usa la última copia guardada del índice.
- **Licencias:** solo grabaciones de dominio público o CC0 (se comprueba en la ficha de Wikimedia Commons); cada entrada guarda su licencia y enlace de origen, y el README del catálogo los lista.