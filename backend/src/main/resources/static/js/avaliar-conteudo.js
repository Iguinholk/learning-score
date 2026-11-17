'use strict';
(async function () {
  const parametros = new URLSearchParams(location.hash.slice(1));
  let token = parametros.get('token') || '';
  const decisao = parametros.get('decisao');
  history.replaceState(null, '', location.pathname); // Retira a chave do histórico após a leitura.
  const mensagem = document.getElementById('mensagem-avaliacao');
  const formulario = document.getElementById('form-avaliacao');
  const botao = document.getElementById('confirmar-avaliacao');
  function erro(texto) { mensagem.textContent = texto; mensagem.setAttribute('role', 'alert'); }
  if (!/^[A-Za-z0-9_-]{43}$/.test(token) || !['APROVAR', 'RECUSAR'].includes(decisao)) {
    erro('Convite inválido. Abra um dos botões do e-mail recebido.'); return;
  }
  try {
    const resposta = await fetch('/api/avaliacoes-conteudo', {
      headers: { 'X-Avaliacao-Token': token }, credentials: 'same-origin', cache: 'no-store', referrerPolicy: 'no-referrer'
    });
    const dados = await resposta.json();
    if (!resposta.ok) throw new Error(dados.mensagem || 'Não foi possível abrir o convite.');
    document.getElementById('titulo-avaliacao').textContent = dados.titulo;
    document.getElementById('descricao-avaliacao').textContent = dados.descricao || '';
    document.getElementById('palavras-avaliacao').textContent = 'Palavras-chave: ' + (dados.palavrasChave || '—');
    document.getElementById('validade-avaliacao').textContent = 'Convite válido até ' + new Date(dados.expiraEm).toLocaleString('pt-BR');
    if (dados.urlVideo) {
      const url = new URL(dados.urlVideo);
      if (url.protocol === 'https:') { const a = document.getElementById('video-avaliacao'); a.href = url.href; a.hidden = false; }
    }
    document.getElementById('detalhes-avaliacao').hidden = false;
    document.getElementById('decisao-avaliacao').textContent = decisao === 'APROVAR' ? 'Sua decisão: aprovar este conteúdo.' : 'Sua decisão: recusar este conteúdo.';
    botao.textContent = decisao === 'APROVAR' ? 'Confirmar aprovação' : 'Confirmar recusa';
    mensagem.textContent = 'Escreva sua justificativa e confirme abaixo.';
    formulario.hidden = false;
  } catch (e) { erro(e.message); return; }
  formulario.addEventListener('submit', async ev => {
    ev.preventDefault();
    if (!formulario.reportValidity()) return;
    const justificativa = document.getElementById('justificativa').value.trim();
    if (!justificativa) { erro('Preencha a justificativa.'); return; }
    botao.disabled = true;
    try {
      // O mesmo cliente CSRF já usado no aplicativo; GET acima materializa o cookie.
      await api.post('/api/avaliacoes-conteudo/confirmar', { token, decisao, justificativa });
      token = '';
      formulario.hidden = true;
      mensagem.setAttribute('role', 'status');
      mensagem.textContent = 'Resposta salva. Obrigado! O estudante poderá consultar sua avaliação e será avisado por e-mail.';
    } catch (e) { erro(e.message); botao.disabled = false; }
  });
})();
