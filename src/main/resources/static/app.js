const TASK_STATUSES = [
    { key: "PENDIENTE", label: "Por hacer" },
    { key: "EN_PROGRESO", label: "En curso" },
    { key: "COMPLETADA", label: "Hecho" }
]

const state = {
    user: null,
    tareas: [],
    proyectos: [],
    usuarios: [],
    authProviders: { googleEnabled: false, googleAuthUrl: "/oauth2/authorization/google" },
    authMode: "login",
    message: null,
    loading: false,
    realtimeConnected: false,
    liveNotifications: [],
    searchTerm: "",
    taskFormOpen: false,
    draggingTaskId: null,
    activeNav: { type: "all", value: null },
    filters: {
        assigneeId: "",
        deadline: "all",
        sort: "deadline"
    }
}

const app = document.querySelector("#app")
let realtimeSocket = null
let realtimeReconnectTimer = null
let realtimeManuallyClosed = false

document.addEventListener("DOMContentLoaded", bootstrap)

async function bootstrap() {
    applyUrlFeedback()

    try {
        state.authProviders = await fetchJson("/api/auth/providers")
    } catch (error) {
        state.authProviders = { googleEnabled: false, googleAuthUrl: "/oauth2/authorization/google" }
    }

    try {
        state.user = await fetchJson("/api/auth/me")
        await loadDashboardData()
        connectRealtime()
    } catch (error) {
        state.user = null
        disconnectRealtime()
    }

    render()
}

function applyUrlFeedback() {
    const params = new URLSearchParams(window.location.search)

    if (params.get("google") === "success") {
        state.message = { type: "info", text: "Sesion iniciada con Google correctamente." }
    }

    if (params.get("google") === "error") {
        state.message = { type: "error", text: "Google no pudo completar el acceso. Revisa tu configuracion OAuth." }
    }

    if (params.toString()) {
        window.history.replaceState({}, document.title, window.location.pathname)
    }
}

async function loadDashboardData() {
    const [tareas, proyectos, usuarios] = await Promise.all([
        fetchJson("/api/tareas"),
        fetchJson("/api/proyectos"),
        fetchJson("/api/usuarios")
    ])

    state.tareas = tareas
    state.proyectos = proyectos
    state.usuarios = usuarios
}

function render() {
    app.innerHTML = state.user ? renderWorkspace() : renderLogin()
    bindEvents()
}

function renderLogin() {
    const isLogin = state.authMode === "login"
    const googleButton = state.authProviders.googleEnabled
        ? `
            <a class="oauth-button" href="${escapeHtml(state.authProviders.googleAuthUrl)}">
                <span class="oauth-mark">G</span>
                <span>Continuar con Google</span>
            </a>
        `
        : `
            <button class="oauth-button oauth-button-disabled" type="button" disabled>
                <span class="oauth-mark">G</span>
                <span>Continuar con Google</span>
            </button>
        `

    return `
        <section class="auth-shell">
            <div class="auth-brand">
                <div class="brand-glyph"></div>
                <h1>TaskMaster API</h1>
                <p>Accede a tu tablero de trabajo</p>
            </div>

            <section class="auth-card">
                <div class="auth-tabs">
                    <button class="auth-tab ${isLogin ? "auth-tab-active" : ""}" type="button" data-auth-switch="login">Iniciar sesion</button>
                    <button class="auth-tab ${!isLogin ? "auth-tab-active" : ""}" type="button" data-auth-switch="register">Registrarse</button>
                </div>

                ${renderMessage()}
                ${isLogin ? renderLoginForm() : renderRegisterForm()}

                <div class="auth-divider"><span>o</span></div>
                ${googleButton}

                <p class="auth-helper">
                    ${state.authProviders.googleEnabled
                        ? "Puedes entrar con Google o con una cuenta local."
                        : "Google Login se activara cuando definas las credenciales en el entorno o en Docker."}
                </p>

                <div class="auth-footnote">
                    ${isLogin
                        ? `No tienes una cuenta? <button type="button" class="text-link" data-auth-switch="register">Registrate</button>`
                        : `Ya tienes una cuenta? <button type="button" class="text-link" data-auth-switch="login">Inicia sesion</button>`}
                </div>
            </section>
        </section>
    `
}

