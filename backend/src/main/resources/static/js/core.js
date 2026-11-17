'use strict';
/* ===== Learning Score — núcleo do front-end =====
   Todo texto vindo do servidor passa por esc() antes de ir ao HTML (prevenção de XSS). */

const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const raw = s => ({ __raw: String(s) });                       // marca HTML já seguro
const html = (strs, ...vals) => strs.reduce((acc, s, i) => acc + s +
    (i < vals.length ? (vals[i] && vals[i].__raw !== undefined ? vals[i].__raw : esc(vals[i])) : ''), '');
const lista = (arr, fn) => raw(arr.map(fn).join(''));

/* ---------- formatação ---------- */
const fmtData = d => d ? new Date(d.length === 10 ? d + 'T00:00:00' : d).toLocaleDateString('pt-BR') : '—';
const fmtDataHora = d => d ? new Date(d).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '—';
const fmtMoeda = v => Number(v ?? 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
const fmtNum = v => v == null ? '—' : Number(v).toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 2 });
const ROTULO = {
  PENDENTE: 'Pendente', EM_ANDAMENTO: 'Em andamento', CONCLUIDA: 'Concluída', ATRASADA: 'Atrasada',
  BAIXA: 'Baixa', MEDIA: 'Média', ALTA: 'Alta', PAGO: 'Pago', ATRASADO: 'Atrasado',
  PROVA: 'Prova', EVENTO_ACADEMICO: 'Evento acadêmico', COMPROMISSO: 'Compromisso', OUTRO: 'Outro', PRAZO_ENTREGA: 'Prazo de entrega',
  SUCESSO: 'Sucesso', FALHA: 'Falha', TIMEOUT: 'Tempo esgotado', MANUAL: 'Manual', AUTOMATICA: 'Automática',
  TERMOS_USO: 'Termos de uso', INTEGRACOES: 'Integrações com plataformas externas', USO_DADOS_EXTERNOS: 'Uso de dados vindos de plataformas externas'
};
const rot = k => ROTULO[k] || k;
const badge = k => html`<span class="badge b-${k}">${rot(k)}</span>`;
const paraInputDataHora = d => d ? d.slice(0, 16) : '';

/* ---------- cliente da API (sessão por cookie + token CSRF) ---------- */
const api = {
  csrf() { const m = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]+)/); return m ? decodeURIComponent(m[1]) : ''; },
  async req(metodo, url, corpo) {
    const op = { method: metodo, headers: {}, credentials: 'same-origin' };
    if (corpo !== undefined) { op.headers['Content-Type'] = 'application/json'; op.body = JSON.stringify(corpo); }
    if (metodo !== 'GET') op.headers['X-XSRF-TOKEN'] = api.csrf();
    const r = await fetch(url, op);
    if (r.status === 401 && !url.includes('/api/auth/login') && !url.endsWith('/api/auth/me') && !location.pathname.includes('login')) {
      location.href = '/login.html';
    }
    if (r.status === 204) return null;
    const dados = await r.json().catch(() => null);
    if (!r.ok) {
      const e = new Error((dados && dados.mensagem) || 'Não foi possível concluir a operação.');
      e.status = r.status; e.campos = dados && dados.campos;
      throw e;
    }
    return dados;
  },
  get: u => api.req('GET', u), post: (u, b) => api.req('POST', u, b ?? {}), put: (u, b) => api.req('PUT', u, b),
  patch: (u, b) => api.req('PATCH', u, b ?? {}), del: u => api.req('DELETE', u)
};

/* ---------- avisos ---------- */
function toast(msg, tipo = '') {
  const c = document.getElementById('toasts'); if (!c) return;
  const t = document.createElement('div'); t.className = 'toast ' + tipo; t.textContent = msg;
  c.appendChild(t); setTimeout(() => t.remove(), 5000);
}

/* ---------- formulário modal genérico ----------
   campos: [{name, label, tipo, obrigatorio, opcoes:[{v,t}], max, dica}]  ·  enviar(valores) -> Promise */
