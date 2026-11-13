# FOLDER_FEATURE.md

# Especificación funcional: gestión avanzada de carpetas

## 1. Objetivo

Añadir al App Drawer de Olauncher un sistema avanzado de carpetas que permita organizar aplicaciones, pinned-shortcuts y otras carpetas de forma jerárquica.

La implementación debe integrarse en la arquitectura existente de Olauncher, manteniendo el comportamiento original de las funcionalidades no relacionadas y procurando que los cambios respecto al repositorio original sean mínimos.

Esta especificación define el comportamiento funcional esperado. Antes de implementar, se debe estudiar la arquitectura real del proyecto y adaptar la solución a sus modelos, mecanismos de persistencia, navegación y componentes existentes.

## 2. Principios generales

1. Las aplicaciones y los pinned-shortcuts pueden organizarse dentro de carpetas.
2. Las carpetas pueden contener aplicaciones, pinned-shortcuts y otras carpetas.
3. Las carpetas pueden anidarse sin establecer un límite artificial de profundidad.
4. Una carpeta solo puede tener una ubicación jerárquica dentro del App Drawer: raíz o dentro de otra carpeta.
5. Una carpeta puede tener referencias adicionales desde el escritorio y las acciones de deslizamiento.
6. Las aplicaciones y los pinned-shortcuts pueden tener referencias en distintas ubicaciones, dependiendo del valor de `allowDuplicateItems`.
7. Mover una carpeta no debe cambiar su identificador ni perder su contenido.
8. Las operaciones sobre carpetas deben mantener la integridad de las referencias y de los datos persistidos.
9. Toda la interfaz nueva debe respetar el diseño y los patrones visuales y funcionales de la versión original de Olauncher.

## 3. Conceptos y tipos de elementos

### 3.1. Aplicaciones

Son las aplicaciones instaladas que aparecen en el App Drawer.

Pueden estar en la raíz del App Drawer o dentro de una o varias carpetas, dependiendo de la configuración `allowDuplicateItems`.

Añadir una aplicación a una carpeta no debe desinstalarla ni modificar su configuración en el sistema Android.

### 3.2. Pinned-shortcuts

Son los accesos directos fijados que Olauncher permite mostrar en el App Drawer.

Pueden estar en la raíz o dentro de carpetas, dependiendo de la configuración `allowDuplicateItems`.

Deben tratarse como elementos distintos de las aplicaciones normales, respetando su comportamiento original.

Cuando una operación implique retirar un pinned-shortcut de su ubicación original, se eliminará de esa ubicación; no se aplicará el comportamiento de ocultar utilizado para las aplicaciones normales.

### 3.3. Carpetas

Cada carpeta debe disponer de:

- Un identificador estable y único.
- Un nombre.
- Un contenido compuesto por aplicaciones, pinned-shortcuts y otras carpetas.
- Una única ubicación jerárquica.
- Un contador persistente de referencias.

Las carpetas pueden tener referencias adicionales desde el escritorio y las acciones de deslizamiento.

El identificador debe mantenerse al renombrar o mover una carpeta.

### 3.4. Rutas de carpetas

Cada carpeta debe poder identificarse mediante su ruta completa.

Ejemplos:

- `Documentos`
- `Trabajo/Documentos`
- `Personal/Documentos`
- `Trabajo/Proyectos/Documentos`

Las rutas deben actualizarse automáticamente cuando se renombre o mueva una carpeta o cualquiera de sus carpetas antecesoras.

## 4. Opción «Añadir a carpeta»

Todos los elementos del App Drawer deben disponer de una opción contextual denominada «Añadir a carpeta».

Esta opción debe estar disponible para:

- Aplicaciones.
- Pinned-shortcuts.
- Carpetas.

Al seleccionarla, se abrirá un selector que mostrará todas las carpetas existentes, independientemente de su nivel jerárquico.

El selector debe:

- Mostrar las carpetas mediante sus rutas completas.
- Permitir distinguir carpetas con el mismo nombre situadas en ubicaciones diferentes.
- Permitir seleccionar cualquier carpeta válida.
- Incluir una opción «Nueva carpeta».
- Incluir una opción «Mover a la raíz» cuando el elemento seleccionado sea una carpeta que esté
  dentro de otra carpeta. Al elegirla, la carpeta se mueve a la raíz del App Drawer conservando su
  identificador, nombre, contenido y referencias externas.
