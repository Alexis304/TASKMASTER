const TASK_STATUSES = [
    { key: "PENDIENTE", label: "Por hacer" },
    { key: "EN_PROGRESO", label: "En curso" },
    { key: "COMPLETADA", label: "Hecho" }
]

const MODULES = [
    { key: "myTasks", label: "Mis Tareas" },
    { key: "team", label: "Equipo de trabajo" },
    { key: "projects", label: "Proyectos" },
    { key: "calendar", label: "Calendario" },
    { key: "profile", label: "Perfil" }
]

const state = {
    user: null,
    tareas: [],
    proyectos: [],
    usuarios: [],
    equipoMiembros: [],
    authProviders: { googleEnabled: false, googleAuthUrl: "/oauth2/authorization/google" },
    authMode: "login",
    message: null,
    loading: false,
    realtimeConnected: false,
    liveNotifications: [],
    sidebarOpen: false,
    activeModule: "myTasks",
    selectedTeamProjectId: "",
    calendarMonth: new Date().toISOString().slice(0, 7),
    searchTerm: "",
    taskFormOpen: false,
    draggingTaskId: null,
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
let dniLookupTimer = null

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
    const [tareas, proyectos, usuarios, equipoMiembros] = await Promise.all([
        fetchJson("/api/tareas"),
        fetchJson("/api/proyectos"),
        fetchJson("/api/usuarios"),
        fetchJson("/api/equipos")
    ])

    state.tareas = tareas
    state.proyectos = proyectos
    state.usuarios = usuarios
    state.equipoMiembros = equipoMiembros

    if (!state.selectedTeamProjectId && proyectos.length) {
        state.selectedTeamProjectId = String(proyectos[0].id)
    }
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
                <span id="dni-lookup-status" class="field-caption">Al completar 8 digitos buscaremos tu nombre automaticamente.</span>
            </label>

            <label class="field">
                <span class="field-label">Nombre completo</span>
                <input id="register-nombres" type="text" name="nombres" placeholder="Se completara con la API de DNI" autocomplete="name" minlength="3" maxlength="120" required>
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

        </form>
    `
}

function renderWorkspace() {
    return `
        <div class="workspace-shell">
            <aside class="sidebar ${state.sidebarOpen ? "sidebar-open" : ""}">
                <div class="sidebar-brand">
                    <div class="brand-glyph brand-glyph-small"></div>
                    <div>
                        <h2>TaskMaster</h2>
                        <p>Workspace</p>
                    </div>
                    <button id="close-menu" class="menu-close" type="button" aria-label="Cerrar menu">x</button>
                </div>

                <div class="sidebar-user">
                    <div class="profile-mini">
                        ${renderAvatar(state.user.nombres, state.user.fotoUrl)}
                        <div>
                            <strong>${escapeHtml(state.user.nombres)}</strong>
                            <span>${escapeHtml(state.user.email)}</span>
                        </div>
                    </div>
                </div>

                <nav class="sidebar-nav">
                    ${MODULES.map(module => renderModuleButton(module)).join("")}
                </nav>

                <div class="sidebar-footer">
                    <button id="logout-btn" class="ghost-button sidebar-button" type="button">Cerrar sesion</button>
                </div>
            </aside>

            ${state.sidebarOpen ? `<button id="sidebar-backdrop" class="sidebar-backdrop" type="button" aria-label="Cerrar menu"></button>` : ""}

            <main class="board-page">
                <header class="mobile-topbar">
                    <button id="open-menu" class="hamburger-button" type="button" aria-label="Abrir menu">
                        <span></span>
                        <span></span>
                        <span></span>
                    </button>
                    <strong>${escapeHtml(resolveActiveModule().label)}</strong>
                </header>

                ${renderModuleContent()}
                ${renderLiveNotifications()}
            </main>
        </div>
    `
}

function renderModuleContent() {
    if (state.activeModule === "team") {
        return renderTeamModule()
    }

    if (state.activeModule === "projects") {
        return renderProjectsModule()
    }

    if (state.activeModule === "calendar") {
        return renderCalendarModule()
    }

    if (state.activeModule === "profile") {
        return renderProfileModule()
    }

    return renderTasksModule()
}

function renderTasksModule() {
    const filteredTasks = getFilteredTasks()
    const columns = buildKanbanColumns(filteredTasks)
    const heading = getWorkspaceHeading(filteredTasks)
    const meta = buildBoardMeta(filteredTasks)

    return `
                <header class="board-header">
                    <div>
                        <p class="board-kicker">${escapeHtml(heading.kicker)}</p>
                        <h1>${escapeHtml(heading.title)}</h1>
                        <p class="board-subtitle">${escapeHtml(heading.subtitle)}</p>
                    </div>

                    <div class="board-actions">
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

function renderTeamModule() {
    const projectId = state.selectedTeamProjectId || (state.proyectos[0]?.id ? String(state.proyectos[0].id) : "")
    const selectedProject = state.proyectos.find(proyecto => String(proyecto.id) === String(projectId))
    const miembros = state.equipoMiembros.filter(miembro => String(miembro.proyectoId) === String(projectId))
    const memberIds = new Set(miembros.map(miembro => String(miembro.usuarioId)))
    const availableUsers = state.usuarios.filter(usuario => !memberIds.has(String(usuario.id)))

    return `
        <section class="module-panel">
            <div>
                <p class="board-kicker">Equipo de trabajo</p>
                <h1>Vinculacion por proyecto</h1>
                <p class="board-subtitle">Selecciona con quien quieres trabajar en cada proyecto. No todos los usuarios quedan asociados automaticamente.</p>
            </div>

            ${renderMessage()}

            <section class="team-manager">
                <label class="field">
                    <span class="field-label">Proyecto</span>
                    <select id="team-project-select">
                        ${state.proyectos.map(proyecto => `
                            <option value="${proyecto.id}" ${String(projectId) === String(proyecto.id) ? "selected" : ""}>
                                ${escapeHtml(proyecto.nombre)}
                            </option>
                        `).join("")}
                    </select>
                </label>

                <form id="team-member-form" class="team-member-form">
                    <label class="field">
                        <span class="field-label">Agregar cuenta al equipo</span>
                        <select name="usuarioId" ${!availableUsers.length ? "disabled" : ""}>
                            ${availableUsers.length
                                ? availableUsers.map(usuario => `
                                    <option value="${usuario.id}">${escapeHtml(usuario.nombres)} - ${escapeHtml(usuario.email)}</option>
                                `).join("")
                                : `<option value="">No hay usuarios disponibles para asociar</option>`}
                        </select>
                    </label>
                    <button class="primary-button" type="submit" ${!selectedProject || !availableUsers.length ? "disabled" : ""}>
                        Asociar
                    </button>
                </form>
            </section>

            <div class="team-grid">
                ${miembros.length
                    ? miembros.map(miembro => `
                        <article class="team-card">
                            ${renderAvatar(miembro.usuarioNombre, miembro.usuarioFotoUrl)}
                            <div>
                                <h3>${escapeHtml(miembro.usuarioNombre)}</h3>
                                <p>${escapeHtml(miembro.usuarioEmail)}</p>
                                <span>${escapeHtml(selectedProject?.nombre || "Proyecto")}</span>
                            </div>
                            <button class="delete-link team-remove" type="button" data-remove-team-member="${miembro.id}" aria-label="Quitar del equipo">x</button>
                        </article>
                    `).join("")
                    : `<div class="empty-state">Este proyecto aun no tiene cuentas asociadas.</div>`}
            </div>
        </section>
    `
}

function renderProjectsModule() {
    return `
        <section class="module-panel">
            <div>
                <p class="board-kicker">Proyectos</p>
                <h1>Configurar proyectos</h1>
                <p class="board-subtitle">Crea proyectos para trabajar en conjunto con las cuentas vinculadas del equipo.</p>
            </div>

            ${renderMessage()}

            <form id="project-form" class="project-form">
                <label class="field">
                    <span class="field-label">Nombre del proyecto</span>
                    <input type="text" name="nombre" placeholder="Ej. Plataforma Kanban" maxlength="120" required>
                </label>
                <label class="field">
                    <span class="field-label">Descripcion</span>
                    <textarea name="descripcion" placeholder="Objetivo, alcance o contexto del proyecto" maxlength="500"></textarea>
                </label>
                <button class="primary-button" type="submit">${state.loading ? "Creando..." : "Crear proyecto"}</button>
            </form>

            <div class="project-list">
                ${state.proyectos.map(proyecto => `
                    <article class="project-card">
                        <div>
                            <h3>${escapeHtml(proyecto.nombre)}</h3>
                            <p>${escapeHtml(proyecto.descripcion || "Sin descripcion")}</p>
                        </div>
                        <span class="count-pill">${state.tareas.filter(tarea => tarea.proyectoId === proyecto.id).length} tareas</span>
                    </article>
                `).join("")}
            </div>
        </section>
    `
}

function renderCalendarModule() {
    const days = buildCalendarDays()

    return `
        <section class="module-panel">
            <header class="calendar-header">
                <div>
                    <p class="board-kicker">Calendario</p>
                    <h1>${escapeHtml(formatMonthTitle(state.calendarMonth))}</h1>
                    <p class="board-subtitle">Vista mensual de tareas programadas, en curso y realizadas.</p>
                </div>

                <div class="calendar-actions">
                    <button class="ghost-button" type="button" data-calendar-nav="-1">Anterior</button>
                    <button class="ghost-button" type="button" data-calendar-nav="today">Hoy</button>
                    <button class="ghost-button" type="button" data-calendar-nav="1">Siguiente</button>
                </div>
            </header>

            ${renderMessage()}

            <section class="calendar-shell">
                <div class="calendar-weekdays">
                    ${["Dom", "Lun", "Mar", "Mie", "Jue", "Vie", "Sab"].map(day => `<span>${day}</span>`).join("")}
                </div>

                <div class="calendar-grid">
                    ${days.map(day => {
                        const tasks = getTasksForDate(day.iso)
                        return `
                            <article class="calendar-day ${day.inMonth ? "" : "calendar-day-muted"} ${day.isToday ? "calendar-day-today" : ""}">
                                <time>${day.date.getDate()}</time>
                                <div class="calendar-events">
                                    ${tasks.slice(0, 4).map(tarea => `
                                        <div class="calendar-event status-${tarea.estado.toLowerCase()}">
                                            <strong>${escapeHtml(tarea.titulo)}</strong>
                                            <span>${escapeHtml(tarea.usuarioAsignadoNombre)}</span>
                                        </div>
                                    `).join("")}
                                    ${tasks.length > 4 ? `<span class="calendar-more">+${tasks.length - 4} mas</span>` : ""}
                                </div>
                            </article>
                        `
                    }).join("")}
                </div>
            </section>
        </section>
    `
}

function buildCalendarDays() {
    const [year, month] = state.calendarMonth.split("-").map(Number)
    const firstDay = new Date(year, month - 1, 1)
    const start = new Date(firstDay)
    start.setDate(firstDay.getDate() - firstDay.getDay())
    const todayIso = toIsoDate(new Date())

    return Array.from({ length: 42 }, (_, index) => {
        const date = new Date(start)
        date.setDate(start.getDate() + index)
        const iso = toIsoDate(date)
        return {
            date,
            iso,
            inMonth: date.getMonth() === month - 1,
            isToday: iso === todayIso
        }
    })
}

function getTasksForDate(isoDate) {
    return state.tareas
        .filter(tarea => tarea.fechaLimite === isoDate)
        .sort((a, b) => a.estado.localeCompare(b.estado))
}

function formatMonthTitle(monthValue) {
    const [year, month] = monthValue.split("-").map(Number)
    return new Date(year, month - 1, 1).toLocaleDateString("es-PE", {
        month: "long",
        year: "numeric"
    })
}

function toIsoDate(date) {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, "0")
    const day = String(date.getDate()).padStart(2, "0")
    return `${year}-${month}-${day}`
}

