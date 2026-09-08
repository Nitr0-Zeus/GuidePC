// ==================== GUIDEPC v4.0 — Controle de Tema (Dark/Light) ====================

// IIFE para nao poluir o escopo global
(function() {
    // Restaurar tema salvo no localStorage ao carregar a pagina
    const temaSalvo = localStorage.getItem('guidepc-tema');
    if (temaSalvo === 'claro') {
        document.body.classList.add('tema-claro');
    }

    // Aguardar o DOM estar pronto antes de configurar o botao
    document.addEventListener('DOMContentLoaded', () => {
        const botaoAlternar = document.getElementById('botaoAlternarTema');
        if (!botaoAlternar) return;

        // Atualizar o icone e texto do botao de acordo com o tema atual
        function atualizarAparenciaBotao() {
            const estaClaro = document.body.classList.contains('tema-claro');
            botaoAlternar.textContent = estaClaro ? '\u263E' : '\u2600';
            botaoAlternar.setAttribute('aria-label', estaClaro ? 'Mudar para tema escuro' : 'Mudar para tema claro');
        }

        // Aplicar aparencia inicial do botao
        atualizarAparenciaBotao();

        // Alternar tema ao clicar no botao
        botaoAlternar.addEventListener('click', () => {
            document.body.classList.toggle('tema-claro');
            const estaClaro = document.body.classList.contains('tema-claro');
            localStorage.setItem('guidepc-tema', estaClaro ? 'claro' : 'escuro');
            atualizarAparenciaBotao();
        });
    });
})();
