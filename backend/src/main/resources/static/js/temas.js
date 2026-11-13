
/* LEARNING SCORE — CONTROLE DE TEMAS */

(() => {
    'use strict';

    const CHAVE = 'learning-score-tema';
    const OPCOES = ['light', 'dark', 'auto'];

    const sistema = window.matchMedia(
        '(prefers-color-scheme: dark)'
    );

    function obterPreferencia() {
        try {
            const salvo = localStorage.getItem(CHAVE);
            return OPCOES.includes(salvo) ? salvo : 'auto';
        } catch {
            return 'auto';
        }
    }

    function aplicarTema(preferencia) {
        const tema = preferencia === 'auto'
            ? (sistema.matches ? 'dark' : 'light')
            : preferencia;

        document.documentElement.dataset.theme = tema;
        document.documentElement.dataset.themePreference = preferencia;

        document.dispatchEvent(
            new CustomEvent('learning-score:tema', {
                detail: { tema, preferencia }
            })
        );
    }

    function definirTema(preferencia) {
        if (!OPCOES.includes(preferencia)) return;

        try {
            localStorage.setItem(CHAVE, preferencia);
        } catch {
            // O tema ainda funciona sem armazenamento.
        }

        aplicarTema(preferencia);
    }

    // Permite controlar o tema de outras partes do projeto.
    window.LearningScoreTema = {
        obter: obterPreferencia,
        definir: definirTema
    };

    aplicarTema(obterPreferencia());

    // Atualiza automaticamente quando o sistema muda.
    sistema.addEventListener('change', () => {
        if (obterPreferencia() === 'auto') {
            aplicarTema('auto');
        }
    });
})();
