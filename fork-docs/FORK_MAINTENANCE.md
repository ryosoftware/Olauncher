# Guía de mantenimiento del fork

## 1. Objetivo

Este documento describe las modificaciones propias introducidas en este fork de Olauncher respecto al repositorio original.

Su objetivo es garantizar que:

- Las funcionalidades añadidas se conserven al incorporar actualizaciones del repositorio original.
- Los cambios realizados puedan entenderse sin depender de conversaciones anteriores con una IA.
- Las modificaciones puedan reproducirse o adaptarse cuando sea necesario.
- El fork permanezca lo más próximo posible al proyecto original.
- El mantenimiento y la sincronización puedan realizarse de forma incremental.

Este documento es una referencia técnica viva y debe actualizarse siempre que se añada, modifique o elimine una funcionalidad propia.

## 2. Principios fundamentales de mantenimiento

1. **Cambios mínimos:** modificar el menor número posible de archivos.
2. **Compatibilidad con upstream:** conservar la arquitectura y el comportamiento originales siempre que sea posible.
3. **Sin refactorizaciones innecesarias:** no reorganizar el código existente salvo que sea estrictamente necesario.
4. **Separación de funcionalidades:** mantener las funcionalidades propias diferenciadas de las originales siempre que resulte práctico.
5. **Reproducibilidad:** documentar suficientemente cada modificación importante para poder comprenderla y reproducirla.
6. **Prevención de regresiones:** garantizar que las funcionalidades originales sigan funcionando.
7. **Implementación incremental:** preferir cambios pequeños que puedan verificarse de forma independiente.
8. **Documentación sincronizada:** la documentación debe reflejar siempre el estado real del código.

## 3. Información del repositorio

Datos comprobados en Git y en la configuración real del proyecto a 2026-11-13.

| Propiedad | Valor |
|---|---|
| Proyecto original | Olauncher |
| Repositorio original (upstream) | `https://github.com/tanujnotes/Olauncher.git` |
| Repositorio del fork | Pendiente de publicar / URL no configurada |
| Ruta local | `/home/roberto/pp/Olauncher` |
| Rama principal de desarrollo | `master` |
| Rama de upstream | `origin/master` (el remoto `origin` apunta a upstream) |
| Commit original de referencia | `2606973` (v6.11.5) |
| Último commit de upstream integrado | `2606973` (no se ha sincronizado nada posterior) |
| Commit actual del fork | `3836b32` (MOD-010 y MOD-011 incluidas) |
| Lenguaje principal | Kotlin |
| Sistema de construcción | Gradle 8.9.1, Android Gradle Plugin 8.9.1 |
| SDK | compileSdk 36, minSdk 24, targetSdk 36 |
| Versión actual en el árbol de trabajo | versionCode 117, versionName `v6.11.5-3` |

El fork está **14 commits por delante** de `2606973`, incluidos MOD-010 y MOD-011 (`3836b32`). No existe remoto `upstream` separado; `origin` sigue apuntando al repositorio original.

## 4. Resumen de funcionalidades propias

| Funcionalidad | Estado | Documentación |
|---|---|---|
| Gestión avanzada de carpetas | Implementada; validación manual en curso | `../FOLDER_FEATURE.md` |

Estados posibles: Planificada, En desarrollo, Implementada, Probada, Necesita adaptación, Obsoleta.

## 5. Registro de modificaciones propias

