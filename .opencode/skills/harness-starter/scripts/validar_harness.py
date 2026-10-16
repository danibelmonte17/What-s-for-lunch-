#!/usr/bin/env python3
"""Valida el harness generado por harness-starter.

Uso: python validar_harness.py <raiz-del-proyecto>
Sale con código 1 si hay errores. Solo usa la biblioteca estándar.
"""
import json
import re
import shutil
import subprocess
import sys
from pathlib import Path

ARCHIVOS = [
    "AGENTS.md", "feature_list.json", "PROGRESS.md", "docs/progress-history.md",
    "init.sh", "init.ps1", "lint.sh", "lint.ps1", "unit.sh", "unit.ps1", "e2e.sh", "e2e.ps1",
]
SCRIPTS_SH = ["init.sh", "lint.sh", "unit.sh", "e2e.sh"]
SCRIPTS_PS1 = ["init.ps1", "lint.ps1", "unit.ps1", "e2e.ps1"]
# El historial es solo-añadir y almacena prosa con `<...>` legítimos: valida su
# existencia y ausencia de secretos, pero no lo escanees en busca de marcadores.
SIN_PLACEHOLDER = {"docs/progress-history.md"}
CATEGORIAS = {"funcional", "tecnica", "seguridad"}
CAMPOS = {"id", "category", "description", "depends_on", "steps", "verify", "status"}
ESTADOS = {"not_started", "ready", "in_progress", "passing", "validated"}
# id legible: minúsculas y guiones (registrar-cuenta); no vale F001 ni solo números
ID_OK = re.compile(r"[a-z][a-z0-9]*(-[a-z0-9]+)*")
ID_NUMERADO = re.compile(r"f?\d+")
UNE_COMPORTAMIENTOS = re.compile(r" y |/|,")
# Marcadores sin rellenar: <algo entre ángulos> o líneas "# Ejemplo:" de las plantillas
PLACEHOLDER = re.compile(r"<[A-Za-zÁÉÍÓÚáéíóúñ][^<>\n]{1,60}>|#\s*Ejemplo:")
SECRETO = re.compile(
    r"(?i)(api[_-]?key|secret|token|passw(or)?d)\s*[=:]\s*['\"]?[A-Za-z0-9/+_\-]{12,}"
    r"|sk-[A-Za-z0-9]{20,}|ghp_[A-Za-z0-9]{20,}|AKIA[0-9A-Z]{16}"
)
ETAPAS_SH = re.compile(r'^\s*echo\s+"==>\s*([^"]+)"', re.M)
ETAPAS_PS1 = re.compile(r'^\s*Write-Host\s+"==>\s*([^"]+)"', re.M)
PUERTOS = re.compile(r"\b\d{4,5}\b")
ARCHIVO_CITADO = re.compile(r"`((?:[\w.\-]+/)*[\w.\-]+\.(?:md|json|sh|ps1))`")

errores, avisos = [], []


def validar_features(raiz: Path) -> None:
    try:
        data = json.loads((raiz / "feature_list.json").read_text(encoding="utf-8"))
    except (OSError, ValueError) as e:
        errores.append(f"feature_list.json no es JSON válido: {e}")
        return
    feats = data.get("features") if isinstance(data, dict) else None
    if not feats or not isinstance(feats, list):
        errores.append("feature_list.json: falta la lista 'features' o está vacía")
        return
    vistos = set()
    en_curso = 0
    for i, f in enumerate(feats):
        fid = f.get("id", f"#{i}") if isinstance(f, dict) else f"#{i}"
        if not isinstance(f, dict):
            errores.append(f"{fid}: la funcionalidad no es un objeto")
            continue
        if set(f) != CAMPOS:
            errores.append(f"{fid}: campos incorrectos (faltan {sorted(CAMPOS - set(f))}, sobran {sorted(set(f) - CAMPOS)})")
            continue
        sid = str(f["id"])
        if not ID_OK.fullmatch(sid) or len(sid) > 50 or ID_NUMERADO.fullmatch(sid):
            errores.append(f"{fid}: el id debe ser un nombre legible en minúsculas con guiones (p. ej. 'registrar-cuenta'), máx. 50 caracteres")
        elif sid in vistos:
            errores.append(f"{fid}: id repetido")
        if f["category"] not in CATEGORIAS:
            errores.append(f"{fid}: category '{f['category']}' no está en {sorted(CATEGORIAS)}")
        if f["status"] not in ESTADOS:
            errores.append(f"{fid}: status '{f['status']}' no está en {sorted(ESTADOS)}")
        elif f["status"] in ("ready", "passing", "validated"):
            errores.append(f"{fid}: no puede crearse '{f['status']}': todas empiezan en not_started")
        elif f["status"] == "in_progress":
            en_curso += 1
        desc = str(f["description"]).strip()
        if not desc:
            errores.append(f"{fid}: description vacía")
        elif UNE_COMPORTAMIENTOS.search(desc):
            avisos.append(f"{fid}: la description tiene ' y ', '/' o comas; si une varios comportamientos, divídela (references/slicing.md)")
        for campo in ("steps", "verify"):
            v = f[campo]
            if not isinstance(v, list) or not v or not all(isinstance(x, str) and x.strip() for x in v):
                errores.append(f"{fid}: '{campo}' debe ser una lista no vacía de textos")
        deps = f["depends_on"]
        if not isinstance(deps, list):
            errores.append(f"{fid}: 'depends_on' debe ser una lista")
        else:
            for d in deps:
                if d not in vistos:
                    errores.append(f"{fid}: depende de '{d}', que no existe o aparece después")
        vistos.add(sid)
    if en_curso > 1:
        errores.append(f"hay {en_curso} funcionalidades in_progress; como máximo una")
    if len(feats) < 10:
        avisos.append(f"solo hay {len(feats)} funcionalidades: si el MVP no es trivial, haz otra pasada de slicing; si lo es, justifícalo en la sesión 0 de docs/progress-history.md")
    elif len(feats) > 30:
        avisos.append(f"hay {len(feats)} funcionalidades: revisa que no sean tareas de bajo nivel")


