# Plan de implantación de tuteuve

## Alcance y fecha objetivo

La primera meta es una **alpha Android TV / Google TV instalable en 1–2 días de trabajo concentrado**, contando desde que empiece el desarrollo. Es una estimación de ejecución, sujeta a disponer de un Google TV o emulador, Android SDK y al menos una fuente de prueba autorizada. Publicar en Google Play y garantizar compatibilidad con todos los proveedores no forma parte de esa meta.

La alpha debe cubrir los journeys J1–J4. J5–J7 quedan planificados y se implementarán después de validar el flujo principal. Esta división evita presentar como grabación real o programación automática algo que la app todavía no puede cumplir.

## Journeys

| Journey | Experiencia de la persona usuaria | Entrega |
| --- | --- | --- |
| J1 · Incorporar fuentes | Nombra una fuente, añade M3U o credenciales Xtream, consulta el estado y actualiza manualmente. Puede tener varias fuentes. | Alpha, con Xtream sujeto a una fuente de prueba autorizada. |
| J2 · Descubrir canales | Entra en carpetas, ve su número de canales, alterna cuadrícula/lista, busca, marca favoritos y crea listas. El mando mantiene el foco visible. | Alpha. |
| J3 · Saber qué emiten | Filtra la guía por carpeta, ve el programa actual y su duración restante, abre la ficha y consulta hoy/mañana. | Alpha si existe XMLTV o EPG del proveedor; sin datos, se informa claramente. |
| J4 · Ver contenido | Selecciona un canal, ve su vista previa y pasa a pantalla completa. La calidad automática elige entre pistas reproducibles; se muestran resolución y FPS reales si se conocen. | Alpha. |
| J5 · Grabar y volver a ver | Inicia la grabación del contenido abierto, la detiene y la reproduce desde la biblioteca. Al salir de la app o abrir otra, se detiene. | Fase siguiente, tras validar formato, almacenamiento y cierre limpio. |
| J6 · No perder un programa | Activa un aviso del sistema antes de un programa futuro; no permite avisos para emisiones ya iniciadas. | Fase siguiente. |
| J7 · Almacenamiento avanzado | Elige almacenamiento local/USB o NAS, revisa espacio y errores; programa grabaciones solo si se diseña un servicio que las ejecute de verdad. | Fase posterior. |

## Arquitectura inicial

- **Kotlin + Compose para TV** para pantallas, foco de mando y listas virtualizadas.
- **Media3 ExoPlayer** para reproducción HLS y otros formatos compatibles con dispositivo y códec. Calidad «automática» significa la mejor pista reproducible según red y equipo; no promete forzar 4K.
- **Módulos de entrada** independientes para M3U, Xtream y XMLTV. La importación se hace fuera del hilo de interfaz y expone errores comprensibles.
- **Persistencia local** de fuentes, carpetas, favoritos, listas y recientes. Las credenciales se protegen con una clave respaldada por Android Keystore y no aparecen en logs, informes ni exportaciones.
- **Sin backend propio** en la alpha. El usuario aporta sus fuentes y la app se comunica directamente con ellas.

## Plan de 2 días

| Bloque | Trabajo | Salida comprobable |
| --- | --- | --- |
| Día 1, mañana | Proyecto Android, navegación TV, tema tuteuve, CI de compilación, datos de prueba propios. | APK de arranque, foco visible y navegación con mando. |
| Día 1, tarde | Alta de fuentes M3U, parser de carpetas, catálogo virtualizado, Media3 y pantalla completa. | Importar y reproducir una fuente autorizada; errores y carga visibles. |
| Día 2, mañana | Adaptador Xtream, XMLTV/EPG, guía filtrada, favoritos, listas y recientes persistentes. | Recorrido J1→J4 con datos reales de prueba. |
| Día 2, tarde | Pruebas en Google TV/emulador, lista grande, red lenta, calidad/pistas, privacidad, README y APK alpha. | Release alpha con limitaciones conocidas y capturas/pruebas documentadas. |

**Regla de corte:** si Xtream o XMLTV dependen de un proveedor no disponible, se entrega una alpha M3U funcional y se deja el adaptador correspondiente abierto en su issue. No se sustituye con datos simulados en la APK. La grabación real no se declara terminada sin archivo reproducible y prueba de parada al cambiar de app.

## Issues y dependencias

Los issues se crean en GitHub con criterios de aceptación verificables. `P0` bloquea la alpha; `P1` prepara la siguiente versión.

| Prioridad | Issue | Journey | Depende de |
| --- | --- | --- | --- |
| P0 | [#1 Base Android TV, CI y navegación con mando](https://github.com/brune-dev/tuteuve/issues/1) | J2 | — |
| P0 | [#2 Fuentes M3U con nombre y múltiples listas](https://github.com/brune-dev/tuteuve/issues/2) | J1 | #1 |
| P0 | [#3 Catálogo por carpetas, búsqueda, cuadrícula/lista y foco](https://github.com/brune-dev/tuteuve/issues/3) | J2 | #2 |
| P0 | [#4 Reproductor Media3, vista previa y calidad automática](https://github.com/brune-dev/tuteuve/issues/4) | J4 | #1, #2 |
| P0 | [#5 EPG XMLTV y guía por carpetas](https://github.com/brune-dev/tuteuve/issues/5) | J3 | #2, #3 |
| P0 | [#6 Favoritos, listas propias y recientes persistentes](https://github.com/brune-dev/tuteuve/issues/6) | J2 | #3 |
| P0 | [#7 Adaptador Xtream y protección de credenciales](https://github.com/brune-dev/tuteuve/issues/7) | J1 | #2, #4 |
| P0 | [#8 Pruebas en TV, privacidad y APK alpha](https://github.com/brune-dev/tuteuve/issues/8) | J1–J4 | #1–#7, con condición Xtream |
| P1 | [#9 Grabación en primer plano: prueba de viabilidad](https://github.com/brune-dev/tuteuve/issues/9) | J5 | #4 |
| P1 | [#10 Biblioteca y reproducción de archivos grabados](https://github.com/brune-dev/tuteuve/issues/10) | J5 | #9 |
| P1 | [#11 Avisos de programa](https://github.com/brune-dev/tuteuve/issues/11) | J6 | #5 |
| P1 | [#12 NAS/USB y programación real de grabaciones](https://github.com/brune-dev/tuteuve/issues/12) | J7 | #9–#11 |

## Criterios de salida de la alpha

- Se instala y abre en un Google TV o emulador, sin ratón ni teclado.
- M3U autorizada: importar, actualizar, recorrer carpetas y reproducir; Xtream se entrega si se valida con una fuente de prueba real.
- No se bloquea la interfaz al cargar una lista grande; objetivo inicial: 20.000 canales sin renderizar todas las filas a la vez.
- La guía distingue entre datos reales, datos ausentes y errores. No inventa programación.
- Favoritos y listas sobreviven al cierre y reinicio; las credenciales no aparecen en logs ni en el repositorio.
- El reproductor informa de fallo de red, formato o pista no admitida y puede volver al catálogo con el mando.
- Se publica el código, la licencia, instrucciones de compilación, una APK alpha y limitaciones conocidas.

## Después de la alpha

La grabación de directos necesita resolver contenedor, variantes HLS/TS, cortes de red, DRM, cierre del archivo y permisos de almacenamiento. Los avisos requieren integración con el sistema. NAS y grabación programada añaden permisos y ejecución fuera de la pantalla activa; se abordarán únicamente como funcionalidades reales y probadas. La aplicación no incluirá listas, canales ni credenciales de terceros.
