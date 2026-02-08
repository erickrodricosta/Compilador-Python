package br.com.ufmt.semantico;

import java.util.HashMap;
import java.util.Map;

/**
 * Gerencia os identificadores (variáveis e funções) durante a compilação.
 * Controla escopos globais e locais, além de endereçamento de memória.
 */
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

    /**
     * Inicia um novo escopo de função, resetando a tabela local e contadores.
     */
    public void entrarEscopoFuncao() {
        this.dentroDeFuncao = true;
        this.proximoEnderecoLocal = 0;
        this.tabelaLocal.clear();
    }

    /**
     * Finaliza o escopo de função atual e retorna ao escopo global.
     */
    public void sairEscopoFuncao() {
        this.dentroDeFuncao = false;
    }

    /**
     * Adiciona uma variável à tabela de símbolos apropriada (Local ou Global).
     * Se a variável já existir no escopo atual, retorna o endereço existente.
     * @param nome O nome da variável.
     * @return O endereço de memória atribuído à variável.
     */
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

    /**
     * Busca informações de uma variável nos escopos disponíveis (Local depois Global).
     * @param nome O nome da variável.
     * @return Um array onde [0] é o endereço e [1] é o escopo (1=Local, 0=Global), ou null se não encontrada.
     */
    public int[] obterInfoVariavel(String nome) {
        if (dentroDeFuncao && tabelaLocal.containsKey(nome)) {
            return new int[]{tabelaLocal.get(nome), 1};
        }
        if (tabelaGlobal.containsKey(nome)) {
            return new int[]{tabelaGlobal.get(nome), 0};
        }
        return null;
    }

    /**
     * Verifica se uma variável está declarada estritamente no escopo atual.
     * Utilizado para resolver Shadowing (variáveis locais com mesmo nome de globais).
     * @param nome O nome da variável.
     * @return true se existir no escopo atual, false caso contrário.
     */
    public boolean existeNoEscopoAtual(String nome) {
        if (dentroDeFuncao) {
            return tabelaLocal.containsKey(nome);
        } else {
            return tabelaGlobal.containsKey(nome);
        }
    }

    /**
     * Registra uma função e seu endereço de início no código objeto.
     * @param nome O nome da função.
     * @param endereco O endereço da instrução inicial da função.
     */
    public void registrarFuncao(String nome, int endereco) {
        tabelaFuncoes.put(nome, endereco);
    }

    /**
     * Recupera o endereço de uma função registrada.
     * @param nome O nome da função.
     * @return O endereço de memória da função ou null se não existir.
     */
    public Integer obterEnderecoFuncao(String nome) {
        return tabelaFuncoes.get(nome);
    }
}