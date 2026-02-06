package br.com.ufmt;

import java.util.ArrayList;
import java.util.List;

public class CodeGenerator {
    // Lista que armazena as instruções (ex: "INPP", "CRVL 1")
    private List<String> instructions;

    public CodeGenerator() {
        this.instructions = new ArrayList<>();
    }

    // Emite uma instrução sem argumentos (ex: SOMA, INPP)
    public int emit(String instruction) {
        instructions.add(instruction);
        return instructions.size() - 1; // Retorna o índice da instrução gerada
    }

    // Emite uma instrução com argumento inteiro (ex: CRVL 10, DSVF 50)
    public int emit(String instruction, int arg) {
        instructions.add(instruction + " " + arg);
        return instructions.size() - 1;
    }

    // Emite uma instrução com argumento double (ex: CRCT 3.4)
    public int emit(String instruction, double arg) {
        instructions.add(instruction + " " + arg);
        return instructions.size() - 1;
    }

    // Retorna o contador de programa atual (próxima linha livre)
    public int getCurrentAddress() {
        return instructions.size();
    }

    // Backpatching: Corrige o argumento de uma instrução anterior
    // Usado para IF e WHILE
    public void patch(int instructionAddress, int newArgument) {
        String original = instructions.get(instructionAddress);
        // Pega o nome da instrução (ex: "DSVF 0" -> "DSVF")
        String opCode = original.split(" ")[0];
        // Atualiza com o novo argumento
        instructions.set(instructionAddress, opCode + " " + newArgument);
    }

    // Retorna todo o código como uma única string (para salvar no arquivo)
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String s : instructions) {
            sb.append(s).append("\n");
        }
        return sb.toString();
    }
}