function renderLoginForm() {
    return `
        <form id="login-form" class="auth-form">
            <label class="field">
                <span class="field-label">Correo electronico</span>
                <input type="email" name="email" placeholder="nombre@empresa.com" autocomplete="email" required>
            </label>

            <label class="field">
                <span class="field-row">
                    <span class="field-label">Contrasena</span>
                    <span class="field-caption">Minimo 6 caracteres</span>
                </span>
                <input type="password" name="password" placeholder="Ingresa tu contrasena" autocomplete="current-password" required>
            </label>

            <button class="primary-button auth-submit" type="submit">
                ${state.loading ? "Ingresando..." : "Iniciar sesion"}
            </button>
        </form>
    `
}

function renderRegisterForm() {
    return `
        <form id="register-form" class="auth-form">
            <label class="field">
                <span class="field-label">DNI</span>
                <input type="text" name="dni" placeholder="Ingresa tu DNI de 8 digitos" inputmode="numeric" maxlength="8" autocomplete="off" required>
            </label>

            <label class="field">
                <span class="field-label">Nombre completo</span>
                <input type="text" name="nombres" placeholder="Ingresa tu nombre completo" autocomplete="name" minlength="3" maxlength="120" required>
            </label>

            <label class="field">
                <span class="field-label">Correo electronico</span>
                <input type="email" name="email" placeholder="nombre@empresa.com" autocomplete="email" required>
            </label>

            <label class="field">
                <span class="field-label">Contrasena</span>
                <input type="password" name="password" placeholder="Crea una contrasena segura" autocomplete="new-password" required>
            </label>

            <label class="field">
                <span class="field-label">Confirmar contrasena</span>
                <input type="password" name="confirmPassword" placeholder="Repite tu contrasena" autocomplete="new-password" required>
            </label>

            <button class="primary-button auth-submit" type="submit">
                ${state.loading ? "Creando cuenta..." : "Crear cuenta"}
            </button>

            <p class="field-caption">Si la API REST encuentra tu DNI, usaremos ese nombre. Si no, guardaremos el nombre completo ingresado.</p>
        </form>
    `
}

