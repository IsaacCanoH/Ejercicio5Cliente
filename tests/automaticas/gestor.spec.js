const { test: base, expect } = require('playwright/test');
const crypto = require('node:crypto');

const backend = process.env.BACKEND_URL || 'http://localhost:8081';

async function json(response) {
  expect(response.ok(), `HTTP ${response.status()} al consultar ${response.url()}`).toBeTruthy();
  return response.json();
}

const test = base.extend({
  app: async ({ page, request }, use, testInfo) => {
    const id = testInfo.title.match(/AUT-[EF]\d+/)[0];
    const prefix = `P2-${id}-${crypto.randomUUID().slice(0, 8)}`;
    const projectNames = new Set([prefix]);
    const userEmails = new Set([`${prefix.toLowerCase()}@example.test`]);
    const user = {
      nombre: `Usuario ${prefix}`,
      correo_electronico: [...userEmails][0],
      contrasena: 'AutoP2_123',
      rol: 'Estudiante',
      imagen_usuario: '#483078'
    };
    const api = {
      post: (url, data) => request.post(`${backend}${url}`, { data }),
      get: url => request.get(`${backend}${url}`),
      delete: (url, data) => request.delete(`${backend}${url}`, { data })
    };
    let project;
    let task;
    const requests = [];
    const pageErrors = [];
    const responses = [];
    page.on('pageerror', error => pageErrors.push(error.message));
    page.on('request', req => {
      const url = new URL(req.url());
      if (url.origin === new URL(testInfo.project.use.baseURL).origin && req.method() === 'POST') {
        const body = req.postData() || '';
        requests.push({ metodo: req.method(), ruta: url.pathname, datos: body.replace(/("?contrasena"?[:=]\s*)[^&,}]+/g, '$1[oculta]') });
      }
    });
    page.on('response', response => {
      const url = new URL(response.url());
      if (url.origin === new URL(testInfo.project.use.baseURL).origin && response.request().method() === 'POST') {
        responses.push({ ruta: url.pathname, estado: response.status() });
      }
    });
    const findProject = async name => (await json(await api.post('/buscarProyectos'))).find(p => p.nombre === name);
    const getTask = async taskId => json(await api.get(`/buscarTarea?id_tarea=${taskId}`));
    const getComments = async taskId => json(await api.post('/getComentario', { id_tarea: taskId }));
    try {
      await json(await api.post('/addUsuario', user));
      const registered = (await json(await api.post('/buscarUsuarios'))).find(u => u.correo_electronico === user.correo_electronico);
      expect(registered).toBeDefined();
      user.id_usuario = registered.id_usuario;
      await json(await api.post('/addProyecto', {
        nombre: prefix, descripcion: `Proyecto base ${prefix}`,
        fecha_inicio: '2026-10-11', fecha_finalizacion: '2026-10-18',
        estado_proyecto: 'En proceso', fondo: '/Images/blanco.png'
      }));
      project = await findProject(prefix);
      expect(project).toBeDefined();
      await json(await api.post('/addTareas', {
        nombre: `Tarea ${prefix}`, descripcion: 'Descripción base',
        fecha_vencimiento: '2026-10-16', lista: 'pendientes',
        estado: 'primary', id_proyecto: project.id_proyecto
      }));
      task = (await json(await api.post('/getTarea', { id_proyecto: project.id_proyecto })))[0];
      await json(await api.post('/addComentario', {
        id_tarea: task.id_tarea, id_usuario: user.nombre,
        texto_comentario: `Comentario base ${prefix}`, fecha_hora: '2026-10-11 12:00:00'
      }));
      await page.addInitScript(value => localStorage.setItem('usuario', JSON.stringify(value)), user);
      const app = {
        prefix, project, task, user, api, projectNames, userEmails, requests, pageErrors,
        findProject, getTask, getComments,
        card: () => page.locator('#cardContainer .card').filter({ has: page.getByRole('heading', { name: prefix, exact: true }) }),
        openBoard: async () => {
          const loaded = page.waitForResponse(r => new URL(r.url()).pathname === '/obtenerTareas');
          await page.goto(`/proyecto?id_proyecto=${project.id_proyecto}`);
          expect((await loaded).ok()).toBeTruthy();
          await expect(page.locator('#tareasPendientes').getByRole('button', { name: task.nombre, exact: true })).toBeVisible();
        },
        openTask: async () => {
          const loaded = page.waitForResponse(r => new URL(r.url()).pathname === '/obtenerComentarios');
          await page.getByRole('button', { name: task.nombre, exact: true }).first().click();
          expect((await loaded).ok()).toBeTruthy();
          await expect(page.locator('#editarTarea')).toBeVisible();
        },
        openProjectEdit: async () => {
          await page.goto('/inicio');
          await expect(app.card()).toBeVisible();
          await app.card().locator('[data-bs-toggle="dropdown"]').click();
          await app.card().getByText('Editar', { exact: true }).click();
        },
        deleteTask: async () => {
          await page.locator('#tareasPendientes li').filter({ has: page.getByRole('button', { name: task.nombre, exact: true }) }).locator('button').last().click();
          await expect(page.locator('#modalAlerta')).toBeVisible();
          await page.locator('#modalAlerta').getByRole('button', { name: 'Aceptar', exact: true }).click();
        },
        deleteProject: async () => {
          await app.card().locator('[data-bs-toggle="dropdown"]').click();
          await app.card().getByText('Eliminar', { exact: true }).click();
          await expect(page.locator('#modalAlerta')).toBeVisible();
          await page.locator('#modalAlerta').getByRole('button', { name: 'Aceptar', exact: true }).click();
        }
      };
      await use(app);
    } finally {
      await testInfo.attach('peticiones-y-respuestas', { body: Buffer.from(JSON.stringify({ requests, responses, pageErrors }, null, 2)), contentType: 'application/json' });
      // Solo limpiar registros creados por este caso, incluyendo los creados antes de un fallo.
      const projects = (await json(await api.post('/buscarProyectos'))).filter(p => projectNames.has(p.nombre));
      const ownedProjectIds = new Set(projects.map(p => p.id_proyecto));
      if (project) ownedProjectIds.add(project.id_proyecto);
      const tasks = (await json(await api.post('/buscarTareas'))).filter(t => ownedProjectIds.has(t.id_proyecto));
      const ownedTaskIds = new Set(tasks.map(t => t.id_tarea));
      if (task) ownedTaskIds.add(task.id_tarea);
      const comments = (await json(await api.post('/buscarComentarios'))).filter(c => ownedTaskIds.has(c.id_tarea));
      for (const comment of comments) await json(await api.delete('/deleteComentario', comment));
      for (const ownedTask of tasks) await json(await api.delete('/deleteTareas', ownedTask));
      for (const ownedProject of projects) await json(await api.delete('/deleteProyecto', ownedProject));
      const users = (await json(await api.post('/buscarUsuarios'))).filter(u => userEmails.has(u.correo_electronico));
      for (const ownedUser of users) await json(await api.delete('/deleteUsuario', ownedUser));
      await testInfo.attach('limpieza', { body: Buffer.from(JSON.stringify({ usuarios: users.length, proyectos: projects.length, tareas: tasks.length, comentarios: comments.length })), contentType: 'application/json' });
    }
  }
});

