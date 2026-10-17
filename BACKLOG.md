# Issues preparados para GitHub

Cada apartado es un issue independiente. `P0` entra en la alpha de dos días; `P1` se inicia después. La entrega de un issue exige cumplir todos sus criterios y documentar los dispositivos y fuentes con que se probó. La alpha requiere además superar [la revisión de seguridad y privacidad](SECURITY.md).

## P0 · 01 · Base Android TV y navegación con mando

**Journey:** J2. **Dependencias:** ninguna. **Bloque:** día 1, mañana.

- [ ] Proyecto Kotlin/Compose para TV, compilación reproducible y comprobación automática de build.
- [ ] Inicio, canales, guía, biblioteca y ajustes navegables con las cuatro direcciones, seleccionar y volver.
- [ ] Foco siempre visible; menú lateral accesible desde la izquierda y ocultable.
- [ ] Marca «tuteuve» solo en el menú lateral, con versión y crédito de desarrollo configurable.
- [ ] Estructura modular, formato consistente y análisis estático integrados en la compilación; comentarios para decisiones no evidentes.

## P0 · 02 · Fuentes M3U y varias listas

**Journey:** J1. **Depende de:** 01. **Bloque:** día 1, tarde.

- [ ] Añadir, nombrar, actualizar y retirar varias fuentes M3U; sin listas incluidas en la app.
- [ ] Importación fuera del hilo de interfaz, con estados de carga, vacío y error.
- [ ] Se conservan grupos, nombres y metadatos disponibles de cada canal.
- [ ] Las URLs privadas no aparecen en logs ni informes de error.
- [ ] Antes de añadir la primera fuente, mostrar el resumen y enlace al [descargo de responsabilidad](DISCLAIMER.md), también accesible después desde Ajustes.

## P0 · 03 · Catálogo, carpetas y búsqueda

**Journey:** J2. **Depende de:** 02. **Bloque:** día 1, tarde.

- [ ] Carpetas con jerarquía, volver y número de canales; nombre largo con desplazamiento legible.
- [ ] Búsqueda, cuadrícula y lista; foco persistente al volver del reproductor.
- [ ] Seleccionar canal abre vista previa y mueve el foco a «Ver en pantalla completa»; una segunda selección la abre.
- [ ] Una lista de prueba de 20.000 canales se recorre sin construir todas las celdas a la vez.

## P0 · 04 · Reproductor y vista previa Media3

**Journey:** J4. **Depende de:** 01 y 02. **Bloque:** día 1, tarde.

- [ ] Vídeo a pantalla completa con controles superpuestos, ocultos tras cinco segundos o al pulsar volver.
- [ ] Pausa, retroceso y vuelta al directo solo cuando la señal ofrece esa capacidad.
- [ ] Selección automática de la mejor pista reproducible, con cambio manual entre pistas disponibles.
- [ ] Muestra resolución y FPS enteros cuando se conocen; explica fallos de red, formato o códec.
- [ ] La vista previa muestra vídeo real solo cuando la fuente es reproducible.

## P0 · 05 · XMLTV y guía por carpetas

**Journey:** J3. **Depende de:** 02 y 03. **Bloque:** día 2, mañana.

- [ ] Vincular XMLTV aportado por el usuario y asociar programas a canales.
- [ ] Filtrar la guía por la misma jerarquía de carpetas que el catálogo.
- [ ] Posición y ancho del programa obedecen a sus horas reales; no se fuerza una cuadrícula de bloques iguales.
- [ ] La ficha indica carpeta del canal y distingue directo, futuro y terminado.
- [ ] Si no hay EPG se presenta ausencia de datos, sin inventar títulos.

## P0 · 06 · Favoritos, listas y recientes

**Journey:** J2. **Depende de:** 03. **Bloque:** día 2, mañana.

- [ ] Añadir y quitar favoritos, crear listas propias y consultar recientes.
- [ ] Los cambios persisten tras cerrar y reiniciar la aplicación.
- [ ] La portada rota los destacados con puntos de posición sin alterar el scroll ni el foco del usuario.

## P0 · 07 · Adaptador Xtream y credenciales