function renderWorkspace() {
    const filteredTasks = getFilteredTasks()
    const columns = buildKanbanColumns(filteredTasks)
    const heading = getWorkspaceHeading(filteredTasks)
    const meta = buildBoardMeta(filteredTasks)

    return `
        <div class="workspace-shell">
            <aside class="sidebar">
                <div class="sidebar-brand">
                    <div class="brand-glyph brand-glyph-small"></div>
                    <div>
                        <h2>TaskMaster</h2>
                        <p>Workspace</p>
                    </div>
                </div>

                <div class="sidebar-user">
                    <strong>${escapeHtml(state.user.nombres)}</strong>
                    <span>${escapeHtml(state.user.email)}</span>
                </div>

                <nav class="sidebar-nav">
                    ${renderNavButton("all", null, "Tablero", state.tareas.length)}
                    ${renderNavButton("my", state.user.id, "Mis tareas", state.tareas.filter(tarea => tarea.usuarioAsignadoId === state.user.id).length)}
                    ${renderNavButton("urgent", null, "Urgentes", getUrgentTasks(state.tareas).length)}
                </nav>

                <section class="sidebar-section">
                    <div class="sidebar-section-title">Proyectos</div>
                    <div class="sidebar-projects">
                        ${buildProjectSummaries().map(project => `
                            <button class="sidebar-project ${isProjectActive(project.id) ? "sidebar-project-active" : ""}" type="button" data-nav-type="project" data-nav-value="${project.id}">
                                <span>${escapeHtml(project.nombre)}</span>
                                <span class="count-pill">${project.count}</span>
                            </button>
                        `).join("")}
                    </div>
                </section>

                <div class="sidebar-footer">
                    <button id="logout-btn" class="ghost-button sidebar-button" type="button">Cerrar sesion</button>
                </div>
            </aside>

            <main class="board-page">
                <header class="board-header">
                    <div>
                        <p class="board-kicker">${escapeHtml(heading.kicker)}</p>
                        <h1>${escapeHtml(heading.title)}</h1>
                        <p class="board-subtitle">${escapeHtml(heading.subtitle)}</p>
                    </div>

                    <div class="board-actions">
                        <span class="live-status ${state.realtimeConnected ? "live-status-on" : "live-status-off"}">
                            ${state.realtimeConnected ? "En vivo" : "Reconectando"}
                        </span>
                        <button id="refresh-btn" class="ghost-button" type="button">Actualizar</button>
                        <button id="export-board-pdf" class="ghost-button" type="button">Exportar PDF</button>
                        <button id="open-task-form" class="primary-button" type="button">Nueva tarea</button>
                    </div>
                </header>

                <section class="board-toolbar">
                    <label class="search-box">
                        <span>Buscar</span>
                        <input id="search-input" type="search" placeholder="Buscar tareas" value="${escapeHtml(state.searchTerm)}">
                    </label>

                    <label class="filter-control">
                        <span>Responsable</span>
                        <select id="assignee-filter">
                            <option value="">Todos</option>
                            ${state.usuarios.map(usuario => `
                                <option value="${usuario.id}" ${String(state.filters.assigneeId) === String(usuario.id) ? "selected" : ""}>
                                    ${escapeHtml(usuario.nombres)}
                                </option>
                            `).join("")}
                        </select>
                    </label>

                    <label class="filter-control">
                        <span>Fecha</span>
                        <select id="deadline-filter">
                            <option value="all" ${state.filters.deadline === "all" ? "selected" : ""}>Todas</option>
                            <option value="today" ${state.filters.deadline === "today" ? "selected" : ""}>Hoy</option>
                            <option value="week" ${state.filters.deadline === "week" ? "selected" : ""}>Semana</option>
                            <option value="late" ${state.filters.deadline === "late" ? "selected" : ""}>Vencidas</option>
                        </select>
                    </label>

                    <label class="filter-control">
                        <span>Ordenar</span>
                        <select id="sort-filter">
                            <option value="deadline" ${state.filters.sort === "deadline" ? "selected" : ""}>Fecha</option>
                            <option value="assignee" ${state.filters.sort === "assignee" ? "selected" : ""}>Responsable</option>
                            <option value="project" ${state.filters.sort === "project" ? "selected" : ""}>Proyecto</option>
                        </select>
                    </label>

                    <button id="clear-filters" class="ghost-button toolbar-clear" type="button">Limpiar</button>
                </section>

                <section class="board-meta">
                    ${meta.map(item => `
                        <div class="meta-pill">
                            <strong>${item.value}</strong>
                            <span>${item.label}</span>
                        </div>
                    `).join("")}
                </section>

                ${renderMessage()}

                <section class="kanban-board">
                    ${TASK_STATUSES.map(status => `
                        <article class="kanban-column">
                            <header class="column-header">
                                <h2>${status.label}</h2>
                                <span class="column-count">${columns[status.key].length}</span>
                            </header>

                            <div class="column-body" data-drop-status="${status.key}">
                                ${columns[status.key].length
                                    ? columns[status.key].map(renderTaskCard).join("")
                                    : `<div class="empty-column">Sin tarjetas</div>`}
                            </div>
                        </article>
                    `).join("")}
                </section>

                ${state.taskFormOpen ? renderTaskModal() : ""}
                ${renderLiveNotifications()}
            </main>
        </div>
    `
}

function renderLiveNotifications() {
    if (!state.liveNotifications.length) {
        return ""
    }

    return `
        <div class="live-notification-stack" aria-live="polite">
            ${state.liveNotifications.map(notification => `
                <div class="live-notification">
                    <strong>Notificacion en vivo</strong>
                    <span>${escapeHtml(notification.text)}</span>
                </div>
            `).join("")}
        </div>
    `
}

function renderNavButton(type, value, label, count) {
    const active = state.activeNav.type === type && String(state.activeNav.value) === String(value)

    return `
        <button class="sidebar-link ${active ? "sidebar-link-active" : ""}" type="button" data-nav-type="${type}" data-nav-value="${value ?? ""}">
            <span>${escapeHtml(label)}</span>
            <span class="count-pill">${count}</span>
        </button>
    `
}

function renderTaskCard(tarea) {
    const dueClass = tarea.advertenciaFecha
        ? "task-date-warning"
        : isLateTask(tarea)
            ? "task-date-late"
            : ""

    return `
        <article class="task-card" draggable="true" data-task-id="${tarea.id}">
            <div class="task-card-row">
                <h3>${escapeHtml(tarea.titulo)}</h3>
                <button class="delete-link" type="button" data-delete="${tarea.id}" aria-label="Eliminar tarea">x</button>
            </div>

            <div class="task-card-footer">
                <div class="assignee-pill">
                    <span class="avatar-badge">${buildInitials(tarea.usuarioAsignadoNombre)}</span>
                    <span>${escapeHtml(tarea.usuarioAsignadoNombre)}</span>
                </div>
                <span class="task-date ${dueClass}">${escapeHtml(formatDate(tarea.fechaLimite))}</span>
            </div>

            ${tarea.advertenciaFecha ? `<p class="task-warning">${escapeHtml(tarea.advertenciaFecha)}</p>` : ""}
        </article>
    `
}

