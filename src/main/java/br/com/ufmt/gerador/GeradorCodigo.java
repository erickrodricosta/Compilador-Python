package br.com.ufmt.gerador;

import java.util.ArrayList;
import java.util.List;

public class GeradorCodigo {
    // Lista que armazena as instruções (ex: "INPP", "CRVL 1")
    private List<String> instrucoes;

    public GeradorCodigo() {
        this.instrucoes = new ArrayList<>();
    }

    // Gera uma instrução sem argumentos (ex: SOMA, INPP)
    public int gerarInstrucao(String instrucao) {
        instrucoes.add(instrucao);
        return instrucoes.size() - 1; // Retorna o índice da instrução gerada
    }

    // Gera uma instrução com argumento inteiro (ex: CRVL 10, DSVF 50)
    public int gerarInstrucao(String instrucao, int argumento) {
        instrucoes.add(instrucao + " " + argumento);
        return instrucoes.size() - 1;
    }

    // Gera uma instrução com argumento double (ex: CRCT 3.4)
    public int gerarInstrucao(String instrucao, double argumento) {
        instrucoes.add(instrucao + " " + argumento);
        return instrucoes.size() - 1;
    }

    // Retorna o contador de programa atual (próxima linha livre)
    public int obterEnderecoAtual() {
        return instrucoes.size();
    }

    // Backpatching: Corrige o argumento de uma instrução anterior (usado em IF e WHILE)
    public void atualizarEnderecoDesvio(int enderecoInstrucao, int novoArgumento) {
        String original = instrucoes.get(enderecoInstrucao);
        // Pega o nome da instrução (ex: "DSVF 0" -> "DSVF")
        String opCode = original.split(" ")[0];
        // Atualiza com o novo argumento (destino do salto)
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