- Excluir los destinos que puedan provocar ciclos cuando el elemento seleccionado sea una carpeta.

Ejemplos de destinos:

- `Documentos`
- `Trabajo/Documentos`
- `Personal/Documentos`
- `Trabajo/Proyectos`

La selección de una carpeta debe ejecutar la operación correspondiente según el tipo de elemento y el valor de `allowDuplicateItems`.

## 5. Añadir elementos a carpetas existentes

### 5.1. Aplicaciones

El comportamiento dependerá de `allowDuplicateItems`.

Cuando `allowDuplicateItems` sea `true`:

- Se añadirá una referencia de la aplicación a la carpeta de destino.
- La aplicación permanecerá en su ubicación original.
- No se eliminará de otras carpetas.
- La aplicación podrá aparecer simultáneamente en distintas ubicaciones.

Cuando `allowDuplicateItems` sea `false`:

- La aplicación se trasladará a la carpeta de destino.
- Si estaba en la raíz del App Drawer, se ocultará de la raíz.
- Si estaba en otra carpeta, se eliminará de la carpeta de origen.
- No se eliminarán otras referencias o configuraciones ajenas a la operación que no formen parte de su ubicación en el App Drawer.

La aplicación seguirá instalada en el dispositivo.

### 5.2. Pinned-shortcuts

El comportamiento dependerá igualmente de `allowDuplicateItems`.

Cuando `allowDuplicateItems` sea `true`:

- Se añadirá una referencia del pinned-shortcut a la carpeta de destino.
- El pinned-shortcut permanecerá en su ubicación original.
- No se eliminará de otras carpetas.

Cuando `allowDuplicateItems` sea `false`:

- El pinned-shortcut se trasladará a la carpeta de destino.
- Si estaba en la raíz del App Drawer, se eliminará de esa ubicación.
- Si estaba en otra carpeta, se eliminará de la carpeta de origen.

No se aplicará el comportamiento de ocultar utilizado para las aplicaciones normales.

### 5.3. Carpetas

Añadir una carpeta a otra siempre será una operación de movimiento, independientemente de `allowDuplicateItems`.

- La carpeta desaparecerá de su ubicación jerárquica anterior.
- Se incorporará a la carpeta de destino.
- Conservará su identificador.
- Conservará su nombre y contenido.
- Mantendrá sus referencias externas desde el escritorio y las acciones de deslizamiento.
- No se creará una copia de la carpeta.

Una carpeta solo puede tener una ubicación jerárquica.

## 6. Creación de carpetas

El selector «Añadir a carpeta» debe incluir una opción «Nueva carpeta».

Al seleccionarla:

1. Se solicitará el nombre de la nueva carpeta.
2. El usuario podrá confirmar o cancelar.
3. No se permitirá confirmar un nombre vacío.
4. La nueva carpeta se creará en la raíz del App Drawer.
5. El elemento seleccionado se añadirá automáticamente a la nueva carpeta.

El comportamiento posterior dependerá del tipo de elemento:

- Aplicación: se aplicará la lógica de `allowDuplicateItems`.
- Pinned-shortcut: se aplicará la lógica de `allowDuplicateItems`.
- Carpeta: se moverá a la nueva carpeta.

### 6.1. Nombres duplicados

No se permitirán dos carpetas con el mismo nombre dentro del mismo nivel jerárquico.

Sí se permitirán carpetas con el mismo nombre en ubicaciones diferentes.

Ejemplos válidos:

- `Trabajo/Documentos`
- `Personal/Documentos`

Si el nombre introducido para crear una carpeta ya existe en el nivel donde se va a crear:

- No se creará una segunda carpeta.
- Se utilizará la carpeta existente como destino.
- Se añadirá o moverá el elemento seleccionado a esa carpeta, aplicando las reglas correspondientes a su tipo.

### 6.2. Cancelación y errores

Cancelar la creación no debe modificar ningún dato.

La creación de una carpeta y la incorporación del elemento deben tratarse como una operación coherente.

Si se produce un error durante la operación, no deben quedar carpetas vacías o referencias incompletas como consecuencia de una operación parcialmente ejecutada.

## 7. Abrir y navegar por carpetas

Al pulsar una carpeta en el App Drawer se mostrará su contenido.

El contenido puede incluir:

- Aplicaciones.
- Pinned-shortcuts.
- Otras carpetas.

La presentación debe respetar las reglas de ordenación definidas en esta especificación.

