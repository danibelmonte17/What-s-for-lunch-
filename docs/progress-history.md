# Historial de progreso — What's for Lunch?

Entradas de sesión en orden cronológico (solo se añaden, nunca se borran). No se lee entero: consulta como mucho la última entrada si necesitas contexto.

### Sesión 0: harness inicial (harness-starter)
- Especificación completada (`docs/build-brief.md`, `docs/domain-model.md`, `docs/technical-discovery.md`, `DESIGN.md`); andamiaje operativo generado; sin código de aplicación.
- Archivos creados: AGENTS.md, feature_list.json, PROGRESS.md, docs/progress-history.md, init.sh, init.ps1, lint.sh, lint.ps1, unit.sh, unit.ps1, e2e.sh, e2e.ps1.
- Funcionalidades: 23, todas en `not_started`. El MVP (app Android de uso personal con datos locales) se ha troceado en 23 slices dentro del rango esperado de 12-30, sin necesidad de justificar la granularidad.
- Primera tarea recomendada: `arrancar-app-en-mis-recetas` (scaffold, tema desde los tokens de `uxdocs/DESIGN.md`, pantalla Mis Recetas con estado vacío e i18n es/en).
- Nota: `init.sh` / `init.ps1` no pueden completarse hasta que exista el scaffold Android (no hay `gradlew` todavía); los scripts lo detectan y terminan con un error que lo explica. Al crear el scaffold, el applicationId se determina leyendo el APK generado (sin fijarlo en los scripts; se puede forzar con la variable de entorno PKG).
