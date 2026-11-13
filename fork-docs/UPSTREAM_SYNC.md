# Guía de sincronización con el repositorio original

## 1. Objetivo

Este documento define el procedimiento para incorporar al fork los cambios publicados en el repositorio original de Olauncher.

El objetivo principal es mantener el fork razonablemente sincronizado con upstream, conservando todas las funcionalidades propias.

La sincronización debe realizarse de forma cuidadosa e incremental, priorizando la compatibilidad con el proyecto original.

## 2. Reglas fundamentales

1. Nunca sobrescribir el fork con el repositorio original.
2. Nunca descartar funcionalidades propias para resolver conflictos más fácilmente.
3. Nunca asumir que una operación de merge o rebase conservará automáticamente el comportamiento esperado.
4. Analizar siempre los cambios de upstream antes de incorporarlos.
5. Identificar las dependencias entre los cambios originales y las modificaciones propias.
6. Conservar el comportamiento original de upstream salvo que exista un requisito documentado del fork que indique lo contrario.
7. Preferir las implementaciones originales cuando sean compatibles con los requisitos del fork.
8. Evitar cambios innecesarios en archivos ajenos a la sincronización.
9. Nunca resolver conflictos seleccionando automáticamente «ours» o «theirs» sin analizar las consecuencias.
10. No considerar completada una sincronización hasta haber verificado el resultado.

## 3. Configuración del repositorio y Git

Antes de la primera sincronización, inspeccionar el repositorio y verificar:

- El remoto de Git denominado `origin`.
- El remoto de Git denominado `upstream`.
- Las ramas locales.
- La rama actual.
- El último commit de upstream.
- El commit común a partir del cual divergen el fork y upstream.

Configuración esperada:

- `origin`: repositorio del fork personalizado.
- `upstream`: repositorio original de Olauncher.

### Estado actual (2026-11-13)

- `origin`: `https://github.com/tanujnotes/Olauncher.git` (upstream).
- No existe remoto `upstream` ni remoto del fork.
- Rama `master` en `ea701c2`, 12 commits por delante de `2606973`.
- Hay cambios locales sin commit: MOD-010 (teclado en carpetas) y MOD-011 (búsqueda recursiva).

Antes de la primera sincronización:

1. Renombrar `origin` a `upstream`.
2. Añadir el repositorio del fork como `origin` cuando exista URL.
3. Confirmar que el árbol de trabajo está limpio o los cambios están preservados.

Comandos de ejemplo:

```bash
git remote -v
git branch -vv
git status
git log --oneline --graph --decorate -20
```

Si no existe el remoto `upstream`, identificar la URL del repositorio original mediante fuentes fiables antes de añadirlo.

Nunca asumir una URL.

## 4. Comprobaciones previas a la sincronización

### 4.1. Comprobar el estado del repositorio

- Asegurarse de que el árbol de trabajo está limpio.
- Identificar modificaciones sin confirmar.
- Comprobar si hay trabajo pendiente.
- Confirmar la rama actual.
- Asegurarse de que el estado actual del fork está confirmado mediante commits o preservado de forma segura.

Nunca descartar modificaciones sin confirmar.

Si el árbol de trabajo no está limpio, detener el procedimiento y explicar qué debe guardarse o confirmarse antes de continuar.

### 4.2. Identificar los puntos de sincronización

Determinar:

- El ancestro común entre el fork y upstream.
- El último commit de upstream incorporado al fork.
- El HEAD actual de upstream.
- El HEAD actual del fork.

Utilizar el historial de Git y la información de merge-base. No depender exclusivamente de la documentación.

### 4.3. Revisar los cambios entrantes

Obtener los cambios de upstream e inspeccionar todos los commits posteriores al último commit integrado.

Revisar:

- Mensajes de los commits.
- Archivos modificados.
- Diferencias relevantes.
- Nuevas dependencias.
- Cambios en interfaces públicas.
- Cambios en modelos de datos.
- Cambios en persistencia.
- Cambios en el ciclo de vida de la aplicación y navegación.
- Cambios en componentes de interfaz.
- Cambios en la configuración de construcción.

No integrar cambios sin comprender previamente su posible impacto.

## 5. Análisis de impacto

Comparar los cambios entrantes de upstream con el registro de modificaciones propias de `FORK_MAINTENANCE.md`.

Clasificar cada cambio entrante:

| Categoría | Descripción |
|---|---|
| No relacionado | No afecta a las funcionalidades propias. |
| Compatible | Afecta a zonas modificadas, pero no entra en conflicto con su comportamiento. |
| Solapado | Modifica los mismos archivos o componentes que alguna funcionalidad propia. |
| Conflictivo | Introduce un comportamiento incompatible con los requisitos del fork. |
| Potencialmente incompatible | Puede invalidar supuestos, persistencia o interfaces utilizadas por funcionalidades propias. |

