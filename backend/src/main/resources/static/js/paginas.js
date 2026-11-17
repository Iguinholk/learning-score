'use strict';
/* ===== Páginas do Learning Score (cada função desenha uma rota dentro de <main>) ===== */

const graficos = [];
const limparGraficos = () => { while (graficos.length) graficos.pop().destroy(); };
const hojeISO = () => new Date().toISOString().slice(0, 10);
const delegar = (el, mapa) => { el.onclick = ev => { const b = ev.target.closest('[data-acao]'); if (b && mapa[b.dataset.acao]) mapa[b.dataset.acao](b.dataset, b); }; };
const cabecalho = (titulo, sub, botoes = '') => html`<div class="pg-head"><div><h1>${titulo}</h1>${raw(sub ? html`<p>${sub}</p>` : '')}</div><div class="acoes">${raw(botoes)}</div></div>`;
const indicador = (v, r, alerta = false) => html`<div class="ind ${alerta ? 'alerta' : ''}"><span class="v">${v}</span><span class="r">${r}</span></div>`;
const ligacaoSegura = u => /^https?:\/\//i.test(u || '') ? u : '#';

/* ---------- RF01 · Dashboard ---------- */

async function pgDashboard(el) {
  const d = await api.get('/api/dashboard');
  const i = d.indicadores;

  const itemAtv = a => html`<li>
    <div>
      <a href="#/atividades/${a.id}">
        <strong>${a.titulo}</strong>
      </a>
      <br>
      <span class="mudo">
        ${a.disciplinaNome || 'Sem disciplina'}, ${fmtDataHora(a.prazo)}
      </span>
    </div>
    <div>
      ${raw(badge(a.prioridade))}
      ${raw(badge(a.status))}
    </div>
  </li>`;


  el.innerHTML = html`
    ${raw(cabecalho(
        d.saudacao + ', ' + d.nome + '!',
        'Veja o que precisa da sua atenção hoje.',
        `<a class="btn sec sino-notificacoes" href="#/notificacoes" aria-label="Notificações" title="Notificações">${svg('sino')}<span class="selo" id="selo-notif" hidden></span></a><a class="btn" href="#/atividades">Ver todas as atividades</a>`
    ))}


    <section class="indicadores" aria-label="Indicadores de organização acadêmica">
      ${raw(indicador(i.taxaConclusaoPercentual + '%', 'das atividades concluídas'))}
      ${raw(indicador(i.entregasEm7Dias, 'entregas nos próximos 7 dias'))}
      ${raw(indicador(i.atrasadas, 'atividades atrasadas', i.atrasadas > 0))}
      ${raw(indicador(fmtNum(i.mediaGeral), 'média geral das notas'))}
      ${raw(indicador(
          i.frequenciaMedia == null
              ? '—'
              : fmtNum(i.frequenciaMedia) + '%',
          'frequência média'
      ))}
      ${raw(indicador(fmtMoeda(i.mensalidadesEmAberto), 'em aberto na instituição'))}
    </section>

    <div class="grade-2">
      <section class="painel">
        <h2>Atividades prioritárias</h2>
        ${raw(
            d.prioritarias.length
                ? html`<ul class="lista-limpa">${lista(d.prioritarias, itemAtv)}</ul>`
                : '<p class="vazio">Nenhuma atividade em aberto. 🎉</p>'
        )}
      </section>

      <section class="painel">
        <h2>Próximas datas de entrega</h2>
        ${raw(
            d.proximasEntregas.length
                ? html`<ul class="lista-limpa">${lista(d.proximasEntregas, itemAtv)}</ul>`
                : '<p class="vazio">Sem entregas futuras cadastradas.</p>'
        )}
      </section>
    </div>

    <div class="grade-2">
      <section class="painel painel-status">
        <h2>Atividades por status</h2>
        <canvas
            id="g1"
            role="img"
            aria-label="Gráfico de atividades por status"
        ></canvas>
        <p class="mudo" id="g1-txt"></p>
      </section>

      <section class="painel">
        <h2>Média por disciplina</h2>
        ${raw(
            d.mediasPorDisciplina.length
                ? '<canvas id="g2" role="img" aria-label="Gráfico de médias por disciplina"></canvas><p class="mudo" id="g2-txt"></p>'
                : '<p class="vazio">Cadastre notas em Acadêmico para ver o gráfico.</p>'
        )}
      </section>
    </div>
  `;


  // ============================================
  // GRÁFICOS DO DASHBOARD
  // ============================================

  if (typeof Chart === 'undefined') return;

  const st = Object.keys(d.atividadesPorStatus);

  const cor = {
    PENDENTE: '#C98F00',
    EM_ANDAMENTO: '#3B5BDB',
    CONCLUIDA: '#0B7A6E',
    ATRASADA: '#B3261E'
  };

  graficos.push(new Chart(document.getElementById('g1'), {
    type: 'doughnut',
    data: {
      labels: st.map(rot),
      datasets: [{
        data: st.map(k => d.atividadesPorStatus[k]),
        backgroundColor: st.map(k => cor[k])
      }]
    },
    options: {
      plugins: {
        legend: {
          position: 'bottom'
        }
      }
    }
  }));

  document.getElementById('g1-txt').textContent =
      st.map(k => rot(k) + ': ' + d.atividadesPorStatus[k]).join('; ') + '.';

  if (d.mediasPorDisciplina.length) {
    graficos.push(new Chart(document.getElementById('g2'), {
      type: 'bar',
      data: {
        labels: d.mediasPorDisciplina.map(m => m.disciplina),
        datasets: [{
          label: 'Média',
          data: d.mediasPorDisciplina.map(m => Number(m.media)),
          backgroundColor: '#1D2B4F'
        }]
      },
      options: {
        scales: {
          y: {
            min: 0,
            max: 10
          }
        },
        plugins: {
          legend: {
            display: false
          }
        }
      }
    }));

    document.getElementById('g2-txt').textContent =
        d.mediasPorDisciplina
            .map(m => m.disciplina + ': ' + fmtNum(m.media))
            .join('; ') + '.';
  }
}


/* ---------- RF02/RF03 · Atividades ---------- */
const OP_PRIORIDADE = ['BAIXA', 'MEDIA', 'ALTA'].map(v => ({ v, t: rot(v) }));
const OP_STATUS_FORM = ['PENDENTE', 'EM_ANDAMENTO', 'CONCLUIDA'].map(v => ({ v, t: rot(v) }));
const camposAtividade = discs => [
  { name: 'titulo', label: 'Título', obrigatorio: true, max: 160 },
  { name: 'descricao', label: 'Descrição', tipo: 'textarea', max: 2000 },
  { name: 'disciplinaId', label: 'Disciplina', tipo: 'select', opcoes: [{ v: '', t: '— Sem disciplina —' }, ...discs.map(d => ({ v: d.id, t: d.nome }))] },
  { name: 'prazo', label: 'Prazo de entrega', tipo: 'datetime-local', obrigatorio: true },
  { name: 'prioridade', label: 'Prioridade', tipo: 'select', opcoes: OP_PRIORIDADE },
  { name: 'status', label: 'Status', tipo: 'select', opcoes: OP_STATUS_FORM },
  { name: 'linkEntrega', label: 'Link da plataforma de entrega (opcional)', tipo: 'url', max: 500, dica: 'Endereço começando com https://. O Learning Score apenas abre o link; não envia trabalhos.' }
];
const corpoAtividade = v => ({ titulo: v.titulo, descricao: v.descricao || null, disciplinaId: v.disciplinaId ? Number(v.disciplinaId) : null,
  prazo: v.prazo.length === 16 ? v.prazo + ':00' : v.prazo, prioridade: v.prioridade, status: v.status, linkEntrega: v.linkEntrega || null });

