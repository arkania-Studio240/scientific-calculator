# Scientific Calculator (Fabric 1.20.1)

Calculadora científica dentro de Minecraft Java 1.20.1. Se abre con `/calc` (o `/calculator`).
Es un mod **solo de cliente**: los cálculos son locales, no se envía nada al servidor, no hay red ni telemetría,
y el servidor no necesita tener el mod. No toca el inventario ni requiere permisos.

> **Estado de verificación:** el motor matemático y el modelo (parser, funciones, memoria, historial, DEG/RAD)
> se probaron con 10 pruebas fuera de Minecraft. La pantalla gráfica, el comando y la compilación Gradle contra
> Minecraft **no se han compilado ni ejecutado** (sin acceso a red para descargar Loom/Yarn/Fabric). Ver "Pendiente".

## Requisitos
- Java 17 (JDK), Gradle 8.5+ (o genera el wrapper con `gradle wrapper --gradle-version 8.6`)
- Minecraft 1.20.1 con Fabric Loader ≥ 0.14.21 y Fabric API 0.92.2+1.20.1 (o posterior para 1.20.1)

## Compilar
```
gradle build          # genera build/libs/scientific-calculator-1.0.0.jar
gradle test           # pruebas unitarias del motor
gradle runClient      # arranca Minecraft de desarrollo
```
Copia `build/libs/scientific-calculator-1.0.0.jar` (no el `-sources`) a la carpeta `mods` junto a Fabric API.

## Uso
- `/calc` abre la calculadora. ESC la cierra (si hay un panel abierto, el primer ESC cierra el panel).
- Los clics en botones nunca cierran la pantalla. También puedes escribir con el teclado: dígitos, `+ - * / ^ ( ) ! % , ;`,
  letras para funciones, Enter = calcular, Retroceso = borrar, Supr = AC, flechas = mover el cursor.
- **SHIFT** activa la función pequeña de cada tecla (gris arriba). **hyp** (SHIFT+π) hace que la siguiente tecla
  trigonométrica sea hiperbólica; SHIFT+sin = arcoseno; hyp y SHIFT juntos no se combinan (primero hyp, luego SHIFT+sin → asinh).
- **DRG** alterna DEG/RAD. **x⇔E** notación científica. **a/b** resultados como fracción. **D°M'S** muestra el resultado en grados-minutos-segundos.
- **MENU**: ángulos, modo complejo, fracción, notación científica, borrar memoria e historial. **HIST** (SHIFT+ANS): historial; clic reutiliza el resultado.
- Tras `=`, pulsar un operador continúa con `ans`; pulsar un número empieza de nuevo.

## Funciones
Aritmética, `%`, `mod`, paréntesis (los abiertos se cierran solos al final), potencias `^`, `sqrt`, `root(n,x)`, `cbrt`, `inv`, `abs`,
`sin cos tan asin acos atan sinh cosh tanh asinh acosh atanh`, `log` (base 10), `ln`, `exp`, `π`, `e`, `n!`, `ncr`, `npr`,
notación `1E3`, `deg()`/`rad()`, `floor ceil round`, `rand()`.
Complejos (`i`, `abs arg conj re im`, `sqrt` y `ln` de negativos con modo CPLX). Matrices `[1,2;3,4]` (+, −, ×, escalar, `^n`, `det`, `tr`, `inv`, hasta 6x6).
`sum(f,desde,hasta)`, `integ(f,a,b)` (Simpson), `deriv(f,x0)`, `lim(f,x0)`; dentro de ellas la variable es `x`.
Estadística: `mean median stdev stdevp min max`. Memoria: `x→M`, `M+`, `M−`, `MR`. Historial de 50 entradas.

## Limitaciones (versión 1.0)
- Los resultados se muestran con 10 cifras significativas; valores con |x| < 1e-15 se muestran como 0.
- Complejos: trigonometría y funciones hiperbólicas solo con reales. Matrices: solo reales.
- `integ` es numérica (2000 subintervalos): no detecta singularidades. `lim` compara los dos lados; no distingue ±∞.
- `sum` admite hasta 20001 términos. Un presupuesto de pasos corta expresiones demasiado costosas ("Expresión demasiado costosa"), así que el hilo principal no se bloquea.
- `(-8)^(1/3)` da error en dominio real; usa `root(3,-8)`.
- La fracción se limita a denominadores ≤ 100000.
- Los glifos `⇔ ∫ Σ ⁻¹ ⁿ ∛` dependen de la fuente de Minecraft; si alguno falta, cambia la etiqueta en `KeyLayout.java`.

## Pendiente de comprobar en juego
Compilación con Loom, que `/calc` abra la pantalla (se abre un tick después para que el chat no la cierre),
y el aspecto en distintas escalas de GUI.

## Estructura
`engine/` (Lexer, Parser, funciones, Complex, Matrix, Formatter) · `model/` (estado, memoria, historial) ·
`gui/` (pantalla y teclas) · `command/` (`/calc`) · `src/test` (pruebas JUnit 5).


## Compilar con GitHub Actions (sin Termux)

1. Extrae este ZIP en el teléfono o en un ordenador.
2. Sube el contenido del directorio `scientific-calculator/` al directorio raíz del repositorio de GitHub. Deben quedar `build.gradle`, `settings.gradle`, `gradle.properties` y `src/` en la raíz.
3. Asegúrate de subir también `.github/workflows/build.yml`.
4. En GitHub abre la pestaña **Actions** y selecciona **Build Scientific Calculator**.
5. Si no se ejecutó automáticamente al subir los archivos, pulsa **Run workflow**.
6. Cuando finalice correctamente, abre la ejecución y descarga el artefacto `scientific-calculator-minecraft-1.20.1`. El `.jar` estará dentro del ZIP descargado.

La compilación no está garantizada hasta que GitHub Actions termine con `success`; si falla, abre los registros del paso que falló.