Para cada cambio solapado, conflictivo o potencialmente incompatible:

1. Identificar la funcionalidad propia afectada.
2. Explicar las consecuencias sobre su comportamiento.
3. Determinar si se puede reutilizar la implementación original.
4. Definir las adaptaciones necesarias.
5. Identificar las pruebas necesarias para validar el resultado.

Actualizar el análisis de impacto antes de realizar modificaciones arriesgadas.

## 6. Estrategia de sincronización

Seleccionar la estrategia más segura según el historial del repositorio.

Preferir la sincronización mediante merge normal cuando sea posible, conservando el historial de commits existente del fork.

Utilizar rebase únicamente cuando se solicite expresamente o exista una razón técnica clara y se comprendan sus implicaciones para las ramas compartidas.

Nunca reescribir el historial publicado sin autorización expresa.

Evitar sustituir el proyecto completo por una copia nueva de upstream.

## 7. Resolución de conflictos

Cuando aparezcan conflictos:

1. Identificar la finalidad del cambio original.
2. Identificar la finalidad de la modificación propia.
3. Comprender el comportamiento esperado de ambos.
4. Implementar una solución combinada siempre que sea posible.
5. Conservar las mejoras originales que no contradigan los requisitos del fork.
6. Conservar las funcionalidades propias.
7. Minimizar el alcance de la resolución.
8. Documentar cualquier divergencia intencionada.

Si un conflicto requiere una decisión funcional ambigua, detenerse y solicitar aclaraciones antes de realizar cambios irreversibles.

No utilizar la resolución automática de conflictos como sustituto de la comprensión del código.

## 8. Validación de funcionalidades propias

Después de integrar los cambios, verificar todas las funcionalidades propias afectadas.

Para la gestión de carpetas, consultar `../FOLDER_FEATURE.md` y comprobar como mínimo:

- Creación de carpetas.
- Renombrado de carpetas.
- Eliminación de carpetas.
- Incorporación de aplicaciones a carpetas.
- Incorporación de pinned-shortcuts a carpetas.
- Movimiento de carpetas entre ubicaciones jerárquicas.
- Navegación por carpetas anidadas.
- Navegación mediante el botón Atrás.
- Ordenamiento de carpetas.
- Integración con el escritorio.
- Integración con acciones de deslizamiento.
- Contador de referencias.
- Persistencia después de reiniciar la aplicación.
- Prevención de relaciones circulares entre carpetas.
- Eliminación correcta de carpetas sin referencias activas.
- Búsqueda recursiva en el nivel actual y sus subcarpetas.
- Visibilidad de la barra de búsqueda dentro de carpetas.
- Teclado automático en carpetas según `autoShowKeyboard` y `showKeyboardInFolders`.

También deben verificarse las funcionalidades originales afectadas.

No asumir que una compilación correcta demuestra que todo funciona correctamente.

## 9. Construcción y verificación

Ejecutar los comandos reales de construcción y pruebas del proyecto.

Determinar los comandos correctos a partir de la configuración y documentación del repositorio.

Como mínimo, cuando estén disponibles:

- Compilar la aplicación.
- Ejecutar pruebas unitarias.
- Ejecutar pruebas de integración relevantes.
- Ejecutar análisis estático o comprobaciones de estilo.
- Verificar el empaquetado de la aplicación.

Registrar:

- Comandos ejecutados.
- Resultados.
- Errores.
- Pruebas no disponibles.
- Pruebas no ejecutadas.
- Riesgos pendientes.

Nunca informar como satisfactorias pruebas que no se hayan ejecutado.

## 10. Actualización de la documentación

Después de completar una sincronización, actualizar:

### FORK_MAINTENANCE.md

- Último commit de upstream integrado.
- Commit actual del fork.
- Inventario de archivos modificados.
- Registro de modificaciones propias.
- Decisiones de implementación que hayan cambiado.
- Nueva deuda técnica o problemas resueltos.

### Documentación de funcionalidades

Actualizar las especificaciones únicamente cuando haya cambiado realmente el comportamiento requerido.

### Este documento

Registrar las decisiones importantes y las lecciones aprendidas durante la sincronización.

## 11. Historial de sincronizaciones

Mantener un historial de las operaciones de sincronización.

| Fecha | Commit de upstream | Commit del fork | Resultado | Observaciones |
|---|---|---|---|---|
| 2026-11-10 | `2606973` | `0dc1c57` | Sin sincronizar | Base del fork; 9 commits propios; hay cambios locales pendientes |

