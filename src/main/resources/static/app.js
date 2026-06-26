const state = {
    user: null,
    tareas: [],
    proyectos: [],
    usuarios: [],
    message: null,
    loading: false
}

const app = document.querySelector("#app")

document.addEventListener("DOMContentLoaded", bootstrap)

async function bootstrap() {
    try {
        const user = await fetchJson("/api/auth/me")
        state.user = user
        await loadDashboardData()
    } catch (error) {
        state.user = null
    }

    render()
}

async function loadDashboardData() {
    const [tareas, proyectos, usuarios] = await Promise.all([
        fetchJson("/api/tareas"),
        fetchJson("/api/catalogo/proyectos"),
        fetchJson("/api/catalogo/usuarios")
    ])

    state.tareas = tareas
    state.proyectos = proyectos
    state.usuarios = usuarios
}

function render() {
    if (!state.user) {
        renderLogin()
        return
    }

    renderDashboard()
}

function renderLogin() {
    app.innerHTML = `
        <section class="panel">
            <div class="panel-inner">
                <h2 class="panel-title">Iniciar sesion</h2>
                <p class="panel-copy">
                    Entra con la cuenta demo para administrar tareas, responsables y fechas limite.
                </p>
                ${renderMessage()}
                <form id="login-form" class="form-grid">
                    <label>
                        Correo
                        <input type="email" name="email" value="admin@taskmaster.local" required>
                    </label>
                    <label>
                        Password
                        <input type="password" name="password" value="Admin123*" required>
                    </label>
                    <button class="btn-primary" type="submit">
                        ${state.loading ? "Ingresando..." : "Entrar al panel"}
                    </button>
                </form>
                <div class="demo-box">
                    <strong>Demo:</strong> admin@taskmaster.local / Admin123*
                </div>
            </div>
        </section>
    `

    document.querySelector("#login-form").addEventListener("submit", handleLogin)
}

function renderDashboard() {
    app.innerHTML = `
        <div class="layout">
            <section class="panel">
                <div class="panel-inner">
                    <h2 class="panel-title">Nueva tarea</h2>
                    <p class="panel-copy">
                        Registra una tarea y asignala a un responsable con fecha limite.
                    </p>
                    ${renderMessage()}
                    <form id="task-form" class="form-grid">
                        <label>
                            Titulo
                            <input type="text" name="titulo" required>
                        </label>
                        <label>
                            Descripcion
                            <textarea name="descripcion" placeholder="Describe el trabajo a realizar"></textarea>
                        </label>
                        <label>
                            Fecha limite
                            <input type="date" name="fechaLimite" required>
                        </label>
                        <label>
                            Proyecto
                            <select name="proyectoId" required>
                                <option value="">Selecciona un proyecto</option>
                                ${state.proyectos.map(proyecto => `
                                    <option value="${proyecto.id}">${escapeHtml(proyecto.nombre)}</option>
                                `).join("")}
                            </select>
                        </label>
                        <label>
                            Responsable
                            <select name="usuarioAsignadoId" required>
                                <option value="">Selecciona un usuario</option>
                                ${state.usuarios.map(usuario => `
                                    <option value="${usuario.id}">${escapeHtml(usuario.nombres)} - ${escapeHtml(usuario.email)}</option>
                                `).join("")}
                            </select>
                        </label>
                        <button class="btn-primary" type="submit">
                            ${state.loading ? "Guardando..." : "Crear tarea"}
                        </button>
                    </form>
                </div>
            </section>

            <section class="panel">
                <div class="panel-inner">
                    <div class="toolbar">
                        <div>
                            <h2 class="panel-title">Panel operativo</h2>
                            <p class="panel-copy">
                                Bienvenido, ${escapeHtml(state.user.nombres)}. Administra el flujo del equipo desde aqui.
                            </p>
                        </div>
                        <div class="toolbar-actions">
                            <span class="pill">${state.tareas.length} tareas</span>
                            <button id="refresh-btn" class="btn-secondary" type="button">Actualizar</button>
                            <button id="logout-btn" class="btn-secondary" type="button">Cerrar sesion</button>
                        </div>
                    </div>

                    <div id="task-list" class="task-list">
                        ${renderTaskList()}
                    </div>
                </div>
            </section>
        </div>
    `

    document.querySelector("#task-form").addEventListener("submit", handleTaskCreate)
    document.querySelector("#refresh-btn").addEventListener("click", refreshTasks)
    document.querySelector("#logout-btn").addEventListener("click", handleLogout)
    document.querySelectorAll("[data-status]").forEach(button => {
        button.addEventListener("click", handleStatusChange)
    })
    document.querySelectorAll("[data-delete]").forEach(button => {
        button.addEventListener("click", handleDelete)
    })
}