async function fillRegistration(page, data) {
  await page.locator('input[name="nombre"]').fill(data.nombre);
  await page.locator('input[name="correo_electronico"]').fill(data.correo_electronico);
  await page.locator('input[name="contrasena"]').fill(data.contrasena);
  await page.locator('select[name="rol"]').selectOption('Estudiante');
}

async function fillProject(page, name) {
  await page.locator('#staticBackdrop #nombre').fill(name);
  await page.locator('#staticBackdrop textarea[name="descripcion"]').fill('Proyecto creado por la prueba automática');
  await page.locator('#fecha_inicio').fill('2026-10-11');
  await page.locator('#fecha_finalizacion').fill('2026-10-18');
  await page.locator('#estado_proyecto').selectOption('En proceso');
}

async function mockError(page, url, status = 500) {
  await page.route(`**/${url}`, route => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify({ status, error: 'Fallo simulado por Playwright' }) }));
  test.info().annotations.push({ type: 'simulacion', description: `HTTP ${status} interceptado en /${url}; el backend permanece disponible.` });
}

test('AUT-E01 | Registro válido: usuario persistido y redirección a login', async ({ page, app }) => {
  const data = { ...app.user, nombre: `Registro ${app.prefix}`, correo_electronico: `registro-${app.prefix.toLowerCase()}@example.test` };
  app.userEmails.add(data.correo_electronico);
  await page.goto('/register');
  await fillRegistration(page, data);
  await page.getByRole('button', { name: 'Registrar', exact: true }).click();
  await expect(page).toHaveURL(/\/login$/);
  const users = await json(await app.api.post('/buscarUsuarios'));
  const saved = users.filter(u => u.correo_electronico === data.correo_electronico);
  expect(saved).toHaveLength(1);
  expect(saved[0]).toMatchObject({ nombre: data.nombre, rol: 'Estudiante' });
});

