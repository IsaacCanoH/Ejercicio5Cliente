# P2 — Pruebas automáticas del frontend

Suite de 20 casos con Playwright Test y Chrome sin ventana. Cada caso utiliza un contexto de navegador nuevo, crea datos exclusivos con un prefijo `P2-AUT-*` y limpia esos datos mediante la API al terminar, incluso cuando falla una aserción. Las capturas y trazas contienen datos ficticios.

## Requisitos y ejecución

- Node.js 20 o superior.
- Chrome instalado; también puede elegirse Edge con `-Browser msedge`.
- Frontend y backend iniciados, y MariaDB disponible para el backend.
- Playwright 1.62.1: se usa la copia instalada con Codex si está disponible. En otro equipo, ejecutar `npm.cmd install` dentro de `Ejercicio5Cliente`.

Desde PowerShell, en `Ejercicio5Cliente`:

```powershell
.\ejecutar-automaticas.ps1
```

Las direcciones se pueden indicar para una instancia independiente del frontend:

```powershell
.\ejecutar-automaticas.ps1 -Run final -FrontendUrl http://localhost:8082
```

Para volver a ejecutar un caso concreto sin sustituir el reporte final:

```powershell
.\ejecutar-automaticas.ps1 -Run comprobacion -Filtro AUT-F03
```

El script retorna 0 si todos los casos seleccionados pasan y un código distinto de 0 si hay fallos. No hay reintentos automáticos ni casos omitidos deliberadamente. Las direcciones por defecto son frontend `http://localhost:8080` y backend `http://localhost:8081`.

## Alcance

Los escenarios válidos operan la interfaz y los servicios reales. La persistencia se consulta de nuevo por la API del backend, que usa sus repositorios y MariaDB; esta suite no ejecuta consultas SQL directas.

AUT-F03, AUT-F05, AUT-F07, AUT-F09 y AUT-F10 interceptan una respuesta del frontend y simulan HTTP 500 o 404. Comprueban el mensaje y la conservación del registro en el backend real. No equivalen a una interrupción real del backend; esa interrupción se cubrió manualmente en INT-11.

Un caso de error pasa cuando la aplicación controla correctamente el error esperado. Los resultados de la suite no representan cobertura de líneas o ramas.

| Función | Caso válido | Caso de error |
|---|---|---|
| Registro | AUT-E01: persiste usuario y redirige a login | AUT-F01: nombre obligatorio vacío bloquea el envío |
| Login | AUT-E02: accede a inicio y conserva usuario | AUT-F02: contraseña incorrecta muestra aviso |
| Consulta | AUT-E03: muestra proyecto, tarea y comentario | AUT-F03: HTTP 500 simulado muestra aviso |
| Crear proyecto | AUT-E04: persiste datos y muestra tarjeta | AUT-F04: nombre vacío bloquea creación |
| Editar proyecto | AUT-E05: conserva ID y cambia descripción | AUT-F05: HTTP 404 simulado al cargar edición |
| Crear tarea | AUT-E06: crea pendiente en el proyecto correcto | AUT-F06: nombre vacío no crea ni envía |
| Editar tarea | AUT-E07: cambia descripción, fecha y lista | AUT-F07: HTTP 500 simulado conserva datos y muestra aviso |
| Crear comentario | AUT-E08: un único envío y comentario persistido | AUT-F08: texto vacío bloquea el envío |
| Eliminar tarea | AUT-E09: desaparece de interfaz y backend | AUT-F09: HTTP 500 simulado conserva tarea y muestra aviso |
| Eliminar proyecto | AUT-E10: desaparece de lista y backend | AUT-F10: HTTP 500 simulado conserva proyecto y muestra aviso |

## Evidencias

Los reportes se guardan en `output/p2/automaticas/<Run>/`, en la raíz del repositorio:

Para abrir el reporte en el navegador, desde `Ejercicio5Cliente`, ejecutar `./ver-reporte.ps1 -Run inicial` o `./ver-reporte.ps1 -Run final`. Mantener esa terminal abierta mientras se consulta el reporte; Ctrl+C cierra el visor.

- `html/index.html`: reporte navegable, con detalles y capturas de todos los casos.
- `resultados.json`: resultados estructurados, errores, anotaciones y datos de ejecución.
- `resultados.xml`: reporte JUnit.
- `artefactos/`: capturas y trazas de los casos fallidos.

El reporte inicial y la reprueba final se conservan en carpetas distintas. `preparacion` identifica la ejecución usada para ajustar selectores de la suite; no se incluye en las métricas de fallos de la aplicación.

Cada caso adjunta las peticiones y estados HTTP observados, los errores JavaScript y el recuento de datos limpiados. La limpieza solo utiliza los nombres e identificadores generados por ese caso; los datos manuales y los proyectos preexistentes permanecen fuera de ese conjunto.
