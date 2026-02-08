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
        this.proximoEnderecoGlobal = 0;
        this.proximoEnderecoLocal = 0;
        this.dentroDeFuncao = false;
    }

    public void entrarEscopoFuncao() {
        this.dentroDeFuncao = true;
        this.proximoEnderecoLocal = 0;
        this.tabelaLocal.clear();
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

    public int[] obterInfoVariavel(String nome) {
        if (dentroDeFuncao && tabelaLocal.containsKey(nome)) {
            return new int[]{tabelaLocal.get(nome), 1};
        }
        if (tabelaGlobal.containsKey(nome)) {
            return new int[]{tabelaGlobal.get(nome), 0};
        }
        return null;
    }

    public boolean existeNoEscopoAtual(String nome) {
        if (dentroDeFuncao) {
            return tabelaLocal.containsKey(nome);
        } else {
            return tabelaGlobal.containsKey(nome);
        }
    }

    public void registrarFuncao(String nome, int endereco) {
        tabelaFuncoes.put(nome, endereco);
    }

    public Integer obterEnderecoFuncao(String nome) {
        return tabelaFuncoes.get(nome);
    }
}