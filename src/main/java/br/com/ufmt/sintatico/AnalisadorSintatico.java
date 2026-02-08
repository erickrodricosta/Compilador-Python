package br.com.ufmt.sintatico;

import br.com.ufmt.gerador.GeradorCodigo;
import br.com.ufmt.semantico.TabelaSimbolos;
import br.com.ufmt.lexico.AnalisadorLexico;
import br.com.ufmt.lexico.TipoToken;
import br.com.ufmt.lexico.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnalisadorSintatico {
    private AnalisadorLexico analisadorLexico;
    private TabelaSimbolos tabelaSimbolos;
    private GeradorCodigo geradorCodigo;
    private Token tokenAtual;

    public AnalisadorSintatico(AnalisadorLexico analisadorLexico, TabelaSimbolos tabelaSimbolos, GeradorCodigo geradorCodigo) {
        this.analisadorLexico = analisadorLexico;
        this.tabelaSimbolos = tabelaSimbolos;
        this.geradorCodigo = geradorCodigo;
        this.tokenAtual = analisadorLexico.obterProximoToken();
    }

    // === Métodos Auxiliares ===

    private void consumirToken(TipoToken tipo) {
        if (tokenAtual.tipo == tipo) {
            tokenAtual = analisadorLexico.obterProximoToken();
        } else {
            lancarErroSintatico("Esperado " + tipo + " mas encontrado " + tokenAtual.tipo);
        }
    }

    private void lancarErroSintatico(String mensagem) {
        throw new RuntimeException("Erro de Sintaxe na linha " + tokenAtual.linha + ": " + mensagem);
    }

    private boolean verificarSeEComando(TipoToken tipo) {
        return tipo == TipoToken.PRINT ||
                tipo == TipoToken.IF ||
                tipo == TipoToken.WHILE ||
                tipo == TipoToken.IDENTIFICADOR;
    }

    // === Regras Gramaticais: Estrutura Geral ===

    public void analisarPrograma() {
        geradorCodigo.gerarInstrucao("INPP");
        analisarCorpo();
        geradorCodigo.gerarInstrucao("PARA");
    }

    private void analisarCorpo() {
        analisarDeclaracoes();
        analisarComandos(false);
    }

    private void analisarDeclaracoes() {
        while (tokenAtual.tipo == TipoToken.IDENTIFICADOR || tokenAtual.tipo == TipoToken.DEF) {
            if (tokenAtual.tipo == TipoToken.DEF) {
                analisarDeclaracaoFuncao();
            }
            else if (tokenAtual.tipo == TipoToken.IDENTIFICADOR) {
                // Se o identificador já é uma função conhecida, paramos as declarações
                if (tabelaSimbolos.obterEnderecoFuncao(tokenAtual.lexema) != null) {
                    break;
                }
                analisarDeclaracaoVariavel();
            }
        }
    }

    private void analisarDeclaracaoVariavel() {
        String nomeVariavel = tokenAtual.lexema;
        consumirToken(TipoToken.IDENTIFICADOR);

        tabelaSimbolos.adicionarVariavel(nomeVariavel);
        geradorCodigo.gerarInstrucao("ALME", 1);

        consumirToken(TipoToken.ATRIBUICAO);
        analisarExpressao();

        // Verifica escopo para decidir entre Local (AMREL) ou Global (ARMZ)
        int[] info = tabelaSimbolos.obterInfoVariavel(nomeVariavel);
        if (info[1] == 1) { // 1 = Local
            geradorCodigo.gerarInstrucao("AMREL", info[0]);
        } else { // 0 = Global
            geradorCodigo.gerarInstrucao("ARMZ", info[0]);
        }
    }

    // === Regras Gramaticais: Funções ===

    private void analisarDeclaracaoFuncao() {
        consumirToken(TipoToken.DEF);
        String nomeFuncao = tokenAtual.lexema;
        consumirToken(TipoToken.IDENTIFICADOR);

        int indiceDesvio = geradorCodigo.gerarInstrucao("DSVI", 0);
        tabelaSimbolos.registrarFuncao(nomeFuncao, geradorCodigo.obterEnderecoAtual());

        tabelaSimbolos.entrarEscopoFuncao(); // Inicia escopo local
        geradorCodigo.gerarInstrucao("ENPR");        // Cria stack frame na VM

        consumirToken(TipoToken.PARENTESES_ESQ);
        List<Integer> enderecosParams = new ArrayList<>();
        analisarListaParametros(enderecosParams);
        consumirToken(TipoToken.PARENTESES_DIR);
        consumirToken(TipoToken.DOIS_PONTOS);

        // Desempilha parâmetros (LIFO) para variáveis locais
        Collections.reverse(enderecosParams);
        for (Integer endereco : enderecosParams) {
            geradorCodigo.gerarInstrucao("AMREL", endereco);
        }

        analisarBloco();

        tabelaSimbolos.sairEscopoFuncao(); // Volta para escopo global

        geradorCodigo.gerarInstrucao("RTPR");
        geradorCodigo.atualizarEnderecoDesvio(indiceDesvio, geradorCodigo.obterEnderecoAtual());
    }

    private void analisarListaParametros(List<Integer> enderecosParams) {
        if (tokenAtual.tipo == TipoToken.IDENTIFICADOR) {
            String nomeVar = tokenAtual.lexema;
            consumirToken(TipoToken.IDENTIFICADOR);

            // Adiciona variável ao escopo local
            int endereco = tabelaSimbolos.adicionarVariavel(nomeVar);
            enderecosParams.add(endereco);

            analisarMaisParametros(enderecosParams);
        }
    }

    private void analisarMaisParametros(List<Integer> enderecosParams) {
        if (tokenAtual.tipo == TipoToken.VIRGULA) {
            consumirToken(TipoToken.VIRGULA);
            analisarListaParametros(enderecosParams);
        }
    }

    // === Regras Gramaticais: Comandos e Blocos ===

    private void analisarBloco() {
        consumirToken(TipoToken.TABULACAO);
        analisarComandos(true);
    }

    private void analisarComandos(boolean dentroDeBloco) {
        // Consome linhas vazias/comentários iniciais
        while (tokenAtual.tipo == TipoToken.TABULACAO) {
            consumirToken(TipoToken.TABULACAO);
            if (tokenAtual.tipo == TipoToken.FIM_ARQUIVO || tokenAtual.tipo == TipoToken.ELSE) {
                return;
            }
        }

        if (verificarSeEComando(tokenAtual.tipo)) {
            analisarComando();
            analisarMaisComandos(dentroDeBloco);
        }
    }

    private void analisarMaisComandos(boolean dentroDeBloco) {
        if (tokenAtual.tipo == TipoToken.FIM_ARQUIVO || tokenAtual.tipo == TipoToken.ELSE) {
            return;
        }

        if (dentroDeBloco) {
            if (tokenAtual.tipo == TipoToken.TABULACAO) {
                consumirToken(TipoToken.TABULACAO);

                if (tokenAtual.tipo == TipoToken.ELSE || tokenAtual.tipo == TipoToken.FIM_ARQUIVO) {
                    return;
                }

                if (verificarSeEComando(tokenAtual.tipo)) {
                    analisarComandos(true);
                }
                // Recursão para pular linhas vazias/comentários no meio do bloco
                else if (tokenAtual.tipo == TipoToken.TABULACAO) {
                    analisarMaisComandos(true);
                }
            } else {
                return; // Dedent (fim do bloco)
            }
        } else {
            // No Main (Escopo Global)
            if (verificarSeEComando(tokenAtual.tipo)) {
                analisarComandos(false);
            }
            else if (tokenAtual.tipo == TipoToken.TABULACAO) {
                consumirToken(TipoToken.TABULACAO);
                if (verificarSeEComando(tokenAtual.tipo)) {
                    analisarComandos(false);
                } else {
                    analisarMaisComandos(false);
                }
            }
        }
    }

    private void analisarComando() {
        if (tokenAtual.tipo == TipoToken.PRINT) {
            consumirToken(TipoToken.PRINT);
            consumirToken(TipoToken.PARENTESES_ESQ);

            String nomeVar = tokenAtual.lexema;
            consumirToken(TipoToken.IDENTIFICADOR); // Validação estrita LALG (só aceita variáveis)

            // Gera carregamento da variável
            int[] info = tabelaSimbolos.obterInfoVariavel(nomeVar);
            if (info == null) lancarErroSintatico("Variavel nao declarada: " + nomeVar);

            if (info[1] == 1) { // Local
                geradorCodigo.gerarInstrucao("CREL", info[0]);
            } else { // Global
                geradorCodigo.gerarInstrucao("CRVL", info[0]);
            }

            consumirToken(TipoToken.PARENTESES_DIR);
            geradorCodigo.gerarInstrucao("IMPR");
        }
        else if (tokenAtual.tipo == TipoToken.IF) {
            analisarSe();
        }
        else if (tokenAtual.tipo == TipoToken.WHILE) {
            analisarEnquanto();
        }
        else if (tokenAtual.tipo == TipoToken.IDENTIFICADOR) {
            String nome = tokenAtual.lexema;
            consumirToken(TipoToken.IDENTIFICADOR);
            analisarRestoIdentificador(nome);
        }
        else {
            lancarErroSintatico("Comando desconhecido: " + tokenAtual.tipo);
        }
    }

    private void analisarRestoIdentificador(String nome) {
        if (tokenAtual.tipo == TipoToken.ATRIBUICAO) {
            consumirToken(TipoToken.ATRIBUICAO);

            // CORREÇÃO DE SHADOWING:
            // Se for atribuição, verificamos se existe no escopo ATUAL.
            // Se não existir (mesmo que exista global), criamos uma nova nesse escopo.
            if (!tabelaSimbolos.existeNoEscopoAtual(nome)) {
                tabelaSimbolos.adicionarVariavel(nome);
                geradorCodigo.gerarInstrucao("ALME", 1);
            }

            analisarExpressao();

            // Agora a busca vai priorizar a local que acabamos de criar (se for o caso)
            int[] info = tabelaSimbolos.obterInfoVariavel(nome);
            if (info[1] == 1) {
                geradorCodigo.gerarInstrucao("AMREL", info[0]);
            } else {
                geradorCodigo.gerarInstrucao("ARMZ", info[0]);
            }
        }
        else if (tokenAtual.tipo == TipoToken.PARENTESES_ESQ) {
            if (tabelaSimbolos.obterEnderecoFuncao(nome) == null) {
                lancarErroSintatico("Funcao nao declarada: " + nome);
            }

            geradorCodigo.gerarInstrucao("PUSHER", 0);
            int indiceParam = geradorCodigo.obterEnderecoAtual() - 1;

            consumirToken(TipoToken.PARENTESES_ESQ);
            analisarArgumentos();
            consumirToken(TipoToken.PARENTESES_DIR);

            int endFuncao = tabelaSimbolos.obterEnderecoFuncao(nome);
            geradorCodigo.gerarInstrucao("CHPR", endFuncao);
            geradorCodigo.atualizarEnderecoDesvio(indiceParam, geradorCodigo.obterEnderecoAtual());
        }
    }

    // === Expressões ===

    private void analisarCondicao() {
        analisarExpressao();
        TipoToken operador = tokenAtual.tipo;
        consumirToken(operador);
        analisarExpressao();

        switch (operador) {
            case IGUAL:       geradorCodigo.gerarInstrucao("CMIG"); break;
            case DIFERENTE:   geradorCodigo.gerarInstrucao("CMDG"); break;
            case MAIOR_IGUAL: geradorCodigo.gerarInstrucao("CMAI"); break;
            case MENOR_IGUAL: geradorCodigo.gerarInstrucao("CPMI"); break;
            case MAIOR:       geradorCodigo.gerarInstrucao("CMMA"); break;
            case MENOR:       geradorCodigo.gerarInstrucao("CMME"); break;
            default: lancarErroSintatico("Operador relacional inválido");
        }
    }

    private void analisarSe() {
        consumirToken(TipoToken.IF);
        analisarCondicao();
        consumirToken(TipoToken.DOIS_PONTOS);

        int endSaltoSeFalso = geradorCodigo.gerarInstrucao("DSVF", 0);
        analisarBloco();

        if (tokenAtual.tipo == TipoToken.ELSE) {
            int endSaltoIncondicional = geradorCodigo.gerarInstrucao("DSVI", 0);
            geradorCodigo.atualizarEnderecoDesvio(endSaltoSeFalso, geradorCodigo.obterEnderecoAtual());

            consumirToken(TipoToken.ELSE);
            consumirToken(TipoToken.DOIS_PONTOS);
            analisarBloco();

            geradorCodigo.atualizarEnderecoDesvio(endSaltoIncondicional, geradorCodigo.obterEnderecoAtual());
        } else {
            geradorCodigo.atualizarEnderecoDesvio(endSaltoSeFalso, geradorCodigo.obterEnderecoAtual());
        }
    }

    private void analisarEnquanto() {
        consumirToken(TipoToken.WHILE);
        int enderecoInicio = geradorCodigo.obterEnderecoAtual();
        analisarCondicao();
        consumirToken(TipoToken.DOIS_PONTOS);

        int endSaltoFim = geradorCodigo.gerarInstrucao("DSVF", 0);
        analisarBloco();
        geradorCodigo.gerarInstrucao("DSVI", enderecoInicio);
        geradorCodigo.atualizarEnderecoDesvio(endSaltoFim, geradorCodigo.obterEnderecoAtual());
    }

    private void analisarArgumentos() {
        if (tokenAtual.tipo != TipoToken.PARENTESES_DIR) {
            analisarExpressao();
            analisarMaisArgumentos();
        }
    }

    private void analisarMaisArgumentos() {
        if (tokenAtual.tipo == TipoToken.VIRGULA) {
            consumirToken(TipoToken.VIRGULA);
            analisarArgumentos();
        }
    }

    private void analisarExpressao() {
        if (tokenAtual.tipo == TipoToken.INPUT) {
            consumirToken(TipoToken.INPUT);
            consumirToken(TipoToken.PARENTESES_ESQ);
            consumirToken(TipoToken.PARENTESES_DIR);
            geradorCodigo.gerarInstrucao("LEIT");
        } else {
            analisarTermo();
            analisarOutrosTermos();
        }
    }

    private void analisarOutrosTermos() {
        while (tokenAtual.tipo == TipoToken.SOMA || tokenAtual.tipo == TipoToken.SUBTRACAO) {
            TipoToken op = tokenAtual.tipo;
            consumirToken(op);
            analisarTermo();

            if (op == TipoToken.SOMA) {
                geradorCodigo.gerarInstrucao("SOMA");
            } else {
                geradorCodigo.gerarInstrucao("SUBT");
            }
        }
    }

    private void analisarTermo() {
        analisarFator();
        analisarMaisFatores();
    }

    private void analisarMaisFatores() {
        while (tokenAtual.tipo == TipoToken.MULTIPLICACAO || tokenAtual.tipo == TipoToken.DIVISAO) {
            TipoToken op = tokenAtual.tipo;
            consumirToken(op);

            analisarFator();

            if (op == TipoToken.MULTIPLICACAO) {
                geradorCodigo.gerarInstrucao("MULT");
            } else {
                geradorCodigo.gerarInstrucao("DIVI");
            }
        }
    }

    private void analisarFator() {
        if (tokenAtual.tipo == TipoToken.IDENTIFICADOR) {
            String nome = tokenAtual.lexema;
            consumirToken(TipoToken.IDENTIFICADOR);

            int[] info = tabelaSimbolos.obterInfoVariavel(nome);
            if (info == null) lancarErroSintatico("Variavel nao declarada: " + nome);

            if (info[1] == 1) {
                geradorCodigo.gerarInstrucao("CREL", info[0]);
            } else {
                geradorCodigo.gerarInstrucao("CRVL", info[0]);
            }
        }
        else if (tokenAtual.tipo == TipoToken.NUMERO) {
            double val = Double.parseDouble(tokenAtual.lexema);
            consumirToken(TipoToken.NUMERO);
            geradorCodigo.gerarInstrucao("CRCT", val);
        }
        else if (tokenAtual.tipo == TipoToken.PARENTESES_ESQ) {
            consumirToken(TipoToken.PARENTESES_ESQ);
            analisarExpressao();
            consumirToken(TipoToken.PARENTESES_DIR);
        }
        else {
            lancarErroSintatico("Fator inesperado: " + tokenAtual.tipo);
        }
    }
}