async function pgAtividades(el) {
  const discs = await api.get('/api/disciplinas');
  const f = { status: '', disciplinaId: '', pagina: 0 };
  el.innerHTML = html`${raw(cabecalho('Atividades', 'Cadastre, acompanhe e entregue no prazo.', '<button class="btn" data-acao="nova">Nova atividade</button>'))}
    <section class="painel">
      <div class="filtros">
        <div><label for="fl-status">Status</label><select id="fl-status"><option value="">Todos</option>${lista(['PENDENTE', 'EM_ANDAMENTO', 'CONCLUIDA', 'ATRASADA'], s => html`<option value="${s}">${rot(s)}</option>`)}</select></div>
        <div><label for="fl-disc">Disciplina</label><select id="fl-disc"><option value="">Todas</option>${lista(discs, d => html`<option value="${d.id}">${d.nome}</option>`)}</select></div>
      </div>
      <div id="resultado"></div>
    </section>`;
  async function desenhar() {
    const q = new URLSearchParams({ pagina: f.pagina, tamanho: 20 });
    if (f.status) q.set('status', f.status); if (f.disciplinaId) q.set('disciplinaId', f.disciplinaId);
    const p = await api.get('/api/atividades?' + q);
    document.getElementById('resultado').innerHTML = !p.itens.length ? '<p class="vazio">Nenhuma atividade encontrada.</p>' : html`
      <div class="tabela-rolagem"><table><caption class="sr-only">Lista de atividades</caption>
        <thead><tr><th scope="col">Atividade</th><th scope="col">Disciplina</th><th scope="col">Prazo</th><th scope="col">Prioridade</th><th scope="col">Status</th><th scope="col">Ações</th></tr></thead>
        <tbody>${lista(p.itens, a => html`<tr><td><a href="#/atividades/${a.id}"><strong>${a.titulo}</strong></a></td><td>${a.disciplinaNome || '—'}</td><td>${fmtDataHora(a.prazo)}</td>
          <td>${raw(badge(a.prioridade))}</td><td>${raw(badge(a.status))}</td>
          <td><div class="acoes"><a class="btn sec peq" href="#/atividades/${a.id}">Detalhes</a>
          ${raw(a.linkEntrega ? html`<a class="btn peq" href="${ligacaoSegura(a.linkEntrega)}" target="_blank" rel="noopener noreferrer">Entregar</a>` : '')}</div></td></tr>`)}</tbody></table></div>
      <div class="paginacao"><span>Página ${p.pagina + 1} de ${Math.max(p.totalPaginas, 1)} (${p.total} atividades)</span>
        <button class="btn sec peq" data-acao="ant" ${raw(p.pagina === 0 ? 'disabled' : '')}>Anterior</button>
        <button class="btn sec peq" data-acao="prox" ${raw(p.pagina + 1 >= p.totalPaginas ? 'disabled' : '')}>Próxima</button></div>`;
  }
  document.getElementById('fl-status').onchange = e => { f.status = e.target.value; f.pagina = 0; desenhar(); };
  document.getElementById('fl-disc').onchange = e => { f.disciplinaId = e.target.value; f.pagina = 0; desenhar(); };
  delegar(el, {
    nova: async () => { if (await abrirForm({ titulo: 'Nova atividade', campos: camposAtividade(discs), valores: { prioridade: 'MEDIA', status: 'PENDENTE' }, enviar: v => api.post('/api/atividades', corpoAtividade(v)) })) { toast('Atividade criada.'); desenhar(); } },
    ant: () => { f.pagina--; desenhar(); }, prox: () => { f.pagina++; desenhar(); }
  });
  await desenhar();
}

async function pgAtividadeDetalhe(el, id) {
  const [a, discs] = await Promise.all([api.get('/api/atividades/' + id), api.get('/api/disciplinas')]);
  el.innerHTML = html`${raw(cabecalho(a.titulo, '', '<a class="btn sec" href="#/atividades">Voltar</a>'))}
    <section class="painel"><dl>
      <dt class="mudo">Disciplina</dt><dd>${a.disciplinaNome || '—'}</dd>
      <dt class="mudo">Prazo de entrega</dt><dd>${fmtDataHora(a.prazo)}</dd>
      <dt class="mudo">Prioridade</dt><dd>${raw(badge(a.prioridade))}</dd>
      <dt class="mudo">Status</dt><dd>${raw(badge(a.status))}</dd>
      <dt class="mudo">Descrição</dt><dd style="white-space:pre-wrap">${a.descricao || '—'}</dd></dl>
      <div class="acoes">
        ${raw(a.linkEntrega ? html`<a class="btn" href="${ligacaoSegura(a.linkEntrega)}" target="_blank" rel="noopener noreferrer">Ir para a plataforma de entrega</a>`
          : '<span class="mudo">Nenhum link de entrega cadastrado. Use “Editar” para adicionar.</span>')}
        ${raw(a.status !== 'CONCLUIDA' ? '<button class="btn sec" data-acao="concluir">Marcar como concluída</button>' : '')}
        <button class="btn sec" data-acao="editar">Editar</button><button class="btn perigo" data-acao="excluir">Excluir</button></div></section>`;
  delegar(el, {
    concluir: async () => { await api.put('/api/atividades/' + id, { ...corpoAtividade({ ...a, prazo: a.prazo, disciplinaId: a.disciplinaId, descricao: a.descricao, linkEntrega: a.linkEntrega }), status: 'CONCLUIDA' }); toast('Atividade concluída.'); pgAtividadeDetalhe(el, id); },
    editar: async () => { if (await abrirForm({ titulo: 'Editar atividade', campos: camposAtividade(discs), valores: { ...a, prazo: paraInputDataHora(a.prazo), disciplinaId: a.disciplinaId ?? '' }, enviar: v => api.put('/api/atividades/' + id, corpoAtividade(v)) })) { toast('Atividade atualizada.'); pgAtividadeDetalhe(el, id); } },
    excluir: async () => { if (await confirmar('Excluir esta atividade?', 'Excluir')) { await api.del('/api/atividades/' + id); toast('Atividade excluída.'); location.hash = '#/atividades'; } }
  });
}