### 7.1. Navegación hacia atrás

La navegación debe respetar la jerarquía:

- Si la carpeta abierta tiene una carpeta padre, Atrás vuelve a esa carpeta padre.
- Si la carpeta abierta está en la raíz, Atrás vuelve a la vista principal del App Drawer.

Debe utilizarse el mecanismo de navegación y el comportamiento de la tecla Atrás que ya emplea Olauncher.

### 7.2. Carpetas abiertas desde otras ubicaciones

Si una carpeta se abre desde el escritorio o desde una acción de deslizamiento, la navegación debe mantener un comportamiento coherente con el contexto desde el que se abrió.

No deben introducirse comportamientos de navegación inconsistentes entre las distintas formas de acceder a una carpeta.

### 7.3. Búsqueda recursiva en carpetas

La búsqueda debe estar disponible tanto en la raíz del App Drawer como dentro de las carpetas.

Desde cualquier nivel, la búsqueda debe abarcar:

- Los elementos del nivel actual.
- El contenido de las carpetas de ese nivel.
- El contenido de todas sus subcarpetas, en cualquier profundidad.

Los resultados pueden incluir aplicaciones, pinned-shortcuts y filas de carpeta de los subniveles, de modo que una coincidencia con el nombre de una subcarpeta permita abrirla. Los resultados se muestran como una lista plana, sin indicar la ruta de origen.

Al abrir una carpeta desde un resultado de búsqueda, Atrás vuelve al nivel desde el que se buscó.

## 8. Renombrar y eliminar carpetas

### 8.1. Renombrar

Las carpetas deben poder renombrarse.

Al cambiar el nombre:

- Se conserva el identificador.
- Se conserva la ubicación jerárquica.
- Se conserva el contenido.
- Se conservan las referencias externas.
- Se actualizan las rutas completas de la carpeta y de sus descendientes.

No se debe crear una carpeta nueva ni eliminar la anterior para realizar un renombrado.

### 8.2. Eliminar

Debe distinguirse entre retirar una carpeta de una ubicación y eliminar definitivamente su registro.

La eliminación definitiva solo se realizará cuando el contador de referencias llegue a cero.

Al eliminar definitivamente una carpeta:

- Se liberarán las referencias a todos los elementos que contiene.
- No se eliminarán las aplicaciones instaladas.
- No se eliminarán los pinned-shortcuts por el mero hecho de haber estado dentro de la carpeta.
- Se liberará la pertenencia jerárquica de las carpetas hijas.
- Las carpetas hijas que conserven otras referencias seguirán existiendo.
- Las carpetas hijas cuyo contador llegue a cero se eliminarán recursivamente, liberando a su vez el contenido.

Las operaciones recursivas deben mantener la integridad de los datos y evitar eliminaciones duplicadas.

## 9. Prevención de ciclos

No se permitirá crear ciclos en la jerarquía de carpetas.

Por tanto:

- Una carpeta no puede añadirse a sí misma.
- Una carpeta no puede moverse dentro de ninguna de sus descendientes.
- Una carpeta puede moverse dentro de cualquier otra carpeta que no sea ella misma ni una de sus descendientes.

El selector de destinos debe excluir las carpetas que provocarían ciclos.

La validación debe realizarse también en la operación de movimiento, no depender exclusivamente de la interfaz.

## 10. Referencias y ciclo de vida de las carpetas

Las carpetas tendrán un contador persistente de referencias.

El contador debe representar las referencias activas a la carpeta procedentes de:

1. Su pertenencia jerárquica: raíz del App Drawer o carpeta padre.
2. El escritorio.
3. Las acciones de deslizamiento.

La pertenencia jerárquica cuenta como una única referencia, independientemente de la profundidad de la carpeta.

### 10.1. Creación

Al crear una carpeta se inicializará su contador de referencias teniendo en cuenta su ubicación jerárquica.

### 10.2. Escritorio

- Añadir una carpeta al escritorio incrementa su contador.
- Quitar una carpeta del escritorio decrementa su contador.

### 10.3. Acciones de deslizamiento

- Asignar una carpeta a una acción de deslizamiento incrementa su contador.
- Sustituir una carpeta por otra actualiza las referencias de ambas.
- Eliminar una acción que apunta a una carpeta decrementa su contador.

No deben duplicarse referencias por una misma asignación.

### 10.4. Movimientos jerárquicos

