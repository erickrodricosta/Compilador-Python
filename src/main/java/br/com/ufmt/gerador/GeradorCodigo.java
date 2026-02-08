package br.com.ufmt.gerador;

import java.util.ArrayList;
import java.util.List;

public class GeradorCodigo {
    private List<String> instrucoes;

    public GeradorCodigo() {
        this.instrucoes = new ArrayList<>();
    }

    public int gerarInstrucao(String instrucao) {
        instrucoes.add(instrucao);
        return instrucoes.size() - 1;
    }

    public int gerarInstrucao(String instrucao, int argumento) {
        instrucoes.add(instrucao + " " + argumento);
        return instrucoes.size() - 1;
    }

    public int gerarInstrucao(String instrucao, double argumento) {
        instrucoes.add(instrucao + " " + argumento);
        return instrucoes.size() - 1;
    }

    public int obterEnderecoAtual() {
        return instrucoes.size();
    }

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