function renderTaskModal() {
    return `
        <div class="modal-backdrop">
            <section class="task-modal">
                <div class="modal-header">
                    <div>
                        <p class="board-kicker">Nueva tarea</p>
                        <h2>Agregar tarjeta</h2>
                    </div>
                    <button id="close-task-form" class="ghost-button" type="button">Cerrar</button>
                </div>

                <form id="task-form" class="task-form">
                    <label class="field">
                        <span class="field-label">Titulo</span>
                        <input type="text" name="titulo" placeholder="Ej. Crear flujo de login" required>
                    </label>

                    <label class="field">
                        <span class="field-label">Descripcion</span>
                        <textarea name="descripcion" placeholder="Contexto corto para la tarea"></textarea>
                    </label>

                    <div class="form-grid">
                        <label class="field">
                            <span class="field-label">Fecha limite</span>
                            <input type="date" name="fechaLimite" required>
                        </label>

                        <label class="field">
                            <span class="field-label">Proyecto</span>
                            <select name="proyectoId" required>
                                <option value="">Selecciona un proyecto</option>
                                ${state.proyectos.map(proyecto => `
                                    <option value="${proyecto.id}">${escapeHtml(proyecto.nombre)}</option>
                                `).join("")}
                            </select>
                        </label>
                    </div>

                    <label class="field">
                        <span class="field-label">Responsable</span>
                        <select name="usuarioAsignadoId" required>
                            <option value="">Selecciona un usuario</option>
                            ${state.usuarios.map(usuario => `
                                <option value="${usuario.id}">${escapeHtml(usuario.nombres)} - ${escapeHtml(usuario.email)}</option>
                            `).join("")}
                        </select>
                    </label>

                    <div class="modal-actions">
                        <button id="cancel-task-form" class="ghost-button" type="button">Cancelar</button>
                        <button class="primary-button" type="submit">${state.loading ? "Guardando..." : "Guardar tarea"}</button>
                    </div>
                </form>
            </section>
        </div>
    `
}

function bindEvents() {
    document.querySelectorAll("[data-auth-switch]").forEach(button => {
        button.addEventListener("click", event => {
            state.authMode = event.currentTarget.dataset.authSwitch
            state.message = null
            render()
        })
    })

    if (!state.user) {
        document.querySelector("#login-form")?.addEventListener("submit", handleLogin)
        document.querySelector("#register-form")?.addEventListener("submit", handleRegister)
        document.querySelector("input[name='dni']")?.addEventListener("input", handleDniInput)
        return
    }

    document.querySelector("#search-input")?.addEventListener("input", event => {
        state.searchTerm = event.target.value
        render()
    })
    document.querySelector("#assignee-filter")?.addEventListener("change", event => {
        state.filters.assigneeId = event.target.value
        render()
    })
    document.querySelector("#deadline-filter")?.addEventListener("change", event => {
        state.filters.deadline = event.target.value
        render()
    })
    document.querySelector("#sort-filter")?.addEventListener("change", event => {
        state.filters.sort = event.target.value
        render()
    })
    document.querySelector("#clear-filters")?.addEventListener("click", resetFilters)
    document.querySelector("#refresh-btn")?.addEventListener("click", () => refreshTasks(true, true))
    document.querySelector("#export-board-pdf")?.addEventListener("click", handleExportBoardPdf)
    document.querySelector("#logout-btn")?.addEventListener("click", handleLogout)
    document.querySelector("#open-task-form")?.addEventListener("click", openTaskForm)
    document.querySelector("#close-task-form")?.addEventListener("click", closeTaskForm)
    document.querySelector("#cancel-task-form")?.addEventListener("click", closeTaskForm)
    document.querySelector("#task-form")?.addEventListener("submit", handleTaskCreate)

    document.querySelectorAll("[data-nav-type]").forEach(button => {
        button.addEventListener("click", handleNavChange)
    })

    document.querySelectorAll("[data-delete]").forEach(button => {
        button.addEventListener("click", handleDelete)
    })

    document.querySelectorAll("[data-task-id]").forEach(card => {
        card.addEventListener("dragstart", handleDragStart)
        card.addEventListener("dragend", handleDragEnd)
    })

    document.querySelectorAll("[data-drop-status]").forEach(zone => {
        zone.addEventListener("dragover", handleDragOver)
        zone.addEventListener("drop", handleDrop)
    })
}