| ID | Modificación | Estado | Archivos principales | Commits |
|---|---|---|---|---|
| MOD-001 | Núcleo de datos y persistencia de carpetas | Implementada / probada | `folders/FolderStore.kt`, `FolderRepository.kt`, `FolderItem.kt`, `FolderContent.kt`, tests | `7a7eb43` |
| MOD-002 | Visualización, ordenación y navegación en el App Drawer | Implementada / probada | `FolderDrawerController.kt`, `AppDrawerAdapter.kt`, `AppDrawerFragment.kt`, layouts, `nav_graph.xml` | `163128b` |
| MOD-003 | Creación, selector, renombrado y retirada de carpetas | Implementada / probada | `FolderPickerFragment.kt`, `FolderPickerAdapter.kt`, `FolderDialogs.kt`, `FolderArgs.kt`, recursos | `095ba5c`, `2b6c64f` |
| MOD-004 | Carpetas en escritorio y acciones de swipe | Implementada / probada | `HomeFragment.kt`, `FolderRepository.kt` | `452cd3d` |
| MOD-005 | Correcciones de refresco, transición y scroll | Implementada / probada | `AppDrawerFragment.kt`, `AppDrawerAdapter.kt`, `adapter_app_drawer.xml` | `c55f5a9` |
| MOD-006 | `showFoldersFirst` por defecto `false` | Decisión de producto | `FolderRepository.kt` | `dac1090` |
| MOD-007 | Optimización del renderizado de contenido de carpetas | Probada por compilación; validación manual pendiente | `AppDrawerFragment.kt`, `FolderDrawerController.kt` | `6fc1019` |
| MOD-008 | Ocultar el header del buscador dentro de carpetas | Obsoleta; revertida por MOD-011 | `AppDrawerFragment.kt`, `fragment_app_drawer.xml`, `layout-land/fragment_app_drawer.xml` | `0dc1c57` |
| MOD-009 | Mover una carpeta anidada a la raíz desde el selector | Implementada / probada | `FolderStore.kt`, `FolderRepository.kt`, `FolderPickerFragment.kt`, `FolderPickerAdapter.kt`, `strings_folders.xml` | `c1ea348` |
| MOD-010 | Preferencia interna de teclado automático dentro de carpetas | Implementada; validación manual pendiente | `FolderRepository.kt`, `FolderDrawerController.kt`, `AppDrawerFragment.kt` | `3836b32` |
| MOD-011 | Búsqueda recursiva en carpetas y subcarpetas | Implementada; validación manual pendiente | `FolderStore.kt`, `FolderDrawerController.kt`, `AppDrawerAdapter.kt`, `AppDrawerFragment.kt`, tests | `3836b32` |
| MOD-012 | Eliminar warning de `RecyclerView.scrollTo` en las transiciones del App Drawer | Implementada; validación manual pendiente | `res/layout/fragment_app_drawer.xml`, `res/layout-land/fragment_app_drawer.xml`, `app/build.gradle` | `5719143` |

### Detalles relevantes

- **MOD-001:** jerarquía de un solo padre; contador de referencias derivado de padre/raíz, escritorio y swipe; serialización `folders-v1`; autocorrección de ciclos, huérfanas y referencias colgantes.
- **MOD-002:** las carpetas se representan como `AppModel.App` con marcador `folder:<id>` para no modificar `AppModel` ni `DiffUtil`; pila de navegación en `FolderDrawerController`; el título muestra solo el nombre de la carpeta actual.
- **MOD-003:** selector a pantalla completa con rutas completas y «New folder» al final; creación en raíz; reutilización de nombres duplicados por nivel; etiqueta dinámica Quitar/Borrar; el pinned-shortcut solo se desancla desde la raíz.
- **MOD-004:** referencias de escritorio/swipe derivadas de los slots reales; apertura directa de la carpeta desde el App Drawer.
- **MOD-005:** se refresca el contenido al cambiar nombres; se enlazan filas con la lista comprometida del adapter; el scroll se aplica tras el commit de la lista.
- **MOD-006:** el fork usa por defecto orden mixto (`false`), mientras la especificación proponía `true`.
- **MOD-007:** se elimina el escaneo completo de apps al abrir carpeta; se reutilizan `currentAppList` y `viewModel.hiddenApps`; se evita el filtro redundante y se cachea el `Collator`.
- **MOD-008:** quedó obsoleta con MOD-011. Ocultaba la fila del buscador dentro de carpetas para ganar altura; ahora la barra vuelve a mostrarse para permitir la búsqueda recursiva. El nombre de la carpeta sigue como título superior.
- **MOD-009:** el selector muestra «Mover a la raíz» como primera fila solo cuando el elemento es una carpeta anidada. Reutiliza `moveFolder(id, null)`, que traspasa la pertenencia jerárquica de forma atómica. Si ya existe una carpeta homónima en la raíz, devuelve `NAME_CONFLICT` y no modifica nada. «New folder» sigue al final.
- **MOD-010:** clave `FOLDERS_SHOW_KEYBOARD_IN_FOLDERS` en el mismo `SharedPreferences`, expuesta por `FolderRepository.showKeyboardInFolders` y `FolderDrawerController.showKeyboardInFolders`. Valor por defecto `false`. Solo tiene efecto combinada con `autoShowKeyboard`: el teclado automático dentro de una carpeta requiere ambas. Sin interfaz de usuario.
- **MOD-011:** la búsqueda incluye el contenido de las carpetas del nivel actual y de todas sus subcarpetas, además de las filas de carpeta de esos subniveles. `FolderStore.subtreeFolderIds/subtreeItemIds` recorren el subárbol; `FolderDrawerController.searchItems` construye el corpus deduplicado por identidad. `AppDrawerAdapter` filtra sobre ese corpus y mantiene la lista visible cuando la consulta está vacía. La barra de búsqueda vuelve a mostrarse dentro de carpetas (revierte MOD-008).
- **MOD-012:** la raíz del App Drawer conserva `animateLayoutChanges="true"`. Al ser el `RecyclerView` hijo directo, cada `LayoutTransition` (abrir/cerrar carpeta, cambio de visibilidad de `folderPath`, refresco de lista) llamaba a `View.scrollTo()` sobre él y `RecyclerView` registraba `RecyclerView does not support scrolling to an absolute position`. Se intercala un `FrameLayout` envolvente que recibe la animación; el RecyclerView queda como nieto y deja de recibir `scrollTo`. No cambia el comportamiento ni las animaciones. El mismo commit incluye el bump de versión a `v6.11.5-3`.