test('AUT-F01 | Registro incompleto: nombre vacío bloquea el envío', async ({ page, app }) => {
  await page.goto('/register');
  await fillRegistration(page, { ...app.user, nombre: '' });
  await page.getByRole('button', { name: 'Registrar', exact: true }).click();
  await expect(page.locator('form')).toHaveClass(/was-validated/);
  expect(await page.locator('input[name="nombre"]').evaluate(el => el.validity.valueMissing)).toBe(true);
  expect(app.requests.filter(r => r.ruta === '/saveUsuario')).toHaveLength(0);
  await expect(page).toHaveURL(/\/register$/);
});

test('AUT-E02 | Login válido: accede a inicio y conserva el usuario', async ({ page, app }) => {
  await page.goto('/login');
  await page.locator('#exampleInputEmail1').fill(app.user.correo_electronico);
  await page.locator('#exampleInputPassword1').fill(app.user.contrasena);
  await page.getByRole('button', { name: /Iniciar sesi/ }).click();
  await expect(page).toHaveURL(/\/inicio$/);
  const logged = await page.evaluate(() => JSON.parse(localStorage.getItem('usuario')));
  expect(logged.id_usuario).toBe(app.user.id_usuario);
  await expect(app.card()).toBeVisible();
});

test('AUT-F02 | Login inválido: rechaza una contraseña incorrecta', async ({ page, app }) => {
  await page.goto('/login');
  await page.evaluate(() => localStorage.removeItem('usuario'));
  await page.locator('#exampleInputEmail1').fill(app.user.correo_electronico);
  await page.locator('#exampleInputPassword1').fill('Incorrecta_123');
  await page.getByRole('button', { name: /Iniciar sesi/ }).click();
  await expect(page.locator('#alerta5')).toBeVisible();
  await expect(page).toHaveURL(/\/login$/);
  expect(await page.evaluate(() => localStorage.getItem('usuario'))).toBeNull();
});

test('AUT-E03 | Consulta válida: muestra proyecto, tarea y comentario reales', async ({ page, app }) => {
  await page.goto('/inicio');
  await expect(app.card()).toContainText(app.project.descripcion);
  await app.card().getByRole('link', { name: 'Ir al proyecto', exact: true }).click();
  await expect(page.locator('#tareasPendientes').getByRole('button', { name: app.task.nombre, exact: true })).toBeVisible();
  await app.openTask();
  await expect(page.locator('#contenedorComentario')).toContainText(`Comentario base ${app.prefix}`);
  await expect(page.locator('#contenedorComentario')).toContainText(app.user.nombre);
  expect(app.pageErrors).toEqual([]);
});

test('AUT-F03 | Consulta fallida: HTTP 500 simulado muestra aviso visible', async ({ page, app }) => {
  await mockError(page, 'consultarProyectos');
  await page.goto('/inicio');
  await expect(page.getByRole('alert')).toContainText('No fue posible cargar los proyectos');
  await expect(page.locator('#cardContainer .card')).toHaveCount(0);
  expect(await app.findProject(app.prefix)).toBeDefined();
});

test('AUT-E04 | Proyecto válido: crea una tarjeta y persiste los datos', async ({ page, app }) => {
  const name = `${app.prefix} nuevo`;
  app.projectNames.add(name);
  await page.goto('/inicio');
  await page.getByRole('button', { name: 'Agregar', exact: true }).click();
  await fillProject(page, name);
  await page.locator('#staticBackdrop').getByRole('button', { name: 'Guardar', exact: true }).click();
  await expect(page.locator('#cardContainer .card').filter({ hasText: name })).toBeVisible();
  const saved = await app.findProject(name);
  expect(saved).toMatchObject({ nombre: name, fecha_inicio: '2026-10-11', fecha_finalizacion: '2026-10-18', estado_proyecto: 'En proceso' });
});

