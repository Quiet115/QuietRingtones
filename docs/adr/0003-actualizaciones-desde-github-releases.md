# Actualizaciones desde GitHub Releases, con instalación confirmada por el usuario

Esto modifica el ADR 0002 (datos solo locales): la app contacta con GitHub, y solo con GitHub, para buscar una versión nueva (`api.github.com`, sin cuenta ni datos del usuario) y descargar su APK. Las actualizaciones automáticas vienen activadas por defecto (buscar al abrir como mucho una vez al día, descargar solo con Wi-Fi) y se pueden desactivar; hay además un botón «Buscar actualizaciones». Las versiones antiguas siguen publicadas en la página de releases.

- **La instalación siempre la confirma el usuario.** Android no permite instalar en silencio una app sideloaded; se abre el instalador del sistema, que además comprueba que el APK esté firmado con la misma clave que la app instalada.
- **La clave de firma es lo que hace posibles las actualizaciones.** Si se pierde, nadie podrá actualizar: habría que desinstalar y reinstalar. Vive fuera del repositorio (`keystore.properties` y el `.jks` están en `.gitignore`) y debe tener copia de seguridad.
- **Cuidado con las builds de depuración:** llevan otra firma, así que no se pueden actualizar con un release; hay que desinstalar antes (se pierden los perfiles y el Estado original guardados en el dispositivo).
- **Solo se aceptan APK de este repositorio** (`github.com/Quiet115/QuietRingtones/releases/download/…`) y se verifica su SHA-256 cuando GitHub lo da (o, si no, el tamaño).
- **Google Play:** las apps de Play no deben actualizarse por su cuenta ni pedir `REQUEST_INSTALL_PACKAGES`. Si algún día se publica en Play, hay que quitar el actualizador (por ejemplo, con una variante de compilación aparte).