async function handleLogin(event) {
    event.preventDefault()
    const formData = new FormData(event.currentTarget)
    const email = String(formData.get("email") || "").trim()
    const password = String(formData.get("password") || "")

    if (!isValidEmail(email)) {
        state.message = { type: "error", text: "Ingresa un correo electronico valido." }
        render()
        return
    }

    if (password.length < 6) {
        state.message = { type: "error", text: "La contrasena debe tener al menos 6 caracteres." }
        render()
        return
    }

    setLoading(true)

    try {
        await fetchJson("/api/auth/login", {
            method: "POST",
            body: JSON.stringify({ email, password })
        })

        state.user = await fetchJson("/api/auth/me")
        await loadDashboardData()
        connectRealtime()
        state.message = { type: "info", text: "Sesion iniciada correctamente." }
    } catch (error) {
        state.message = { type: "error", text: error.message }
    } finally {
        setLoading(false)
        render()
    }
}

async function handleRegister(event) {
    event.preventDefault()
    const formData = new FormData(event.currentTarget)
    const dni = normalizeDni(formData.get("dni"))
    const nombres = String(formData.get("nombres") || "").trim().replace(/\s+/g, " ")
    const email = String(formData.get("email") || "").trim()
    const password = String(formData.get("password") || "")
    const confirmPassword = String(formData.get("confirmPassword") || "")

    if (!/^\d{8}$/.test(dni)) {
        state.message = { type: "error", text: "Ingresa un DNI valido de exactamente 8 digitos." }
        render()
        return
    }

    if (nombres.length < 3) {
        state.message = { type: "error", text: "Ingresa tu nombre completo." }
        render()
        return
    }

    if (!isValidEmail(email)) {
        state.message = { type: "error", text: "Ingresa un correo electronico valido." }
        render()
        return
    }

    if (password.length < 8) {
        state.message = { type: "error", text: "La contrasena debe tener al menos 8 caracteres." }
        render()
        return
    }

    if (password !== confirmPassword) {
        state.message = { type: "error", text: "Las contrasenas no coinciden." }
        render()
        return
    }

    setLoading(true)

    try {
        await fetchJson("/api/auth/register", {
            method: "POST",
            body: JSON.stringify({ dni, nombres, email, password })
        })

        state.user = await fetchJson("/api/auth/me")
        await loadDashboardData()
        connectRealtime()
        state.message = { type: "info", text: "Cuenta creada correctamente. Bienvenido, " + nombres + "." }
    } catch (error) {
        state.message = { type: "error", text: error.message }
    } finally {
        setLoading(false)
        render()
    }
}

function handleDniInput(event) {
    event.target.value = normalizeDni(event.target.value).slice(0, 8)
}

function normalizeDni(value) {
    return String(value || "").replace(/\D/g, "")
}

function handleNavChange(event) {
    const type = event.currentTarget.dataset.navType
    const rawValue = event.currentTarget.dataset.navValue

    state.activeNav = {
        type,
        value: rawValue === "" ? null : Number.isNaN(Number(rawValue)) ? rawValue : Number(rawValue)
    }

    render()
}

function handleDragStart(event) {
    state.draggingTaskId = Number(event.currentTarget.dataset.taskId)
    event.dataTransfer.effectAllowed = "move"
    event.dataTransfer.setData("text/plain", String(state.draggingTaskId))
}

function handleDragEnd() {
    state.draggingTaskId = null
}

function handleDragOver(event) {
    event.preventDefault()
    event.dataTransfer.dropEffect = "move"
}

async function handleDrop(event) {
    event.preventDefault()
    const status = event.currentTarget.dataset.dropStatus
    const taskId = Number(event.dataTransfer.getData("text/plain") || state.draggingTaskId)
    const task = state.tareas.find(item => item.id === taskId)

    if (!task || task.estado === status) {
        return
    }

    setLoading(true)

    try {
        await fetchJson(`/api/tareas/${taskId}?estado=${encodeURIComponent(status)}`, { method: "PUT" })
        await refreshTasks(false, false)
        state.message = { type: "info", text: `Tarea movida a ${formatStatus(status).toLowerCase()}.` }
    } catch (error) {
        state.message = { type: "error", text: error.message }
    } finally {
        state.draggingTaskId = null
        setLoading(false)
        render()
    }
}