Mover una carpeta de una ubicación a otra debe transferir su pertenencia jerárquica de forma atómica.

La operación no debe provocar que el contador pase transitoriamente por cero ni que la carpeta se elimine durante el movimiento.

Debe conservarse el registro de la carpeta en Settings y su contenido.

### 10.5. Referencias a cero

Cuando el contador de referencias llegue a cero:

- Se eliminará definitivamente el registro de la carpeta de Settings.
- Se liberarán las referencias a su contenido.
- Se aplicará recursivamente el ciclo de vida de las carpetas hijas.

### 10.6. Integridad

El contador debe mantenerse sincronizado con las referencias reales.

Las operaciones deben evitar:

- Incrementos duplicados.
- Decrementos duplicados.
- Referencias huérfanas.
- Eliminaciones prematuras.
- Carpetas que permanezcan indefinidamente sin referencias.

Se deben reutilizar los mecanismos de persistencia y transacciones existentes en Olauncher siempre que sea posible.

## 11. Integración con escritorio y acciones de deslizamiento

Las carpetas podrán utilizarse como elementos del escritorio y como destinos de acciones de deslizamiento, siempre que la arquitectura existente lo permita.

Estas referencias externas no alteran la ubicación jerárquica única de la carpeta dentro del App Drawer.

Una carpeta puede estar, por ejemplo:

- Dentro de otra carpeta en el App Drawer.
- Referenciada desde el escritorio.
- Asignada a una acción de deslizamiento.

Las operaciones de asignación, sustitución y eliminación de estas referencias deben actualizar correctamente el contador de referencias.

Si una carpeta se mueve dentro del App Drawer, sus referencias externas deben seguir apuntando al mismo identificador.

## 12. Configuración

Se deben incorporar dos opciones de configuración internas.

**Por ahora no se debe añadir ninguna interfaz de usuario para consultar o modificar estas opciones.** Deben existir como valores de configuración con sus valores predeterminados indicados.

### 12.1. `allowDuplicateItems`

Tipo: boolean.

Valor predeterminado: `false`.

Controla si una aplicación o pinned-shortcut puede conservar su ubicación original cuando se añade a una carpeta.

Cuando vale `false`:

- Las aplicaciones se trasladan a la carpeta de destino.
- Si una aplicación estaba en la raíz del App Drawer, se oculta de esa ubicación.
- Si estaba en otra carpeta, se elimina de la carpeta de origen.
- Los pinned-shortcuts se trasladan a la carpeta de destino.
- Si un pinned-shortcut estaba en la raíz del App Drawer, se elimina de esa ubicación.
- Si estaba en otra carpeta, se elimina de la carpeta de origen.

Cuando vale `true`:

- Añadir una aplicación a una carpeta crea una referencia adicional.
- La aplicación permanece en su ubicación original.
- Añadir un pinned-shortcut a una carpeta crea una referencia adicional.
- El pinned-shortcut permanece en su ubicación original.
- Se permite que estos elementos aparezcan en varias ubicaciones simultáneamente.

Esta opción no modifica el comportamiento de las carpetas: añadir una carpeta a otra siempre implica moverla, nunca duplicarla.

### 12.2. `showFoldersFirst`

Tipo: boolean.

Valor predeterminado: `true`.

Controla cómo se ordenan los elementos dentro del App Drawer y de las carpetas.

Cuando vale `true`:

1. Las carpetas se muestran primero.
2. Las carpetas se ordenan alfabéticamente.
3. A continuación se muestran las aplicaciones y los pinned-shortcuts, ordenados alfabéticamente entre sí.

Cuando vale `false`:

- Las carpetas, aplicaciones y pinned-shortcuts se ordenan conjuntamente por orden alfabético.

En ambos casos, la comparación alfabética debe ignorar:

- Diferencias entre mayúsculas y minúsculas.
- Diferencias entre caracteres acentuados y no acentuados.

La ordenación debe ser coherente en la raíz del App Drawer y dentro de las carpetas.

### 12.3. Interfaz de configuración

No se deben añadir todavía controles, pantallas ni diálogos para modificar `allowDuplicateItems` o `showFoldersFirst`.

La implementación debe permitir utilizar estos valores internamente y facilitar que se pueda incorporar una interfaz de configuración en el futuro sin tener que modificar la lógica principal de las carpetas.

### 12.4. `showKeyboardInFolders` (específica del fork)

Tipo: boolean.

Valor predeterminado: `false`.