function renderTaskList() {
    if (!state.tareas.length) {
        return `<div class="empty-state">Todavia no hay tareas registradas.</div>`
    }

    return state.tareas.map(tarea => `
        <article class="task-card">
            <div class="toolbar">
                <div>
                    <h3>${escapeHtml(tarea.titulo)}</h3>
                    <span class="status ${tarea.estado}">${formatStatus(tarea.estado)}</span>
                </div>
                <span class="pill">${escapeHtml(tarea.proyectoNombre)}</span>
            </div>

            <p>${escapeHtml(tarea.descripcion || "Sin descripcion adicional.")}</p>

            <div class="task-meta">
                <span>Responsable: ${escapeHtml(tarea.usuarioAsignadoNombre)}</span>
                <span>Fecha limite: ${escapeHtml(tarea.fechaLimite || "No definida")}</span>
            </div>

            ${tarea.advertenciaFecha ? `<div class="message warn" style="margin-top:14px;">${escapeHtml(tarea.advertenciaFecha)}</div>` : ""}

            <div class="task-actions">
                ${renderStatusButton(tarea, "PENDIENTE", "Pendiente")}
                ${renderStatusButton(tarea, "EN_PROGRESO", "En progreso")}
                ${renderStatusButton(tarea, "COMPLETADA", "Completar")}
                <button class="btn-danger" type="button" data-delete="${tarea.id}">Eliminar</button>
            </div>
        </article>
    `).join("")
}

function renderStatusButton(tarea, estado, label) {
    const style = tarea.estado === estado ? "btn-primary" : "btn-secondary"
    return `<button class="${style}" type="button" data-id="${tarea.id}" data-status="${estado}">${label}</button>`
}

function renderMessage() {
    if (!state.message) {
        return ""
    }

    return `<div class="message ${state.message.type}" style="margin-bottom:16px;">${escapeHtml(state.message.text)}</div>`
}

async function handleLogin(event) {
    event.preventDefault()
    setLoading(true)

    const formData = new FormData(event.currentTarget)
    const payload = {
        email: formData.get("email"),
        password: formData.get("password")
    }

    try {
        await fetchJson("/api/auth/login", {
            method: "POST",
            body: JSON.stringify(payload)
        })

        state.message = { type: "info", text: "Sesion iniciada correctamente." }
        state.user = await fetchJson("/api/auth/me")
        await loadDashboardData()
    } catch (error) {
        state.message = { type: "error", text: error.message }
    } finally {
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

        state.tareas.unshift(tarea)
        state.message = {
            type: tarea.advertenciaFecha ? "warn" : "info",
            text: tarea.advertenciaFecha || "Tarea creada correctamente."
        }
        event.currentTarget.reset()
    } catch (error) {
        state.message = { type: "error", text: error.message }
    } finally {
        setLoading(false)
        render()
    }
}

async function handleStatusChange(event) {
    const button = event.currentTarget
    const id = button.dataset.id
    const estado = button.dataset.status
    setLoading(true)

    try {
        await fetchJson(`/api/tareas/${id}?estado=${encodeURIComponent(estado)}`, {
            method: "PUT"
        })
        state.message = { type: "info", text: "Estado actualizado." }
        await refreshTasks(false)
    } catch (error) {
        state.message = { type: "error", text: error.message }
    } finally {
        setLoading(false)
        render()
    }
}

async function handleDelete(event) {
    const id = event.currentTarget.dataset.delete
    setLoading(true)

    try {
        await fetchJson(`/api/tareas/${id}`, { method: "DELETE" })
        state.tareas = state.tareas.filter(tarea => String(tarea.id) !== String(id))
        state.message = { type: "info", text: "Tarea eliminada." }
    } catch (error) {
        state.message = { type: "error", text: error.message }
    } finally {
        setLoading(false)
        render()
    }
}

async function refreshTasks(renderAfter = true) {
    try {
        state.tareas = await fetchJson("/api/tareas")
        state.message = { type: "info", text: "Datos actualizados." }
    } catch (error) {
        state.message = { type: "error", text: error.message }
    }

    if (renderAfter) {
        render()
    }
}

async function handleLogout() {
    setLoading(true)

    try {
        await fetchJson("/api/auth/logout", { method: "POST" })
    } catch (error) {
        state.message = { type: "error", text: error.message }
    } finally {
        state.user = null
        state.tareas = []
        state.message = { type: "info", text: "Sesion cerrada." }
        setLoading(false)
        render()
    }
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

function setLoading(isLoading) {
    state.loading = isLoading
}

function formatStatus(status) {
    return status.replace("_", " ")
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;")
}
