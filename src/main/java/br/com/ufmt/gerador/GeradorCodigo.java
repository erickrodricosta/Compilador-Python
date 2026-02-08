package br.com.ufmt.gerador;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsável por emitir as instruções da Máquina Hipotética (Código Objeto).
 * Gerencia a lista de instruções e endereçamento para desvios (backpatching).
 */
public class GeradorCodigo {
    private List<String> instrucoes;

    public GeradorCodigo() {
        this.instrucoes = new ArrayList<>();
    }

    /**
     * Emite uma instrução sem argumentos (ex: SOMA, INPP).
     * @param instrucao Mnemônico da instrução.
     * @return O índice da instrução gerada.
     */
    public int gerarInstrucao(String instrucao) {
        instrucoes.add(instrucao);
        return instrucoes.size() - 1;
    }

    /**
     * Emite uma instrução com argumento inteiro (ex: CRVL 1, DSVF 10).
     * @param instrucao Mnemônico da instrução.
     * @param argumento Argumento inteiro.
     * @return O índice da instrução gerada.
     */
    public int gerarInstrucao(String instrucao, int argumento) {
        instrucoes.add(instrucao + " " + argumento);
        return instrucoes.size() - 1;
    }

    /**
     * Emite uma instrução com argumento real (ex: CRCT 1.5).
     * @param instrucao Mnemônico da instrução.
     * @param argumento Argumento double.
     * @return O índice da instrução gerada.
     */
    public int gerarInstrucao(String instrucao, double argumento) {
        instrucoes.add(instrucao + " " + argumento);
        return instrucoes.size() - 1;
    }

    /**
     * Retorna o endereço da próxima instrução a ser gerada.
     * @return Índice atual da lista de instruções.
     */
    public int obterEnderecoAtual() {
        return instrucoes.size();
    }

    /**
     * Atualiza o argumento de uma instrução gerada anteriormente.
     * Essencial para estruturas de controle como IF e WHILE onde o destino do salto não é conhecido inicialmente.
     * @param enderecoInstrucao O índice da instrução a ser atualizada.
     * @param novoArgumento O novo valor do argumento (geralmente um endereço de desvio).
     */
    public void atualizarEnderecoDesvio(int enderecoInstrucao, int novoArgumento) {
        String original = instrucoes.get(enderecoInstrucao);
        String opCode = original.split(" ")[0];
        instrucoes.set(enderecoInstrucao, opCode + " " + novoArgumento);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String s : instrucoes) {
            sb.append(s).append("\n");
        }
        return sb.toString();
    }
}