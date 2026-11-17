'use strict';
/* ===== Casca da aplicação: menu lateral, roteador por hash e mini-player ===== */

const ICONES = {
  painel: '<path d="M3 3h7v9H3zM14 3h7v5h-7zM14 12h7v9h-7zM3 16h7v5H3z"/>',
  atividades: '<path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>',
  calendario: '<rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/>',
  academico: '<path d="M22 10L12 5 2 10l10 5 10-5z"/><path d="M6 12v5c3 2 9 2 12 0v-5"/>',
  financeiro: '<rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20"/>',
  sino: '<path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9M13.7 21a2 2 0 0 1-3.4 0"/>',
  estrela: '<path d="M12 2l3 7 7 .6-5.3 4.7 1.6 7.2L12 17.8 5.7 21.5l1.6-7.2L2 9.6 9 9z"/>',
  video: '<rect x="2" y="5" width="14" height="14" rx="2"/><path d="M22 7l-6 5 6 5z"/>',
  engrenagem: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"/>',
  escudo: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>',
  sair: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"/>'
};
const ROTAS = [
  { rota: 'dashboard', rotulo: 'Dashboard', icone: 'painel', fn: pgDashboard },
  { rota: 'atividades', rotulo: 'Atividades', icone: 'atividades', fn: pgAtividades },
  { rota: 'calendario', rotulo: 'Calendário', icone: 'calendario', fn: pgCalendario },
  { rota: 'academico', rotulo: 'Acadêmico', icone: 'academico', fn: pgAcademico },
  { rota: 'financeiro', rotulo: 'Financeiro', icone: 'financeiro', fn: pgFinanceiro },
  { rota: 'recomendacoes', rotulo: 'Recomendações', icone: 'estrela', fn: pgRecomendacoes },
  { rota: 'conteudos', rotulo: 'Conteúdos', icone: 'video', fn: pgConteudos },
  { rota: 'configuracoes', rotulo: 'Configurações', icone: 'engrenagem', fn: pgConfiguracoes },
];

const ROTAS_INTERNAS = {
  notificacoes: { rota: 'notificacoes', rotulo: 'Notificações', fn: pgNotificacoes },
  privacidade: {
    rota: 'privacidade',
    rotulo: 'Privacidade e conta',
    fn: pgPrivacidade
  }
};

const svg = n => `<svg viewBox="0 0 24 24" aria-hidden="true">${ICONES[n]}</svg>`;


/* Iniciais para o avatar: "Igor Gonçalves" vira "IG" (ignora marcas como "[DEMO]") */
function iniciais(nome) {
  const partes = nome.replace(/\[.*?\]/g, '').trim().split(/\s+/).filter(Boolean);
  if (!partes.length) return '?';
  const primeira = partes[0][0];
  const ultima = partes.length > 1 ? partes[partes.length - 1][0] : '';
  return (primeira + ultima).toUpperCase();
}

function desenharMenu() {
  const nome = window.usuario.nome;

  document.getElementById('lateral').innerHTML = html`
    <div class="menu-cabecalho">
      <a class="marca" href="#/dashboard"><img src="/img/logo-horizontal.png" alt="Learning Score"></a>
      <img class="logo-pequena" src="/img/favicon-192.png" alt="" aria-hidden="true">
    </div>

    <nav aria-label="Principal">
      ${lista(ROTAS, r => html`
        <a href="#/${r.rota}" data-rota="${r.rota}">
          ${raw(svg(r.icone))}
          <span>${r.rotulo}</span>
          ${raw(r.selo
              ? '<span class="selo" id="selo-notif" hidden></span>'
              : ''
          )}
        </a>
      `)}
    </nav>

    <div class="perfil">
      <span class="perfil-avatar" aria-hidden="true">${iniciais(nome)}</span>
      <div class="perfil-info">
        <span class="perfil-nome">${nome}</span>
        <a class="perfil-link" href="#/privacidade" data-rota="privacidade">Minha conta</a>
      </div>
      <button id="btn-sair" class="perfil-sair" aria-label="Sair" title="Sair">${raw(svg('sair'))}</button>
    </div>
  `;

  // Botão de sair da conta
  document.getElementById('btn-sair').onclick = async () => {
    try {
      await api.post('/api/auth/logout');
    } catch (e) {
      /* ignora erros de logout */
    }
    location.href = '/login.html';
  };
}


async function atualizarSelo() {
  try {
    const d = await api.get('/api/dashboard');
    const s = document.getElementById('selo-notif'); if (!s) return;
    const n = d.indicadores.notificacoesNaoLidas; s.textContent = n; s.hidden = n === 0;
    s.setAttribute('aria-label', n + ' notificações não lidas');
    s.closest('a').setAttribute('aria-label', n ? 'Notificações: ' + n + ' não lidas' : 'Notificações');
  } catch (e) { /* silencioso */ }
}

async function rotear() {
  const main = document.getElementById('conteudo');
  const partes = (location.hash || '#/dashboard').replace(/^#\/?/, '').split('/');
  const nome = partes[0] || 'dashboard';
  limparGraficos();
  document.body.classList.remove('menu-aberto');
  document.getElementById('btn-menu').setAttribute('aria-expanded', 'false');
  document.querySelectorAll('#lateral [data-rota]').forEach(a => { if (a.dataset.rota === nome) a.setAttribute('aria-current', 'page'); else a.removeAttribute('aria-current'); });
  main.innerHTML = '<p class="vazio">Carregando…</p>';
  try {
    if (nome === 'atividades' && partes[1]) await pgAtividadeDetalhe(main, partes[1]);

    else {
      const r = ROTAS.find(x => x.rota === nome)
          || ROTAS_INTERNAS[nome];

      if (!r) {
        location.hash = '#/dashboard';
        return;
      }

      await r.fn(main);
    }

    document.title = (
        ROTAS.find(x => x.rota === nome)?.rotulo
        || ROTAS_INTERNAS[nome]?.rotulo
        || 'Learning Score'
    ) + ' — Learning Score';
    if (nome !== 'notificacoes') atualizarSelo();
    main.focus({ preventScroll: true });
  } catch (e) {
    main.innerHTML = html`<div class="aviso erro" role="alert">${e.message}</div>`;
  }
}

(async function iniciar() {
  try { window.usuario = await api.get('/api/auth/me'); }
  catch (e) { location.href = '/login.html'; return; }
  desenharMenu();
  document.getElementById('btn-menu').onclick = e => { const a = document.body.classList.toggle('menu-aberto'); e.currentTarget.setAttribute('aria-expanded', String(a)); };
  document.getElementById('player-min').onclick = () => player.minimizar();
  document.getElementById('player-fechar').onclick = () => player.fechar();
  window.addEventListener('hashchange', rotear);
  rotear();
})();