## 6. Arquitectura y decisiones de implementación

- **Separación por paquete:** toda la lógica nueva vive en `app/src/main/java/app/olauncher/folders/`.
- **Un solo padre:** el padre se deriva de la lista de hijos. No se almacena un contador de referencias; se calcula a partir de padre existente y referencias externas. Es equivalente al contador persistente de la especificación y evita desincronización.
- **Persistencia sin tocar `Prefs`:** `FolderRepository` usa el mismo `SharedPreferences` (`app.olauncher`) con claves propias: `FOLDERS_DATA`, `FOLDERS_ALLOW_DUPLICATE_ITEMS`, `FOLDERS_SHOW_FOLDERS_FIRST`, `FOLDERS_SHOW_KEYBOARD_IN_FOLDERS`.
- **Referencias externas:** se reconstruyen en cada carga desde los slots reales de escritorio y swipe; las claves legacy no se usan.
- **Representación de fila de carpeta:** `AppModel.App` con `appPackage = folder:<id>`. Se evita modificar `AppModel`, `DiffUtil`, `MainViewModel`, `Prefs` y otras clases originales.
- **Navegación:** pila interna en el fragmento, con `OnBackPressedCallback`; al volver del selector se restaura la ruta mediante `savedStateHandle`.
- **Contenido de carpeta:** se resuelve con `currentAppList + viewModel.hiddenApps`; no se vuelve a ejecutar `getAppsList` al abrir carpetas.
- **Selector:** `FolderPickerFragment` reutiliza el layout visual del App Drawer; las rutas se muestran completas y se excluyen destinos que crearían ciclos.
- **Búsqueda:** en raíz, selector y dentro de carpetas. Desde cualquier nivel busca en los elementos de ese nivel y en el contenido de sus carpetas y subcarpetas; el corpus se resuelve con `apps + hiddenApps` y se deduplica por identidad.
- **Teclado en carpetas:** `autoShowKeyboard` sigue siendo la preferencia global; `FOLDERS_SHOW_KEYBOARD_IN_FOLDERS` decide si el teclado se muestra automáticamente dentro de carpetas. Por defecto no.
- **Pinned-shortcuts:** Android solo muestra al launcher los shortcuts fijados por él mismo. Los fijados por otro launcher requieren volver a fijarse desde la app de origen.

## 7. Inventario de archivos modificados

Comprobado con `git diff 2606973..HEAD` y el árbol de trabajo.

