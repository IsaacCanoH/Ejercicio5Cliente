# Optimización de recursos del cliente

Fecha: 21 de noviembre de 2026. Proyecto: `Ejercicio5Cliente`, rama `feature/circuito-devops`.

## Tamaño medido

| Elemento | Antes (MiB) | Después (MiB) | Reducción |
|---|---:|---:|---:|
| Recursos estáticos | 421.79 | 93.03 | 77.9% |
| JAR ejecutable | 431.76 | 107.46 | 75.1% |

La medición inicial de recursos incluye el MP4 de acceso creado previamente, además del GIF original; el JAR inicial aún contenía el GIF. Estas cifras corresponden al contenido actual del cliente y a los JAR construidos, no al historial de Git ni al tamaño de una imagen Docker.

## Cambios

- Fondo de acceso y registro: GIF de 76.70 MiB sustituido por el MP4 existente de 2.38 MiB. Tiene imagen de espera WebP; con movimiento reducido no se solicita el video. La reproducción se pausa cuando se oculta la pestaña.
- Diez fondos GIF convertidos a WebP. Se verificaron dimensiones y duración de las animaciones; `animacion5.gif` contenía 150 cuadros idénticos y se simplificó a una imagen estática sin cambio visual. El formato sigue funcionando en elementos `img` y en los fondos de proyectos.
- PNG utilizados convertidos a WebP sin pérdida, comparando los píxeles visibles. La fotografía JPG utiliza WebP con calidad 85; se conserva el icono ICO.
- Selector: diez miniaturas animadas de 100 × 60 px, solicitadas al abrir el diálogo. Al elegir una miniatura se guarda la ruta del fondo completo.
- Manual y tutorial: H.264 con CRF 23, píxeles `yuv420p`, cabecera preparada para reproducción progresiva. Se conservaron resolución, duración y pista de audio; el audio del tutorial se codificó a AAC de 128 kbit/s. Los controles permiten avanzar; no se reproducen ni repiten automáticamente. `object-fit: contain` evita recortar la grabación.
- Rutas antiguas de archivos convertidos y duplicados: redirección HTTP 308 mediante `legacy-media.properties`. El proyecto existente que conserva `/Images/animacion1.gif` funciona sin modificar la base de datos.
- Se corrigieron las selecciones antiguas sin extensión y la referencia CSS `/css/fondo.jpg`, que ya devolvía 404 antes del cambio.
- Se retiró `pplmproyecto.js` y los manejadores de registro de la página principal: únicamente apuntaban a un formulario comentado y provocaban tres errores JavaScript en la revisión inicial. Los enlaces activos de ayuda, contacto e información se comprobaron en el navegador.

## Archivos de mayor tamaño

| Recurso | Antes (MiB) | Después (MiB) |
|---|---:|---:|
| `Images/animacion9.gif` | 28.18 | 5.35 |
| `Images/animacion10.gif` | 12.69 | 1.14 |
| `Images/manual.mp4` | 81.78 | 46.75 |
| `Images/tutorial.mp4` | 50.20 | 25.20 |

## Recursos retirados

Se contrastaron referencias en código, plantillas, estilos y pruebas, y se consultaron las rutas de imagen persistidas: un fondo `/Images/animacion1.gif`; las imágenes de usuario son colores hexadecimales. No se eliminaron datos de la aplicación.

Duplicados idénticos a los originales de `Images/`:

- `animacion6.gif`
- `animacion7.gif`
- `animacion8.gif`

Sin referencias encontradas:

- `Images/amarillo.png`
- `Images/fondo.gif`
- `Images/fondo.png`
- `Images/fondo1.png`
- `Images/fondoInicio.png`
- `Images/gris.gif`
- `Images/grisClaro.png`
- `Images/grisMedio.png`
- `Images/grisOscuro.png`
- `Images/hola.gif`
- `Images/rojoCoral.png`
- `Images/verde.png`
- `Images/verdePasto.png`
- `Images/violeta.png`
- `js/pplmproyecto.js`

## Validación

- `mvnw.cmd clean verify`: 53 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Pruebas nuevas: todas las rutas de compatibilidad apuntan a recursos empaquetados y los videos responden a solicitudes de rangos HTTP.
- Chrome: inicio, acceso, registro, lista de proyectos, selector de fondos, manual y tutorial; acceso en vista móvil y con movimiento reducido. Se verificaron la carga diferida, selección de fondo completo, redirección del fondo persistido y búsqueda por rangos del video. Sin errores JavaScript ni respuestas locales 4xx/5xx durante estas comprobaciones.
- Comprobación de imágenes y videos con Pillow y FFprobe; revisión visual de capturas y de texto en las grabaciones.

## Respaldo y reproducción

Los originales se conservaron fuera del empaquetado en:

`C:\Users\hisac\Documents\ChatGPT\Unidad 3 2\optimizacion-recursos-20261121\respaldo-cliente`

Inventario, tamaños, parámetros de conversión, comprobaciones y capturas están en la carpeta superior del respaldo. Las herramientas utilizadas fueron FFmpeg/FFprobe y los codificadores oficiales `cwebp`/`gif2webp` instalados previamente. No hace falta instalarlas para ejecutar el cliente ya compilado.

Para volver a construir desde PowerShell en la raíz del repositorio:

```powershell
.\mvnw.cmd clean verify
```

La optimización no reduce los archivos grandes que ya existen en commits anteriores de Git. No se reescribió el historial. El siguiente paso del circuito es construir la imagen Docker del cliente con estos recursos.