async function handleTaskCreate(event) {
    event.preventDefault()
    setLoading(true)

    const formData = new FormData(event.currentTarget)
    const payload = {
        titulo: formData.get("titulo"),
        descripcion: formData.get("descripcion"),
        fechaLimite: formData.get("fechaLimite"),
        proyectoId: Number(formData.get("proyectoId")),
        usuarioAsignadoId: Number(formData.get("usuarioAsignadoId"))
    }

    try {
        const tarea = await fetchJson("/api/tareas", {
            method: "POST",
            body: JSON.stringify(payload)
        })

        state.tareas.push(tarea)
        state.taskFormOpen = false
        state.message = {
            type: tarea.advertenciaFecha ? "warn" : "info",
            text: tarea.advertenciaFecha || "Tarea creada correctamente."
        }
    } catch (error) {
        state.message = { type: "error", text: error.message }
    } finally {
        setLoading(false)
        render()
    }
}

async function handleDelete(event) {
    setLoading(true)

    try {
        await fetchJson(`/api/tareas/${event.currentTarget.dataset.delete}`, { method: "DELETE" })
        state.tareas = state.tareas.filter(tarea => String(tarea.id) !== String(event.currentTarget.dataset.delete))
        state.message = { type: "info", text: "Tarea eliminada." }
    } catch (error) {
        state.message = { type: "error", text: error.message }
    } finally {
        setLoading(false)
        render()
    }
}

async function handleExportBoardPdf() {
    setLoading(true)

    try {
        const { blob, fileName } = await fetchBlob(buildReportUrl())
        const downloadUrl = URL.createObjectURL(blob)
        const link = document.createElement("a")
        link.href = downloadUrl
        link.download = fileName || "taskmaster-tablero.pdf"
        document.body.appendChild(link)
        link.click()
        link.remove()
        URL.revokeObjectURL(downloadUrl)
        state.message = { type: "info", text: "Reporte PDF generado correctamente." }
    } catch (error) {
        state.message = { type: "error", text: error.message }
    } finally {
        setLoading(false)
        render()
    }
}

async function refreshTasks(renderAfter = true, notify = false) {
    try {
        state.tareas = await fetchJson("/api/tareas")
        if (notify) {
            state.message = { type: "info", text: "Tablero actualizado." }
        }
    } catch (error) {
        state.message = { type: "error", text: error.message }
    }

    if (renderAfter) {
        render()
    }
}

function buildReportUrl() {
    const params = new URLSearchParams()
    const term = state.searchTerm.trim()

    if (term) {
        params.set("q", term)
    }

    if (state.activeNav.type === "project") {
        params.set("proyectoId", state.activeNav.value)
    }

    if (state.activeNav.type === "my") {
        params.set("usuarioId", state.user.id)
    }

    if (state.filters.assigneeId) {
        params.set("usuarioId", state.filters.assigneeId)
    }

    const queryString = params.toString()
    return queryString ? `/api/reportes/tablero.pdf?${queryString}` : "/api/reportes/tablero.pdf"
}

async function handleLogout() {
    setLoading(true)

    try {
        await fetchJson("/api/auth/logout", { method: "POST" })
    } catch (error) {
        state.message = { type: "error", text: error.message }
    } finally {
        disconnectRealtime()
        state.user = null
        state.tareas = []
        state.proyectos = []
        state.usuarios = []
        state.taskFormOpen = false
        state.searchTerm = ""
        state.activeNav = { type: "all", value: null }
        state.filters = { assigneeId: "", deadline: "all", sort: "deadline" }
        state.liveNotifications = []
        state.authMode = "login"
        state.message = { type: "info", text: "Sesion cerrada." }
        setLoading(false)
        render()
    }
}

function connectRealtime() {
    if (!state.user || (realtimeSocket && realtimeSocket.readyState <= WebSocket.OPEN)) {
        return
    }

    clearTimeout(realtimeReconnectTimer)
    realtimeManuallyClosed = false
    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:"
    realtimeSocket = new WebSocket(`${protocol}//${window.location.host}/realtime/tasks`)

    realtimeSocket.addEventListener("open", () => {
        state.realtimeConnected = true
        render()
    })

    realtimeSocket.addEventListener("message", event => {
        handleRealtimeEvent(event.data)
    })

    realtimeSocket.addEventListener("close", () => {
        state.realtimeConnected = false
        realtimeSocket = null
        if (!realtimeManuallyClosed && state.user) {
            render()
            realtimeReconnectTimer = setTimeout(connectRealtime, 4000)
        }
    })

    realtimeSocket.addEventListener("error", () => {
        realtimeSocket.close()
    })
}

