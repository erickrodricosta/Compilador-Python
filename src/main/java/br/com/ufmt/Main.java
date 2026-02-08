package br.com.ufmt;

import br.com.ufmt.gerador.GeradorCodigo;
import br.com.ufmt.lexico.AnalisadorLexico;
import br.com.ufmt.semantico.TabelaSimbolos;
import br.com.ufmt.sintatico.AnalisadorSintatico;
import br.com.ufmt.vm.MaquinaVirtual;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        try {
            // --- ETAPA 1: COMPILAÇÃO ---
            System.out.println("=== INICIANDO COMPILAÇÃO ===");

            // 1. Leia o código fonte
            String codigoFonte = new String(Files.readAllBytes(Paths.get("codigo.txt")));

            // 2. Inicialize as partes do compilador (Classes Renomeadas)
            AnalisadorLexico lexico = new AnalisadorLexico(codigoFonte);
            TabelaSimbolos tabela = new TabelaSimbolos();
            GeradorCodigo gerador = new GeradorCodigo();

            // 3. Inicialize o Sintático e execute
            AnalisadorSintatico sintatico = new AnalisadorSintatico(lexico, tabela, gerador);
            sintatico.analisarPrograma();

            // 4. Salve o Código Objeto
            String codigoObjeto = gerador.toString();
            Files.write(Paths.get("codigo_objeto.txt"), codigoObjeto.getBytes());

            System.out.println("Compilação com sucesso! Arquivo 'codigo_objeto.txt' gerado.");
            System.out.println("---------------------------------------------------------");

            // --- ETAPA 2: EXECUÇÃO (VM) ---
            System.out.println("=== INICIANDO MÁQUINA VIRTUAL ===");

            MaquinaVirtual vm = new MaquinaVirtual();
            vm.carregarPrograma("codigo_objeto.txt");
            vm.executar();

        } catch (IOException e) {
            System.err.println("Erro ao ler/escrever arquivos: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("Erro de Compilação/Execução: " + e.getMessage());
            e.printStackTrace();
        }
    }
}