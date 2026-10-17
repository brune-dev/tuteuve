# tuteuve

Reproductor IPTV libre, local y pensado para Google TV. El objetivo es ofrecer navegación con mando, guía diaria, listas, favoritos y reproducción de calidad sin suscripción. **El desarrollo Android ha comenzado; todavía no hay una APK alpha validada.**

## Visión

- Trae tus propias fuentes M3U o Xtream y tu guía XMLTV. La app no incluye canales, listas, credenciales ni contenido audiovisual.
- Funciona de forma local: no requiere cuenta de tuteuve, telemetría ni servidor propio.
- Muestra la mejor variante que el dispositivo y la conexión puedan reproducir, con información real de resolución y FPS cuando la señal la ofrezca.
- Se diseña primero para el mando de Google TV, con foco visible y respuesta rápida incluso con listas grandes.
- No integra analítica, telemetría ni anuncios. Las fuentes y credenciales permanecen en el dispositivo. Consulta los [controles y pruebas de seguridad](SECURITY.md).

**Uso de fuentes:** tuteuve es una herramienta. No proporciona ni valida canales, listas o derechos de contenido. Cada persona debe asegurarse de que puede usar las fuentes que añade y las funciones que aplica. Consulta el [descargo de responsabilidad](DISCLAIMER.md).

El [prototipo visual aprobado](https://iptv-sala-prototipo.xcorreogenerico.chatgpt.site/) documenta los flujos de interfaz. Sus emisiones, grabaciones y guías son simuladas; la app Android será una implementación nueva.

## Plan

[Journeys y calendario](ROADMAP.md) · [13 issues con criterios de aceptación](BACKLOG.md). La meta de **1–2 días de desarrollo** es una *alpha instalable*, sujeta a superar las pruebas de calidad y seguridad; no es una versión de producción con todas las funciones. Grabación real, avisos, NAS y programación de grabaciones tienen issues separados y requieren pruebas adicionales en dispositivos y fuentes reales.

## Estado del proyecto

1. Base Android TV en desarrollo: navegación, importación local M3U y reproducción Media3 inicial. La importación acepta por ahora emisiones HTTPS; las entradas no compatibles se omiten y se cuentan.
2. Pendiente: comprobar la compilación, completar catálogo, vista previa, EPG y fuentes Xtream, y ejecutar la [auditoría de seguridad](SECURITY.md).
3. Entregar una APK alpha para instalación manual solo tras superar las pruebas de calidad y seguridad.

### Compilar la rama de desarrollo

Se necesitan JDK 17, Android SDK 36 y Gradle 8.13, o Android Studio con versiones compatibles. En la raíz del repositorio: `gradle :app:testDebugUnitTest :app:assembleDebug`. La compilación automática de GitHub ejecuta ambos pasos en cada pull request. `minSdk` es 23 (Android TV 6); la compatibilidad efectiva se verificará en un dispositivo Android TV 14 y en emuladores de versiones anteriores.

## Comunidad

Se aceptan propuestas, informes de errores y contribuciones. Consulta [CONTRIBUTING.md](CONTRIBUTING.md). Para probar la app, utiliza únicamente fuentes y contenidos que tengas derecho a reproducir y, cuando corresponda, grabar. No publiques URLs privadas, contraseñas, tokens ni listas de terceros en los issues.

## Licencia

El código de tuteuve se distribuye bajo [MIT](LICENSE). Las dependencias y los recursos de terceros conservan sus respectivas licencias; se documentarán en el repositorio antes de incluirlos.