Indica si el teclado debe mostrarse automáticamente al entrar en una carpeta. Solo tiene efecto si la preferencia global `autoShowKeyboard` está activa; si esta está desactivada, el teclado no se muestra nunca de forma automática.

La barra de búsqueda es visible dentro de las carpetas con independencia de este valor; la preferencia solo controla la aparición automática del teclado. No debe añadirse interfaz de usuario para modificarla.

## 13. Ordenación y presentación

### 13.1. Ordenación

La ordenación se regirá por el valor de `showFoldersFirst`, descrito en el apartado 12.2.

Debe mantenerse el comportamiento habitual de Olauncher en todo aquello que no contradiga expresamente esta especificación.

La comparación alfabética debe ser insensible a mayúsculas, minúsculas y acentos.

### 13.2. Indicador visual de carpetas

Las carpetas deben mostrar el carácter `>` alineado a la derecha de su línea.

Este indicador permite distinguir visualmente las carpetas de las aplicaciones y los pinned-shortcuts.

El carácter debe aparecer en todas las ubicaciones donde se representen carpetas como elementos de una lista, incluyendo:

- La raíz del App Drawer.
- El contenido de otras carpetas.

Debe respetar la alineación y las dimensiones de las filas existentes en Olauncher.

No debe alterar innecesariamente el diseño ni la distribución de los demás elementos.

### 13.3. Coherencia con la interfaz original

Todas las pantallas, diálogos, selectores, menús y demás elementos de interfaz que se incorporen deben ser coherentes visual y funcionalmente con la interfaz actual de Olauncher.

Antes de desarrollar cualquier elemento visual nuevo, se debe estudiar cómo está implementada la interfaz existente.

Se deben reutilizar siempre que sea posible:

- Componentes existentes.
- Estilos y temas.
- Tipografías.
- Tamaños y dimensiones.
- Colores.
- Espaciados.
- Patrones de navegación.
- Comportamientos de interacción.
- Diálogos y menús existentes.

No se debe introducir un estilo visual que parezca pertenecer a otra aplicación.

Esta regla se aplica especialmente a:

- El selector de carpetas y la visualización de rutas completas.
- La creación y el renombrado de carpetas.
- Los menús contextuales.
- La navegación entre carpetas.
- El comportamiento de Atrás.
- Los diálogos de confirmación.
- Los mensajes de error y avisos.
- El indicador visual `>` de las carpetas.

Si no existe un componente equivalente y es necesario crear uno nuevo, debe respetar las dimensiones, tipografía, colores, espaciados e interacciones de los componentes originales.

No se deben introducir animaciones, elementos decorativos ni patrones visuales ajenos a Olauncher por iniciativa propia.

Solo se admitirán elementos visuales nuevos cuando sean necesarios para cubrir una funcionalidad y mantengan la coherencia con el diseño existente.

## 14. Persistencia y compatibilidad

La información de las carpetas debe persistir correctamente al:

- Cerrar y volver a abrir la aplicación.
- Reiniciar el dispositivo.
- Actualizar la aplicación, siempre que los datos existentes se conserven.

Se debe estudiar el sistema actual de Settings y persistencia antes de decidir dónde almacenar la información.

La implementación debe ser compatible con configuraciones creadas por versiones anteriores de Olauncher.

Si una instalación anterior no contiene datos de carpetas, se debe interpretar como una instalación sin carpetas.

No se deben introducir migraciones innecesarias.

Si es imprescindible realizar una migración, debe ser mínima, segura y compatible con los datos existentes.

Las opciones `allowDuplicateItems` y `showFoldersFirst` deben disponer de sus valores predeterminados incluso en instalaciones que no tengan datos previos de estas opciones.

## 15. Restricciones de implementación

La implementación debe seguir estas reglas:

1. Mantener los cambios respecto al repositorio original tan pequeños como sea razonablemente posible.
2. No realizar refactorizaciones generales que no sean necesarias para esta funcionalidad.
3. No modificar comportamientos ajenos a la gestión de carpetas.
4. Reutilizar modelos, repositorios, persistencia y componentes existentes cuando sea posible.
5. No introducir dependencias nuevas sin una justificación técnica clara.
6. No inventar clases, tablas, repositorios o abstracciones sin estudiar previamente la arquitectura real.
7. Mantener la compatibilidad con futuras actualizaciones del repositorio original.
8. Actualizar la documentación técnica cuando se realicen cambios relevantes.