def validar_scripts(raiz: Path) -> None:
    # init.sh e init.ps1 comparten etapas (echo/Write-Host "==> Etapa") y puertos.
    sh = (raiz / "init.sh").read_text(encoding="utf-8")
    ps1 = (raiz / "init.ps1").read_text(encoding="utf-8")
    e_sh, e_ps1 = ETAPAS_SH.findall(sh), ETAPAS_PS1.findall(ps1)
    if not e_sh:
        errores.append('init.sh: no hay etapas con el formato echo "==> Etapa"')
    if e_sh != e_ps1:
        errores.append(f"init.sh e init.ps1 tienen etapas distintas: {e_sh} vs {e_ps1}")
    if not any("erific" in e for e in e_sh):
        errores.append("init.sh: falta la etapa de verificación")
    if set(PUERTOS.findall(sh)) != set(PUERTOS.findall(ps1)):
        errores.append("init.sh e init.ps1 usan puertos distintos")
    # Sintaxis de los cuatro scripts .sh y los cuatro .ps1.
    if shutil.which("bash"):
        for nombre in SCRIPTS_SH:
            r = subprocess.run(["bash", "-n", str(raiz / nombre)], capture_output=True, text=True)
            if r.returncode:
                errores.append(f"{nombre} no pasa 'bash -n': {r.stderr.strip()}")
    ps = shutil.which("pwsh") or shutil.which("powershell")
    if ps:
        for nombre in SCRIPTS_PS1:
            cmd = ("$e=$null;[void][System.Management.Automation.Language.Parser]::ParseFile("
                   f"'{raiz / nombre}',[ref]$null,[ref]$e);if($e){{$e;exit 1}}")
            r = subprocess.run([ps, "-NoProfile", "-Command", cmd], capture_output=True, text=True)
            if r.returncode:
                errores.append(f"{nombre} tiene errores de sintaxis: {r.stdout.strip()}")
    else:
        avisos.append("PowerShell no disponible: los .ps1 sin validar sintácticamente, revísalos a mano")


def validar_agents(raiz: Path) -> None:
    texto = (raiz / "AGENTS.md").read_text(encoding="utf-8")
    for nombre in sorted(set(ARCHIVO_CITADO.findall(texto))):
        if not (raiz / nombre).exists():
            errores.append(f"AGENTS.md cita `{nombre}`, que no existe: quítalo de la lista o créalo")


def validar_limpieza(raiz: Path) -> None:
    for nombre in ARCHIVOS:
        texto = (raiz / nombre).read_text(encoding="utf-8")
        if nombre not in SIN_PLACEHOLDER:
            for m in PLACEHOLDER.finditer(texto):
                errores.append(f"{nombre}: marcador sin rellenar '{m.group(0)}'")
        if SECRETO.search(texto):
            errores.append(f"{nombre}: parece contener una credencial o token")


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__)
        return 2
    raiz = Path(sys.argv[1]).resolve()
    faltan = [a for a in ARCHIVOS if not (raiz / a).is_file()]
    if faltan:
        print(f"ERROR: faltan archivos en {raiz}: {', '.join(faltan)}")
        return 1
    validar_features(raiz)
    validar_scripts(raiz)
    validar_agents(raiz)
    validar_limpieza(raiz)
    for a in avisos:
        print(f"AVISO: {a}")
    for e in errores:
        print(f"ERROR: {e}")
    print(f"{'FALLA' if errores else 'OK'}: {len(errores)} error(es), {len(avisos)} aviso(s)")
    return 1 if errores else 0


if __name__ == "__main__":
    sys.exit(main())
