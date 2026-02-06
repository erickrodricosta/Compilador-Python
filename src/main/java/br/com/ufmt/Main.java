package br.com.ufmt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        try {
            // --- ETAPA 1: COMPILAÇÃO ---
            System.out.println("=== INICIANDO COMPILAÇÃO ===");

            // 1. Leia o código fonte
            String sourceCode = new String(Files.readAllBytes(Paths.get("codigo.txt")));

            // 2. Inicialize as partes do compilador
            Lexer lexer = new Lexer(sourceCode);
            SymbolTable symbolTable = new SymbolTable();
            CodeGenerator codeGen = new CodeGenerator();

            Parser parser = new Parser(lexer, symbolTable, codeGen);

            // 3. Execute o parsing
            parser.parsePrograma();

            // 4. Salve o Código Objeto
            String objectCode = codeGen.toString();
            Files.write(Paths.get("codigo_objeto.txt"), objectCode.getBytes());

            System.out.println("Compilação com sucesso! Arquivo 'codigo_objeto.txt' gerado.");
            System.out.println("---------------------------------------------------------");

            // --- ETAPA 2: EXECUÇÃO (VM) ---
            System.out.println("=== INICIANDO MÁQUINA VIRTUAL ===");

            VirtualMachine vm = new VirtualMachine();
            vm.loadProgram("codigo_objeto.txt");
            vm.execute();

        } catch (IOException e) {
            System.err.println("Erro ao ler/escrever arquivos: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("Erro de Compilação/Execução: " + e.getMessage());
            e.printStackTrace(); // Descomente para debug detalhado
        }
    }
}