test('AUT-F04 | Proyecto incompleto: nombre vacío bloquea la creación', async ({ page, app }) => {
  await page.goto('/inicio');
  await page.getByRole('button', { name: 'Agregar', exact: true }).click();
  await fillProject(page, '');
  await page.locator('#staticBackdrop').getByRole('button', { name: 'Guardar', exact: true }).click();
  expect(await page.locator('#nombre').evaluate(el => el.validity.valueMissing)).toBe(true);
  expect(app.requests.filter(r => r.ruta === '/saveProyecto')).toHaveLength(0);
  await expect(page.locator('#staticBackdrop')).toBeVisible();
});

test('AUT-E05 | Edición válida: conserva ID y actualiza descripción del proyecto', async ({ page, app }) => {
  await app.openProjectEdit();
  await expect(page.locator('#modalEditar.modal')).toBeVisible();
  await page.locator('#editarDescripcion').fill(`Edición ${app.prefix}`);
  await page.locator('#modalEditar.modal').getByRole('button', { name: 'Guardar', exact: true }).click();
  await expect(app.card()).toContainText(`Edición ${app.prefix}`);
  const saved = await app.findProject(app.prefix);
  expect(saved.id_proyecto).toBe(app.project.id_proyecto);
  expect(saved.descripcion).toBe(`Edición ${app.prefix}`);
  expect(app.pageErrors).toEqual([]);
});

test('AUT-F05 | Edición fallida: proyecto no encontrado simulado informa el error', async ({ page, app }) => {
  await mockError(page, 'actualizarProyecto', 404);
  await app.openProjectEdit();
  await expect(page.getByRole('alert').filter({ hasText: /proyecto/i })).toBeVisible();
  await expect(page.locator('#modalEditar.modal')).toBeHidden();
  expect((await app.findProject(app.prefix)).descripcion).toBe(app.project.descripcion);
});

test('AUT-E06 | Tarea válida: crea una tarea pendiente en el proyecto correcto', async ({ page, app }) => {
  const name = `Nueva ${app.prefix}`;
  await app.openBoard();
  await page.locator('#btnAgregarTarea').click();
  await page.locator('#inputTarea').fill(name);
  await page.locator('#btnTarea').click();
  await expect(page.locator('#tareasPendientes').getByRole('button', { name, exact: true })).toBeVisible();
  const tasks = await json(await app.api.post('/getTarea', { id_proyecto: app.project.id_proyecto }));
  expect(tasks.filter(t => t.nombre === name)).toHaveLength(1);
  expect(tasks.find(t => t.nombre === name)).toMatchObject({ lista: 'pendientes', id_proyecto: app.project.id_proyecto });
});

test('AUT-F06 | Tarea incompleta: nombre vacío no envía ni crea un registro', async ({ page, app }) => {
  await app.openBoard();
  await page.locator('#btnAgregarTarea').click();
  await page.locator('#btnTarea').click();
  await expect(page.getByRole('alert').filter({ hasText: /nombre.*tarea/i })).toBeVisible();
  expect(app.requests.filter(r => r.ruta === '/verificarTarea')).toHaveLength(0);
  const tasks = await json(await app.api.post('/getTarea', { id_proyecto: app.project.id_proyecto }));
  expect(tasks).toHaveLength(1);
});

test('AUT-E07 | Edición válida: actualiza descripción, fecha y lista de la tarea', async ({ page, app }) => {
  await app.openBoard();
  await app.openTask();
  await page.locator('#descripcionT').fill(`Actualizada ${app.prefix}`);
  await page.locator('#editarTarea').getByRole('button', { name: /Fechas/ }).click();
  await page.locator('#inputFechas').fill('2026-10-17');
  await page.locator('#cambiarLista').selectOption('terminado');
  await page.locator('#editarTarea').getByRole('button', { name: 'Guardar cambios', exact: true }).click();
  await expect(page.locator('#trabajoTerminado').getByRole('button', { name: app.task.nombre, exact: true })).toBeVisible();
  expect(await app.getTask(app.task.id_tarea)).toMatchObject({ id_tarea: app.task.id_tarea, id_proyecto: app.project.id_proyecto, descripcion: `Actualizada ${app.prefix}`, fecha_vencimiento: '2026-10-17', lista: 'terminado' });
});

