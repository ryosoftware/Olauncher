# Instrucciones permanentes para el mantenimiento del fork de Olauncher

Este repositorio es una versión modificada de Olauncher que incorpora funcionalidades adicionales, especialmente un sistema avanzado de gestión de carpetas.

## Objetivo principal

Mantener las funcionalidades propias del fork y permitir incorporar periódicamente las actualizaciones del repositorio original de Olauncher.

La prioridad es conservar las funcionalidades originales y las modificaciones propias, minimizando los conflictos y evitando perder cambios.

## Documentación obligatoria

Antes de modificar código, consulta:

- `FOLDER_FEATURE.md`: especificación funcional de las carpetas y anexo de implementación.
- `FORK_MAINTENANCE.md`: registro técnico de las modificaciones realizadas.
- `MIGRATION.md`: registro histórico del proceso de migración desde el fork de Android/Projects.
- `UPSTREAM_SYNC.md`: procedimiento de sincronización con el repositorio original.

Consulta también `AGENTS.md` y cualquier otra instrucción aplicable.

## Primera implementación

Cuando implementes una funcionalidad nueva:

1. Analiza el código original.
2. Identifica todos los archivos que necesitan modificaciones.
3. Documenta las decisiones técnicas.
4. Implementa los cambios con el mínimo impacto posible.
5. Actualiza la documentación técnica.
6. Registra los archivos modificados y el motivo de cada modificación.
7. Documenta las pruebas realizadas.

No consideres terminada una funcionalidad hasta que:

1. Su documentación permita entender y mantener los cambios sin depender del historial de esta conversación.
2. El correcto funcionamiento haya sido confirmado por el agente humano.

## Sincronización con el repositorio original

Cuando el usuario solicite incorporar cambios del repositorio original:

1. Identifica el commit de referencia del fork y el estado actual del repositorio original.
2. Analiza los commits nuevos del repositorio original.
3. Identifica los archivos modificados.
4. Comprueba si esos archivos han sido modificados también por el fork.
5. Determina cómo afectan los cambios originales a las funcionalidades añadidas.
6. Incorpora los cambios originales preservando las funcionalidades propias.
7. Resuelve los conflictos de forma consciente, no simplemente conservando una de las dos versiones.
8. Ejecuta las pruebas y comprobaciones disponibles.
9. Actualiza la documentación técnica y los procedimientos de mantenimiento.

No elimines funcionalidades del fork para facilitar una sincronización.

No sobrescribas cambios originales sin comprender su finalidad.

No realices refactorizaciones innecesarias durante una sincronización.

Si existe un conflicto cuya resolución pueda alterar el comportamiento esperado, documenta el problema y solicita confirmación antes de tomar una decisión irreversible.

No hagas commit. Los commit los hace el humano al cargo.

## Mantenimiento de la documentación

La documentación debe evolucionar junto con el código.

Cada modificación debe reflejarse en `FORK_MAINTENANCE.md`, incluyendo:

- Funcionalidad afectada.
- Archivos modificados.
- Motivo del cambio.
- Diferencias respecto al comportamiento original.
- Dependencias con otras modificaciones.
- Pruebas necesarias.

Si se descubre que la documentación está desactualizada, corrígela.

## Reproducibilidad

El objetivo es que una IA que no haya participado en conversaciones anteriores pueda:

- Comprender las funcionalidades añadidas.
- Identificar las diferencias con Olauncher original.
- Reproducir los cambios cuando sea necesario.
- Adaptar las modificaciones a nuevas versiones del repositorio original.
- Verificar que el resultado mantiene el comportamiento esperado.

Toda la información necesaria debe estar almacenada en el repositorio, no depender de la memoria de la IA.