| Archivo | Funcionalidad | Motivo | Riesgo de conflicto |
|---|---|---|---|
| `app/src/main/java/app/olauncher/folders/` (10 archivos) | Carpetas | Lógica nueva aislada | Bajo |
| `app/src/test/java/app/olauncher/folders/` (2 archivos) | Carpetas | Pruebas del modelo | Bajo |
| `ui/AppDrawerAdapter.kt` | Carpetas | Indicador, acciones, etiquetas, enlace de filas y corpus de búsqueda | Alto |
| `ui/AppDrawerFragment.kt` | Carpetas | Navegación, selector, teclado y refresco | Alto |
| `ui/HomeFragment.kt` | Carpetas | Carpetas en escritorio y swipe | Medio |
| `res/layout/adapter_app_drawer.xml` | Carpetas | Celda «Carpeta» | Medio |
| `res/layout/fragment_app_drawer.xml` y `layout-land/...` | Carpetas | Título de carpeta, `searchRow` y wrapper del RecyclerView (MOD-012) | Medio |
| `res/navigation/nav_graph.xml` | Carpetas | Destino del selector | Medio |
| `res/values/strings_folders.xml` y traducciones es | Carpetas | Cadenas nuevas | Bajo |
| `res/drawable/ic_folder.xml`, `ic_chevron_right.xml` | Carpetas | Iconos | Bajo |
| `res/layout/adapter_folder_picker_row.xml`, `dialog_folder_name.xml` | Carpetas | Selector y alta | Bajo |

MOD-010 (preferencia de teclado en carpetas) y MOD-011 (búsqueda recursiva) están confirmadas en `3836b32`; MOD-012 está confirmada en `5719143`. MOD-009 ya está confirmada en `c1ea348`.

## 8. Pruebas y prevención de regresiones