function renderProfileModule() {
    return `
        <section class="module-panel">
            <div>
                <p class="board-kicker">Perfil</p>
                <h1>Configuracion de cuenta</h1>
                <p class="board-subtitle">Actualiza tu nombre y la foto visible para el equipo.</p>
            </div>

            ${renderMessage()}

            <form id="profile-form" class="profile-form">
                <div class="profile-preview">
                    ${renderAvatar(state.user.nombres, state.user.fotoUrl, "profile-avatar-large")}
                    <div>
                        <strong>${escapeHtml(state.user.nombres)}</strong>
                        <span>${escapeHtml(state.user.email)}</span>
                    </div>
                </div>

                <label class="field">
                    <span class="field-label">Nombre completo</span>
                    <input type="text" name="nombres" value="${escapeHtml(state.user.nombres)}" minlength="3" maxlength="120" required>
                </label>

                <label class="field">
                    <span class="field-label">Foto de perfil</span>
                    <input type="file" name="foto" accept="image/jpeg,image/png,image/webp">
                    <span class="field-caption">Formatos permitidos: JPG, PNG o WEBP. Maximo 2 MB.</span>
                </label>

                <button class="primary-button" type="submit">${state.loading ? "Guardando..." : "Guardar perfil"}</button>
            </form>
        </section>
    `
}

