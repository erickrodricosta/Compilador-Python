package br.com.ufmt;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    // Mapa: Nome da variável -> Endereço de Memória (ex: "a" -> 1)
    private Map<String, Integer> variableTable;

    // Mapa: Nome da função -> Endereço da Instrução de inicio (ex: "um" -> 45)
    private Map<String, Integer> functionTable;

    // Contador global para o próximo endereço de memória livre
    private int nextAddress;

    public SymbolTable() {
        this.variableTable = new HashMap<>();
        this.functionTable = new HashMap<>();
        this.nextAddress = 1; // Começamos do endereço 1 (conforme exemplo INPP)
    }

    // Adiciona uma variável e retorna o endereço alocado
    // Se já existe, retorna o endereço existente (ou lança erro dependendo da rigidez)
    public int addVariable(String name) {
        if (!variableTable.containsKey(name)) {
            variableTable.put(name, nextAddress++);
        }
        return variableTable.get(name);
    }

    // Retorna o endereço de uma variável
    public Integer getVariableAddress(String name) {
        return variableTable.get(name);
    }

    // Verifica se variável existe
    public boolean containsVariable(String name) {
        return variableTable.containsKey(name);
    }

    // Registra onde uma função começa
    public void addFunction(String name, int instructionIndex) {
        functionTable.put(name, instructionIndex);
    }

    // Retorna o endereço de início de uma função
    public Integer getFunctionAddress(String name) {
        return functionTable.get(name);
    }
}