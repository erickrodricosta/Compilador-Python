package br.com.ufmt.sintatico;

import br.com.ufmt.gerador.GeradorCodigo;
import br.com.ufmt.semantico.TabelaSimbolos;
import br.com.ufmt.lexico.AnalisadorLexico;
import br.com.ufmt.lexico.TipoToken;
import br.com.ufmt.lexico.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Realiza a Análise Sintática Descendente Recursiva do código fonte.
 * Verifica se a sequência de tokens obedece à gramática da linguagem e coordena
 * a análise semântica e geração de código.
 */
public class AnalisadorSintatico {
    private AnalisadorLexico analisadorLexico;
    private TabelaSimbolos tabelaSimbolos;
    private GeradorCodigo geradorCodigo;
    private Token tokenAtual;

    /**
     * Inicializa o analisador sintático.
     * @param analisadorLexico Fonte dos tokens.
     * @param tabelaSimbolos Gerenciador de contexto.
     * @param geradorCodigo Emissor de código objeto.
     */
    public AnalisadorSintatico(AnalisadorLexico analisadorLexico, TabelaSimbolos tabelaSimbolos, GeradorCodigo geradorCodigo) {
        this.analisadorLexico = analisadorLexico;
        this.tabelaSimbolos = tabelaSimbolos;
        this.geradorCodigo = geradorCodigo;
        this.tokenAtual = analisadorLexico.obterProximoToken();
    }

    /**
     * Verifica se o token atual é do tipo esperado e avança para o próximo.
     * Caso contrário, lança um erro sintático.
     * @param tipo Tipo de token esperado.
     */
    private void consumirToken(TipoToken tipo) {
        if (tokenAtual.tipo == tipo) {
            tokenAtual = analisadorLexico.obterProximoToken();
        } else {
            lancarErroSintatico("Esperado " + tipo + " mas encontrado " + tokenAtual.tipo);
        }
    }

    /**
     * Lança uma exceção de erro sintático formatada com a linha atual.
     * @param mensagem Descrição do erro encontrado.
     */
    private void lancarErroSintatico(String mensagem) {
        throw new RuntimeException("Erro de Sintaxe na linha " + tokenAtual.linha + ": " + mensagem);
    }

    /**
     * Verifica se o tipo do token corresponde ao início de um comando válido.
     * @param tipo O tipo do token a ser verificado.
     * @return true se for PRINT, IF, WHILE ou IDENTIFICADOR; false caso contrário.
     */
    private boolean verificarSeEComando(TipoToken tipo) {
        return tipo == TipoToken.PRINT ||
                tipo == TipoToken.IF ||
                tipo == TipoToken.WHILE ||
                tipo == TipoToken.IDENTIFICADOR;
    }

    /**
     * Regra inicial da gramática. Analisa o programa completo.
     * Gera as instruções de início (INPP) e fim (PARA).
     */
    public void analisarPrograma() {
        geradorCodigo.gerarInstrucao("INPP");
        analisarCorpo();
        geradorCodigo.gerarInstrucao("PARA");
    }

    /**
     * Analisa o corpo do programa, composto por declarações (variáveis/funções)
     * seguidas pelos comandos do bloco principal.
     */
    private void analisarCorpo() {
        analisarDeclaracoes();
        analisarComandos(false);
    }

    /**
     * Processa a sequência de declarações de variáveis globais e funções.
     * Identifica se é uma função (DEF) ou variável (IDENT) e chama o método apropriado.
     */
    private void analisarDeclaracoes() {
        while (tokenAtual.tipo == TipoToken.IDENTIFICADOR || tokenAtual.tipo == TipoToken.DEF) {
            if (tokenAtual.tipo == TipoToken.DEF) {
                analisarDeclaracaoFuncao();
            }
            else if (tokenAtual.tipo == TipoToken.IDENTIFICADOR) {
                // Se o identificador já é uma função conhecida, paramos as declarações (início do main)
                if (tabelaSimbolos.obterEnderecoFuncao(tokenAtual.lexema) != null) {
                    break;
                }
                analisarDeclaracaoVariavel();
            }
        }
    }

    /**
     * Analisa a declaração e inicialização de uma variável.
     * Gera código para alocação (ALME) e atribuição do valor inicial.
     */
    private void analisarDeclaracaoVariavel() {
        String nomeVariavel = tokenAtual.lexema;
        consumirToken(TipoToken.IDENTIFICADOR);

        tabelaSimbolos.adicionarVariavel(nomeVariavel);
        geradorCodigo.gerarInstrucao("ALME", 1);

        consumirToken(TipoToken.ATRIBUICAO);
        analisarExpressao();

        int[] info = tabelaSimbolos.obterInfoVariavel(nomeVariavel);
        if (info[1] == 1) { // 1 = Local
            geradorCodigo.gerarInstrucao("AMREL", info[0]);
        } else { // 0 = Global
            geradorCodigo.gerarInstrucao("ARMZ", info[0]);
        }
    }