Comandos ejecutados:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew lintDebug
```

Resultados reales (2026-11-13, tras MOD-010 y MOD-011):

- `testDebugUnitTest`: **46 tests, BUILD SUCCESSFUL**. Incluyen jerarquía, ciclos, rutas, referencias, cascada, serialización, ordenación, política de duplicados, conflicto de nombre al mover a la raíz y recorrido de subárboles para la búsqueda.
- `assembleDebug`: **BUILD SUCCESSFUL**.
- `assembleRelease`: BUILD SUCCESSFUL en la validación anterior (2026-11-11); no se ha vuelto a ejecutar tras MOD-010/MOD-011.
- `lintDebug`: **22 errores y 106 avisos preexistentes** (`NewApi`, `MissingTranslation` originales, `UnsafeImplicitIntentLaunch`). No hay hallazgos en el paquete de carpetas, `AppDrawerAdapter.kt`, `AppDrawerFragment.kt` ni en los layouts del App Drawer.

Resultados reales (2026-11-14, tras MOD-012):

- `testDebugUnitTest`: **BUILD SUCCESSFUL**.
- `assembleDebug`: **BUILD SUCCESSFUL**.
- `lintDebug`: mismos **22 errores y 106 avisos** preexistentes; sin hallazgos en `fragment_app_drawer.xml` ni `layout-land/fragment_app_drawer.xml`.

Pruebas manuales realizadas durante el desarrollo:

- Crear, abrir, anidar, renombrar y quitar carpetas.
- Selector a pantalla completa con rutas y «New folder» al final.
- Añadir apps y pinned-shortcuts; políticas de duplicados.
- Carpetas en escritorio y swipe.
- Etiquetas Quitar/Borrar y menú de shortcuts.
- Persistencia y navegación Atrás.
- Barra de búsqueda visible dentro de carpetas y búsqueda de apps/shortcuts y filas de carpeta en subcarpetas.
- Teclado automático dentro de carpetas con la nueva preferencia activada y desactivada.
- Limitación observada: pinned-shortcuts fijados por otro launcher no son visibles para Olauncher.

Pendiente de validación manual: comportamiento final de MOD-010 y MOD-011 (teclado y búsqueda recursiva en dispositivos reales), comprobar en dispositivo que desaparece el warning `RecyclerView does not support scrolling...` con MOD-012 y una pasada completa de aceptación.

No existen pruebas instrumentadas en el proyecto.

## 9. Instrucciones para agentes de IA

Cualquier agente que trabaje en este repositorio debe:

1. Leer este documento antes de modificar funcionalidades propias.
2. Consultar `../FOLDER_FEATURE.md`.
3. Inspeccionar la implementación real antes de hacer suposiciones.
4. Comparar la implementación con el comportamiento documentado.
5. Actualizar este documento después de cambios significativos.
6. No asumir que propuestas anteriores coinciden con el código actual.
7. No descartar modificaciones propias porque upstream haya implementado algo similar.
8. Investigar si una implementación original puede sustituir o simplificar el código propio sin alterar el comportamiento requerido.
9. No crear commits; los realiza exclusivamente el usuario.

## 10. Historial de mantenimiento

| Fecha | Modificación | Funcionalidad | Commit |
|---|---|---|---|
| 2026-11-10 | Núcleo de datos y pruebas | Carpetas | `7a7eb43` |
| 2026-11-10 | Visualización y navegación | Carpetas | `163128b` |
| 2026-11-10 | Creación, selector y acciones | Carpetas | `095ba5c` |
| 2026-11-10 | Escritorio y swipe | Carpetas | `452cd3d` |
| 2026-11-10 | Ajustes de retirada y shortcuts | Carpetas | `2b6c64f` |
| 2026-11-10 | Correcciones de transición y refresco | Carpetas | `c55f5a9` |
| 2026-11-10 | `showFoldersFirst=false` por defecto | Carpetas | `dac1090` |
| 2026-11-10 | Optimización de renderizado de carpetas | Carpetas | `6fc1019` |
| 2026-11-10 | Ocultar header del buscador en carpetas | Carpetas | `0dc1c57` |
| 2026-11-10 | Documentación de estado actual | Infraestructura | `b89b930` |
| 2026-11-11 | Mover carpeta anidada a la raíz desde el selector | Carpetas | `c1ea348` |
| 2026-11-13 | Preferencia de teclado automático dentro de carpetas | Carpetas | `3836b32` |
| 2026-11-13 | Búsqueda recursiva en carpetas y subcarpetas | Carpetas | `3836b32` |
| 2026-11-14 | Eliminar warning de `RecyclerView.scrollTo` con wrapper | Carpetas | `5719143` |

## 11. Problemas conocidos y deuda técnica

- Lint mantiene 22 errores preexistentes ajenos a las carpetas.
- `allowDuplicateItems`, `showFoldersFirst` y `showKeyboardInFolders` no tienen interfaz de usuario, por especificación.
- `showFoldersFirst` usa `false` por defecto en el fork; la especificación proponía `true`.
- La búsqueda recursiva no muestra la ruta de la carpeta de origen de cada resultado; los resultados de subcarpetas aparecen como filas planas.
- Al abrir una carpeta desde un resultado de búsqueda, Atrás vuelve al nivel donde se estaba buscando, no a la carpeta padre real del resultado.
- La pila de navegación de carpetas es interna al fragmento y no se conserva ante recreación por configuración.
- Los pinned-shortcuts fijados por otro launcher no son visibles para Olauncher; hay que volver a fijarlos desde la app de origen.
- La identidad de app dentro de carpetas es `package|user`, sin actividad; varias actividades del mismo paquete se colapsan.
- Las referencias a apps desinstaladas o shortcuts desanclados se toleran y se ignoran hasta que vuelvan a existir.
- Las cadenas nuevas de carpetas solo están traducidas a inglés y español; el resto usa inglés.
- El código temporal de migración/exportación/importación fue eliminado.
- No hay benchmark formal del renderizado de carpetas; la mejora se basa en eliminar el escaneo completo y las entregas duplicadas.

## 12. Regla final de mantenimiento

Este documento debe describir el estado real del repositorio, no una implementación prevista o hipotética.

Cuando exista una discrepancia entre la documentación y el código, investigar su origen y actualizar la documentación únicamente después de determinar cuál es el comportamiento correcto.

### Restricción sobre commits de Git

La IA tiene prohibido crear commits, modificar commits existentes, hacer amend, rebase que reescriba commits o ejecutar cualquier operación que genere nuevos commits.

- Todos los commits del repositorio los realiza exclusivamente el usuario humano.
- La IA puede modificar archivos, ejecutar pruebas, revisar diferencias y preparar los cambios para que puedan ser confirmados.
- Cuando considere que los cambios están listos para un commit, debe indicárselo expresamente al usuario.
- Debe explicar brevemente qué cambios incluye ese posible commit y, si resulta útil, proponer un mensaje de commit.
- La IA debe esperar a que el usuario realice el commit. No debe intentar hacerlo por su cuenta, aunque el usuario haya pedido implementar una funcionalidad completa.
- Si necesita que el usuario haga un commit para poder continuar con seguridad, debe detenerse en ese punto, explicar el motivo y solicitarlo expresamente.

Esta restricción prevalece sobre cualquier instrucción genérica que sugiera realizar commits automáticamente.