## 12. Recuperación y reversión

Antes de una sincronización arriesgada:

- Preservar el estado actual del fork.
- Asegurarse de que el commit inicial pueda identificarse.
- Utilizar una rama específica para la sincronización.
- Mantener los cambios de sincronización separados de otros trabajos de desarrollo.

Si la integración falla:

1. No descartar el estado de trabajo actual.
2. Identificar el punto del fallo.
3. Preservar el trabajo útil realizado durante la resolución de conflictos.
4. Restaurar el último estado conocido como correcto si es necesario.
5. Documentar el fallo y los pasos de recuperación.

Nunca ejecutar operaciones destructivas de Git sin comprender sus consecuencias.

## 13. Instrucciones para agentes de IA

Cualquier agente de IA que realice una sincronización debe:

1. Leer `AGENTS.md`.
2. Leer `FORK_MAINTENANCE.md`.
3. Leer las especificaciones de las funcionalidades afectadas.
4. Leer este documento.
5. Inspeccionar el historial real de Git y el estado del árbol de trabajo.
6. Revisar los cambios entrantes de upstream antes de modificar archivos.
7. Conservar las funcionalidades propias.
8. Ejecutar las comprobaciones disponibles.
9. Actualizar toda la documentación afectada.
10. Presentar un informe final que describa qué se ha integrado, qué se ha adaptado y qué queda pendiente.

La IA no debe asumir que las decisiones tomadas en sincronizaciones anteriores siguen siendo válidas para futuras versiones de upstream.

Cada sincronización debe evaluarse en función de los cambios reales que se incorporan.

## 14. Criterios para considerar completada una sincronización

Una sincronización solo se considera satisfactoria cuando:

- Se han incorporado los commits de upstream previstos.
- Se han conservado las funcionalidades propias del fork.
- Los conflictos se han resuelto y revisado.
- La aplicación compila correctamente, siempre que el entorno lo permita.
- Se han ejecutado las pruebas relevantes disponibles.
- Las limitaciones pendientes están documentadas.
- El historial de Git y el árbol de trabajo se encuentran en un estado conocido y coherente.

Un merge de Git satisfactorio, por sí solo, no constituye una sincronización satisfactoria.

### Restricción sobre commits durante la sincronización

La IA no puede crear commits ni modificar el historial de Git durante el proceso de sincronización con upstream.

- Los commits los realiza exclusivamente el usuario humano.
- La IA puede preparar y resolver cambios, ejecutar comprobaciones y dejar el árbol de trabajo listo para que el usuario revise y confirme los resultados.
- Cuando una fase de sincronización esté lista para confirmarse, la IA debe indicárselo al usuario y explicar qué cambios quedarían incluidos.
- Puede proponer un mensaje de commit, pero nunca ejecutarlo.
- Si necesita que el usuario haga un commit antes de continuar, debe explicar por qué es necesario y esperar a que lo realice.
- No debe ejecutar `git commit`, `git commit --amend` ni ninguna otra operación que cree o reescriba commits.

Esta regla se aplica también a los commits de merge y a cualquier otra operación de integración de cambios upstream.

## 15. Notas específicas de la funcionalidad de carpetas

Archivos de mayor riesgo de conflicto con upstream:

- `ui/AppDrawerAdapter.kt`
- `ui/AppDrawerFragment.kt`
- `ui/HomeFragment.kt`
- `res/layout/adapter_app_drawer.xml`
- `res/layout/fragment_app_drawer.xml` y `res/layout-land/fragment_app_drawer.xml`
- `res/navigation/nav_graph.xml`

El resto de la funcionalidad está aislado en `app/src/main/java/app/olauncher/folders/` y en
recursos nuevos. Al sincronizar, revisar especialmente los enlaces con `AppModel`, `Prefs`,
`MainViewModel` y el ciclo de vida de `MainActivity` (el launcher vuelve a Home en `onStop`).

Estado de referencia para la validación:

- 46 pruebas unitarias.
- `assembleDebug` correcto; `assembleRelease` correcto en la validación anterior.
- Lint con 22 errores y 106 avisos preexistentes; sin hallazgos nuevos en carpetas ni en las
  clases modificadas por MOD-010/MOD-011.
- Búsqueda recursiva en carpetas y subcarpetas; barra visible dentro de carpetas.
- Teclado automático en carpetas controlado por `FOLDERS_SHOW_KEYBOARD_IN_FOLDERS` (por defecto
  `false`) además de `autoShowKeyboard`.
- Pinned-shortcuts: Android solo entrega al launcher los fijados por él mismo.

