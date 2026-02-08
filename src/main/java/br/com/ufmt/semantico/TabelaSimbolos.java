package br.com.ufmt.semantico;

import java.util.HashMap;
import java.util.Map;

public class TabelaSimbolos {
    private Map<String, Integer> tabelaGlobal;
    private Map<String, Integer> tabelaLocal;
    private Map<String, Integer> tabelaFuncoes;

    private int proximoEnderecoGlobal;
    private int proximoEnderecoLocal;
    private boolean dentroDeFuncao;

    public TabelaSimbolos() {
        this.tabelaGlobal = new HashMap<>();
        this.tabelaLocal = new HashMap<>();
        this.tabelaFuncoes = new HashMap<>();
        this.proximoEnderecoGlobal = 0; // Começa do 0
        this.proximoEnderecoLocal = 0;
        this.dentroDeFuncao = false;
    }

    public void entrarEscopoFuncao() {
        this.dentroDeFuncao = true;
        this.proximoEnderecoLocal = 0; // Reinicia contagem para a nova função
        this.tabelaLocal.clear();      // Limpa variáveis da função anterior
    }

    public void sairEscopoFuncao() {
        this.dentroDeFuncao = false;
    }

    public int adicionarVariavel(String nome) {
        if (dentroDeFuncao) {
            if (!tabelaLocal.containsKey(nome)) {
                tabelaLocal.put(nome, proximoEnderecoLocal++);
            }
            return tabelaLocal.get(nome);
        } else {
            if (!tabelaGlobal.containsKey(nome)) {
                tabelaGlobal.put(nome, proximoEnderecoGlobal++);
            }
            return tabelaGlobal.get(nome);
        }
    }

    // Retorna um array: [endereço, escopo]
    // escopo: 0 = Global, 1 = Local
    public int[] obterInfoVariavel(String nome) {
        if (dentroDeFuncao && tabelaLocal.containsKey(nome)) {
            return new int[]{tabelaLocal.get(nome), 1}; // 1 = Local
        }
        if (tabelaGlobal.containsKey(nome)) {
            return new int[]{tabelaGlobal.get(nome), 0}; // 0 = Global
        }
        return null; // Não encontrada
    }

    public void registrarFuncao(String nome, int endereco) {
        tabelaFuncoes.put(nome, endereco);
    }

    public Integer obterEnderecoFuncao(String nome) {
        return tabelaFuncoes.get(nome);
    }

    public boolean contemVariavel(String nome) {
        return (dentroDeFuncao && tabelaLocal.containsKey(nome)) || tabelaGlobal.containsKey(nome);
    }
}