function disconnectRealtime() {
    clearTimeout(realtimeReconnectTimer)
    realtimeManuallyClosed = true
    state.realtimeConnected = false

    if (realtimeSocket) {
        realtimeSocket.close()
        realtimeSocket = null
    }
}

function handleRealtimeEvent(rawPayload) {
    let event
    try {
        event = JSON.parse(rawPayload)
    } catch (error) {
        return
    }

    if (event.type === "TASK_DELETED" && event.deletedTaskId) {
        state.tareas = state.tareas.filter(tarea => String(tarea.id) !== String(event.deletedTaskId))
    } else if (event.tarea) {
        upsertTask(event.tarea)
    } else {
        refreshTasks(false, false)
    }

    addLiveNotification(event.message || "El tablero fue actualizado.")
    render()
}

function upsertTask(updatedTask) {
    const index = state.tareas.findIndex(tarea => tarea.id === updatedTask.id)

    if (index >= 0) {
        state.tareas.splice(index, 1, updatedTask)
        return
    }

    state.tareas.unshift(updatedTask)
}

function addLiveNotification(text) {
    const id = Date.now() + Math.random()
    state.liveNotifications = [
        { id, text },
        ...state.liveNotifications
    ].slice(0, 4)

    setTimeout(() => {
        state.liveNotifications = state.liveNotifications.filter(notification => notification.id !== id)
        if (state.user) {
            render()
        }
    }, 5500)
}

function buildProjectSummaries() {
    return state.proyectos.map(project => ({
        ...project,
        count: state.tareas.filter(tarea => tarea.proyectoId === project.id).length
    }))
}

function buildKanbanColumns(tasks) {
    return TASK_STATUSES.reduce((acc, status) => {
        acc[status.key] = tasks.filter(tarea => tarea.estado === status.key)
        return acc
    }, {})
}

function buildBoardMeta(tasks) {
    return [
        { label: "Visibles", value: tasks.length },
        { label: "En curso", value: tasks.filter(tarea => tarea.estado === "EN_PROGRESO").length },
        { label: "Vencidas", value: tasks.filter(isLateTask).length }
    ]
}

function getWorkspaceHeading(tasks) {
    if (state.activeNav.type === "my") {
        return {
            kicker: "Vista personal",
            title: "Mis tareas",
            subtitle: `${tasks.length} tarjetas bajo tu responsabilidad.`
        }
    }

    if (state.activeNav.type === "urgent") {
        return {
            kicker: "Vista prioritaria",
            title: "Tareas urgentes",
            subtitle: "Lo mas cercano al vencimiento aparece primero."
        }
    }

    if (state.activeNav.type === "project") {
        const project = state.proyectos.find(item => String(item.id) === String(state.activeNav.value))
        return {
            kicker: "Proyecto",
            title: project ? project.nombre : "Proyecto",
            subtitle: project?.descripcion || "Tablero filtrado por proyecto."
        }
    }

    return {
        kicker: "Tablero",
        title: "Workspace general",
        subtitle: "Un flujo simple para organizar, mover y cerrar tareas."
    }
}

function getFilteredTasks() {
    let tasks = [...state.tareas]

    tasks = applyActiveNav(tasks)

    if (state.filters.assigneeId) {
        tasks = tasks.filter(tarea => String(tarea.usuarioAsignadoId) === String(state.filters.assigneeId))
    }

    if (state.filters.deadline !== "all") {
        tasks = tasks.filter(tarea => matchesDeadlineFilter(tarea, state.filters.deadline))
    }

    const term = state.searchTerm.trim().toLowerCase()
    if (term) {
        tasks = tasks.filter(tarea =>
            [tarea.titulo, tarea.descripcion, tarea.proyectoNombre, tarea.usuarioAsignadoNombre, tarea.estado]
                .filter(Boolean)
                .some(value => value.toLowerCase().includes(term))
        )
    }

    return sortTasks(tasks, state.filters.sort)
}

function applyActiveNav(tasks) {
    if (state.activeNav.type === "my") {
        return tasks.filter(tarea => tarea.usuarioAsignadoId === state.user.id)
    }

    if (state.activeNav.type === "urgent") {
        return getUrgentTasks(tasks)
    }

    if (state.activeNav.type === "project") {
        return tasks.filter(tarea => tarea.proyectoId === state.activeNav.value)
    }

    return tasks
}

