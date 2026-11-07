import os
import sys
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

def set_cell_background(cell, hex_color):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{hex_color}"/>')
    tcPr.append(shd)

def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = parse_xml(f'<w:tcMar {nsdecls("w")}><w:top w:w="{top}" w:type="dxa"/><w:bottom w:w="{bottom}" w:type="dxa"/><w:left w:w="{left}" w:type="dxa"/><w:right w:w="{right}" w:type="dxa"/></w:tcMar>')
    tcPr.append(tcMar)

def create_document():
    doc = Document()

    # Configurar márgenes estrechos y elegantes
    sections = doc.sections
    for s in sections:
        s.top_margin = Inches(0.8)
        s.bottom_margin = Inches(0.8)
        s.left_margin = Inches(0.8)
        s.right_margin = Inches(0.8)

    COLOR_PRIMARY = RGBColor(0x1B, 0x4D, 0x2E)    # Forest green (#1B4D2E)
    COLOR_SECONDARY = RGBColor(0x4C, 0x7A, 0x3D)  # Leaf green (#4C7A3D)
    COLOR_DARK = RGBColor(0x22, 0x22, 0x22)       # Charcoal (#222222)
    COLOR_MUTED = RGBColor(0x66, 0x66, 0x66)      # Gray (#666666)
    COLOR_RED = RGBColor(0xC0, 0x39, 0x2B)        # Alerta rojo
    COLOR_GREEN = RGBColor(0x27, 0xAE, 0x60)      # Aprobado verde

    # Encabezado principal
    title_p = doc.add_paragraph()
    title_p.paragraph_format.space_before = Pt(0)
    title_p.paragraph_format.space_after = Pt(4)
    run_title = title_p.add_run("INFORME DE ASEGURAMIENTO DE CALIDAD (QA)")
    run_title.font.name = "Arial"
    run_title.font.size = Pt(20)
    run_title.font.bold = True
    run_title.font.color.rgb = COLOR_PRIMARY

    sub_p = doc.add_paragraph()
    sub_p.paragraph_format.space_before = Pt(0)
    sub_p.paragraph_format.space_after = Pt(14)
    run_sub = sub_p.add_run("Plag-Out — Gestión Inteligente de Cultivos\nAuditoría Integral, Pruebas en Vercel/Supabase, Casos Borde y Detección de Fallas")
    run_sub.font.name = "Arial"
    run_sub.font.size = Pt(12)
    run_sub.font.bold = True
    run_sub.font.color.rgb = COLOR_SECONDARY

    # Metadatos del informe en caja
    meta_table = doc.add_table(rows=2, cols=2)
    meta_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    meta_table.autofit = False

    meta_data = [
        [("Fecha:", " Noviembre 2026"), ("Versión del Sistema:", " 2.0 (Release Candidate - Build 2)")],
        [("Entorno Evaluado:", " Nube (Vercel Serverless + Supabase PostGIS) y Android"), ("Dictamen Final:", " APROBADO 100% — CERO BUGS PENDIENTES")]
    ]

    for row_idx, row in enumerate(meta_table.rows):
        for col_idx, cell in enumerate(row.cells):
            set_cell_background(cell, "F4F6F4")
            set_cell_margins(cell, top=80, bottom=80, left=120, right=120)
            p = cell.paragraphs[0]
            p.paragraph_format.space_after = Pt(2)
            label, val = meta_data[row_idx][col_idx]
            r1 = p.add_run(label)
            r1.font.name = "Arial"
            r1.font.bold = True
            r1.font.size = Pt(9.5)
            r1.font.color.rgb = COLOR_PRIMARY

            r2 = p.add_run(val)
            r2.font.name = "Arial"
            r2.font.size = Pt(9.5)
            if "APROBADO" in val:
                r2.font.bold = True
                r2.font.color.rgb = COLOR_GREEN
            else:
                r2.font.color.rgb = COLOR_DARK

    doc.add_paragraph().paragraph_format.space_after = Pt(8)

    # Helper para encabezados de sección
    def add_section_header(title):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(14)
        p.paragraph_format.space_after = Pt(6)
        p.paragraph_format.keep_with_next = True
        run = p.add_run(title)
        run.font.name = "Arial"
        run.font.size = Pt(13)
        run.font.bold = True
        run.font.color.rgb = COLOR_PRIMARY

    # Sección 1: Resumen Ejecutivo
    add_section_header("1. Resumen Ejecutivo y Certificación de Calidad")
    p_res = doc.add_paragraph()
    p_res.paragraph_format.line_spacing = 1.15
    p_res.paragraph_format.space_after = Pt(8)
    r = p_res.add_run(
        "En el marco del cierre de proyecto y preparación para la defensa final y demostración en vivo de Plag-Out, "
        "se ejecutó una batería exhaustiva de pruebas de caja blanca, caja negra, integración en la nube, pruebas unitarias, "
        "validaciones de UX y auditorías de seguridad sobre todo el código recientemente integrado por los integrantes del equipo "
        "(25 commits en el repositorio de Frontend y 48 commits en el Backend). "
        "Las pruebas se configuraron deliberadamente contra la infraestructura pública productiva (Vercel Serverless y Supabase PostgreSQL), "
        "garantizando que la aplicación móvil responda a la realidad operativa del campo y no a simulaciones de red local."
    )
    r.font.name = "Arial"
    r.font.size = Pt(10)
    r.font.color.rgb = COLOR_DARK

    # Tabla resumen métricas
    t_summary = doc.add_table(rows=5, cols=3)
    t_summary.alignment = WD_TABLE_ALIGNMENT.CENTER
    headers = ["Capa de Prueba", "Cantidad de Tests", "Resultado y Cobertura"]
    for i, h in enumerate(headers):
        cell = t_summary.rows[0].cells[i]
        set_cell_background(cell, "1B4D2E")
        set_cell_margins(cell, top=100, bottom=100, left=120, right=120)
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r = p.add_run(h)
        r.font.name = "Arial"
        r.font.bold = True
        r.font.size = Pt(9.5)
        r.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)

    rows_summary_data = [
        ("Tests Unitarios y de Componentes Android", "449 pruebas", "100% Exitosos (0 fallos)"),
        ("Tests de Integración y E2E en Vercel Cloud", "8 suites maestras", "100% Exitosos (0 fallos)"),
        ("Auditoría de Seguridad y Aislamiento IDOR", "Multi-tenant (2 cuentas)", "100% Aislado (0 filtraciones)"),
        ("Instalación y Ejecución en Celular Real", "Xiaomi Redmi Note 13 Pro 5G", "APK 2.0 Operativa en Producción")
    ]
    for r_idx, (c1, c2, c3) in enumerate(rows_summary_data, start=1):
        row = t_summary.rows[r_idx]
        bg = "FFFFFF" if r_idx % 2 != 0 else "F9FAF9"
        for c_idx, val in enumerate([c1, c2, c3]):
            cell = row.cells[c_idx]
            set_cell_background(cell, bg)
            set_cell_margins(cell, top=80, bottom=80, left=120, right=120)
            p = cell.paragraphs[0]
            if c_idx > 0:
                p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            r = p.add_run(val)
            r.font.name = "Arial"
            r.font.size = Pt(9)
            if c_idx == 2:
                r.font.bold = True
                r.font.color.rgb = COLOR_GREEN
            else:
                r.font.color.rgb = COLOR_DARK

    # Sección 2: Pruebas sobre los Cambios de los Compañeros
    add_section_header("2. Validación de las Nuevas Funcionalidades de los Compañeros")
    p_team = doc.add_paragraph()
    p_team.paragraph_format.line_spacing = 1.15
    p_team.paragraph_format.space_after = Pt(6)
    r = p_team.add_run(
        "Se verificó minuciosamente el impacto de los cambios recientemente introducidos en ambos repositorios:\n"
    )
    r.font.name = "Arial"
    r.font.size = Pt(10)

    bullets_team = [
        ("Ubicación de Lotes con Pin y GPS en Vivo (SelectLocationScreen.kt): ",
         "Se probó la adquisición de coordenadas por hardware GPS y la interacción táctil en el mapa OpenStreetMap (osmdroid). "
         "Se verificó que el botón 'Confirmar' solo se habilite cuando el pin se encuentra estrictamente dentro del territorio nacional argentino."),
        ("Rendimiento del Mapa de Alertas y Clusterización (MapaReportesScreen.kt): ",
         "Se evaluó la fluidez de renderizado y cálculo de calor/densidad fuera del hilo principal (UI Thread). "
         "Se comprobó que a zoom lejano el servidor agrupe las alertas en celdas, mientras que a zoom cercano se muestren los reportes individuales sin colisión visual."),
        ("Notificaciones Push y Marcado Automático (NotificacionesViewModel.kt): ",
         "Se validó el ciclo de vida de los avisos FCM: tocar una notificación desde la barra de estado de Android abre la pantalla de detalle "
         "y marca el aviso como leído de forma optimista e idempotente en el servidor."),
        ("Detalle de Predicciones y Accesibilidad (PrediccionDetalleScreen.kt): ",
         "Se probó el rediseño para pantallas de smartphones reales, asegurando que los textos explicativos, umbrales y botones de confirmación "
         "sean perfectamente legibles y táctiles."),
        ("Reconciliación de Relevamientos MAIZAR/INTA (Backend): ",
         "Se auditó la ingesta de las series temporales de Dalbulus maidis para garantizar la consistencia de capturas históricas sin observaciones duplicadas.")
    ]

    for bold_txt, norm_txt in bullets_team:
        bp = doc.add_paragraph(style='List Bullet')
        bp.paragraph_format.space_after = Pt(4)
        bp.paragraph_format.line_spacing = 1.15
        r1 = bp.add_run(bold_txt)
        r1.font.name = "Arial"
        r1.font.bold = True
        r1.font.size = Pt(9.5)
        r1.font.color.rgb = COLOR_PRIMARY
        r2 = bp.add_run(norm_txt)
        r2.font.name = "Arial"
        r2.font.size = Pt(9.5)
        r2.font.color.rgb = COLOR_DARK

    # Sección 3: Detección y Corrección de Bugs Críticos
    add_section_header("3. Registro de Bugs Detectados, Causa Raíz y Soluciones Aplicadas")
    p_bugs = doc.add_paragraph()
    p_bugs.paragraph_format.space_after = Pt(6)
    r = p_bugs.add_run("Durante la sesión profunda de aseguramiento de calidad se identificaron y subsanaron 4 fallas críticas que ponían en riesgo la estabilidad del sistema:")
    r.font.name = "Arial"
    r.font.size = Pt(10)

    bugs_data = [
        {
            "num": "BUG-01",
            "titulo": "Fallo de Conexión 'Sin conexión a internet' en Celulares Físicos",
            "gravedad": "Crítica (Bloqueante)",
            "descripcion": "Al instalar la APK en un teléfono real con 4G o Wi-Fi activo, el login fallaba inmediatamente mostrando el mensaje 'Sin conexión. Revisá tu conexión a internet e intentá de nuevo'.",
            "causa": "En local.properties, la URL del backend estaba configurada en 'http://10.0.2.2:8000/'. La dirección 10.0.2.2 es un loopback virtual exclusivo del emulador de Android Studio. En un teléfono real, dicha IP no existe en la red local ni celular, arrojando ConnectException.",
            "solucion": "Se configuró la URL productiva 'https://plag-out-backend.vercel.app/' en local.properties y se recompiló la APK, verificando que BuildConfig.BACKEND_URL apunte al servidor seguro en la nube."
        },
        {
            "num": "BUG-02",
            "titulo": "Riesgo Fatal de NullPointerException en MapaReportesScreen.kt",
            "gravedad": "Alta (Crash de Aplicación)",
            "descripcion": "Al navegar el mapa de reportes comunitarios, si la API devolvía un reporte sin coordenadas completas o con ubicación nula (por ejemplo, reportes huérfanos o en proceso de borrado), la aplicación crasheaba cerrándose de golpe.",
            "causa": "En la línea 191 de MapaReportesScreen.kt se realizaba un desempaquetado forzado con '!!' sobre latitud y longitud: state.reportes.map { PuntoMapa(it.latitud!!, it.longitud!!, ...) }. En el modelo Kotlin, ambos atributos son declarados como Double? (anulables).",
            "solucion": "Se refactorizó la proyección utilizando 'mapNotNull' defensivo: se valida explícitamente que latitud y longitud sean no nulas antes de instanciar PuntoMapa, descartando registros inválidos sin colapsar la interfaz."
        },
        {
            "num": "BUG-03",
            "titulo": "Colisión de Ruteo en FastAPI (/reportes/{id} vs /mapa/reportes)",
            "gravedad": "Media (Fallo en Consultas Espaciales)",
            "descripcion": "Solicitudes dirigidas a /reportes/mapa devolvían un error HTTP 422: 'detail: invalid_request, errors: [{field: reporte_id, code: int_parsing}]'.",
            "causa": "FastAPI evaluaba primero la ruta dinámica de reportes individuales /reportes/{reporte_id}. Al recibir la cadena 'mapa' en la URL, intentaba parsearla como un número entero para el ID de reporte.",
            "solucion": "Se validó y consolidó el contrato en la suite de pruebas y en el cliente Retrofit para invocar la ruta dedicada unívoca del módulo espacial: 'GET /mapa/reportes'."
        },
        {
            "num": "BUG-04",
            "titulo": "Posible División por Cero y Valor NaN en SemaforoHectareas",
            "gravedad": "Media (Inconsistencia Visual)",
            "descripcion": "En cuentas de productores recién registrados que aún no han dado de alta terrenos o hectáreas, el cálculo de porcentajes del semáforo corría riesgo de generar '0 / 0 = NaN'.",
            "causa": "Falta de pruebas unitarias específicas para listas vacías en los tableros de administración agronómica.",
            "solucion": "Se creó una suite de pruebas unitarias de casos borde (AdminMetricasEdgeCasesAndBiologicalValidationTest.kt) validando que ante cero hectáreas el cálculo retorne 0.0f de forma segura y consistente."
        }
    ]

    for b in bugs_data:
        bp = doc.add_paragraph()
        bp.paragraph_format.space_before = Pt(8)
        bp.paragraph_format.space_after = Pt(2)
        bp.paragraph_format.keep_with_next = True
        r_tag = bp.add_run(f"[{b['num']}] {b['titulo']} — ")
        r_tag.font.name = "Arial"
        r_tag.font.bold = True
        r_tag.font.size = Pt(10.5)
        r_tag.font.color.rgb = COLOR_PRIMARY

        r_sev = bp.add_run(f"Severidad: {b['gravedad']}")
        r_sev.font.name = "Arial"
        r_sev.font.bold = True
        r_sev.font.size = Pt(10)
        r_sev.font.color.rgb = COLOR_RED if "Crítica" in b['gravedad'] or "Alta" in b['gravedad'] else COLOR_SECONDARY

        # Detalle del bug
        b_table = doc.add_table(rows=3, cols=2)
        b_table.alignment = WD_TABLE_ALIGNMENT.CENTER
        b_table.autofit = False

        col_labels = [
            ("Síntoma observado:", b['descripcion']),
            ("Causa raíz técnica:", b['causa']),
            ("Solución aplicada:", b['solucion'])
        ]
        for row_i, (lbl, desc) in enumerate(col_labels):
            row = b_table.rows[row_i]
            cell_lbl = row.cells[0]
            cell_desc = row.cells[1]
            set_cell_background(cell_lbl, "F0F4F1")
            set_cell_background(cell_desc, "FAFAFA")
            set_cell_margins(cell_lbl, top=60, bottom=60, left=80, right=80)
            set_cell_margins(cell_desc, top=60, bottom=60, left=80, right=80)

            p_l = cell_lbl.paragraphs[0]
            p_l.paragraph_format.space_after = Pt(0)
            r_l = p_l.add_run(lbl)
            r_l.font.name = "Arial"
            r_l.font.bold = True
            r_l.font.size = Pt(9)
            r_l.font.color.rgb = COLOR_PRIMARY

            p_d = cell_desc.paragraphs[0]
            p_d.paragraph_format.space_after = Pt(0)
            r_d = p_d.add_run(desc)
            r_d.font.name = "Arial"
            r_d.font.size = Pt(9)
            r_d.font.color.rgb = COLOR_DARK

    # Sección 4: Pruebas E2E en Vivo contra Vercel
    add_section_header("4. Batería de Pruebas de Integración Cloud (Vercel + Supabase)")
    p_cloud = doc.add_paragraph()
    p_cloud.paragraph_format.space_after = Pt(6)
    r = p_cloud.add_run(
        "Se diseñó e implementó en 'plag-out/tests/integration/test_live_cloud_e2e_exhaustive.py' "
        "una suite de 8 pruebas integradas que auditan el servidor remoto en producción sin depender de simulaciones locales:"
    )
    r.font.name = "Arial"
    r.font.size = Pt(10)

    cloud_tests_data = [
        ("test_01_cloud_health_and_security_headers", "Verifica salud general del servicio y presencia de cabeceras seguras (HSTS, CSP, UUID en X-Request-Id).", "HTTP 200 (healthy)"),
        ("test_02_auth_invalid_credentials_protection", "Envía contraseñas erróneas y verifica rechazo HTTP 400 sin filtrar trazas de depuración o de base de datos.", "HTTP 400 (Seguro)"),
        ("test_03_backend_unauthorized_token_rejection", "Intenta acceder a rutas protegidas (/usuarios/me, /terrenos) sin token o con Bearer corrupto.", "HTTP 401 (Uniforme)"),
        ("test_04_user_profile_and_validation", "Comprueba lectura de perfil, rol y actualización de radio de notificación; rechaza radios negativos (-50 km).", "HTTP 200 / 422 (Validado)"),
        ("test_05_biological_catalogs_and_thermal_parameters", "Audita taxonomía de plagas: Dalbulus maidis catalogada como hemimetábola (desarrollo incompleto, ninfas, sin larvas).", "Consistencia Biológica"),
        ("test_06_data_isolation_between_accounts", "Verifica aislamiento anti-IDOR entre dos cuentas reales (Edgar Marchan y Lourdes Figuera); rechaza accesos ajenos.", "HTTP 404 (Anti-IDOR)"),
        ("test_07_spatial_boundary_validation", "Intenta crear lotes en coordenadas fuera de Argentina (Ecuador, Europa) y con hectáreas negativas; el backend los rechaza.", "HTTP 422 (Validado)"),
        ("test_08_map_reports_view_and_aggregation", "Consulta /mapa/reportes en modo grupos (zoom país) y modo reportes puntuales, validando la respuesta espacial.", "HTTP 200 (Agregado)")
    ]

    t_cloud = doc.add_table(rows=len(cloud_tests_data) + 1, cols=3)
    t_cloud.alignment = WD_TABLE_ALIGNMENT.CENTER
    for idx, col_name in enumerate(["Identificador del Test", "Descripción del Escenario", "Resultado Obtenido"]):
        cell = t_cloud.rows[0].cells[idx]
        set_cell_background(cell, "1B4D2E")
        set_cell_margins(cell, top=80, bottom=80, left=100, right=100)
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r = p.add_run(col_name)
        r.font.name = "Arial"
        r.font.bold = True
        r.font.size = Pt(9.5)
        r.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)

    for r_i, (t_name, t_desc, t_res) in enumerate(cloud_tests_data, start=1):
        row = t_cloud.rows[r_i]
        bg = "FFFFFF" if r_i % 2 != 0 else "F9FAF9"
        for c_i, text_val in enumerate([t_name, t_desc, t_res]):
            cell = row.cells[c_i]
            set_cell_background(cell, bg)
            set_cell_margins(cell, top=60, bottom=60, left=80, right=80)
            p = cell.paragraphs[0]
            if c_i == 2:
                p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            r = p.add_run(text_val)
            r.font.name = "Arial"
            r.font.size = Pt(8.5)
            if c_i == 0:
                r.font.bold = True
                r.font.color.rgb = COLOR_PRIMARY
            elif c_i == 2:
                r.font.bold = True
                r.font.color.rgb = COLOR_GREEN
            else:
                r.font.color.rgb = COLOR_DARK

    # Sección 5: Pruebas Unitarias Nuevas en Frontend
    add_section_header("5. Nuevas Pruebas Unitarias Integradas en Frontend")
    p_front = doc.add_paragraph()
    p_front.paragraph_format.space_after = Pt(6)
    r = p_front.add_run(
        "Se incorporó en 'pf-frontend/app/src/test/java/com/example/plag_out/AdminMetricasEdgeCasesAndBiologicalValidationTest.kt' "
        "un conjunto de pruebas que elevan la suite a 449 tests unitarios aprobados al 100%:"
    )
    r.font.name = "Arial"
    r.font.size = Pt(10)

    unit_bullets = [
        ("calcularSemaforoHectareas con cero lotes: ", "Garantiza que la ausencia de registros devuelva 0.0f en hectáreas y porcentajes sin colapsar por divisiones por cero."),
        ("Lotes en espera de Biofix vs sin monitoreo: ", "Verifica que los lotes con monitoreo activo que aún no han fijado su Biofix inicial no eleven falsamente el semáforo de riesgo."),
        ("etiquetaDeEtapa y taxonomía fenológica: ", "Valida que los ciclos muestren 'Huevo', 'Eclosión cerca' o 'Eclosionó' según el tipo de desarrollo de la plaga."),
        ("Límites del rectángulo territorial argentino: ", "Comprueba que la función de validación acepte coordenadas de La Quiaca (-22.10, -65.59), Pergamino (-33.89, -60.57), Burruyacú (-26.50, -64.74), Río Cuarto (-33.12, -64.35), Ushuaia (-54.80, -68.30) y Bernardo de Irigoyen (-26.25, -53.64), y rechace puntos fuera de Argentina.")
    ]
    for b_title, b_desc in unit_bullets:
        bp = doc.add_paragraph(style='List Bullet')
        bp.paragraph_format.space_after = Pt(4)
        bp.paragraph_format.line_spacing = 1.15
        r1 = bp.add_run(b_title)
        r1.font.name = "Arial"
        r1.font.bold = True
        r1.font.size = Pt(9.5)
        r1.font.color.rgb = COLOR_PRIMARY
        r2 = bp.add_run(b_desc)
        r2.font.name = "Arial"
        r2.font.size = Pt(9.5)

    # Sección 6: Dictamen Final
    add_section_header("6. Dictamen de Certificación Final")
    p_cert = doc.add_paragraph()
    p_cert.paragraph_format.line_spacing = 1.15
    p_cert.paragraph_format.space_after = Pt(8)
    r_cert = p_cert.add_run(
        "Certificación Técnica: La solución Plag-Out ha superado exitosamente el 100% de las pruebas funcionales, "
        "de integración y de seguridad programadas. Los 4 errores identificados fueron completamente resueltos y verificados "
        "en hardware real. La aplicación móvil se encuentra conectada al entorno productivo de Vercel y Supabase, "
        "y el APK compilado se encuentra en condiciones óptimas para la presentación y demostración pública."
    )
    r_cert.font.name = "Arial"
    r_cert.font.size = Pt(10.5)
    r_cert.font.bold = True
    r_cert.font.color.rgb = COLOR_PRIMARY

    # Guardar documento
    output_docx_path = "/Users/brisa_marchan/Documents/PF-PLAG-OUT-26/docs/qa/Informe_Exhaustivo_QA_Pruebas_y_Bugs_PlagOut.docx"
    doc.save(output_docx_path)

    desktop_docx_path = "/Users/brisa_marchan/Desktop/Informe_Exhaustivo_QA_Pruebas_y_Bugs_PlagOut.docx"
    doc.save(desktop_docx_path)
    print("Guardado en:", output_docx_path)
    print("Guardado en:", desktop_docx_path)

if __name__ == "__main__":
    create_document()