### 15.1. Análisis previo obligatorio

Antes de modificar el código, se debe estudiar como mínimo:

- La estructura general del proyecto.
- Los modelos de aplicaciones.
- Los modelos de pinned-shortcuts.
- El funcionamiento actual del App Drawer.
- La gestión del escritorio.
- Las acciones de deslizamiento.
- Settings y los mecanismos de persistencia.
- La ordenación actual de los elementos.
- La navegación y el comportamiento de Atrás.
- Los componentes visuales, estilos y diálogos existentes.

Después del análisis se debe proponer un plan de implementación que identifique los archivos que previsiblemente habrá que modificar y explique cómo se integrará la funcionalidad.

Si una limitación real de la arquitectura entra en conflicto con esta especificación, se debe explicar el problema y proponer alternativas antes de modificar el comportamiento solicitado.

### 15.2. Commits e historial Git

La IA tiene prohibido crear commits o modificar el historial de Git.

Todos los commits serán realizados exclusivamente por el usuario.

La IA puede:

- Modificar archivos.
- Crear archivos.
- Ejecutar pruebas.
- Revisar diferencias.
- Analizar el historial.
- Preparar los cambios para un commit.

Cuando los cambios estén listos, debe avisar al usuario y explicar qué archivos y modificaciones incluiría el commit.

Puede proponer un mensaje de commit, pero nunca ejecutarlo.

Esta prohibición incluye expresamente:

- `git commit`
- `git commit --amend`
- Cualquier operación que cree, reescriba o modifique commits existentes.

Si necesita que el usuario cree un commit para poder continuar, debe explicarlo y esperar.

## 16. Criterios de aceptación

La funcionalidad se considerará implementada cuando se cumplan los siguientes criterios.

### 16.1. Gestión de carpetas

- Se pueden crear carpetas.
- Se pueden renombrar carpetas.
- Se pueden eliminar carpetas respetando el contador de referencias.
- Se pueden abrir carpetas.
- Se puede navegar entre carpetas anidadas.
- Se puede volver a la vista anterior mediante Atrás.
- Se pueden anidar carpetas sin un límite artificial de profundidad.

### 16.2. Añadir elementos

- Todos los elementos del App Drawer disponen de «Añadir a carpeta».
- El selector muestra todas las carpetas válidas.
- Las rutas completas permiten distinguir carpetas homónimas.
- Se pueden crear carpetas desde el selector.
- Una carpeta creada desde este flujo se crea en la raíz.
- El selector ofrece «Mover a la raíz» para carpetas anidadas y la mueve a la raíz del App Drawer.
- Los nombres duplicados dentro de un mismo nivel reutilizan la carpeta existente.
- Añadir aplicaciones respeta `allowDuplicateItems`.
- Añadir pinned-shortcuts respeta `allowDuplicateItems`.
- Añadir carpetas siempre las mueve y conserva su identificador y contenido.
- No se producen ciclos.
- La búsqueda abarca el nivel actual y todas sus subcarpetas, incluidos los nombres de subcarpeta.
- La barra de búsqueda es visible dentro de las carpetas.

### 16.3. Configuración

- `allowDuplicateItems` existe y su valor predeterminado es `false`.
- Con `allowDuplicateItems = false`, las aplicaciones se ocultan de la raíz o se eliminan de la carpeta de origen al trasladarlas.
- Con `allowDuplicateItems = false`, los pinned-shortcuts se eliminan de la raíz o de la carpeta de origen al trasladarlos.
- Con `allowDuplicateItems = true`, las aplicaciones y los pinned-shortcuts conservan su ubicación original al añadirse a otra carpeta.
- `showFoldersFirst` existe y su valor predeterminado es `true`.
- Con `showFoldersFirst = true`, las carpetas aparecen antes que aplicaciones y pinned-shortcuts.
- Con `showFoldersFirst = false`, todos los elementos se ordenan conjuntamente.
- La ordenación ignora mayúsculas, minúsculas y acentos.
- `showKeyboardInFolders` existe y su valor predeterminado es `false`.
- El teclado solo se muestra automáticamente en carpetas si `autoShowKeyboard` y `showKeyboardInFolders` están activos.
- No se ha añadido interfaz de usuario para modificar estas opciones.

### 16.4. Presentación

