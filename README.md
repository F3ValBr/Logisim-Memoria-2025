# LogisimCL

#### Este es un fork de [Logisim](https://cburch.com/logisim/index.html), trabajo original realizado por Carl Burch.

*Este es el repositorio de **LogisimCL**, proyecto llevado a cabo por diversos alumnos de la Universidad de Chile para la adición de nuevas herramientas y funcionalidades al simulador base. Actualmente el repositorio cumple la función de albergar la construcción del conversor de circuitos desde un diseño RTL, parte del desarrollo del trabajo de título "Traducción de circuitos implementados en (System)Verilog a Logisim"*

* **Tabla de contenidos**
    * [Funcionalidades](#Funcionalidades)
    * [Requisitos](#Requisitos)
    * **[Descargas](#Descargas)**
    * **[Uso](doc/repo/use.md)**
      * [Importador de `SystemVerilog`](doc/repo/howtoverilog.md)
    * [Más Información](#más-información)
    * [Creditos y contribuciones](#créditos-y-contribuciones)

## Funcionalidades

`LogisimCL` es un software educacional para el diseño y simulación de circuitos digitales. Como extensión del trabajo realizado
en `Logisim` original, esta versión es de uso [libre, abierto](LICENSE) y [multiplataforma](#Requisitos).

Este proyecto cuenta con las siguientes características:
* Diseño de circuitos fácil de usar.
* Simulación de circuitos lógicos.
* Extensión de los componentes preexistentes en la versión original de `Logisim`.
* Nuevos componentes para el diseño de circuitos complejos.
  * Temporizadores
  * Variantes de multiplexores
  * Variantes de puertas lógicas
* Soporte para la conversión desde `SystemVerilog`.
* Soporte para procesadores `Risc-V`.
* ¡Y más por venir!

![logisimCL_ex.png](doc/repo/resources/logisimCL_ex.png)

## Requisitos
`LogisimCL` es una aplicación Java, por lo que puede correr en cualquier máquina con la `JRE`.
Es necesario contar con la versión [Java 17 o superior](https://adoptium.net/temurin/releases/).
Adicionalmente, para usar las funcionalidades del conversor, es necesario contar con [`YoSYS`](https://yosyshq.net/yosys/) 
instalado en sus versiones más recientes. El uso de esta herramienta se detalla en la [documentación del importador](doc/repo/howtoverilog.md).

## Descargas
`LogisimCL` está únicamente disponible en el presente repositorio, tanto en el directorio de [Releases](releases) como en el
apartado de [lanzamientos dentro del repositorio](https://github.com/F3ValBr/LogisimCL/releases). La descarga es directa,
una vez se encuentre en tu máquina lo puedes ejecutar de forma inmediata.

## Más Información

La documentación presentada para `LogisimCL` se enfoca mayoritariamente en las nuevas funcionalidades añadidas al sistema original.
Para ver más información, puedes revisar la documentación interna del proyecto, siguiendo las instrucciones a continuación, o bien revisar
la documentación desde la [página web de `Logisim`](https://cburch.com/logisim/docs/2.7/en/html/guide/index.html) (esta documentación se enfoca
en la versión 2.7.1, la última disponible lanzada por Carl Burch).

Para revisar la documentación de forma interna en el proyecto, que incluye actualizaciones sobre el contenido de componentes del sistema original, junto con la inclusión
de las nuevas herramientas en `LogisimCL`, puedes abrir la opción **Ayuda** en la parte superior del programa, Ahí se desplegarán las opciones de
documentación disponibles. Como información a considerar, actualmente la documentación se encuentran únicamente en inglés.

## Créditos y contribuciones

`LogisimCL` corresponde a un trabajo basado en `Logisim`, implementado por Carl Burch, [Hendrix College](https://www.hendrix.edu/), Estados Unidos, y colaboradores involucrados en las traducciones y testing existentes.
Adicionalmente, las siguientes personas e instituciones colaboraron en el desarrollo de `LogisimCL` y sus nuevas funcionalidades:
* Esteban López Estrada, [Universidad de Chile](https://uchile.cl), Chile, desarrollador de las componentes Risc-V
* Felipe Valdebenito Bravo, [Universidad de Chile](https://uchile.cl), Chile, desarrollador del sistema de conversión `SystemVerilog` a `Logisim`.

Si deseas colaborar en el proyecto, puedes hacerlo mediante un *fork* a este repositorio y realizando un *pull request*. Eres libre de darte el crédito
de lo que construyas en este apartado, como corresponde. Además, puedes ayudar reportando errores y sugiriendo funcionalidades para esta versión del simulador.
¡Todas las contribuciones son bienvenidas!