    /**
     * Analisa a definição de uma função (def nome(params): bloco).
     * Gerencia a criação do escopo local, desvio do fluxo principal e retorno.
     */
    private void analisarDeclaracaoFuncao() {
        consumirToken(TipoToken.DEF);
        String nomeFuncao = tokenAtual.lexema;
        consumirToken(TipoToken.IDENTIFICADOR);

        int indiceDesvio = geradorCodigo.gerarInstrucao("DSVI", 0);
        tabelaSimbolos.registrarFuncao(nomeFuncao, geradorCodigo.obterEnderecoAtual());

        tabelaSimbolos.entrarEscopoFuncao();
        geradorCodigo.gerarInstrucao("ENPR");

        consumirToken(TipoToken.PARENTESES_ESQ);
        List<Integer> enderecosParams = new ArrayList<>();
        analisarListaParametros(enderecosParams);
        consumirToken(TipoToken.PARENTESES_DIR);
        consumirToken(TipoToken.DOIS_PONTOS);

        Collections.reverse(enderecosParams);
        for (Integer endereco : enderecosParams) {
            geradorCodigo.gerarInstrucao("AMREL", endereco);
        }

        analisarBloco();

        tabelaSimbolos.sairEscopoFuncao();

        geradorCodigo.gerarInstrucao("RTPR"); // Retorno de procedimento
        geradorCodigo.atualizarEnderecoDesvio(indiceDesvio, geradorCodigo.obterEnderecoAtual());
    }

    /**
     * Analisa a lista de parâmetros formais na definição de uma função.
     * @param enderecosParams Lista para armazenar os endereços das variáveis criadas.
     */
    private void analisarListaParametros(List<Integer> enderecosParams) {
        if (tokenAtual.tipo == TipoToken.IDENTIFICADOR) {
            String nomeVar = tokenAtual.lexema;
            consumirToken(TipoToken.IDENTIFICADOR);

            int endereco = tabelaSimbolos.adicionarVariavel(nomeVar);
            enderecosParams.add(endereco);

            analisarMaisParametros(enderecosParams);
        }
    }

    /**
     * Analisa os parâmetros subsequentes, separados por vírgula.
     * @param enderecosParams Lista para armazenar os endereços das variáveis criadas.
     */
    private void analisarMaisParametros(List<Integer> enderecosParams) {
        if (tokenAtual.tipo == TipoToken.VIRGULA) {
            consumirToken(TipoToken.VIRGULA);
            analisarListaParametros(enderecosParams);
        }
    }

    /**
     * Analisa um bloco de código identado.
     * Espera uma tabulação inicial e depois uma sequência de comandos.
     */
    private void analisarBloco() {
        consumirToken(TipoToken.TABULACAO);
        analisarComandos(true);
    }

    /**
     * Analisa uma sequência de comandos.
     * Lida com linhas vazias (tabulações extras) e verifica o fim do bloco.
     * @param dentroDeBloco Indica se estamos dentro de uma estrutura identada.
     */
    private void analisarComandos(boolean dentroDeBloco) {
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

    /**
     * Analisa recursivamente os próximos comandos da sequência.
     * Controla a identação para determinar se o bloco continua ou termina (dedent).
     * @param dentroDeBloco Indica se estamos dentro de uma estrutura identada.
     */
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
                else if (tokenAtual.tipo == TipoToken.TABULACAO) {
                    analisarMaisComandos(true);
                }
            } else {
                return;
            }
        } else {
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

    /**
     * Analisa um comando individual (print, if, while, atribuição ou chamada).
     * Desvia o fluxo para o método específico de cada comando.
     */
    private void analisarComando() {
        if (tokenAtual.tipo == TipoToken.PRINT) {
            consumirToken(TipoToken.PRINT);
            consumirToken(TipoToken.PARENTESES_ESQ);

            String nomeVar = tokenAtual.lexema;
            consumirToken(TipoToken.IDENTIFICADOR);

            int[] info = tabelaSimbolos.obterInfoVariavel(nomeVar);
            if (info == null) lancarErroSintatico("Variavel nao declarada: " + nomeVar);

            if (info[1] == 1) {
                geradorCodigo.gerarInstrucao("CREL", info[0]);
            } else {
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

    /**
     * Analisa o restante de uma instrução que começou com um identificador.
     * Pode ser uma atribuição (var = expr) ou uma chamada de função (func()).
     * @param nome O nome do identificador lido inicialmente.
     */
    private void analisarRestoIdentificador(String nome) {
        if (tokenAtual.tipo == TipoToken.ATRIBUICAO) {
            consumirToken(TipoToken.ATRIBUICAO);

            if (!tabelaSimbolos.existeNoEscopoAtual(nome)) {
                tabelaSimbolos.adicionarVariavel(nome);
                geradorCodigo.gerarInstrucao("ALME", 1);
            }

            analisarExpressao();

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

    /**
     * Analisa uma condição relacional (ex: a > b).
     * Gera instrução de comparação correspondente.
     */
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

    /**
     * Analisa a estrutura condicional IF / ELSE.
     * Gera desvios condicionais (DSVF) e incondicionais (DSVI) para controle de fluxo.
     */
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

    /**
     * Analisa a estrutura de repetição WHILE.
     * Salva o endereço de início para o loop e gera desvio se a condição for falsa.
     */
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

    /**
     * Analisa os argumentos passados em uma chamada de função.
     */
    private void analisarArgumentos() {
        if (tokenAtual.tipo != TipoToken.PARENTESES_DIR) {
            analisarExpressao();
            analisarMaisArgumentos();
        }
    }

    /**
     * Analisa argumentos adicionais separados por vírgula.
     */
    private void analisarMaisArgumentos() {
        if (tokenAtual.tipo == TipoToken.VIRGULA) {
            consumirToken(TipoToken.VIRGULA);
            analisarArgumentos();
        }
    }

    /**
     * Analisa uma expressão aritmética ou um comando de entrada (input).
     */
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

    /**
     * Analisa operações de soma e subtração (+, -).
     */
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

    /**
     * Analisa um termo (fator seguido de multiplicações ou divisões).
     */
    private void analisarTermo() {
        analisarFator();
        analisarMaisFatores();
    }

    /**
     * Analisa operações de multiplicação e divisão (*, /).
     */
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

    /**
     * Analisa o fator básico de uma expressão: identificador, número ou sub-expressão entre parênteses.
     */
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