# Android nativo (Kotlin + Compose), núcleo sin dependencias de Android

Toda la funcionalidad es API del sistema (RingtoneManager, Settings.System, MediaStore), así que un framework multiplataforma no aporta valor ahora. La lógica (perfiles, restaurar, reserva) vive en un módulo Kotlin puro `core` detrás de puertos, para poder ampliar a otras plataformas y probarla en JVM sin emulador. iPhone no puede cambiar tonos del sistema, por lo que su fase será un exportador `.m4r`, no un port de esta app.