- Las carpetas muestran el carácter `>` alineado a la derecha de su línea.
- El indicador aparece tanto en la raíz como dentro de otras carpetas.
- La interfaz nueva mantiene la coherencia visual y funcional con Olauncher.
- Se reutilizan componentes y estilos originales siempre que sea posible.
- No se introducen patrones visuales ajenos a la aplicación.

### 16.5. Referencias y persistencia

- El contador de referencias se actualiza correctamente.
- Las referencias del escritorio se contabilizan.
- Las referencias de las acciones de deslizamiento se contabilizan.
- Los movimientos no provocan eliminaciones transitorias.
- Las carpetas con referencias activas no se eliminan.
- Las carpetas sin referencias se eliminan definitivamente.
- La eliminación recursiva libera correctamente el contenido.
- Las aplicaciones y los pinned-shortcuts no se eliminan por eliminar una carpeta.
- Los datos persisten al cerrar la aplicación y reiniciar el dispositivo.
- Las instalaciones anteriores siguen siendo compatibles.

### 16.6. Comportamiento original

- La gestión original de aplicaciones sigue funcionando.
- Ocultar una aplicación en la vista principal no elimina sus referencias dentro de carpetas.
- No se modifica el comportamiento de funcionalidades ajenas a las carpetas.
- La ordenación habitual se conserva en aquello que no contradiga esta especificación.

## 17. Pruebas adicionales recomendadas

Además de las pruebas necesarias para los criterios de aceptación, se recomienda probar los siguientes escenarios:

1. Crear varias carpetas en la raíz.
2. Crear carpetas con el mismo nombre en niveles diferentes.
3. Intentar crear una carpeta con un nombre ya existente en el mismo nivel.
4. Añadir una aplicación a una carpeta con `allowDuplicateItems = false`.
5. Añadir una aplicación a una carpeta con `allowDuplicateItems = true`.
6. Añadir un pinned-shortcut a una carpeta con ambos valores de configuración.
7. Mover una carpeta que contiene otras carpetas.
8. Intentar mover una carpeta dentro de sí misma.
9. Intentar mover una carpeta dentro de una de sus descendientes.
10. Renombrar una carpeta que tiene descendientes.
11. Eliminar una carpeta que contiene aplicaciones y pinned-shortcuts.
12. Eliminar una carpeta que contiene otras carpetas.
13. Eliminar una carpeta que sigue referenciada desde el escritorio.
14. Eliminar una carpeta asignada a una acción de deslizamiento.
15. Sustituir una carpeta asignada a una acción de deslizamiento.
16. Mover una carpeta que también está referenciada desde el escritorio.
17. Cerrar y volver a abrir la aplicación.
18. Reiniciar el dispositivo.
19. Probar la ordenación con nombres que contienen mayúsculas, minúsculas y acentos.
20. Probar ambos valores de `showFoldersFirst`.
21. Comprobar visualmente el indicador `>` en diferentes niveles.
22. Comprobar visualmente el selector, los diálogos y la navegación para verificar que mantienen el estilo original de Olauncher.

## 18. Decisiones técnicas pendientes de validar durante el análisis

La especificación define el comportamiento funcional, pero no impone una arquitectura concreta.

Antes de implementar se debe determinar, a partir del código real:

- Dónde almacenar los registros de carpetas.
- Cómo representar las relaciones jerárquicas.
- Cómo integrar las referencias con el sistema actual de Settings.
- Cómo contabilizar las referencias externas.
- Cómo realizar operaciones atómicas con la persistencia disponible.
- Cómo reutilizar el sistema de navegación existente.
- Cómo integrar la ordenación sin duplicar innecesariamente la lógica.
- Cómo implementar las opciones de configuración utilizando los mecanismos existentes.

Estas decisiones deben documentarse en `FORK_MAINTENANCE.md` cuando se conozca la solución definitiva.

No se deben introducir abstracciones adicionales si los mecanismos existentes permiten resolver el problema de forma sencilla y segura.

## 19. Mantenimiento de esta especificación

Este documento es la referencia funcional principal para la implementación de la gestión avanzada de carpetas.

Si durante el desarrollo se acuerda modificar algún comportamiento:

1. Se debe actualizar este documento.
2. Se debe dejar constancia de la decisión técnica relevante en `FORK_MAINTENANCE.md`.
3. Se debe mantener la coherencia entre la especificación, el código y las pruebas.
4. No se deben introducir cambios funcionales importantes que contradigan este documento sin autorización expresa del usuario.

---

