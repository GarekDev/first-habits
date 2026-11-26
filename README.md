<p align="center">
  <img src="docs/logo.png" width="180" alt="Llama de First Habits">
</p>

<h1 align="center">First Habits</h1>

<p align="center">
  Registro de hábitos para Android. Se queda en el teléfono.
</p>

First Habits es la app de hábitos de First You. Abres el día, ves tus hábitos y marcas lo que hiciste. No hay cuenta, no hay servidor y no hay anuncios.

## La lista

La pantalla principal se llama **First Habits**. A la izquierda están los nombres. A la derecha, los días, con el más reciente a la izquierda.

- Un hábito **Sí/No** se marca con una palomita. Si el día ya pasó y no lo hiciste, se ve una cruz.
- Un hábito **medible** guarda un número. Tú pones la unidad y la meta del día, por ejemplo 30 minutos. Si no llegas a la meta, el número sale apagado. Si la cumples, sale del color del hábito.
- Mantén el dedo sobre un hábito para subirlo o bajarlo. Ese orden también cambia en el widget.
- Un toque en el nombre abre el hábito.

## Dentro de un hábito

Todo está en la misma página. Al bajar aparecen las secciones, separadas por una línea:

- **Resumen**, con la puntuación, la racha y la mejor racha.
- **Puntuación**, una gráfica de las últimas semanas.
- **Calendario**.
- **Mejores rachas**.
- **Frecuencia**, de lunes a domingo.

La puntuación sube cuando cumples un día previsto y baja cuando lo dejas pasar. El día de hoy, si todavía no lo marcas, no rompe la racha.

## Widgets

Hay dos widgets para la pantalla de inicio.

- **Lista de hábitos.** Marcas el día de hoy sin abrir la app. El fondo, el texto, el tamaño y cuántos hábitos se ven se eligen al ponerlo.
- **Gráfica de hábito.** Eliges un hábito y ves su puntuación, con el nombre arriba y las fechas abajo.

## Ajustes

El tema oscuro viene activado. También hay tema claro y un color de acento. En Ajustes está la copia de seguridad: exportar guarda hábitos, días marcados, cantidades, colores, recordatorios y archivados. Importar sustituye lo que hay en este teléfono, y la app lo pide antes de hacerlo.

Cada hábito puede tener un recordatorio a una hora.

## Qué no hace

No pide cuenta. No habla con un servidor. No tiene anuncios ni compras. No lee el correo. Desinstalarla sin haber exportado el archivo borra la copia del teléfono.

## Cómo abrirla

En Android Studio: **File → Open** y elige esta carpeta. Hace falta el Android SDK 37 y un teléfono o emulador con Android 8 o mayor. El botón verde de Run la instala.

## Dónde están los datos

En el teléfono. La copia que controlas es el archivo `first-habits-backup.json`. Ese archivo no entra en Git.

## Licencia

MIT. El texto está en `LICENSE`.
