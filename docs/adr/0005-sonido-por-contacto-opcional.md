# Sonido por contacto: opcional, desactivado de serie y sin copiar datos

Dar a un contacto su propio tono exige leer y modificar los contactos del teléfono (`READ_CONTACTS` y `WRITE_CONTACTS`), un permiso sensible. Por eso la función viene **desactivada**; al activarla se explica en pocas palabras qué hace falta y que no se guarda ni se envía nada, y solo entonces se piden los permisos. Si se rechazan, la función queda apagada.

- **Sin copias:** los contactos no se guardan en la app ni salen del teléfono. La lista de contactos con tono propio se lee del sistema (`ContactsContract`) cada vez que se muestra, y el único dato que se escribe es el campo «tono» (`CUSTOM_RINGTONE`) del propio contacto.
- **Selector del sistema:** el contacto se elige con el selector de contactos de Android, no con una lista propia.
- **Alcance:** el campo de tono de Android es solo para llamadas; los sonidos de mensajes por contacto dependen de cada app y no se pueden cambiar desde aquí.
- **Reversible:** «Quitar» devuelve al contacto al tono predeterminado.