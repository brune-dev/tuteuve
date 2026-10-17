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

1. La versión interna incluye navegación con mando, catálogo, búsqueda, vista previa, reproducción Media3 y fuentes M3U HTTPS con alta, actualización manual y retirada confirmada. También permite favoritos, listas propias y recientes persistentes; la portada rota canales favoritos sin mover el foco. La guía admite una URL XMLTV HTTPS o una importación manual desde un servidor Xtream HTTPS, filtra por las mismas carpetas, muestra hoy y mañana, y presenta la duración real de cada programa. La carga de archivos `.m3u` está suspendida. Las direcciones de M3U y XMLTV y las credenciales usadas para importar la guía Xtream no se conservan; para actualizar hay que introducirlas de nuevo. Los canales y la guía quedan en almacenamiento privado excluido de copias de seguridad. La biblioteca guarda solo identificadores de canal. Las solicitudes de lista, guía, vista previa y vídeo comparten el bloqueo de destinos locales y redirecciones.
2. La rama de desarrollo se comprueba con compilación, pruebas unitarias y Android Lint en el PR. En el emulador TV se importaron una lista y una guía sintéticas por URL pública; se comprobó el cambio de día, la ficha de programa y la persistencia de la guía sin guardar su URL. La vía Xtream XMLTV requiere validación con una fuente autorizada introducida directamente en el dispositivo. Pendiente: validar guía y reproducción con fuentes autorizadas, completar la importación de canales Xtream y su protección de credenciales con Keystore, probar listas grandes y ejecutar la [auditoría de seguridad](SECURITY.md).
3. Entregar una APK alpha para instalación manual solo tras superar las pruebas de calidad y seguridad.

### Compilar la rama de desarrollo

Se necesitan JDK 17 y Android SDK 36. El proyecto incluye `gradlew` y `gradlew.bat`, que descargan Gradle 8.13 con verificación SHA-256; no hay que instalar Gradle manualmente. En Android Studio, selecciona JDK 17 como **Gradle JDK**. En PowerShell, si `java -version` aún muestra Java 8, establece JDK 17 para esa sesión:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

La APK local queda en `app/build/outputs/apk/debug/app-debug.apk`. La compilación automática de GitHub ejecuta esas mismas comprobaciones en cada pull request. `minSdk` es 23 (Android TV 6); la compatibilidad efectiva se verificará en un dispositivo Android TV 14 y en emuladores de versiones anteriores.

## Comunidad

Se aceptan propuestas, informes de errores y contribuciones. Consulta [CONTRIBUTING.md](CONTRIBUTING.md). Para probar la app, utiliza únicamente fuentes y contenidos que tengas derecho a reproducir y, cuando corresponda, grabar. No publiques URLs privadas, contraseñas, tokens ni listas de terceros en los issues.

## Licencia

El código de tuteuve se distribuye bajo [MIT](LICENSE). Las dependencias y los recursos de terceros conservan sus respectivas licencias; se documentarán en el repositorio antes de incluirlos.
