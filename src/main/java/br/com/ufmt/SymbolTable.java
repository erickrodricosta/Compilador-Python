package br.com.ufmt;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private Map<String, Integer> globalTable;
    private Map<String, Integer> localTable;
    private Map<String, Integer> functionTable;

    private int nextGlobalAddr;
    private int nextLocalAddr;
    private boolean insideFunction;

    public SymbolTable() {
        this.globalTable = new HashMap<>();
        this.localTable = new HashMap<>();
        this.functionTable = new HashMap<>();
        this.nextGlobalAddr = 0; // Começa do 0
        this.nextLocalAddr = 0;
        this.insideFunction = false;
    }

    public void enterFunction() {
        this.insideFunction = true;
        this.nextLocalAddr = 0; // Reinicia contagem para a nova função (0, 1, 2...)
        this.localTable.clear(); // Limpa variaveis da função anterior
    }

    public void exitFunction() {
        this.insideFunction = false;
    }

    public int addVariable(String name) {
        if (insideFunction) {
            if (!localTable.containsKey(name)) {
                localTable.put(name, nextLocalAddr++);
            }
            return localTable.get(name);
        } else {
            if (!globalTable.containsKey(name)) {
                globalTable.put(name, nextGlobalAddr++);
            }
            return globalTable.get(name);
        }
    }

    // Retorna um objeto ou array simples para dizer TIPO e ENDEREÇO
    // int[0] = endereço, int[1] = 0 (Global) ou 1 (Local)
    public int[] getVariableInfo(String name) {
        if (insideFunction && localTable.containsKey(name)) {
            return new int[]{localTable.get(name), 1}; // 1 = Local
        }
        if (globalTable.containsKey(name)) {
            return new int[]{globalTable.get(name), 0}; // 0 = Global
        }
        return null; // Não achou
    }

    // Métodos de Função mantêm iguais
    public void addFunction(String name, int addr) { functionTable.put(name, addr); }
    public Integer getFunctionAddress(String name) { return functionTable.get(name); }
    public boolean containsVariable(String name) {
        return (insideFunction && localTable.containsKey(name)) || globalTable.containsKey(name);
    }
}