function renderAvatar(name, fotoUrl, extraClass = "") {
    if (fotoUrl) {
        return `<img class="avatar-image ${extraClass}" src="${escapeHtml(fotoUrl)}" alt="${escapeHtml(name)}">`
    }

    return `<span class="avatar-badge ${extraClass}">${buildInitials(name)}</span>`
}

function renderModuleButton(module) {
    const active = state.activeModule === module.key
    const count = getModuleCount(module.key)

    return `
        <button class="sidebar-link ${active ? "sidebar-link-active" : ""}" type="button" data-module="${module.key}">
            <span>${escapeHtml(module.label)}</span>
            <span class="count-pill">${count}</span>
        </button>
    `
}

function renderTaskCard(tarea) {
    const completed = tarea.estado === "COMPLETADA"
    const dueClass = tarea.advertenciaFecha
        ? "task-date-warning"
        : isLateTask(tarea)
            ? "task-date-late"
            : ""

    return `
        <article class="task-card ${completed ? "task-card-locked" : ""}" draggable="${completed ? "false" : "true"}" data-task-id="${tarea.id}">
            <div class="task-card-row">
                <h3>${escapeHtml(tarea.titulo)}</h3>
                ${completed
                    ? `<span class="locked-pill">Finalizada</span>`
                    : `<button class="delete-link" type="button" data-delete="${tarea.id}" aria-label="Eliminar tarea">x</button>`}
            </div>

            <p class="task-description ${tarea.descripcion ? "" : "task-description-empty"}">
                ${escapeHtml(tarea.descripcion || "Sin descripcion")}
            </p>

            <div class="task-card-footer">
                <div class="assignee-pill">
                    <span class="avatar-badge">${buildInitials(tarea.usuarioAsignadoNombre)}</span>
                    <span>${escapeHtml(tarea.usuarioAsignadoNombre)}</span>
                </div>
                <span class="task-date ${dueClass}">${escapeHtml(formatDate(tarea.fechaLimite))}</span>
            </div>

            ${tarea.advertenciaFecha ? `<p class="task-warning">${escapeHtml(tarea.advertenciaFecha)}</p>` : ""}
            ${completed ? `<p class="task-locked-note">Esta tarea esta en Hecho y ya no se puede modificar.</p>` : ""}
        </article>
    `
}

