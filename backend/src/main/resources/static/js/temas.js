
/* LEARNING SCORE — CONTROLE DE TEMAS */

(() => {
    'use strict';

    const CHAVE = 'learning-score-tema';
    const CHAVE_TAMANHO_FONTE = 'learning-score-tamanho-fonte';
    const CHAVE_REDUZIR_MOVIMENTO = 'learning-score-reduzir-movimento';
    const OPCOES = ['light', 'dark', 'auto'];
    const TAMANHOS_FONTE = ['pequeno', 'padrao', 'grande'];

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

    function obterTamanhoFonte() {
        try {
            const salvo = localStorage.getItem(CHAVE_TAMANHO_FONTE);
            return TAMANHOS_FONTE.includes(salvo) ? salvo : 'padrao';
        } catch {
            return 'padrao';
        }
    }

    function definirTamanhoFonte(tamanho) {
        if (!TAMANHOS_FONTE.includes(tamanho)) return;
        try {
            localStorage.setItem(CHAVE_TAMANHO_FONTE, tamanho);
        } catch {
            // O ajuste continua valendo até a página ser fechada.
        }
        document.documentElement.dataset.fontSize = tamanho;
    }

    function obterReducaoMovimento() {
        try {
            return localStorage.getItem(CHAVE_REDUZIR_MOVIMENTO) === 'true';
        } catch {
            return false;
        }
    }

    function definirReducaoMovimento(reduzido) {
        const valor = Boolean(reduzido);
        try {
            localStorage.setItem(CHAVE_REDUZIR_MOVIMENTO, String(valor));
        } catch {
            // O ajuste continua valendo até a página ser fechada.
        }
        document.documentElement.dataset.reduceMotion = String(valor);
    }

    // Permite controlar o tema de outras partes do projeto.
    window.LearningScoreTema = {
        obter: obterPreferencia,
        definir: definirTema,
        obterTamanhoFonte,
        definirTamanhoFonte,
        obterReducaoMovimento,
        definirReducaoMovimento
    };

    aplicarTema(obterPreferencia());
    document.documentElement.dataset.fontSize = obterTamanhoFonte();
    document.documentElement.dataset.reduceMotion = String(obterReducaoMovimento());

    // Atualiza automaticamente quando o sistema muda.
    sistema.addEventListener('change', () => {
        if (obterPreferencia() === 'auto') {
            aplicarTema('auto');
        }
    });
})();