function abrirForm({ titulo, campos, valores = {}, enviar, rotulo = 'Salvar', intro = '' }) {
  return new Promise(resolve => {
    const dlg = document.createElement('dialog');
    dlg.className = 'modal'; dlg.setAttribute('aria-labelledby', 'dlg-titulo');
    const campo = c => {
      const id = 'f-' + c.name, v = valores[c.name] ?? c.valor ?? '';
      const req = c.obrigatorio ? 'required' : '';
      const dica = c.dica ? html`<small class="mudo">${c.dica}</small>` : '';
      let ctl;
      if (c.tipo === 'select') {
        ctl = html`<select id="${id}" name="${c.name}" ${raw(req)}>${lista(c.opcoes, o =>
          html`<option value="${o.v}" ${raw(String(o.v) === String(v) ? 'selected' : '')}>${o.t}</option>`)}</select>`;
      } else if (c.tipo === 'textarea') {
        ctl = html`<textarea id="${id}" name="${c.name}" rows="3" ${raw(c.max ? 'maxlength="' + Number(c.max) + '"' : '')} ${raw(req)}>${v}</textarea>`;
      } else if (c.tipo === 'checkbox') {
        return html`<div class="linha-check"><input type="checkbox" id="${id}" name="${c.name}" ${raw(v ? 'checked' : '')}><label for="${id}">${c.label}</label></div>`;
      } else {
        ctl = html`<input id="${id}" name="${c.name}" type="${c.tipo || 'text'}" value="${v}" ${raw(c.max ? 'maxlength="' + Number(c.max) + '"' : '')}
          ${raw(c.passo ? 'step="' + esc(c.passo) + '"' : '')} ${raw(c.min != null ? 'min="' + esc(c.min) + '"' : '')} ${raw(c.maxVal != null ? 'max="' + esc(c.maxVal) + '"' : '')}
          ${raw(c.tipo === 'password' ? 'autocomplete="current-password"' : '')} ${raw(req)}>`;
      }
      return html`<label for="${id}">${c.label}${raw(c.obrigatorio ? ' <span aria-hidden="true">*</span>' : '')}</label>${raw(ctl)}${raw(dica)}<p class="erro-campo" id="e-${c.name}" role="alert"></p>`;
    };
    dlg.innerHTML = html`<form method="dialog" novalidate>
      <h2 id="dlg-titulo">${titulo}</h2>${raw(intro ? html`<p class="mudo">${intro}</p>` : '')}
      <div class="aviso erro" id="erro-form" role="alert" hidden></div>
      ${lista(campos, campo)}
      <div class="modal-acoes"><button type="button" class="btn sec" data-cancelar>Cancelar</button><button type="submit" class="btn">${rotulo}</button></div></form>`;
    document.body.appendChild(dlg);
    let ok = false;
    const form = dlg.querySelector('form');
    dlg.querySelector('[data-cancelar]').onclick = () => dlg.close();
    dlg.addEventListener('close', () => { dlg.remove(); resolve(ok); });
    form.addEventListener('submit', async ev => {
      ev.preventDefault();
      if (!form.checkValidity()) { form.reportValidity(); return; }
      dlg.querySelectorAll('.erro-campo').forEach(p => p.textContent = '');
      const erroForm = dlg.querySelector('#erro-form'); erroForm.hidden = true;
      const dados = {};
      campos.forEach(c => {
        const el = form.elements[c.name];
        dados[c.name] = c.tipo === 'checkbox' ? el.checked : el.value;
      });
      const botao = form.querySelector('button[type=submit]'); botao.disabled = true;
      try {
        await enviar(dados); ok = true; dlg.close();
      } catch (e) {
        erroForm.textContent = e.message; erroForm.hidden = false;
        if (e.campos) Object.entries(e.campos).forEach(([k, m]) => { const p = dlg.querySelector('#e-' + k); if (p) p.textContent = m; });
        botao.disabled = false;
      }
    });
    dlg.showModal();
    const primeiro = dlg.querySelector('input,select,textarea'); if (primeiro) primeiro.focus();
  });
}

function confirmar(mensagem, rotulo = 'Confirmar', perigo = true) {
  return new Promise(resolve => {
    const dlg = document.createElement('dialog');
    dlg.className = 'modal'; dlg.setAttribute('aria-labelledby', 'dlg-conf');
    dlg.innerHTML = html`<h2 id="dlg-conf">${mensagem}</h2><div class="modal-acoes">
      <button class="btn sec" data-nao>Cancelar</button><button class="btn ${perigo ? 'perigo' : ''}" data-sim>${rotulo}</button></div>`;
    document.body.appendChild(dlg);
    let r = false;
    dlg.querySelector('[data-nao]').onclick = () => dlg.close();
    dlg.querySelector('[data-sim]').onclick = () => { r = true; dlg.close(); };
    dlg.addEventListener('close', () => { dlg.remove(); resolve(r); });
    dlg.showModal();
  });
}

/* ---------- mini-player (RF11) ----------
   Fica fora da área de conteúdo, por isso continua tocando enquanto o estudante navega. */
const player = {
  abrir(titulo, urlEmbed) {
    const el = document.getElementById('player'); if (!el) return;
    if (!/^https:\/\/(www\.youtube-nocookie\.com\/embed\/|player\.vimeo\.com\/video\/)/.test(urlEmbed || '')) { toast('Este conteúdo não possui vídeo.', 'erro'); return; }
    const origem = document.getElementById('player-origem');
    const videoId = urlEmbed.split('/').pop();
    origem.href = urlEmbed.includes('youtube-nocookie.com') ? 'https://www.youtube.com/watch?v=' + videoId : 'https://vimeo.com/' + videoId;
    origem.hidden = false;
    document.getElementById('player-titulo').textContent = titulo;
    const box = document.getElementById('player-video'); box.textContent = '';
    const f = document.createElement('iframe');
    f.src = urlEmbed; f.title = 'Vídeo: ' + titulo; f.allowFullscreen = true;
    f.allow = 'autoplay; encrypted-media; picture-in-picture'; f.referrerPolicy = 'strict-origin-when-cross-origin';
    box.appendChild(f);
    el.hidden = false; el.classList.remove('min');
    document.getElementById('player-min').setAttribute('aria-pressed', 'false');
  },
  minimizar() {
    const el = document.getElementById('player'); const min = el.classList.toggle('min');
    document.getElementById('player-min').setAttribute('aria-pressed', String(min));
    document.getElementById('player-min').setAttribute('aria-label', min ? 'Expandir player' : 'Minimizar player');
  },
  fechar() { const el = document.getElementById('player'); document.getElementById('player-video').textContent = ''; el.hidden = true; }
};
