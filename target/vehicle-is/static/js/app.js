/* ============================================================
 * Vehicle IS — клиент (vanilla JS + hash-router).
 * Все операции выполняются на сервере (Spring MVC REST API),
 * изменения синхронизируются с PostgreSQL через EclipseLink.
 * Автообновление у всех клиентов — через SSE.
 * ============================================================ */
"use strict";

const API = (() => {
    const path = new URL(document.currentScript.src, location.href).pathname;
    const staticMarker = '/static/';
    const staticIndex = path.indexOf(staticMarker);
    return staticIndex >= 0
        ? path.substring(0, staticIndex) + '/api'
        : '/api';
})();

const VEHICLE_TYPES = ["DRONE", "BOAT", "SHIP", "BICYCLE"];
const FUEL_TYPES = ["GASOLINE", "KEROSENE", "ELECTRICITY", "NUCLEAR", "ANTIMATTER"];

const COLUMNS = [
    { key: "id", title: "ID" },
    { key: "name", title: "Название" },
    { key: "coordX", title: "Коорд. X" },
    { key: "coordY", title: "Коорд. Y" },
    { key: "creationDate", title: "Дата создания" },
    { key: "type", title: "Тип" },
    { key: "enginePower", title: "Мощн. двигателя" },
    { key: "numberOfWheels", title: "Колёса" },
    { key: "capacity", title: "Вместимость" },
    { key: "distanceTravelled", title: "Пробег" },
    { key: "fuelConsumption", title: "Расход топлива" },
    { key: "fuelType", title: "Топливо" }
];

const state = {
    page: 1,
    size: 8,
    sortField: "id",
    asc: true,
    filterField: "name",
    filterValue: "",
    total: 0,
    items: []
};

/* ------------------------- helpers ------------------------- */

function el(html) {
    const t = document.createElement("template");
    t.innerHTML = html.trim();
    return t.content.firstElementChild;
}