function getUrgentTasks(tasks) {
    return tasks.filter(tarea => {
        if (!tarea.fechaLimite || tarea.estado === "COMPLETADA") {
            return false
        }

        return getDaysUntil(tarea.fechaLimite) <= 3
    })
}

function matchesDeadlineFilter(tarea, filter) {
    if (!tarea.fechaLimite) {
        return false
    }

    const days = getDaysUntil(tarea.fechaLimite)

    if (filter === "today") {
        return days === 0
    }

    if (filter === "week") {
        return days >= 0 && days <= 7
    }

    if (filter === "late") {
        return days < 0
    }

    return true
}

function sortTasks(tasks, sortMode) {
    const sorted = [...tasks]

    if (sortMode === "assignee") {
        return sorted.sort((a, b) => a.usuarioAsignadoNombre.localeCompare(b.usuarioAsignadoNombre))
    }

    if (sortMode === "project") {
        return sorted.sort((a, b) => a.proyectoNombre.localeCompare(b.proyectoNombre))
    }

    return sorted.sort((a, b) => {
        if (!a.fechaLimite) {
            return 1
        }

        if (!b.fechaLimite) {
            return -1
        }

        return a.fechaLimite.localeCompare(b.fechaLimite)
    })
}

function resetFilters() {
    state.searchTerm = ""
    state.filters = {
        assigneeId: "",
        deadline: "all",
        sort: "deadline"
    }
    state.activeNav = { type: "all", value: null }
    render()
}

function isProjectActive(projectId) {
    return state.activeNav.type === "project" && String(state.activeNav.value) === String(projectId)
}

function renderMessage() {
    if (!state.message) {
        return ""
    }

    return `<div class="message ${state.message.type}">${escapeHtml(state.message.text)}</div>`
}

function openTaskForm() {
    state.taskFormOpen = true
    render()
}

function closeTaskForm() {
    state.taskFormOpen = false
    render()
}

async function fetchJson(url, options = {}) {
    const response = await fetch(url, {
        credentials: "include",
        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {})
        },
        ...options
    })

    if (response.status === 204) {
        return null
    }

    const contentType = response.headers.get("content-type") || ""
    const payload = contentType.includes("application/json")
        ? await response.json()
        : await response.text()

    if (!response.ok) {
        if (response.status === 401) {
            state.user = null
            throw new Error("Tu sesion expiro. Vuelve a iniciar sesion.")
        }

        const message = typeof payload === "string"
            ? payload
            : payload.message || "Ocurrio un error inesperado."
        throw new Error(message)
    }

    return payload
}

async function fetchBlob(url) {
    const response = await fetch(url, {
        credentials: "include",
        headers: {
            "Accept": "application/pdf"
        }
    })

    if (!response.ok) {
        if (response.status === 401) {
            state.user = null
            throw new Error("Tu sesion expiro. Vuelve a iniciar sesion.")
        }

        const message = await response.text()
        throw new Error(message || "No se pudo generar el PDF.")
    }

    const disposition = response.headers.get("content-disposition") || ""
    const match = disposition.match(/filename="?(.*?)"?$/)

    return {
        blob: await response.blob(),
        fileName: match ? match[1] : "taskmaster-tablero.pdf"
    }
}

function getDaysUntil(dateValue) {
    const today = new Date()
    today.setHours(0, 0, 0, 0)

    const target = new Date(`${dateValue}T00:00:00`)
    return Math.round((target - today) / 86400000)
}

function isLateTask(tarea) {
    return tarea.fechaLimite && getDaysUntil(tarea.fechaLimite) < 0 && tarea.estado !== "COMPLETADA"
}

function formatDate(value) {
    if (!value) {
        return "Sin fecha"
    }

    return new Date(`${value}T00:00:00`).toLocaleDateString("es-PE", {
        month: "short",
        day: "2-digit",
        year: "numeric"
    })
}

function setLoading(isLoading) {
    state.loading = isLoading
}

function formatStatus(status) {
    return status.replace("_", " ")
}

function buildInitials(name) {
    return String(name || "TM")
        .split(" ")
        .filter(Boolean)
        .slice(0, 2)
        .map(fragment => fragment[0].toUpperCase())
        .join("")
}

function isValidEmail(value) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;")
}
