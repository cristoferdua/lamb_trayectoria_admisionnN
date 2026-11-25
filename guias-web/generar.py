"""Genera la guia unica (sesiones S1-S16) pegando el codigo REAL y probado de las carpetas de ejemplo.

Uso (desde la carpeta guias-web):  python generar.py

  plantillas/sesiones/*.md  -> docs/sesiones/*.md      (guia unica: agregar un servicio a Lamb, con o sin CRUD)
  plantillas/proyecto-sello/*.md -> docs/proyecto-sello/*.md (guia: crear un servicio desde cero, con o sin CRUD)

  {{lamb:ruta}}  -> archivo de  Lamb-Modulo02-con-Biblioteca  (ejemplo probado)
  {{file:ruta}}  -> archivo de  Servicio-Desde-Cero
"""
import pathlib
import re
import sys

BASE = pathlib.Path(r"C:\Users\crist\Documents\lamb uopeu 2.0")
COPIA = BASE / "Lamb-Modulo02-con-Biblioteca"
ORIGEN = {"lamb": COPIA, "file": BASE / "Servicio-Desde-Cero"}
AQUI = pathlib.Path(__file__).parent
WEB = AQUI / "docs"
PLANT = AQUI / "plantillas"

LANG = {".yml": "yaml", ".yaml": "yaml", ".json": "json", ".xml": "xml", ".java": "java",
        ".sql": "sql", ".html": "html", ".ts": "ts", ".gitignore": "text"}


# El ejemplo probado se llama "biblioteca" con CRUD "Libro" (titulo, autor); la guia lo muestra como
# servicio "evaluacionn" con CRUD "Examen" (titulo, curso). Se aplica solo al codigo de {{lamb:...}}.
RENOMBRE = [
    (r"BIBLIOTECARIO", "EVALUADOR"), (r"Bibliotecario", "Evaluador"), (r"bibliotecario", "evaluador"),
    (r"BIBLIOTECA", "EVALUACIONN"), (r"Biblioteca", "Evaluacionn"), (r"biblioteca", "evaluacionn"),
    (r"LIBROS", "EXAMENES"), (r"LIBRO", "EXAMEN"),
    (r"Libros", "Examenes"), (r"Libro", "Examen"),
    (r"libros", "examenes"), (r"libro", "examen"),
    (r"(?<![A-Za-z])autor(?![a-z])", "curso"), (r"Autor(?![a-z])", "Curso"),
    (r"El principito", "Parcial 1"), (r"\(ilustrado\)", "(recuperacion)"),
    (r"Antoine de Saint-Exup\S*?ry|Saint-Exup\S*?ry", "Matematica"),
]


def renombrar(txt: str) -> str:
    for rx, nuevo in RENOMBRE:
        txt = re.sub(rx, nuevo, txt)
    return txt


def bloque(tipo: str, rel: str) -> str:
    p = ORIGEN[tipo] / rel
    txt = p.read_text(encoding="utf-8").replace("\r\n", "\n").rstrip("\n")
    if tipo == "lamb":
        txt = renombrar(txt)
    lang = LANG.get(p.suffix or p.name, "text")
    return f"```{lang}\n{txt}\n```"


# --- "Qué cambiar en este código": se arma solo, leyendo el código que se va a pegar ---------------
# (regex, texto). El texto dice qué es y da un ejemplo. Se cuentan las apariciones en el código.
CAMBIOS = {
    "sesiones": {
        "ejemplo": "si tu servicio es **Tesorería** y tu CRUD es **Pagos** con `monto` y `fecha`",
        "reglas": [
            (r"evaluacionn", "`evaluacionn` → el nombre de **tu servicio**, en minúscula. Ej.: `tesoreria`"),
            (r"Evaluacionn", "`Evaluacionn` → el nombre de **tu servicio**, con mayúscula. Ej.: `Tesoreria`"),
            (r"EVALUACIONN", "`EVALUACIONN` → el nombre de **tu servicio**, en mayúsculas. Ej.: `TESORERIA`"),
            (r"examen(es)?(?![a-z])", "`examen` / `examenes` → el nombre de **tu CRUD**, en minúscula (singular / plural). Ej.: `pago` / `pagos`"),
            (r"Examen(es)?(?![a-z])", "`Examen` → el nombre de **tu CRUD**, con mayúscula. Ej.: `Pago`"),
            (r"(?i)(?<![a-z])(titulo|curso)(?![a-z])",
             "`titulo`, `curso` → **tus atributos** (uno por cada atributo que te pidan). Ej.: `monto`, `fecha`. "
             "El tipo de cada uno está en la [tabla 6.1 de la S1](S01_Construccion_Servicio_Base.md#61-traduce-los-atributos-que-te-piden)"),
            (r"(?<!\d)8095(?!\d)", "`8095` → el puerto de **tu servicio**"),
            (r"(?<!\d)8096(?!\d)", "`8096` → el puerto de la **2da instancia** de tu servicio"),
            (r"(?<!\d)15460(?!\d)", "`15460` → el puerto de la **base de datos** de tu servicio"),
        ],
        "atributos": r"(?i)examen",
    },
    "proyecto-sello": {
        "ejemplo": "si tu servicio es **tienda** y tu CRUD es **Alumnos** con `dni` y `fechaNacimiento`",
        "reglas": [
            (r"mi-servicio", "`mi-servicio` → el nombre de **tu servicio**. Ej.: `tienda`"),
            (r"mi_servicio", "`mi_servicio` → el nombre de **tu servicio** con guion bajo (base de datos). Ej.: `tienda`"),
            (r"productos?(?![a-z])", "`producto` / `productos` → el nombre de **tu CRUD**, en minúscula (singular / plural). Ej.: `alumno` / `alumnos`"),
            (r"Productos?(?![a-z])", "`Producto` → el nombre de **tu CRUD**, con mayúscula. Ej.: `Alumno`"),
            (r"(?i)(?<![a-z])(nombre|precio)(?![a-z])",
             "`nombre`, `precio` → **tus atributos** (uno por cada atributo que te pidan). Ej.: `dni`, `fechaNacimiento`. "
             "El tipo de cada uno está en la [tabla 6.1](#61-traduce-los-atributos-que-te-piden)"),
            (r"(?<!\d)8100(?!\d)", "`8100` → el puerto de **tu servicio** (solo si está ocupado)"),
        ],
        "atributos": r"(?i)producto",
        "nota": "`com.ejemplo.miservicio` y `MiServicio` **no hace falta cambiarlos** (es opcional, ver sección 11).",
    },
}
SIN_LISTA = {"powershell", "bash", "mermaid", ""}
SOLO = re.compile(r"^(S0[1-9]|S1[01])_|^servicio-desde-cero")