function esc(s) {
    return String(s ?? "").replace(/[&<>"']/g, c => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    }[c]));
}

async function apiFetch(path, options = {}) {
    const resp = await fetch(`${API}${path}`, {
        headers: { "Content-Type": "application/json" },
        ...options
    });
    let body = null;
    try { body = await resp.json(); } catch (e) { /* пустой ответ */ }
    if (!resp.ok) {
        const err = new Error(body?.error || "Ошибка сервера (" + resp.status + ")");
        err.status = resp.status;
        err.body = body;
        throw err;
    }
    return body;
}

let toastTimer;
function toast(msg) {
    let t = document.querySelector(".toast");
    if (!t) { t = el(`<div class="toast"></div>`); document.body.appendChild(t); }
    t.textContent = msg;
    t.classList.add("show");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => t.classList.remove("show"), 3200);
}

/* ------------------------- роутер ------------------------- */

const routes = {
    "/": renderListPage,
    "/new": () => { openVehicleForm(null, "Создание объекта"); location.hash = "#/"; },
    "/special": renderSpecialPage,
    "/view/:id": (params) => renderDetailPage(Number(params.id))
};

function router() {
    const hash = location.hash.replace(/^#/, "") || "/";
    document.querySelectorAll(".menu-link").forEach(a =>
        a.classList.toggle("active", a.getAttribute("href") === "#" + hash));
    for (const [pattern, handler] of Object.entries(routes)) {
        const rx = new RegExp("^" + pattern.replace(/:[^/]+/g, "(?<p>[^/]+)") + "$");
        const m = hash.match(rx);
        if (m) {
            const names = [...pattern.matchAll(/:(\w+)/g)].map(x => x[1]);
            const params = {};
            names.forEach((n, i) => params[n] = m.groups["p" + (i + 1)] ?? m[i + 1]);
            handler(names.length ? params : undefined);
            return;
        }
    }
    renderListPage();
}

window.addEventListener("hashchange", router);

/* ------------------------- страница списка ------------------------- */

async function renderListPage() {
    const app = document.getElementById("app");
    app.innerHTML = `<h2 class="page-title">Список транспортных средств</h2>
        <div class="panel">
            <div class="toolbar" id="toolbar"></div>
            <div class="table-wrap"><table class="grid" id="grid"></table></div>
            <div class="pagination" id="pagination"></div>
        </div>`;
    renderToolbar();
    await refreshTable();
}

function renderToolbar() {
    const tb = document.getElementById("toolbar");
    tb.innerHTML = "";
    tb.appendChild(el(`
        <div class="filter-controls">
            <label>Фильтр по колонке:
                <select id="f-field">${COLUMNS.map(c =>
                    `<option value="${c.key}" ${c.key === state.filterField ? "selected" : ""}>${esc(c.title)}</option>`).join("")}
                </select>
            </label>
            <input type="text" id="f-value" placeholder="подстрока (нечётное совпадение)" value="${esc(state.filterValue)}">
            <button class="btn small" id="f-apply">Применить</button>
            <button class="btn small secondary" id="f-clear">Сбросить</button>
        </div>
    `));
    tb.appendChild(el(`<span style="flex:1"></span>`));
    tb.appendChild(el(`<button class="btn" id="btn-create">+ Создать объект</button>`));
    tb.querySelector("#f-apply").onclick = () => {
        state.filterField = tb.querySelector("#f-field").value;
        state.filterValue = tb.querySelector("#f-value").value;
        state.page = 1;
        refreshTable();
    };
    tb.querySelector("#f-clear").onclick = () => {
        state.filterValue = "";
        state.page = 1;
        refreshTable();
    };
    tb.querySelector("#f-value").addEventListener("keydown", e => {
        if (e.key === "Enter") tb.querySelector("#f-apply").click();
    });
    tb.querySelector("#btn-create").onclick = () => openVehicleForm(null, "Создание объекта");
}

async function refreshTable() {
    const grid = document.getElementById("grid");
    if (!grid) return;
    const q = new URLSearchParams({
        page: state.page, size: state.size,
        sortField: state.sortField, asc: state.asc
    });
    if (state.filterValue.trim()) {
        q.set("filterField", state.filterField);
        q.set("filterValue", state.filterValue.trim());
    }
    let data;
    try {
        data = await apiFetch(`/vehicles?${q}`);
    } catch (e) {
        toast("Ошибка загрузки: " + e.message);
        grid.innerHTML = `<tbody><tr><td colspan="${COLUMNS.length + 1}" style="text-align:center;color:#d43f3f">Не удалось загрузить список: ${esc(e.message)}</td></tr></tbody>`;
        return;
    }
    try {
        state.items = Array.isArray(data?.items) ? data.items : [];
        state.total = Number.isFinite(Number(data?.total)) ? Number(data.total) : state.items.length;
        drawGrid();
        drawPagination();
    } catch (e) {
        console.error("Ошибка отображения списка Vehicle", e, data);
        grid.innerHTML = `<tbody><tr><td colspan="${COLUMNS.length + 1}" style="text-align:center;color:#d43f3f">Ошибка отображения списка объектов</td></tr></tbody>`;
    }
}

function drawGrid() {
    const grid = document.getElementById("grid");
    const head = `<tr>${COLUMNS.map(c => {
        const arrow = state.sortField === c.key ? (state.asc ? " ▲" : " ▼") : "";
        return `<th data-key="${c.key}">${esc(c.title)}<span class="arrow">${arrow}</span></th>`;
    }).join("")}<th>Действия</th></tr>`;
    const rows = state.items.map(v => `<tr data-id="${v.id}">${
        COLUMNS.map(c => {
            let val;
            if (c.key === "coordX") val = v.coordinates?.x;
            else if (c.key === "coordY") val = v.coordinates?.y;
            else val = v[c.key];
            return `<td>${val == null || val === "" ? '<span class="badge">null</span>' : esc(val)}</td>`;
        }).join("")
    }<td>
        <button class="btn small secondary act-view">Просмотр</button>
        <button class="btn small act-edit">Изменить</button>
        <button class="btn small danger act-del">Удалить</button>
    </td></tr>`).join("");
    grid.innerHTML = `<thead>${head}</thead><tbody>${rows ||
        `<tr><td colspan="${COLUMNS.length + 1}" style="text-align:center;color:#7a8199">Нет данных</td></tr>`}</tbody>`;

    grid.querySelectorAll("th[data-key]").forEach(th => th.onclick = () => {
        const k = th.dataset.key;
        if (state.sortField === k) state.asc = !state.asc;
        else { state.sortField = k; state.asc = true; }
        refreshTable();
    });
    grid.querySelectorAll("tbody tr[data-id]").forEach(tr => {
        const id = Number(tr.dataset.id);
        tr.querySelector(".act-view").onclick = () => location.hash = `#/view/${id}`;
        tr.querySelector(".act-edit").onclick = () => openVehicleForm(id, "Обновление объекта");
        tr.querySelector(".act-del").onclick = () => confirmDelete(id);
    });
}

function drawPagination() {
    const p = document.getElementById("pagination");
    const pages = Math.max(1, Math.ceil(state.total / state.size));
    p.innerHTML = "";
    p.append(
        el(`<button class="btn small secondary" ${state.page <= 1 ? "disabled" : ""} id="pg-prev">← Пред.</button>`),
        el(`<span class="page-info">Страница ${state.page} из ${pages} (всего: ${state.total})</span>`),
        el(`<button class="btn small secondary" ${state.page >= pages ? "disabled" : ""} id="pg-next">След. →</button>`),
        el(`<label style="margin-left:16px">На странице:
              <select id="pg-size">${[5, 8, 10, 20, 50].map(n =>
                `<option ${n === state.size ? "selected" : ""}>${n}</option>`).join("")}</select></label>`)
    );
    p.querySelector("#pg-prev").onclick = () => { state.page--; refreshTable(); };
    p.querySelector("#pg-next").onclick = () => { state.page++; refreshTable(); };
    p.querySelector("#pg-size").onchange = e => { state.size = Number(e.target.value); state.page = 1; refreshTable(); };
}

/* ------------------------- форма (отдельное диалоговое окно) ------------------------- */

function fieldHtml(name, label, inputHtml, hint) {
    return `<div class="field" data-field="${name}">
        <label>${esc(label)}</label>${inputHtml}
        <div class="hint">${esc(hint || "")}</div>
        <div class="error-msg"></div></div>`;
}

function vehicleFormHtml(v = {}, coordinates = []) {
    return `
    <form id="vehicle-form" novalidate>
      <div class="form-grid">
        ${fieldHtml("name", "Название *", `<input name="name" type="text" value="${esc(v.name ?? "")}">`,
        "Не может быть null или пустым")}
        ${fieldHtml("coordinates.x", "Координата X *", `<input name="coordinates.x" type="number" step="any" max="862" value="${esc(v.coordinates?.x ?? "")}">`,
        "Число, максимум 862")}
        ${fieldHtml("coordinates.y", "Координата Y *", `<input name="coordinates.y" type="number" step="1" min="-859" value="${esc(v.coordinates?.y ?? "")}">`,
        "Целое число, больше -860")}
        ${fieldHtml("coordinates.id", "Существующие Coordinates", `<select name="coordinates.id">
            <option value="">Создать новый объект Coordinates</option>
            ${coordinates.map(c => `<option value="${c.id}" ${v.coordinates?.id === c.id ? "selected" : ""}>#${c.id}: (${c.x}, ${c.y})</option>`).join("")}
        </select>`, "Можно связать Vehicle с уже сохранённым объектом")}
        ${fieldHtml("type", "Тип ТС", `<select name="type"><option value="">— не указан —</option>${
        VEHICLE_TYPES.map(t => `<option ${v.type === t ? "selected" : ""}>${t}</option>`).join("")}</select>`,
        "Поле может быть null")}
        ${fieldHtml("enginePower", "Мощность двигателя *", `<input name="enginePower" type="number" step="any" min="0.0000001" value="${esc(v.enginePower ?? "")}">`,
        "Больше 0")}
        ${fieldHtml("numberOfWheels", "Количество колёс", `<input name="numberOfWheels" type="number" step="1" min="1" value="${esc(v.numberOfWheels ?? "")}">`,
        "Может быть null, больше 0")}
        ${fieldHtml("capacity", "Вместимость *", `<input name="capacity" type="number" step="1" min="1" value="${esc(v.capacity ?? "")}">`,
        "Больше 0")}
        ${fieldHtml("distanceTravelled", "Пробег *", `<input name="distanceTravelled" type="number" step="any" min="0.0000001" value="${esc(v.distanceTravelled ?? "")}">`,
        "Больше 0")}
        ${fieldHtml("fuelConsumption", "Расход топлива *", `<input name="fuelConsumption" type="number" step="any" min="0.0000001" value="${esc(v.fuelConsumption ?? "")}">`,
        "Больше 0")}
        ${fieldHtml("fuelType", "Тип топлива", `<select name="fuelType"><option value="">— не указан —</option>${
        FUEL_TYPES.map(t => `<option ${v.fuelType === t ? "selected" : ""}>${t}</option>`).join("")}</select>`,
        "Поле может быть null")}
      </div>
      <div class="result-box error hidden" id="form-general-error" style="display:none"></div>
      <div style="margin-top:16px; display:flex; gap:10px; justify-content:flex-end">
        <button type="button" class="btn secondary" onclick="App.closeModal()">Отмена</button>
        <button type="submit" class="btn" id="form-submit">Сохранить</button>
      </div>
    </form>`;
}

/** Клиентская валидация (зеркалирует ограничения класса). */
function clientValidate(form) {
    const errors = {};
    const val = n => form.querySelector(`[name="${n}"]`).value.trim();
    const num = n => val(n) === "" ? null : Number(val(n));

    if (!val("name")) errors["name"] = "Поле name не может быть null, строка не может быть пустой";
    const x = num("coordinates.x");
    if (x === null || isNaN(x)) errors["coordinates.x"] = "Поле x не может быть null";
    else if (x > 862) errors["coordinates.x"] = "Максимальное значение поля x: 862";
    const y = num("coordinates.y");
    if (y === null || isNaN(y)) errors["coordinates.y"] = "Поле y обязательно";
    else if (!Number.isInteger(y)) errors["coordinates.y"] = "Поле y — целое число";
    else if (y <= -860) errors["coordinates.y"] = "Значение поля y должно быть больше -860";
    const ep = num("enginePower");
    if (ep === null || isNaN(ep)) errors["enginePower"] = "Поле enginePower обязательно";
    else if (ep <= 0) errors["enginePower"] = "Значение поля enginePower должно быть больше 0";
    const nw = num("numberOfWheels");
    if (nw !== null && (isNaN(nw) || nw <= 0)) errors["numberOfWheels"] = "Значение поля numberOfWheels должно быть больше 0";
    const cap = num("capacity");
    if (cap === null || isNaN(cap)) errors["capacity"] = "Поле capacity обязательно";
    else if (cap <= 0 || !Number.isInteger(cap)) errors["capacity"] = "Значение поля capacity должно быть целым и больше 0";
    const dt = num("distanceTravelled");
    if (dt === null || isNaN(dt)) errors["distanceTravelled"] = "Поле distanceTravelled обязательно";
    else if (dt <= 0) errors["distanceTravelled"] = "Значение поля distanceTravelled должно быть больше 0";
    const fc = num("fuelConsumption");
    if (fc === null || isNaN(fc)) errors["fuelConsumption"] = "Поле fuelConsumption обязательно";
    else if (fc <= 0) errors["fuelConsumption"] = "Значение поля fuelConsumption должно быть больше 0";
    return errors;
}

function showFieldErrors(form, errors) {
    form.querySelectorAll(".error-msg").forEach(e => { e.classList.remove("show"); e.textContent = ""; });
    form.querySelectorAll("input, select").forEach(i => i.classList.remove("invalid"));
    for (const [field, msg] of Object.entries(errors)) {
        const wrap = form.querySelector(`.field[data-field="${CSS.escape(field)}"]`);
        if (wrap) {
            const em = wrap.querySelector(".error-msg");
            em.textContent = msg;
            em.classList.add("show");
            wrap.querySelector("input, select")?.classList.add("invalid");
        }
    }
    const ge = document.getElementById("form-general-error");
    if (errors.general || errors["general"]) {
        ge.textContent = errors.general || errors["general"];
        ge.style.display = "block";
    } else ge.style.display = "none";
}

function collectPayload(form) {
    const val = n => form.querySelector(`[name="${n}"]`).value.trim();
    const numOrNull = n => val(n) === "" ? null : Number(val(n));
    const coordinatesId = val("coordinates.id");
    return {
        name: val("name"),
        coordinates: { id: coordinatesId === "" ? null : Number(coordinatesId), x: numOrNull("coordinates.x"), y: numOrNull("coordinates.y") },
        type: val("type") || null,
        enginePower: numOrNull("enginePower"),
        numberOfWheels: numOrNull("numberOfWheels"),
        capacity: numOrNull("capacity"),
        distanceTravelled: numOrNull("distanceTravelled"),
        fuelConsumption: numOrNull("fuelConsumption"),
        fuelType: val("fuelType") || null
    };
}

async function openVehicleForm(id, title) {
    let v = {};
    if (id != null) {
        try { v = await apiFetch(`/vehicles/${id}`); }
        catch (e) { toast(e.message); return; }
    }
    let coordinates = [];
    try { coordinates = await apiFetch("/coordinates"); }
    catch (e) { toast("Не удалось загрузить список Coordinates: " + e.message); }
    const backdrop = document.getElementById("modal-backdrop");
    document.getElementById("modal-title").textContent = title;
    const body = document.getElementById("modal-body");
    body.innerHTML = vehicleFormHtml(v, coordinates);
    backdrop.classList.remove("hidden");
    const form = body.querySelector("#vehicle-form");
    form.querySelector('[name="coordinates.id"]').onchange = e => {
        const selected = coordinates.find(c => String(c.id) === e.target.value);
        if (!selected) return;
        form.querySelector('[name="coordinates.x"]').value = selected.x;
        form.querySelector('[name="coordinates.y"]').value = selected.y;
    };
    form.onsubmit = async ev => {
        ev.preventDefault();
        const errors = clientValidate(form);
        if (Object.keys(errors).length) { showFieldErrors(form, errors); return; }
        showFieldErrors(form, {});
        const payload = collectPayload(form);
        try {
            if (id == null) {
                await apiFetch("/vehicles", { method: "POST", body: JSON.stringify(payload) });
                toast("Объект создан");
            } else {
                await apiFetch(`/vehicles/${id}`, { method: "PUT", body: JSON.stringify(payload) });
                toast("Объект обновлён");
            }
            closeModal();
            refreshTable();
        } catch (e) {
            if (e.body?.errors) showFieldErrors(form, e.body.errors);
            else showFieldErrors(form, { general: e.message });
        }
    };
}

function closeModal() {
    document.getElementById("modal-backdrop").classList.add("hidden");
    document.getElementById("modal-body").innerHTML = "";
}

document.getElementById("modal-backdrop").addEventListener("click", e => {
    if (e.target.id === "modal-backdrop") closeModal();
});

/* ------------------------- просмотр объекта (отдельное окно) ------------------------- */

async function renderDetailPage(id) {
    const app = document.getElementById("app");
    app.innerHTML = `<h2 class="page-title">Информация об объекте №${id}</h2><div class="panel">Загрузка…</div>`;
    let v;
    try { v = await apiFetch(`/vehicles/${id}`); }
    catch (e) {
        app.innerHTML = `<h2 class="page-title">Информация об объекте</h2>
            <div class="panel"><div class="result-box error">${esc(e.message)}</div>
            <p><a href="#/">← К списку объектов</a></p></div>`;
        return;
    }
    const row = (k, val) => `<tr><td>${esc(k)}</td><td>${val == null || val === "" ? '<span class="badge">null</span>' : esc(val)}</td></tr>`;
    app.innerHTML = `<h2 class="page-title">Информация об объекте №${id}</h2>
        <div class="panel">
            <table class="grid detail-table"><tbody>
                ${row("ID", v.id)}
                ${row("Название", v.name)}
                ${row("Координаты (связанный объект Coordinates)", v.coordinates ? `x=${v.coordinates.x}, y=${v.coordinates.y}` : null)}
                ${row("Дата создания", v.creationDate)}
                ${row("Тип ТС (VehicleType)", v.type)}
                ${row("Мощность двигателя", v.enginePower)}
                ${row("Количество колёс", v.numberOfWheels)}
                ${row("Вместимость", v.capacity)}
                ${row("Пробег", v.distanceTravelled)}
                ${row("Расход топлива", v.fuelConsumption)}
                ${row("Тип топлива (FuelType)", v.fuelType)}
            </tbody></table>
            <div style="margin-top:16px; display:flex; gap:10px;">
                <button class="btn" id="d-edit">Обновить объект</button>
                <button class="btn danger" id="d-del">Удалить</button>
                <a href="#/" class="btn secondary" style="text-decoration:none">← К списку</a>
            </div>
        </div>`;
    document.getElementById("d-edit").onclick = () => openVehicleForm(id, "Обновление объекта");
    document.getElementById("d-del").onclick = () => confirmDelete(id);
}

/* ------------------------- удаление ------------------------- */

async function confirmDelete(id) {
    if (!confirm(`Удалить транспортное средство №${id}?`)) return;
    try {
        await apiFetch(`/vehicles/${id}`, { method: "DELETE" });
        toast("Объект удалён");
        if (location.hash.startsWith("#/view/")) location.hash = "#/";
        else refreshTable();
    } catch (e) {
        toast(e.message);
    }
}

/* ------------------------- специальные операции ------------------------- */

async function renderSpecialPage() {
    const app = document.getElementById("app");
    app.innerHTML = `<h2 class="page-title">Специальные операции</h2>

    <div class="panel"><b>1. Сумма значений поля fuelConsumption для всех объектов</b><br><br>
        <button class="btn" id="op1">Выполнить</button>
        <div class="result-box" id="r1" style="display:none"></div></div>

    <div class="panel"><b>2. Вернуть один (любой) объект, значение поля type которого минимально</b><br><br>
        <button class="btn" id="op2">Выполнить</button>
        <div class="result-box" id="r2" style="display:none"></div></div>

    <div class="panel"><b>3. Количество объектов, значение поля fuelType которых больше заданного</b><br><br>
        <label>FuelType:
            <select id="op3-fuel">${FUEL_TYPES.map(f => `<option>${f}</option>`).join("")}</select>
        </label>
        <button class="btn" id="op3">Выполнить</button>
        <div class="result-box" id="r3" style="display:none"></div></div>

    <div class="panel"><b>4. Найти все транспортные средства заданного типа</b><br><br>
        <label>VehicleType:
            <select id="op4-type">${VEHICLE_TYPES.map(t => `<option>${t}</option>`).join("")}</select>
        </label>
        <button class="btn" id="op4">Выполнить</button>
        <div class="result-box" id="r4" style="display:none"></div></div>

    <div class="panel"><b>5. Добавить транспортному средству с заданным id указанное число колёс</b><br><br>
        <label>ID: <input type="number" id="op5-id" min="1" style="width:100px"></label>
        <label>Число колёс: <input type="number" id="op5-count" min="1" value="2" style="width:100px"></label>
        <button class="btn" id="op5">Выполнить</button>
        <div class="result-box" id="r5" style="display:none"></div></div>`;

    const show = (boxId, html, isError) => {
        const b = document.getElementById(boxId);
        b.style.display = "block";
        b.classList.toggle("error", !!isError);
        b.innerHTML = html;
    };

    document.getElementById("op1").onclick = async () => {
        try { const r = await apiFetch("/special/fuel-consumption-sum");
            show("r1", `Σ fuelConsumption = <b>${r.result}</b>`);
        } catch (e) { show("r1", esc(e.message), true); }
    };
    document.getElementById("op2").onclick = async () => {
        try { const v = await apiFetch("/special/min-by-type");
            show("r2", `Объект с минимальным type (<b>${esc(v.type)}</b>):
                 ID=${v.id}, название «${esc(v.name)}» — <a href="#/view/${v.id}">открыть</a>`);
        } catch (e) { show("r2", esc(e.message), true); }
    };
    document.getElementById("op3").onclick = async () => {
        const ft = document.getElementById("op3-fuel").value;
        try { const r = await apiFetch(`/special/count-fuel-type-greater?fuelType=${ft}`);
            show("r3", `Объектов с fuelType &gt; <b>${ft}</b>: <b>${r.result}</b>`);
        } catch (e) { show("r3", esc(e.body?.errors?.fuelType || e.message), true); }
    };
    document.getElementById("op4").onclick = async () => {
        const t = document.getElementById("op4-type").value;
        try { const r = await apiFetch(`/special/by-type?type=${t}`);
            show("r4", r.items.length
                ? `Найдено объектов: <b>${r.items.length}</b><ul>` +
                  r.items.map(v => `<li>ID=${v.id} — «${esc(v.name)}» (<a href="#/view/${v.id}">просмотр</a>)</li>`).join("") + "</ul>"
                : "Объектов заданного типа не найдено");
        } catch (e) { show("r4", esc(e.body?.errors?.type || e.message), true); }
    };
    document.getElementById("op5").onclick = async () => {
        const id = document.getElementById("op5-id").value.trim();
        const count = document.getElementById("op5-count").value.trim();
        if (!id || Number(id) <= 0) { show("r5", "Укажите корректный ID (больше 0)", true); return; }
        if (!count || Number(count) <= 0) { show("r5", "Число добавляемых колёс должно быть больше 0", true); return; }
        try { const v = await apiFetch(`/special/${id}/wheels?count=${count}`, { method: "POST" });
            show("r5", `Готово. Теперь у объекта «${esc(v.name)}» колёс: <b>${v.numberOfWheels}</b>`);
            refreshTable();
        } catch (e) { show("r5", esc(e.body?.error || e.body?.errors?.wheelsToAdd || e.message), true); }
    };
}

/* ------------------------- SSE: автообновление таблицы у всех клиентов ------------------------- */

let sseSource = null;
function connectSse() {
    if (sseSource) sseSource.close();
    sseSource = new EventSource(`${API}/vehicles/events`);
    sseSource.onopen = () => {
        const s = document.getElementById("sse-status");
        s.textContent = "● online"; s.classList.add("online");
    };
    sseSource.onerror = () => {
        const s = document.getElementById("sse-status");
        s.textContent = "● offline"; s.classList.remove("online");
    };
    const handler = name => e => {
        try {
            const { vehicle } = JSON.parse(e.data);
            toast(`Объект «${vehicle.name}» (id=${vehicle.id}) ${
                name === "created" ? "добавлен другим пользователем" :
                name === "updated" ? "изменён другим пользователем" : "удалён другим пользователем"}`);
            refreshTable();
            if (location.hash === `#/view/${vehicle.id}` && name === "deleted") location.hash = "#/";
        } catch (err) { /* ignore */ }
    };
    sseSource.addEventListener("created", handler("created"));
    sseSource.addEventListener("updated", handler("updated"));
    sseSource.addEventListener("deleted", handler("deleted"));
}

/* ------------------------- init ------------------------- */

window.App = { closeModal, openVehicleForm };
document.addEventListener("DOMContentLoaded", () => {
    router();
    connectSse();
});