test('AUT-F07 | Edición fallida: HTTP 500 simulado informa y conserva la tarea', async ({ page, app }) => {
  await app.openBoard();
  await app.openTask();
  await mockError(page, 'saveTarea');
  await page.locator('#descripcionT').fill(`No guardar ${app.prefix}`);
  await page.locator('#editarTarea').getByRole('button', { name: 'Guardar cambios', exact: true }).click();
  await expect(page.locator('#editarTarea').getByRole('alert')).toContainText(/guardar.*tarea/i);
  await expect(page.locator('#editarTarea')).toBeVisible();
  expect((await app.getTask(app.task.id_tarea)).descripcion).toBe(app.task.descripcion);
});

test('AUT-E08 | Comentario válido: un único envío, respuesta exitosa y persistencia', async ({ page, app }) => {
  await app.openBoard();
  await app.openTask();
  const text = `Comentario automático ${app.prefix}`;
  await page.locator('#comentarioT').fill(text);
  const response = page.waitForResponse(r => new URL(r.url()).pathname === '/saveComentario');
  await page.locator('button[onclick="guardarComentario()"]').click();
  const result = await response;
  expect(result.ok(), `saveComentario respondió HTTP ${result.status()}`).toBeTruthy();
  expect(result.headers()['content-type']).toContain('application/json');
  await expect(page.locator('#editarTarea')).toBeHidden();
  await expect(page.getByRole('button', { name: app.task.nombre, exact: true }).first()).toBeVisible();
  await app.openTask();
  await expect(page.locator('#contenedorComentario')).toContainText(text);
  const comments = await app.getComments(app.task.id_tarea);
  expect(comments.filter(c => c.texto_comentario === text)).toHaveLength(1);
  expect(comments.find(c => c.texto_comentario === text)).toMatchObject({ id_tarea: app.task.id_tarea, id_usuario: app.user.nombre });
  expect(app.requests.filter(r => r.ruta === '/saveComentario')).toHaveLength(1);
  expect(app.requests.filter(r => r.ruta === '/saveTarea')).toHaveLength(0);
});

test('AUT-F08 | Comentario incompleto: texto vacío bloquea el envío', async ({ page, app }) => {
  await app.openBoard();
  await app.openTask();
  await page.locator('button[onclick="guardarComentario()"]').click();
  await expect(page.locator('#editarTarea').getByRole('alert')).toContainText(/comentario/i);
  expect(app.requests.filter(r => r.ruta === '/saveComentario')).toHaveLength(0);
  expect(await app.getComments(app.task.id_tarea)).toHaveLength(1);
});

test('AUT-E09 | Eliminación válida: tarea ausente del tablero y del backend', async ({ page, app }) => {
  await app.openBoard();
  await app.deleteTask();
  await expect(page.locator('#modalAlerta')).toBeHidden();
  await expect(page.locator('#tareasPendientes').getByRole('button', { name: app.task.nombre, exact: true })).toHaveCount(0);
  const tasks = await json(await app.api.post('/getTarea', { id_proyecto: app.project.id_proyecto }));
  expect(tasks.some(t => t.id_tarea === app.task.id_tarea)).toBe(false);
});

test('AUT-F09 | Eliminación fallida: HTTP 500 simulado conserva tarea y muestra error', async ({ page, app }) => {
  await app.openBoard();
  await mockError(page, 'eliminarTarea');
  await app.deleteTask();
  await expect(page.getByRole('alert').filter({ hasText: /eliminar.*tarea/i })).toBeVisible();
  await expect(page.locator('#tareasPendientes').getByRole('button', { name: app.task.nombre, exact: true })).toBeVisible();
  expect((await app.getTask(app.task.id_tarea)).id_tarea).toBe(app.task.id_tarea);
});

test('AUT-E10 | Eliminación válida: proyecto ausente de la lista y del backend', async ({ page, app }) => {
  await page.goto('/inicio');
  await expect(app.card()).toBeVisible();
  await app.deleteProject();
  await expect(page.locator('#modalAlerta')).toBeHidden();
  await expect(app.card()).toHaveCount(0);
  expect(await app.findProject(app.prefix)).toBeUndefined();
});

test('AUT-F10 | Eliminación fallida: HTTP 500 simulado conserva proyecto y muestra error', async ({ page, app }) => {
  await page.goto('/inicio');
  await expect(app.card()).toBeVisible();
  await mockError(page, 'eliminarProyecto');
  await app.deleteProject();
  await expect(page.getByRole('alert').filter({ hasText: /eliminar.*proyecto/i })).toBeVisible();
  await expect(app.card()).toBeVisible();
  expect((await app.findProject(app.prefix)).id_proyecto).toBe(app.project.id_proyecto);
});