**Journey:** J1. **Depende de:** 02 y 04. **Bloque:** día 2, mañana. **Condición:** fuente de prueba autorizada.

- [ ] Añadir una fuente Xtream sin mostrar contraseña o token una vez guardados.
- [ ] Importar categorías y canales, y reproducir al menos una señal de la fuente de prueba.
- [ ] Proteger credenciales mediante una clave respaldada por Android Keystore; excluirlas de logs, copias y diagnósticos.
- [ ] Registrar con claridad fallos de conexión o formato sin exponer datos sensibles.

## P0 · 08 · Validación y APK alpha

**Journeys:** J1–J4. **Depende de:** 01–07, con la excepción Xtream documentada en el plan. **Bloque:** día 2, tarde.

- [ ] Pruebas en Google TV o emulador con mando, lista grande y conexión lenta.
- [ ] Compilación de APK instalable e instrucciones de compilación e instalación.
- [ ] Pruebas funcionales y de regresión, revisión de código y cumplimiento de la revisión de seguridad (#13).
- [ ] Publicar resultados, fuentes de prueba permitidas y limitaciones conocidas.
- [ ] Revisar que APK, repositorio, registros y capturas no contengan fuentes privadas, tokens ni credenciales.
- [ ] Comprobar que el descargo se entiende y puede consultarse desde Ajustes y desde el alta de fuentes.

## P1 · 09 · Prueba de viabilidad de grabación en primer plano

**Journey:** J5. **Depende de:** 04.

- [ ] Grabar desde el momento de pulsar «Grabar» mientras el contenido sigue abierto.
- [ ] Al detener o salir de la app, cerrar un archivo que se pueda reproducir.
- [ ] Definir formatos admitidos, límites de DRM, espacio, cortes de red y cambios de calidad.
- [ ] Comunicar en la interfaz que la grabación se detiene al abrir otra app.

## P1 · 10 · Biblioteca de grabaciones

**Journey:** J5. **Depende de:** 09.

- [ ] Listar grabaciones válidas con duración, tamaño y fecha.
- [ ] Reproducir archivos con controles de vídeo bajo demanda, sin controles de directo.
- [ ] Informar y permitir retirar un archivo incompleto o ilegible.

## P1 · 11 · Avisos de programas

**Journey:** J6. **Depende de:** 05.

- [ ] Crear y cancelar avisos para programas futuros y abrir el programa desde el aviso.
- [ ] En un programa ya iniciado, «Avisar» está deshabilitado, «Grabar» actúa solo si 09 está disponible y «Ver programa» abre el directo.
- [ ] Verificar comportamiento tras reiniciar la app y el dispositivo.

## P1 · 12 · Almacenamiento y grabación programada

**Journey:** J7. **Depende de:** 09–11. **Se estima por separado.**

- [ ] Elegir y comprobar destino local/USB o NAS con permisos y espacio disponibles.
- [ ] Diseñar la ejecución real de grabaciones programadas y su comportamiento fuera de la app.
- [ ] Informar de fallos de red, permisos o almacenamiento y recuperar archivos parciales cuando sea posible.
- [ ] No mostrar «Programar grabación» hasta que funcione y se haya probado en TV.

## P0 · 13 · Revisión de seguridad y privacidad

**Journeys:** J1–J4. **Depende de:** 01–07. **Bloquea:** 08 y publicación de la APK.

- [ ] Modelo de amenazas revisado para listas/guías maliciosas, otras apps de la TV, red, almacenamiento y dependencias.
- [ ] Manifest y APK final auditados: permisos mínimos, componentes internos no exportados y ausencia de SDK de analítica/telemetría.
- [ ] Pruebas con M3U/XMLTV hostiles, URLs internas/redirecciones, intents externos y acceso a archivos privados; sin lectura de listas ni ejecución de acciones no autorizadas.
- [ ] Captura de tráfico en arranque, reposo, navegación y reproducción; solo conexiones justificadas por fuentes configuradas y sus contenidos.
- [ ] Comprobación de logs, backups, archivos temporales y repositorio sin listas, tokens, contraseñas ni historial privado.
- [ ] Hallazgos corregidos y pruebas repetidas; resultados y límites publicados sin datos sensibles.
