# Seguridad y privacidad

## Compromisos del producto

- Sin analítica, telemetría, anuncios, identificadores publicitarios, informes automáticos de fallos ni SDK externos que recopilen uso.
- Sin cuenta, backend ni servicio propio de tuteuve. La app solo se conecta a las fuentes y guías configuradas por la persona usuaria, y a los destinos necesarios para reproducir sus señales.
- Las listas, favoritos, historial y credenciales se guardan en el dispositivo. No se exportan ni se incluyen en copias automáticas de Android. Las credenciales se protegen con una clave respaldada por Android Keystore.
- Permisos mínimos. Los componentes Android internos no se exportan. Cualquier entrada desde otra app se valida y se limita a la función imprescindible.
- Una app no puede ocultar al proveedor IPTV ni al operador de red las conexiones necesarias para obtener una señal. La promesa es que **tuteuve no añade vigilancia ni envía datos de uso a terceros**; no garantiza anonimato frente al proveedor, la red o el sistema operativo.

## Modelo de amenazas de la primera entrega

1. **Lista o guía maliciosa:** M3U/XMLTV con tamaños extremos, rutas extrañas, URLs internas, redirecciones, texto inesperado o metadatos que intentan bloquear el parser o lanzar conexiones no deseadas.
2. **Otra app del televisor:** intents, deep links, receivers, servicios o proveedores que tratan de leer listas, credenciales o archivos internos, o de ejecutar acciones sin autorización.
3. **Fuga accidental:** logs, capturas, informes de fallos, backup, archivos temporales o dependencias que exponen URLs con tokens, contraseñas o historial.
4. **Red y transporte:** credenciales por HTTP, certificados no válidos, redirecciones entre hosts, cambios de DNS y tráfico hacia servicios ajenos a la fuente elegida.
5. **Cadena de suministro:** dependencias o tareas de compilación con permisos excesivos, vulnerabilidades conocidas o comportamiento de red no documentado.

## Controles exigidos en la alpha

- Revisar manifest y permisos: no pedir acceso global al almacenamiento, accesibilidad, superposición, micrófono, cámara ni permisos innecesarios. Actividades, servicios, receivers y proveedores internos con `exported=false`; la entrada de lanzamiento requerida por Android no recibe datos sensibles y valida todo dato externo.
- Mantener datos en almacenamiento privado de la app. Desactivar la copia automática de fuentes, credenciales, favoritos e historial; no crear proveedores o permisos de lectura amplios.
- Evitar URLs, cabeceras, tokens, contraseñas y nombres de listas en logs, excepciones, notificaciones y pantallas de error. No registrar historial de reproducción. Las pruebas usan datos sintéticos.
- Exigir TLS válido para credenciales. Una fuente que requiera usuario y contraseña por HTTP se rechaza en la alpha. Revisar y limitar redirecciones y peticiones derivadas de listas/guías; documentar cualquier excepción de transporte necesaria para contenidos sin credenciales.
- Validar tamaños, formatos y campos de M3U/XMLTV; procesar sin ejecutar código o comandos; limitar memoria, tiempo, profundidad y número de entradas; tolerar errores sin bloquear la interfaz.
- Inventariar dependencias, fijar versiones, revisar licencias y vulnerabilidades, y generar una compilación reproducible. No integrar analítica ni crash reporting automático.

## Pruebas de seguridad antes de publicar una APK

- Pruebas con M3U/XMLTV manipulados: entradas enormes, caracteres y codificaciones inesperadas, URLs a `localhost` y red local, redirecciones, XML malformado y valores de metadatos hostiles. Confirmar que se rechazan o aíslan sin fuga ni bloqueo.
- Intentar desde otra app de prueba abrir componentes, enviar intents y leer proveedores/archivos de tuteuve. Confirmar denegación de acceso a listas y credenciales.
- Inspeccionar el APK final: manifest, permisos, componentes exportados, dependencias y configuración de red.
- Capturar el tráfico de la app en un entorno de prueba durante instalación, reposo, navegación y reproducción. Solo deben aparecer conexiones explicables por las fuentes configuradas y su contenido; ninguna al iniciar la app sin fuente.
- Buscar secretos y URLs privadas en repositorio, APK, logs, backups, informes, capturas y archivos temporales. Verificar que borrar una fuente retira sus credenciales.
- Ejecutar comprobaciones automáticas de dependencias y análisis estático, además de una revisión manual del flujo de datos. Documentar hallazgos y repetir las pruebas tras corregirlos.

La alpha no se publica si alguna prueba detecta fuga de datos, un componente accesible sin necesidad, una dependencia que envía telemetría o un permiso excesivo. El plazo de dos días es un objetivo de trabajo, no una excepción a este criterio.

## Comunicar vulnerabilidades

No publiques una vulnerabilidad explotable, una lista privada ni credenciales en un issue público. Hasta configurar un canal privado de avisos de seguridad en GitHub, abre un issue público sin detalles de explotación para solicitar un canal de contacto seguro. La coordinación y la corrección se documentarán públicamente cuando deje de poner en riesgo a usuarios.