/* ---------- RF05/RF06 · Calendário ---------- */
async function pgCalendario(el) {
  let ref = new Date(); ref.setDate(1);
  let sel = null;
  const iso = (a, m, d) => `${a}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
  const OP_CAT = ['PROVA', 'EVENTO_ACADEMICO', 'COMPROMISSO', 'OUTRO'].map(v => ({ v, t: rot(v) }));
  const camposEvento = [
    { name: 'titulo', label: 'Título', obrigatorio: true, max: 160 }, { name: 'descricao', label: 'Descrição', tipo: 'textarea', max: 2000 },
    { name: 'data', label: 'Data', tipo: 'date', obrigatorio: true }, { name: 'horario', label: 'Horário (opcional)', tipo: 'time' },
    { name: 'categoria', label: 'Categoria', tipo: 'select', opcoes: OP_CAT }, { name: 'observacoes', label: 'Observações', tipo: 'textarea', max: 1000 }];
  const corpoEvento = v => ({ titulo: v.titulo, descricao: v.descricao || null, data: v.data, horario: v.horario || null, categoria: v.categoria, observacoes: v.observacoes || null });
  let itens = [];

  async function desenhar() {
    const ano = ref.getFullYear(), mes = ref.getMonth() + 1;
    itens = await api.get(`/api/calendario?ano=${ano}&mes=${mes}`);
    const porDia = {}; itens.forEach(i => (porDia[i.data] ||= []).push(i));
    const dias = new Date(ano, mes, 0).getDate(), inicio = ref.getDay();
    const hoje = hojeISO();
    const celulas = [];
    for (let k = 0; k < inicio; k++) celulas.push('<div aria-hidden="true"></div>');
    for (let d = 1; d <= dias; d++) {
      const data = iso(ano, mes, d), its = porDia[data] || [];
      celulas.push(html`<button class="dia ${data === hoje ? 'hoje' : ''}" data-acao="dia" data-dia="${data}" aria-pressed="${sel === data}"
        aria-label="${d} de ${ref.toLocaleDateString('pt-BR', { month: 'long' })}: ${its.length} item(ns)"><strong>${d}</strong>
        ${lista(its.slice(0, 2), i => html`<span class="chip ${i.origem === 'ATIVIDADE' ? 'ATIVIDADE' : i.categoria}">${i.titulo}</span>`)}
        ${raw(its.length > 2 ? html`<span class="mais">+${its.length - 2} mais</span>` : '')}</button>`);
    }
    el.innerHTML = html`${raw(cabecalho('Calendário', 'Prazos de entrega, provas, eventos e compromissos.', '<button class="btn" data-acao="novo">Novo evento</button>'))}
      <section class="painel"><div class="acoes" style="justify-content:space-between;align-items:center;margin-bottom:.8rem">
        <button class="btn sec peq" data-acao="mes-ant" aria-label="Mês anterior">&lsaquo; Anterior</button>
        <h2 style="margin:0;text-transform:capitalize" aria-live="polite">${ref.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' })}</h2>
        <div class="acoes"><button class="btn sec peq" data-acao="hoje">Hoje</button><button class="btn sec peq" data-acao="mes-prox" aria-label="Próximo mês">Próximo &rsaquo;</button></div></div>
        <div class="cal" role="group" aria-label="Dias do mês">${lista(['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sáb'], n => html`<div class="dow">${n}</div>`)}${raw(celulas.join(''))}</div></section>
      <section class="painel" id="detalhes" aria-live="polite"></section>`;
    detalhes();
  }
  function detalhes() {
    const box = document.getElementById('detalhes');
    if (!sel) { box.innerHTML = '<p class="vazio">Selecione um dia para ver os detalhes.</p>'; return; }
    const its = itens.filter(i => i.data === sel);
    box.innerHTML = html`<h2>${fmtData(sel)}</h2>${raw(!its.length ? '<p class="vazio">Nada neste dia.</p>' : '')}
      ${lista(its, i => html`<div class="cartao"><strong>${i.titulo}</strong> <span class="chip ${i.origem === 'ATIVIDADE' ? 'ATIVIDADE' : i.categoria}" style="display:inline-block">${rot(i.categoria)}</span>
        <div class="mudo">${i.horario ? i.horario.slice(0, 5) : 'Dia todo'}${raw(i.status ? ' · ' + badge(i.status) : '')}</div>
        ${raw(i.descricao ? html`<p style="white-space:pre-wrap">${i.descricao}</p>` : '')}
        <div class="acoes">${raw(i.origem === 'ATIVIDADE' ? html`<a class="btn sec peq" href="#/atividades/${i.id}">Abrir atividade</a>` + (i.link ? html`<a class="btn peq" href="${ligacaoSegura(i.link)}" target="_blank" rel="noopener noreferrer">Entregar</a>` : '')
          : html`<button class="btn sec peq" data-acao="editar" data-id="${i.id}">Editar</button><button class="btn perigo peq" data-acao="excluir" data-id="${i.id}">Excluir</button>`)}</div></div>`)}`;
  }
  delegar(el, {
    dia: d => { sel = d.dia; desenhar(); },
    'mes-ant': () => { ref.setMonth(ref.getMonth() - 1); desenhar(); }, 'mes-prox': () => { ref.setMonth(ref.getMonth() + 1); desenhar(); },
    hoje: () => { ref = new Date(); ref.setDate(1); sel = hojeISO(); desenhar(); },
    novo: async () => { if (await abrirForm({ titulo: 'Novo evento', campos: camposEvento, valores: { data: sel || hojeISO(), categoria: 'COMPROMISSO' }, enviar: v => api.post('/api/eventos', corpoEvento(v)) })) { toast('Evento criado.'); desenhar(); } },
    editar: async d => { const e = await api.get('/api/eventos/' + d.id);
      if (await abrirForm({ titulo: 'Editar evento', campos: camposEvento, valores: { ...e, horario: e.horario ? e.horario.slice(0, 5) : '' }, enviar: v => api.put('/api/eventos/' + d.id, corpoEvento(v)) })) { toast('Evento atualizado.'); desenhar(); } },
    excluir: async d => { if (await confirmar('Excluir este evento?', 'Excluir')) { await api.del('/api/eventos/' + d.id); toast('Evento excluído.'); desenhar(); } }
  });
  await desenhar();
}

/* ---------- RF04 · Informações acadêmicas ---------- */
async function pgAcademico(el) {
  let discs = [];
  const camposDisc = [
    { name: 'nome', label: 'Disciplina', obrigatorio: true, max: 120 }, { name: 'professor', label: 'Professor(a)', max: 120 },
    { name: 'palavrasChave', label: 'Palavras-chave', max: 500, dica: 'Separe por vírgulas. São usadas para recomendar conteúdos (ex.: java, classes, herança).' },
    { name: 'frequenciaPercentual', label: 'Frequência (%) — se disponível', tipo: 'number', passo: '0.01', min: 0, maxVal: 100 }];
  const camposNota = [
    { name: 'descricao', label: 'Avaliação', obrigatorio: true, max: 120 },
    { name: 'valorAvaliacao', label: 'Valor da prova (máximo 10)', tipo: 'number', passo: '0.01', min: 0.01, maxVal: 10, obrigatorio: true },
    { name: 'valor', label: 'Nota obtida', tipo: 'number', passo: '0.01', min: 0, maxVal: 10, obrigatorio: true },
    { name: 'dataLancamento', label: 'Data', tipo: 'date', obrigatorio: true }];
  const corpoDisc = v => ({ nome: v.nome, professor: v.professor || null, palavrasChave: v.palavrasChave || '', frequenciaPercentual: v.frequenciaPercentual === '' ? null : Number(v.frequenciaPercentual) });
  const corpoNota = v => {
    if (Number(v.valor) > Number(v.valorAvaliacao)) throw new Error('A nota obtida não pode ultrapassar o valor da prova.');
    return { descricao: v.descricao, valor: Number(v.valor), valorAvaliacao: Number(v.valorAvaliacao), dataLancamento: v.dataLancamento };
  };

  async function desenhar() {
    discs = await api.get('/api/disciplinas');
    el.innerHTML = html`${raw(cabecalho('Informações acadêmicas', 'Disciplinas, notas e frequência (cadastro manual nesta versão).', '<button class="btn" data-acao="nova-disc">Nova disciplina</button>'))}
      <div class="aviso">Os dados abaixo são inseridos por você. Ainda não há integração real com sistemas de instituições.</div>
      ${raw(!discs.length ? '<p class="vazio">Nenhuma disciplina cadastrada.</p>' : '')}
      ${lista(discs, d => html`<section class="painel"><div class="pg-head" style="margin-bottom:.5rem"><div><h2>${d.nome}</h2><p>${d.professor || 'Professor não informado'}</p></div>
        <div class="acoes"><button class="btn sec peq" data-acao="nova-nota" data-id="${d.id}">Lançar nota</button><button class="btn sec peq" data-acao="ed-disc" data-id="${d.id}">Editar</button>
        <button class="btn perigo peq" data-acao="ex-disc" data-id="${d.id}">Excluir</button></div></div>
        <p><strong>Média simples:</strong> ${fmtNum(d.media)} &nbsp; <strong>Frequência:</strong> ${d.frequenciaPercentual == null ? '—' : fmtNum(d.frequenciaPercentual) + '%'}</p>
        ${raw(d.notas.length ? html`<div class="tabela-rolagem"><table><caption class="sr-only">Notas de ${d.nome}</caption><thead><tr><th scope="col">Avaliação</th><th scope="col">Data</th><th scope="col">Nota obtida</th><th scope="col">Valor da prova</th><th scope="col">Ações</th></tr></thead>
          <tbody>${lista(d.notas, n => html`<tr><td>${n.descricao}</td><td>${fmtData(n.dataLancamento)}</td><td>${fmtNum(n.valor)}</td><td>${fmtNum(n.valorAvaliacao)}</td>
            <td><div class="acoes"><button class="btn sec peq" data-acao="ed-nota" data-id="${n.id}" data-disc="${d.id}">Editar</button><button class="btn perigo peq" data-acao="ex-nota" data-id="${n.id}">Excluir</button></div></td></tr>`)}</tbody></table></div>`
          : '<p class="vazio">Sem notas lançadas.</p>')}</section>`)}`;
  }
  const disc = id => discs.find(d => String(d.id) === String(id));
  delegar(el, {
    'nova-disc': async () => { if (await abrirForm({ titulo: 'Nova disciplina', campos: camposDisc, enviar: v => api.post('/api/disciplinas', corpoDisc(v)) })) { toast('Disciplina criada.'); desenhar(); } },
    'ed-disc': async d => { if (await abrirForm({ titulo: 'Editar disciplina', campos: camposDisc, valores: disc(d.id), enviar: v => api.put('/api/disciplinas/' + d.id, corpoDisc(v)) })) { toast('Disciplina atualizada.'); desenhar(); } },
    'ex-disc': async d => { if (await confirmar('Excluir a disciplina e suas notas?', 'Excluir')) { await api.del('/api/disciplinas/' + d.id); toast('Disciplina excluída.'); desenhar(); } },
    'nova-nota': async d => { if (await abrirForm({ titulo: 'Lançar nota', campos: camposNota, valores: { dataLancamento: hojeISO() }, enviar: v => api.post(`/api/disciplinas/${d.id}/notas`, corpoNota(v)) })) { toast('Nota lançada.'); desenhar(); } },
    'ed-nota': async d => { const n = disc(d.disc).notas.find(x => String(x.id) === d.id);
      if (await abrirForm({ titulo: 'Editar nota', campos: camposNota, valores: n, enviar: v => api.put('/api/notas/' + d.id, corpoNota(v)) })) { toast('Nota atualizada.'); desenhar(); } },
    'ex-nota': async d => { if (await confirmar('Excluir esta nota?', 'Excluir')) { await api.del('/api/notas/' + d.id); toast('Nota excluída.'); desenhar(); } }
  });
  await desenhar();
}

/* ---------- RF04 · Informações financeiras ---------- */
async function pgFinanceiro(el) {
  let dados;
  const campos = [
    { name: 'descricao', label: 'Descrição', obrigatorio: true, max: 160 }, { name: 'competencia', label: 'Competência (mês de referência)', tipo: 'date', obrigatorio: true },
    { name: 'valor', label: 'Valor (R$)', tipo: 'number', passo: '0.01', min: 0, obrigatorio: true }, { name: 'vencimento', label: 'Vencimento', tipo: 'date', obrigatorio: true },
    { name: 'status', label: 'Situação', tipo: 'select', opcoes: [{ v: 'PENDENTE', t: 'Em aberto' }, { v: 'PAGO', t: 'Pago' }] },
    { name: 'dataPagamento', label: 'Data do pagamento (se pago)', tipo: 'date' }];
  const corpo = v => ({ descricao: v.descricao, competencia: v.competencia, valor: Number(v.valor), vencimento: v.vencimento, status: v.status, dataPagamento: v.status === 'PAGO' ? (v.dataPagamento || null) : null });
  async function desenhar() {
    dados = await api.get('/api/financeiro'); const r = dados.resumo;
    el.innerHTML = html`${raw(cabecalho('Informações financeiras', 'Mensalidades e situação financeira (cadastro manual nesta versão).', '<button class="btn" data-acao="novo">Novo registro</button>'))}
      <div class="aviso">Registros inseridos por você. Não representam dados oficiais de nenhuma instituição.</div>
      <section class="indicadores">${raw(indicador(fmtMoeda(r.totalPago), 'pago'))}${raw(indicador(fmtMoeda(r.totalPendente), 'a vencer'))}${raw(indicador(fmtMoeda(r.totalAtrasado), 'em atraso', Number(r.totalAtrasado) > 0))}</section>
      <section class="painel"><div class="tabela-rolagem">${raw(!dados.registros.length ? '<p class="vazio">Nenhum registro.</p>' : html`<table><caption class="sr-only">Histórico financeiro</caption>
        <thead><tr><th scope="col">Descrição</th><th scope="col">Competência</th><th scope="col">Vencimento</th><th scope="col">Valor</th><th scope="col">Situação</th><th scope="col">Ações</th></tr></thead>
        <tbody>${lista(dados.registros, x => html`<tr><td>${x.descricao}</td><td>${fmtData(x.competencia)}</td><td>${fmtData(x.vencimento)}</td><td>${fmtMoeda(x.valor)}</td><td>${raw(badge(x.situacao))}</td>
          <td><div class="acoes">${raw(x.situacao !== 'PAGO' ? html`<button class="btn peq" data-acao="pagar" data-id="${x.id}">Marcar pago</button>` : '')}
          <button class="btn sec peq" data-acao="editar" data-id="${x.id}">Editar</button><button class="btn perigo peq" data-acao="excluir" data-id="${x.id}">Excluir</button></div></td></tr>`)}</tbody></table>`)}</div></section>`;
  }
  const achar = id => dados.registros.find(x => String(x.id) === String(id));
  const paraForm = x => ({ ...x, status: x.situacao === 'PAGO' ? 'PAGO' : 'PENDENTE' });
  delegar(el, {
    novo: async () => { if (await abrirForm({ titulo: 'Novo registro financeiro', campos, valores: { status: 'PENDENTE', competencia: hojeISO() }, enviar: v => api.post('/api/financeiro', corpo(v)) })) { toast('Registro criado.'); desenhar(); } },
    editar: async d => { if (await abrirForm({ titulo: 'Editar registro', campos, valores: paraForm(achar(d.id)), enviar: v => api.put('/api/financeiro/' + d.id, corpo(v)) })) { toast('Registro atualizado.'); desenhar(); } },
    pagar: async d => { const x = achar(d.id); await api.put('/api/financeiro/' + d.id, corpo({ ...x, status: 'PAGO', dataPagamento: hojeISO(), valor: x.valor })); toast('Pagamento registrado.'); desenhar(); },
    excluir: async d => { if (await confirmar('Excluir este registro?', 'Excluir')) { await api.del('/api/financeiro/' + d.id); toast('Registro excluído.'); desenhar(); } }
  });
  await desenhar();
}

/* ---------- RF07 · Notificações ---------- */
async function pgNotificacoes(el) {
  async function desenhar() {
    const r = await api.get('/api/notificacoes');
    el.innerHTML = html`${raw(cabecalho('Notificações', r.naoLidas + ' não lida(s). Escolha os tipos em Configurações.', '<button class="btn sec" data-acao="todas">Marcar todas como lidas</button>'))}
      <section class="painel">${raw(!r.itens.length ? '<p class="vazio">Você não tem notificações.</p>' : html`<ul class="lista-limpa">${lista(r.itens, n => html`<li>
        <div><strong>${n.titulo}</strong> ${raw(n.lida ? '' : '<span class="badge b-MEDIA">Nova</span>')}<br>${n.mensagem}<br><span class="mudo">${fmtDataHora(n.criadaEm)}</span></div>
        <div class="acoes"><button class="btn sec peq" data-acao="lida" data-id="${n.id}" data-lida="${!n.lida}">${n.lida ? 'Marcar como não lida' : 'Marcar como lida'}</button>
        <button class="btn perigo peq" data-acao="excluir" data-id="${n.id}">Excluir</button></div></li>`)}</ul>`)}</section>`;
    atualizarSelo();
  }
  delegar(el, {
    todas: async () => { await api.post('/api/notificacoes/marcar-todas-lidas'); desenhar(); },
    lida: async d => { await api.patch(`/api/notificacoes/${d.id}/lida?lida=${d.lida}`); desenhar(); },
    excluir: async d => { await api.del('/api/notificacoes/' + d.id); desenhar(); }
  });
  await desenhar();
}

/* ---------- RF09 · Recomendações ---------- */
async function pgRecomendacoes(el) {
  const r = await api.get('/api/recomendacoes');
  window._recs = r.flatMap(d => d.recomendacoes);
  el.innerHTML = html`${raw(cabecalho('Recomendações', 'Conteúdos aprovados por professores, escolhidos por critério objetivo.'))}
    <div class="aviso"><strong>Como funciona (sem inteligência artificial):</strong> a compatibilidade é o percentual das palavras-chave da disciplina que também aparecem no conteúdo.
      Só entram conteúdos aprovados por professores com pelo menos 80%, até 3 por disciplina.</div>
    ${raw(!r.length ? '<p class="vazio">Cadastre disciplinas com palavras-chave em Acadêmico.</p>' : '')}
    ${lista(r, d => html`<section class="painel"><h2>${d.disciplinaNome}</h2><p class="mudo">Palavras-chave: ${d.palavrasChave || '—'}</p>
      ${lista(d.recomendacoes, x => html`<div class="cartao"><div class="acoes" style="justify-content:space-between"><strong>${x.conteudo.titulo}</strong><span class="comp">${fmtNum(x.compatibilidade)}% compatível</span></div>
        <p class="mudo">Aprovado por: ${x.conteudo.aprovadoPor || '—'}</p>
        ${raw(x.conteudo.urlEmbed ? html`<button class="btn peq" data-acao="tocar" data-id="${x.id}">Assistir no mini-player</button>` : '<span class="mudo">Sem vídeo cadastrado.</span>')}</div>`)}
      ${raw(d.mensagem ? html`<p class="aviso">${d.mensagem}</p>` : '')}</section>`)}`;
  delegar(el, { tocar: d => { const x = window._recs.find(i => String(i.id) === d.id); player.abrir(x.conteudo.titulo, x.conteudo.urlEmbed); } });
}

/* ---------- RF11 · Conteúdos e mini-player ---------- */
async function pgConteudos(el) {
  async function desenhar() {
    const [lst, minhas] = await Promise.all([api.get('/api/conteudos'), api.get('/api/conteudos/minhas-sugestoes')]);
    window._conts = lst;
    const status = { PENDENTE: 'Pendente de avaliação', APROVADO: 'Aprovado', RECUSADO: 'Recusado' };
    el.innerHTML = html`${raw(cabecalho('Conteúdos', 'Sugira vídeos e acompanhe a resposta do professor.', '<button class="btn" data-acao="sugerir">Sugerir conteúdo</button>'))}
      <section class="painel"><h2>Conteúdos aprovados</h2>${raw(!lst.length ? '<p class="vazio">Nenhum conteúdo aprovado.</p>' : '')}
        ${lista(lst, c => html`<div class="cartao"><strong>${c.titulo}</strong><p class="mudo">${c.descricao || ''}</p><p class="mudo">Palavras-chave: ${c.palavrasChave || '—'} · Aprovado por ${c.aprovadoPor || '—'}</p>
          ${raw(c.urlEmbed ? html`<button class="btn peq" data-acao="tocar" data-id="${c.id}">Assistir no mini-player</button>` : '<span class="mudo">Sem vídeo.</span>')}</div>`)}</section>
      <section class="painel"><h2>Minhas sugestões</h2>${raw(!minhas.length ? '<p class="vazio">Você ainda não enviou sugestões.</p>' : '')}
        ${lista(minhas, c => html`<article class="cartao"><h3>${c.titulo}</h3><p class="mudo">${c.descricao || ''}</p>
          <p><strong>${status[c.status] || c.status}</strong></p>
          ${raw(c.respondidoEm ? html`<p>Respondido em ${fmtDataHora(c.respondidoEm)}</p><h4>Justificativa e links sugeridos</h4><p class="texto-resposta">${c.justificativa || 'Sem justificativa registrada.'}</p>
            <ul class="links-professor">${lista(c.linksSugeridos || [], url => html`<li><a href="${ligacaoSegura(url)}" target="_blank" rel="noopener noreferrer">${url}</a></li>`)}</ul>
            <p class="mudo">${c.emailResultadoEnviado ? 'Resultado enviado por e-mail.' : 'O e-mail com o resultado está aguardando envio. A resposta já está salva aqui.'}</p>`
            : c.status === 'PENDENTE' && c.emailProfessor ? html`<p>Professor: ${c.emailProfessor}</p>
              <p class="mudo">${c.emailConviteEnviado ? 'Convite enviado por e-mail.' : 'O convite ainda não foi enviado. Verifique a configuração de e-mail e tente reenviar.'}</p>
              <p class="mudo">Convite válido até ${fmtDataHora(c.expiraEm)}. Reenviar invalida o convite anterior.</p>
              <button class="btn sec peq" data-acao="reenviar" data-id="${c.id}">Reenviar convite</button>`
            : c.status === 'PENDENTE' ? '<p class="mudo">Sugestão antiga: envie novamente informando o e-mail do professor.</p>' : '<p class="mudo">Avaliação anterior à atualização, sem justificativa registrada.</p>')}
        </article>`)}</section>`;
  }
  delegar(el, {
    tocar: d => { const c = window._conts.find(x => String(x.id) === d.id); player.abrir(c.titulo, c.urlEmbed); },
    reenviar: async d => { try { await api.post('/api/conteudos/' + d.id + '/reenviar-convite'); toast('Novo convite solicitado. Confira o status abaixo.'); await desenhar(); } catch (e) { toast(e.message, 'erro'); } },
    sugerir: async () => { if (await abrirForm({ titulo: 'Sugerir conteúdo', intro: 'O professor receberá um convite por e-mail. O conteúdo só será aprovado depois que ele confirmar.',
      campos: [{ name: 'titulo', label: 'Título', obrigatorio: true, max: 160 }, { name: 'descricao', label: 'Descrição', tipo: 'textarea', max: 1000 },
        { name: 'urlVideo', label: 'Link do vídeo (YouTube ou Vimeo)', tipo: 'url', max: 300, dica: 'Exemplo: https://www.youtube.com/watch?v=...' },
        { name: 'palavrasChave', label: 'Palavras-chave', max: 500, dica: 'Separe por vírgulas.' },
        { name: 'emailProfessor', label: 'E-mail do professor', tipo: 'email', obrigatorio: true, max: 180 }],
      enviar: v => api.post('/api/conteudos', { titulo: v.titulo, descricao: v.descricao || null, urlVideo: v.urlVideo || null, palavrasChave: v.palavrasChave || '', emailProfessor: v.emailProfessor.trim() }) })) { toast('Sugestão registrada. Confira abaixo o envio do convite.'); await desenhar(); } }
  });
  await desenhar();
}


/* ---------- RF08/RF10 · Configurações ---------- */
async function pgConfiguracoes(el) {

  async function desenhar() {
    const [prefs, plats, hist, cons] = await Promise.all([
      api.get('/api/preferencias-notificacao'),
      api.get('/api/integracoes'),
      api.get('/api/integracoes/historico'),
      api.get('/api/privacidade/consentimentos')
    ]);

    const temConsent = cons.some(
        c => c.tipo === 'INTEGRACOES' && c.concedido
    );

    el.innerHTML = html`
      ${raw(cabecalho('Configurações'))}

      <!-- APARÊNCIA -->
      <div class="aparencia-layout">
        <section class="painel painel-aparencia">
          <h2>Tema do aplicativo</h2>
          <p class="mudo">Escolha como prefere ver suas páginas.</p>

          <div class="opcoes-tema" role="group" aria-label="Escolher tema">
            <button type="button" class="opcao-tema" data-tema="light" aria-pressed="false">
              <span class="tema-mini tema-mini-claro" aria-hidden="true">
                <span class="tema-mini-lateral"><i></i><i></i><i></i><i></i></span>
                <span class="tema-mini-conteudo"><i class="tema-mini-titulo"></i><i></i><span><i></i><i></i></span></span>
              </span>
              <span class="opcao-tema-rodape"><span>Claro</span><span class="opcao-tema-status"></span><span class="opcao-tema-check" aria-hidden="true">✓</span></span>
            </button>

            <button type="button" class="opcao-tema" data-tema="dark" aria-pressed="false">
              <span class="tema-mini tema-mini-escuro" aria-hidden="true">
                <span class="tema-mini-lateral"><i></i><i></i><i></i><i></i></span>
                <span class="tema-mini-conteudo"><i class="tema-mini-titulo"></i><i></i><span><i></i><i></i></span></span>
              </span>
              <span class="opcao-tema-rodape"><span>Escuro</span><span class="opcao-tema-status"></span><span class="opcao-tema-check" aria-hidden="true">✓</span></span>
            </button>
          </div>

          <hr class="preferencia-separador">

          <section class="controle-leitura" aria-labelledby="titulo-tamanho-texto">
            <h3 id="titulo-tamanho-texto">Conforto de leitura</h3>
            <p class="mudo">Ajuste o tamanho do texto nas páginas.</p>
            <label for="controle-tamanho-fonte">Tamanho do texto</label>
            <div class="controle-fonte">
              <span class="amostra-fonte amostra-fonte-pequena" aria-hidden="true">A</span>
              <input id="controle-tamanho-fonte" type="range" min="0" max="2" step="1" value="1" aria-valuetext="Padrão">
              <span class="amostra-fonte amostra-fonte-grande" aria-hidden="true">A</span>
              <output id="valor-tamanho-fonte" for="controle-tamanho-fonte">Padrão</output>
            </div>
          </section>

          <hr class="preferencia-separador">

          <div class="preferencia-movimento">
            <div>
              <h3>Reduzir animações</h3>
              <p class="mudo">Use menos movimentos ao navegar pelo sistema.</p>
            </div>
            <button type="button" class="interruptor" id="controle-reduzir-movimento" role="switch" aria-checked="false" aria-label="Reduzir animações"><span></span></button>
          </div>
        </section>

        <aside class="aparencia-apoio" aria-label="Prévia e dica de aparência">
          <section class="painel painel-previa-aparencia">
            <h3>Prévia da sua área</h3>
            <p class="mudo" id="rotulo-previa-tema">Assim fica o tema claro.</p>
            <div class="mini-app" id="mini-app-previa" data-tema-previa="light" aria-hidden="true">
              <div class="mini-app-lateral"><i></i><i></i><i></i><i></i><i></i></div>
              <div class="mini-app-conteudo">
                <strong>Bom dia, ${window.usuario.nome.split(' ')[0]}</strong>
                <small>Seu plano de hoje</small>
                <div class="mini-app-tarefa"><i></i><span><b>Revisar matéria</b><small>Hoje · 14h30</small></span></div>
                <div class="mini-app-cartoes"><i></i><i></i></div>
              </div>
            </div>
            <p class="nota-aplicacao">As mudanças são aplicadas automaticamente.</p>
          </section>
          <aside class="dica-aparencia">
            <h3>Uma dica</h3>
            <p>O tema escuro pode ajudar em ambientes com pouca luz. Você pode trocar quando quiser.</p>
          </aside>
        </aside>
      </div>


      <!-- PRIVACIDADE E CONTA -->
      <section class="painel">
        <h2>Privacidade e conta</h2>

        <p class="mudo">
          Consulte seus dados pessoais, gerencie consentimentos
          e acesse as opções de exclusão da conta.
        </p>

        <a class="btn" href="#/privacidade">
          Gerenciar privacidade e conta →
        </a>
      </section>


      <!-- PREFERÊNCIAS DE NOTIFICAÇÃO -->
      <section class="painel">
        <h2>Preferências de notificação</h2>

        <form id="form-prefs">
          ${lista(prefs, p => html`
            <div class="linha-check">
              <input
                  type="checkbox"
                  id="p-${p.tipo}"
                  name="${p.tipo}"
                  ${raw(p.ativa ? 'checked' : '')}
              >
              <label for="p-${p.tipo}">${p.rotulo}</label>
            </div>
          `)}

          <button class="btn" type="submit">
            Salvar preferências
          </button>
        </form>
      </section>

      <!-- PLATAFORMAS EXTERNAS -->
      <section class="painel">
        <h2>Plataformas externas</h2>

        <div class="aviso">
          <strong>Simulação:</strong>
          não há integração real com plataformas educacionais nesta versão.
          A sincronização apenas demonstra o fluxo
          (manual: 1 por minuto; automática: a cada 6 horas)
          e nenhum dado é importado.
        </div>

        ${raw(
            !temConsent
                ? '<div class="aviso erro">Para associar plataformas é necessário conceder consentimento em <a href="#/privacidade">Privacidade e conta</a>.</div>'
                : ''
        )}

        <div class="acoes" style="margin-bottom:.8rem">
          <button
              class="btn"
              data-acao="associar"
              ${raw(temConsent ? '' : 'disabled')}
          >
            Associar plataforma
          </button>
        </div>

        ${raw(
            !plats.length
                ? '<p class="vazio">Nenhuma plataforma associada.</p>'
                : html`
                  <ul class="lista-limpa">
                    ${lista(plats, p => html`
                      <li>
                        <div>
                          <strong>${p.nome}</strong>
                          <span class="badge b-PENDENTE">
                        ${p.simulada ? 'Simulação' : 'Real'}
                      </span>

                          ${raw(
                              p.ativa
                                  ? ''
                                  : '<span class="badge b-ATRASADA">Inativa</span>'
                          )}

                          <br>

                          <span class="mudo">
                        Tipo ${p.tipo}.
                        Última sincronização:
                        ${fmtDataHora(p.ultimaSincronizacao)}
                      </span>
                        </div>

                        <div class="acoes">
                          <button
                              class="btn sec peq"
                              data-acao="sinc"
                              data-id="${p.id}"
                          >
                            Sincronizar agora
                          </button>

                          <button
                              class="btn perigo peq"
                              data-acao="remover"
                              data-id="${p.id}"
                          >
                            Remover
                          </button>
                        </div>
                      </li>
                    `)}
                  </ul>
                `
        )}

        ${raw(
            hist.length
                ? html`
                  <h3 style="margin-top:1rem">
                    Histórico de sincronizações
                  </h3>

                  <div class="tabela-rolagem">
                    <table>
                      <thead>
                      <tr>
                        <th scope="col">Quando</th>
                        <th scope="col">Plataforma</th>
                        <th scope="col">Tipo</th>
                        <th scope="col">Resultado</th>
                        <th scope="col">Mensagem</th>
                      </tr>
                      </thead>

                      <tbody>
                      ${lista(hist, h => html`
                        <tr>
                          <td>${fmtDataHora(h.iniciadoEm)}</td>
                          <td>${h.plataforma}</td>
                          <td>${rot(h.tipo)}</td>
                          <td>${raw(badge(h.status))}</td>
                          <td>${h.mensagem}</td>
                        </tr>
                      `)}
                      </tbody>
                    </table>
                  </div>
                `
                : ''
        )}
      </section>
    `;

    // ========================================
    // SELETOR DE TEMA
    // ========================================

    const botoesTema = el.querySelectorAll('.opcao-tema');

    function atualizarSelecaoTema() {
      const temaAtual =
          document.documentElement.dataset.theme || 'light';

      botoesTema.forEach(botao => {
        const selecionado = botao.dataset.tema === temaAtual;

        botao.classList.toggle('selecionado', selecionado);
        botao.setAttribute('aria-pressed', String(selecionado));
        botao.querySelector('.opcao-tema-status').textContent = selecionado ? 'Atual' : '';
      });

      const previa = el.querySelector('#mini-app-previa');
      const rotulo = el.querySelector('#rotulo-previa-tema');
      previa.dataset.temaPrevia = temaAtual;
      rotulo.textContent = `Assim fica o tema ${temaAtual === 'dark' ? 'escuro' : 'claro'}.`;
    }

    botoesTema.forEach(botao => {
      botao.addEventListener('click', () => {
        if (!window.LearningScoreTema) {
          toast('O sistema de temas não está disponível.', 'erro');
          return;
        }

        window.LearningScoreTema.definir(botao.dataset.tema);
        atualizarSelecaoTema();
      });
    });

    atualizarSelecaoTema();

    // Tamanho do texto: a preferência fica salva neste navegador.
    const controleFonte = el.querySelector('#controle-tamanho-fonte');
    const rotulosFonte = ['Pequeno', 'Padrão', 'Grande'];
    const niveisFonte = ['pequeno', 'padrao', 'grande'];
    const indiceFonte = niveisFonte.indexOf(window.LearningScoreTema?.obterTamanhoFonte?.() || 'padrao');
    controleFonte.value = String(indiceFonte < 0 ? 1 : indiceFonte);
    const atualizarRotuloFonte = () => {
      const rotulo = rotulosFonte[Number(controleFonte.value)];
      controleFonte.setAttribute('aria-valuetext', rotulo);
      el.querySelector('#valor-tamanho-fonte').textContent = rotulo;
    };
    atualizarRotuloFonte();
    controleFonte.addEventListener('input', () => {
      const nivel = niveisFonte[Number(controleFonte.value)];
      window.LearningScoreTema?.definirTamanhoFonte?.(nivel);
      atualizarRotuloFonte();
    });

    // A preferência de movimento também é lembrada neste navegador.
    const controleMovimento = el.querySelector('#controle-reduzir-movimento');
    const atualizarMovimento = reduzido => {
      controleMovimento.setAttribute('aria-checked', String(reduzido));
      controleMovimento.classList.toggle('ativo', reduzido);
    };
    atualizarMovimento(window.LearningScoreTema?.obterReducaoMovimento?.() || false);
    controleMovimento.addEventListener('click', () => {
      const reduzido = controleMovimento.getAttribute('aria-checked') !== 'true';
      window.LearningScoreTema?.definirReducaoMovimento?.(reduzido);
      atualizarMovimento(reduzido);
    });

    // ========================================
    // PREFERÊNCIAS DE NOTIFICAÇÃO
    // ========================================

    el.querySelector('#form-prefs').onsubmit = async ev => {
      ev.preventDefault();

      try {
        await api.put('/api/preferencias-notificacao', {
          preferencias: prefs.map(p => ({
            tipo: p.tipo,
            rotulo: p.rotulo,
            ativa: ev.target.elements[p.tipo].checked
          }))
        });

        toast('Preferências salvas.');
      } catch (e) {
        toast(e.message, 'erro');
      }
    };
  }

  // ========================================
  // AÇÕES DAS PLATAFORMAS
  // ========================================

  delegar(el, {
    associar: async () => {
      if (await abrirForm({
        titulo: 'Associar plataforma',
        intro: 'Será criada uma associação demonstrativa (simulação).',
        campos: [
          {
            name: 'nome',
            label: 'Nome da plataforma',
            obrigatorio: true,
            max: 80
          },
          {
            name: 'tipo',
            label: 'Tipo',
            tipo: 'select',
            opcoes: [
              'AVA',
              'MOODLE',
              'CLASSROOM',
              'PORTAL_ACADEMICO',
              'OUTRA'
            ].map(v => ({ v, t: v }))
          },
          {
            name: 'identificadorExterno',
            label: 'Seu identificador na plataforma (opcional)',
            max: 120
          }
        ],
        enviar: v => api.post('/api/integracoes', {
          nome: v.nome,
          tipo: v.tipo,
          identificadorExterno: v.identificadorExterno || null
        })
      })) {
        toast('Plataforma associada.');
        desenhar();
      }
    },

    sinc: async d => {
      try {
        const h = await api.post(
            `/api/integracoes/${d.id}/sincronizar`
        );
        toast(h.mensagem);
      } catch (e) {
        toast(e.message, 'erro');
      }

      desenhar();
    },

    remover: async d => {
      if (await confirmar('Remover esta plataforma?', 'Remover')) {
        await api.del('/api/integracoes/' + d.id);
        desenhar();
      }
    }
  });

  await desenhar();
}


/* ---------- RF12 · Privacidade e conta ---------- */
/* ---------- RF12 · Conta e privacidade (em abas) ---------- */
let abaPrivacidade = 'conta';   // aba que está aberta

async function pgPrivacidade(el) {
  abaPrivacidade = 'conta';     // sempre abre na aba "Conta"
  const abas = [{ id: 'conta', rotulo: 'Conta' }, { id: 'privacidade', rotulo: 'Privacidade' }];

  // Troca de aba sem recarregar os dados
  function mostrarAba(id) {
    abaPrivacidade = id;
    el.querySelectorAll('[role=tab]').forEach(b => {
      const ativa = b.dataset.aba === id;
      b.setAttribute('aria-selected', String(ativa));
      b.tabIndex = ativa ? 0 : -1;
    });
    el.querySelectorAll('[role=tabpanel]').forEach(p => { p.hidden = p.id !== 'painel-' + id; });
  }

  async function desenhar() {
    const m = await api.get('/api/privacidade/meus-dados');
    const cs = Object.fromEntries(m.consentimentos.map(c => [c.tipo, c]));
    const opcional = ['INTEGRACOES', 'USO_DADOS_EXTERNOS'];

    // Conteúdo da aba CONTA
    const painelConta = html`<section class="painel"><h2>Seus dados pessoais</h2>
        <dl><dt class="mudo">Nome</dt><dd>${m.conta.nome}</dd><dt class="mudo">E-mail</dt><dd>${m.conta.email}</dd><dt class="mudo">Perfil</dt><dd>${m.conta.papel}</dd><dt class="mudo">Conta criada em</dt><dd>${fmtDataHora(m.conta.criadoEm)}</dd></dl>
        <p class="mudo">Registros armazenados: ${Object.entries(m.quantidades).map(([k, v]) => v + ' ' + k.replace(/([A-Z])/g, ' $1').toLowerCase()).join(', ')}.</p></section>`;

    // Conteúdo da aba PRIVACIDADE (consentimentos + exclusão da conta)
    const painelPrivacidade = html`<section class="painel"><h2>Consentimentos</h2>
        <p class="mudo">Termos de uso aceitos em ${cs.TERMOS_USO ? fmtDataHora(cs.TERMOS_USO.registradoEm) : '—'}.</p>
        ${lista(opcional, t => html`<div class="linha-check"><input type="checkbox" id="c-${t}" data-acao="consent" data-tipo="${t}" ${raw(cs[t] && cs[t].concedido ? 'checked' : '')}><label for="c-${t}">${rot(t)}</label></div>`)}
        <p class="mudo">Sem consentimento, integrações e sincronizações ficam bloqueadas. Você pode revogar a qualquer momento.</p></section>
      <section class="painel"><h2>Excluir conta</h2>
        ${raw(m.exclusao ? html`<div class="aviso erro">Solicitação registrada em ${fmtDataHora(m.exclusao.solicitadaEm)}. Sua conta e seus dados serão excluídos em até 24 horas.</div>
            <button class="btn sec" data-acao="cancelar-ex">Cancelar solicitação</button>`
          : html`<p>A exclusão remove permanentemente seus dados pessoais. Será mantido apenas o registro anônimo da solicitação.</p><button class="btn perigo" data-acao="excluir">Solicitar exclusão da conta</button>`)}</section>`;

    el.innerHTML = html`${raw(cabecalho('Conta e privacidade', 'Seus dados e suas escolhas (LGPD).'))}
      <div class="abas" role="tablist" aria-label="Seções da página">
        ${lista(abas, a => html`<button class="aba" role="tab" id="aba-${a.id}" aria-controls="painel-${a.id}" aria-selected="${abaPrivacidade === a.id}" tabindex="${abaPrivacidade === a.id ? 0 : -1}" data-acao="aba" data-aba="${a.id}">${a.rotulo}</button>`)}
      </div>
      <div id="painel-conta" role="tabpanel" aria-labelledby="aba-conta" ${raw(abaPrivacidade === 'conta' ? '' : 'hidden')}>${raw(painelConta)}</div>
      <div id="painel-privacidade" role="tabpanel" aria-labelledby="aba-privacidade" ${raw(abaPrivacidade === 'privacidade' ? '' : 'hidden')}>${raw(painelPrivacidade)}</div>`;

    // Setas do teclado trocam de aba
    el.querySelector('[role=tablist]').onkeydown = ev => {
      if (ev.key !== 'ArrowRight' && ev.key !== 'ArrowLeft') return;
      const ids = abas.map(a => a.id);
      const novo = ids[(ids.indexOf(abaPrivacidade) + (ev.key === 'ArrowRight' ? 1 : ids.length - 1)) % ids.length];
      mostrarAba(novo);
      el.querySelector('#aba-' + novo).focus();
    };

    el.querySelectorAll('[data-acao=consent]').forEach(chk => chk.onchange = async () => {
      try { await api.put('/api/privacidade/consentimentos', { tipo: chk.dataset.tipo, concedido: chk.checked }); toast('Consentimento atualizado.'); }
      catch (e) { toast(e.message, 'erro'); chk.checked = !chk.checked; }
    });
  }

  delegar(el, {
    aba: d => mostrarAba(d.aba),
    excluir: async () => { if (await abrirForm({ titulo: 'Confirmar exclusão da conta', intro: 'Digite sua senha para confirmar. Você terá um curto período para cancelar.', rotulo: 'Solicitar exclusão',
      campos: [{ name: 'senha', label: 'Senha', tipo: 'password', obrigatorio: true }],
      enviar: async v => { const r = await api.post('/api/privacidade/exclusao', { senha: v.senha }); toast(r.mensagem); } })) desenhar(); },
    'cancelar-ex': async () => { await api.del('/api/privacidade/exclusao'); toast('Solicitação cancelada.'); desenhar(); }
  });
  await desenhar();
}