function renderTaskModal() {
    const initialProjectId = state.proyectos[0]?.id || ""
    const initialUsers = getAssignableUsers(initialProjectId)

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
                            <select id="task-project-select" name="proyectoId" required>
                                ${state.proyectos.map(proyecto => `
                                    <option value="${proyecto.id}" ${String(initialProjectId) === String(proyecto.id) ? "selected" : ""}>
                                        ${escapeHtml(proyecto.nombre)}
                                    </option>
                                `).join("")}
                            </select>
                        </label>
                    </div>

                    <label class="field">
                        <span class="field-label">Responsable</span>
                        <select id="task-assignee-select" name="usuarioAsignadoId" required>
                            <option value="">Selecciona un usuario</option>
                            ${initialUsers.map(usuario => `
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
            clearTimeout(dniLookupTimer)
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
    document.querySelector("#open-menu")?.addEventListener("click", openSidebar)
    document.querySelector("#close-menu")?.addEventListener("click", closeSidebar)
    document.querySelector("#sidebar-backdrop")?.addEventListener("click", closeSidebar)
    document.querySelector("#open-task-form")?.addEventListener("click", openTaskForm)
    document.querySelector("#close-task-form")?.addEventListener("click", closeTaskForm)
    document.querySelector("#cancel-task-form")?.addEventListener("click", closeTaskForm)
    document.querySelector("#task-form")?.addEventListener("submit", handleTaskCreate)
    document.querySelector("#task-project-select")?.addEventListener("change", handleTaskProjectChange)
    document.querySelector("#project-form")?.addEventListener("submit", handleProjectCreate)
    document.querySelector("#profile-form")?.addEventListener("submit", handleProfileUpdate)
    document.querySelector("#team-project-select")?.addEventListener("change", handleTeamProjectChange)
    document.querySelector("#team-member-form")?.addEventListener("submit", handleTeamMemberCreate)

    document.querySelectorAll("[data-module]").forEach(button => {
        button.addEventListener("click", handleModuleChange)
    })

    document.querySelectorAll("[data-remove-team-member]").forEach(button => {
        button.addEventListener("click", handleTeamMemberDelete)
    })

    document.querySelectorAll("[data-calendar-nav]").forEach(button => {
        button.addEventListener("click", handleCalendarNav)
    })

    document.querySelectorAll("[data-delete]").forEach(button => {
        button.addEventListener("click", handleDelete)
    })

    document.querySelectorAll("[data-task-id]:not(.task-card-locked)").forEach(card => {
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
        state.message = { type: "info", text: "Cuenta creada correctamente. Bienvenido, " + state.user.nombres + "." }
    } catch (error) {
        state.message = { type: "error", text: error.message }
    } finally {
        setLoading(false)
        render()
    }
}

function handleDniInput(event) {
    event.target.value = normalizeDni(event.target.value).slice(0, 8)
    const dni = event.target.value
    const status = document.querySelector("#dni-lookup-status")
    const nombresInput = document.querySelector("#register-nombres")

    clearTimeout(dniLookupTimer)

    if (!status || !nombresInput) {
        return
    }

    status.className = "field-caption"

    if (dni.length < 8) {
        status.textContent = "Al completar 8 digitos buscaremos tu nombre automaticamente."
        nombresInput.readOnly = false
        return
    }

    status.textContent = "Consultando DNI en la API..."
    nombresInput.readOnly = true

    dniLookupTimer = setTimeout(async () => {
        try {
            const persona = await fetchJson(`/api/dni/${encodeURIComponent(dni)}`)
            nombresInput.value = persona.nombreCompleto || ""
            nombresInput.readOnly = Boolean(persona.nombreCompleto)
            status.className = "field-caption field-caption-success"
            status.textContent = persona.nombreCompleto
                ? "Nombre encontrado y completado automaticamente."
                : "La API respondio sin nombre. Ingresa el nombre manualmente."
        } catch (error) {
            nombresInput.readOnly = false
            status.className = "field-caption field-caption-error"
            status.textContent = error.message || "No se pudo consultar el DNI. Ingresa el nombre manualmente."
        }
    }, 350)
}

function normalizeDni(value) {
    return String(value || "").replace(/\D/g, "")
}

function openSidebar() {
    state.sidebarOpen = true
    render()
}

function closeSidebar() {
    state.sidebarOpen = false
    render()
}

function handleModuleChange(event) {
    state.activeModule = event.currentTarget.dataset.module
    state.sidebarOpen = false
    state.taskFormOpen = false
    state.message = null
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

    if (task.estado === "COMPLETADA") {
        state.message = { type: "warn", text: "Las tareas en Hecho ya no se pueden modificar." }
        render()
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

async function handleProjectCreate(event) {
    event.preventDefault()
    setLoading(true)

    const formData = new FormData(event.currentTarget)
    const payload = {
        nombre: String(formData.get("nombre") || "").trim(),
        descripcion: String(formData.get("descripcion") || "").trim()
    }

    try {
        const proyecto = await fetchJson("/api/proyectos", {
            method: "POST",
            body: JSON.stringify(payload)
        })

        state.proyectos.push(proyecto)
        state.proyectos.sort((a, b) => a.nombre.localeCompare(b.nombre))
        state.selectedTeamProjectId = String(proyecto.id)
        state.message = { type: "info", text: "Proyecto creado correctamente." }
    } catch (error) {
        state.message = { type: "error", text: error.message }
    } finally {
        setLoading(false)
        render()
    }
}

function handleTeamProjectChange(event) {
    state.selectedTeamProjectId = event.target.value
    state.message = null
    render()
}

async function handleTeamMemberCreate(event) {
    event.preventDefault()

    const formData = new FormData(event.currentTarget)
    const usuarioId = Number(formData.get("usuarioId"))
    const proyectoId = Number(state.selectedTeamProjectId)

    if (!proyectoId || !usuarioId) {
        state.message = { type: "error", text: "Selecciona proyecto y usuario para asociar." }
        render()
        return
    }

    setLoading(true)

    try {
        const miembro = await fetchJson("/api/equipos", {
            method: "POST",
            body: JSON.stringify({ proyectoId, usuarioId })
        })
        state.equipoMiembros.push(miembro)
        state.message = { type: "info", text: "Cuenta asociada al proyecto correctamente." }
    } catch (error) {
        state.message = { type: "error", text: error.message }
    } finally {
        setLoading(false)
        render()
    }
}

async function handleTeamMemberDelete(event) {
    setLoading(true)
    const id = event.currentTarget.dataset.removeTeamMember

    try {
        await fetchJson(`/api/equipos/${id}`, { method: "DELETE" })
        state.equipoMiembros = state.equipoMiembros.filter(miembro => String(miembro.id) !== String(id))
        state.message = { type: "info", text: "Cuenta removida del proyecto." }
    } catch (error) {
        state.message = { type: "error", text: error.message }
    } finally {
        setLoading(false)
        render()
    }
}

function handleTaskProjectChange(event) {
    const assigneeSelect = document.querySelector("#task-assignee-select")
    if (!assigneeSelect) {
        return
    }

    const users = getAssignableUsers(event.target.value)
    assigneeSelect.innerHTML = `
        <option value="">Selecciona un usuario</option>
        ${users.map(usuario => `
            <option value="${usuario.id}">${escapeHtml(usuario.nombres)} - ${escapeHtml(usuario.email)}</option>
        `).join("")}
    `
}

async function handleProfileUpdate(event) {
    event.preventDefault()
    setLoading(true)

    const formData = new FormData(event.currentTarget)
    const payload = {
        nombres: String(formData.get("nombres") || "").trim().replace(/\s+/g, " ")
    }

    try {
        state.user = await fetchJson("/api/auth/me", {
            method: "PUT",
            body: JSON.stringify(payload)
        })

        const photo = formData.get("foto")
        if (photo && photo.size > 0) {
            const photoData = new FormData()
            photoData.append("file", photo)
            state.user = await fetchMultipart("/api/auth/me/photo", photoData)
        }

        await loadDashboardData()
        state.message = { type: "info", text: "Perfil actualizado correctamente." }
    } catch (error) {
        state.message = { type: "error", text: error.message }
    } finally {
        setLoading(false)
        render()
    }
}

function handleCalendarNav(event) {
    const direction = event.currentTarget.dataset.calendarNav

    if (direction === "today") {
        state.calendarMonth = new Date().toISOString().slice(0, 7)
        render()
        return
    }

    const [year, month] = state.calendarMonth.split("-").map(Number)
    const nextDate = new Date(year, month - 1 + Number(direction), 1)
    state.calendarMonth = nextDate.toISOString().slice(0, 7)
    render()
}

async function handleDelete(event) {
    const task = state.tareas.find(item => String(item.id) === String(event.currentTarget.dataset.delete))
    if (task?.estado === "COMPLETADA") {
        state.message = { type: "warn", text: "Las tareas en Hecho ya no se pueden modificar." }
        render()
        return
    }

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

    params.set("usuarioId", state.user.id)

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
        state.activeModule = "myTasks"
        state.sidebarOpen = false
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

function buildKanbanColumns(tasks) {
    return TASK_STATUSES.reduce((acc, status) => {
        acc[status.key] = tasks.filter(tarea => tarea.estado === status.key)
        return acc
    }, {})
}

function resolveActiveModule() {
    return MODULES.find(module => module.key === state.activeModule) || MODULES[0]
}

function getModuleCount(moduleKey) {
    if (moduleKey === "myTasks") {
        return state.tareas.filter(tarea => tarea.usuarioAsignadoId === state.user.id).length
    }

    if (moduleKey === "team") {
        return state.equipoMiembros.length
    }

    if (moduleKey === "projects") {
        return state.proyectos.length
    }

    if (moduleKey === "calendar") {
        return state.tareas.length
    }

    return ""
}

function getAssignableUsers(projectId) {
    const miembros = state.equipoMiembros.filter(miembro => String(miembro.proyectoId) === String(projectId))

    if (!miembros.length) {
        return state.usuarios
    }

    const memberIds = new Set(miembros.map(miembro => String(miembro.usuarioId)))
    return state.usuarios.filter(usuario => memberIds.has(String(usuario.id)))
}

function buildBoardMeta(tasks) {
    return [
        { label: "Visibles", value: tasks.length },
        { label: "En curso", value: tasks.filter(tarea => tarea.estado === "EN_PROGRESO").length },
        { label: "Vencidas", value: tasks.filter(isLateTask).length }
    ]
}

function getWorkspaceHeading(tasks) {
    return {
        kicker: "Vista personal",
        title: "Mis Tareas",
        subtitle: `${tasks.length} tarjetas bajo tu responsabilidad.`
    }
}

function getFilteredTasks() {
    let tasks = state.tareas.filter(tarea => tarea.usuarioAsignadoId === state.user.id)

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
    render()
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

async function fetchMultipart(url, formData) {
    const response = await fetch(url, {
        method: "POST",
        credentials: "include",
        body: formData
    })

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
            : payload.message || "No se pudo subir la foto."
        throw new Error(message)
    }

    return payload
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