def que_cambiar(codigo: str, cfg: dict) -> str:
    items = []
    for rx, texto in cfg["reglas"]:
        if "titulo" in rx or "precio" in rx:
            if not re.search(cfg["atributos"], codigo):
                continue  # sin CRUD en este código: esas palabras no son atributos
        n = len(re.findall(rx, codigo))
        if n:
            veces = f"aparece {n} {'vez' if n == 1 else 'veces'}"
            items.append("- " + texto.replace("` → ", f"` ({veces}) → ", 1) + ".")
    if not items:
        return "**Qué cambiar en este código:** nada, se pega tal cual.\n"
    out = f"**Qué cambiar en este código** (ejemplo: {cfg['ejemplo']}):\n\n" + "\n".join(items) + "\n"
    if cfg.get("nota") and re.search(r"miservicio|MiServicio", codigo):
        out += f"- {cfg['nota']}\n"
    out += "- Todo lo demás se copia **igual**. Tip: `Ctrl+H` en VS Code, con **Aa** activado (respeta mayúsculas).\n"
    return out


def agregar_cambios(txt: str, carpeta: str) -> str:
    cfg = CAMBIOS[carpeta]
    lineas, out = txt.split("\n"), []
    prosa, i, anterior_pega = [], 0, False
    while i < len(lineas):
        l = lineas[i]
        m = re.match(r"^```(\w*)\s*$", l)
        if not m:
            out.append(l); prosa.append(l); i += 1
            continue
        j = i + 1
        while j < len(lineas) and not lineas[j].startswith("```"):
            j += 1
        bloque_lineas = lineas[i:j + 1]
        out.extend(bloque_lineas)
        texto_previo = "\n".join(prosa)
        pega = bool(re.search(r"\b[Pp]ega\b", texto_previo)) or (anterior_pega and "### Camino" in texto_previo)
        if pega and m.group(1) not in SIN_LISTA:
            out.append("")
            out.extend(que_cambiar("\n".join(bloque_lineas[1:-1]), cfg).rstrip("\n").split("\n"))
            k = j + 1
            while k < len(lineas) and not lineas[k].strip():
                k += 1
            if k < len(lineas) and lineas[k] == '!!! note "Qué cambia"':
                lineas[k] = '!!! note "Más detalle"'
        anterior_pega = pega and m.group(1) not in SIN_LISTA
        prosa = []
        i = j + 1
    return "\n".join(out)


def render(f: pathlib.Path) -> str:
    txt = f.read_text(encoding="utf-8")
    txt = re.sub(r"\{\{(lamb|file):([^}]+)\}\}", lambda m: bloque(m.group(1), m.group(2)), txt)
    if re.search(r"\{\{(lamb|file):", txt):
        sys.exit(f"placeholder sin resolver en {f.name}")
    if SOLO.match(f.name):
        txt = agregar_cambios(txt, f.parent.name)
    return txt


for carpeta in ("sesiones", "proyecto-sello"):
    for f in sorted((PLANT / carpeta).glob("*.md")):
        destino = WEB / carpeta / f.name
        destino.parent.mkdir(parents=True, exist_ok=True)
        destino.write_text(render(f), encoding="utf-8", newline="\n")
        print("ok", destino)