# Anexo: estado de implementación en este fork (2026-11-10)

Este anexo describe cómo se ha implementado la especificación y las diferencias conscientes
respecto al texto original. La especificación sigue siendo la referencia funcional.

## Implementación

- La lógica vive en `Olauncher/app/src/main/java/app/olauncher/folders/`.
- Jerarquía de un solo padre; el padre se deriva de la lista de hijos.
- Contador de referencias derivado de la ubicación jerárquica y de las referencias externas
  (escritorio y swipe). No se persiste el número.
- Persistencia en el mismo `SharedPreferences` (`app.olauncher`) con `FOLDERS_DATA` en formato
  `folders-v1`; sin migraciones ni cambios en `Prefs`.
- Las filas de carpeta se representan como `AppModel.App` con `appPackage = folder:<id>` para
  minimizar cambios en el modelo original y en `DiffUtil`.
- Navegación entre carpetas con pila interna y `OnBackPressedCallback`; la ruta se restaura al
  volver del selector con `savedStateHandle`.
- `FolderPickerFragment` es una pantalla completa que reutiliza el layout del App Drawer; muestra
  rutas completas, excluye destinos cíclicos, ofrece «Move to root» para carpetas anidadas y deja
  «New folder» como último elemento.
- El contenido de carpeta se resuelve con `currentAppList` y las apps ocultas del `ViewModel`, sin
  volver a ejecutar un escaneo completo de aplicaciones.

## Diferencias o decisiones de producto

- `allowDuplicateItems`: valor por defecto `false`.
- `showFoldersFirst`: en este fork el valor por defecto es `false` (orden mixto). La especificación
  proponía `true`.
- El título dentro de una carpeta muestra solo el nombre de la carpeta actual, no la ruta.
- El título puede ocupar varias líneas.
- La celda de acción «Carpeta» usa el texto corto «Folder»/«Carpeta» con icono.
- La retirada de una carpeta muestra «Quitar» si quedan referencias y «Borrar» si es la última.
- «Mover a la raíz» rechaza el movimiento si ya existe otra carpeta con el mismo nombre en la raíz.
- El pinned-shortcut solo se desancla desde la raíz; dentro de una carpeta se retira de la carpeta.
- MOD-008 ocultaba la fila del buscador dentro de carpetas; MOD-011 la vuelve a mostrar para
  permitir la búsqueda recursiva.
- La búsqueda incluye el contenido de las carpetas del nivel actual y de todas sus subcarpetas,
  incluidas las filas de carpeta de esos subniveles. Los resultados son una lista plana sin ruta.
- `showKeyboardInFolders` (clave `FOLDERS_SHOW_KEYBOARD_IN_FOLDERS`) permite mostrar el teclado
  automáticamente dentro de carpetas, combinada con `autoShowKeyboard`. Por defecto `false`.
- No se ha añadido interfaz para `allowDuplicateItems`, `showFoldersFirst` ni
  `showKeyboardInFolders`.

## Limitaciones conocidas

- Android solo entrega al launcher los pinned-shortcuts fijados por él mismo. Los fijados por otro
  launcher no aparecen ni pueden migrarse automáticamente; deben volver a fijarse desde la app de
  origen con Olauncher como launcher predeterminado.
- La identidad de app en carpetas es `package|user`, sin actividad. Varias actividades del mismo
  paquete se colapsan en una.
- La pila de navegación no se conserva ante recreación por cambio de configuración.
- Las referencias a apps desinstaladas o shortcuts desanclados se toleran y se ignoran.
- Solo hay traducciones nuevas en inglés y español; el resto de locales usa inglés.
- Persisten 22 errores de lint preexistentes ajenos a las carpetas.

## Pruebas

- 46 pruebas unitarias, incluidas las de recorrido de subárboles para la búsqueda.
- `assembleDebug` correcto tras MOD-010/MOD-011; `assembleRelease` correcto en la validación
  anterior.
- Pruebas manuales de creación, selección, anidamiento, renombrado, retirada, escritorio, swipe,
  persistencia y navegación.
- Pendiente una pasada completa de aceptación y la validación manual de MOD-010 y MOD-011.

## Commits de referencia

`7a7eb43`, `163128b`, `095ba5c`, `452cd3d`, `2b6c64f`, `c55f5a9`, `dac1090`, `6fc1019`,
`0dc1c57`, `c1ea348`. MOD-010 y MOD-011 están pendientes de commit.
