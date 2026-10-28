# QuietRingtones

App que cambia los sonidos predeterminados del sistema (tono, notificación, alarma) y los agrupa en perfiles reversibles.

## Language

**Tipo de sonido**:
Una de las tres categorías de sonido predeterminado del sistema: Tono de llamada, Notificación o Alarma.
_Avoid_: Categoría, canal

**Sonido**:
Un audio concreto elegible para un Tipo de sonido, con un nombre visible y una referencia estable al sistema.
_Avoid_: Tono (ambiguo), ringtone, pista

**Sonido activo**:
El Sonido que el sistema usa ahora como predeterminado de un Tipo de sonido. Puede ser "silencio" (ninguno).
_Avoid_: Sonido actual, sonido por defecto

**Silencio**:
Estado deliberado de un Tipo de sonido sin ningún Sonido asignado; distinto de "nunca capturado".
_Avoid_: Vacío, nulo, sin valor

**Aplicar**:
Fijar un Sonido como Sonido activo de un Tipo de sonido en el sistema.
_Avoid_: Guardar, establecer, setear

**Importar**:
Incorporar un archivo de audio del usuario al sistema para que sea un Sonido de un Tipo de sonido.
_Avoid_: Subir, añadir

**Perfil**:
Conjunto con nombre de tres elecciones (una por Tipo de sonido) que se aplica de una vez.
_Avoid_: Modo, preset, tema

**Estado original**:
Los Sonidos activos (incluido Silencio) capturados antes del primer cambio hecho por la app, por Tipo de sonido.
_Avoid_: Backup, copia

**Restaurar**:
Volver a Aplicar el Estado original de cada Tipo de sonido.
_Avoid_: Deshacer, resetear

**Sonido de reserva**:
El Sonido que se usa en lugar de uno de un Perfil que ya no existe, para no dejar el sistema sin sonido por accidente.
_Avoid_: Fallback, sustituto

**Permiso de ajustes**:
Autorización especial que el usuario concede a mano para que la app pueda Aplicar sonidos.
_Avoid_: Permiso de escritura
