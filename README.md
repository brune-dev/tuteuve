# tuteuve

Reproductor IPTV libre, local y pensado para Google TV. El objetivo es ofrecer navegación con mando, guía diaria, listas, favoritos y reproducción de calidad sin suscripción. **El repositorio está en fase de planificación; todavía no contiene una APK funcional.**

## Visión

- Trae tus propias fuentes M3U o Xtream y tu guía XMLTV. La app no incluye canales, listas, credenciales ni contenido audiovisual.
- Funciona de forma local: no requiere cuenta de tuteuve, telemetría ni servidor propio.
- Muestra la mejor variante que el dispositivo y la conexión puedan reproducir, con información real de resolución y FPS cuando la señal la ofrezca.
- Se diseña primero para el mando de Google TV, con foco visible y respuesta rápida incluso con listas grandes.

El [prototipo visual aprobado](https://iptv-sala-prototipo.xcorreogenerico.chatgpt.site/) documenta los flujos de interfaz. Sus emisiones, grabaciones y guías son simuladas; la app Android será una implementación nueva.

## Plan

[Journeys y calendario](ROADMAP.md) · [12 issues con criterios de aceptación](BACKLOG.md). La meta de **1–2 días de desarrollo** es una *alpha instalable*, no una versión de producción con todas las funciones. Grabación real, avisos, NAS y programación de grabaciones tienen issues separados y requieren pruebas adicionales en dispositivos y fuentes reales.

## Estado del proyecto

1. Abrir los issues de la alpha y los de la fase siguiente.
2. Crear el proyecto Android con Kotlin, Compose para TV y Media3.
3. Entregar una APK alpha para instalación manual y publicar resultados de pruebas.

## Comunidad

Se aceptan propuestas, informes de errores y contribuciones. Consulta [CONTRIBUTING.md](CONTRIBUTING.md). Para probar la app, utiliza únicamente fuentes y contenidos que tengas derecho a reproducir y, cuando corresponda, grabar. No publiques URLs privadas, contraseñas, tokens ni listas de terceros en los issues.

## Licencia

El código de tuteuve se distribuye bajo [MIT](LICENSE). Las dependencias y los recursos de terceros conservan sus respectivas licencias; se documentarán en el repositorio antes de incluirlos.
