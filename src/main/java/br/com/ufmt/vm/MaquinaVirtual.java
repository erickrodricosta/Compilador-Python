package br.com.ufmt.vm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Máquina Virtual Hipotética (Interpretador de Stack Machine).
 * Executa o código objeto gerado pelo compilador.
 * Suporta operações aritméticas, lógicas, E/S e manipulação de pilha de execução (Stack Frames).
 */
public class MaquinaVirtual {
    private List<String> instrucoes;
    private Stack<Double> pilhaOperandos;
    private ArrayList<Double> memoriaDados;
    private int ponteiroInstrucao;
    private boolean executando;

    private int ponteiroQuadroExecucao;
    private Stack<Integer> pilhaChamadas;

    public MaquinaVirtual() {
        this.pilhaOperandos = new Stack<>();
        this.memoriaDados = new ArrayList<>();
        this.ponteiroInstrucao = 0;
        this.ponteiroQuadroExecucao = 0;
        this.pilhaChamadas = new Stack<>();
    }

    /**
     * Carrega o arquivo de código objeto para a memória de instruções.
     * @param caminhoArquivo Caminho do arquivo contendo o código objeto.
     * @throws IOException Se houver erro de leitura.
     */
    public void carregarPrograma(String caminhoArquivo) throws IOException {
        this.instrucoes = Files.readAllLines(Paths.get(caminhoArquivo));
        System.out.println("--- Programa Carregado (" + instrucoes.size() + " instruções) ---");
    }

    /**
     * Inicia o ciclo de busca e execução das instruções (Fetch-Execute Cycle).
     */
    public void executar() {
        if (instrucoes == null || instrucoes.isEmpty()) {
            System.out.println("Nenhum programa carregado.");
            return;
        }

        executando = true;
        Scanner leitor = new Scanner(System.in);
        leitor.useLocale(Locale.US);

        ponteiroInstrucao = 0;

        while (executando && ponteiroInstrucao < instrucoes.size()) {
            String linha = instrucoes.get(ponteiroInstrucao).trim();
            if (linha.isEmpty()) { ponteiroInstrucao++; continue; }

            String[] partes = linha.split("\\s+");
            String operacao = partes[0];

            try {
                switch (operacao) {
                    case "INPP": // Iniciar Programa: Prepara a VM para execução (no-op neste interpretador)
                        ponteiroInstrucao++;
                        break;

                    case "PARA": // Parar Programa: Finaliza o loop de execução
                        executando = false;
                        break;

                    case "ALME": // Alocar Memória: Reserva espaço na memória de dados (inicializa com 0.0)
                        memoriaDados.add(0.0);
                        ponteiroInstrucao++;
                        break;

                    case "CRCT": // Carrega Constante: Empilha um valor literal imediato na pilha de operandos
                        double valorConstante = Double.parseDouble(partes[1]);
                        pilhaOperandos.push(valorConstante);
                        ponteiroInstrucao++;
                        break;

                    case "ARMZ": // Armazena (Global): Desempilha um valor e salva no endereço absoluto informado
                        int enderecoGlobal = Integer.parseInt(partes[1]);
                        Double valorArmz = pilhaOperandos.pop();
                        if (enderecoGlobal >= memoriaDados.size()) {
                            while (memoriaDados.size() <= enderecoGlobal) memoriaDados.add(0.0);
                        }
                        memoriaDados.set(enderecoGlobal, valorArmz);
                        ponteiroInstrucao++;
                        break;

                    case "CRVL": // Carrega Valor (Global): Busca valor do endereço absoluto e empilha
                        int endGlobalCarregar = Integer.parseInt(partes[1]);
                        pilhaOperandos.push(memoriaDados.get(endGlobalCarregar));
                        ponteiroInstrucao++;
                        break;

                    case "CREL": // Carrega Relativo (Local): Busca valor relativo ao Frame Pointer (FP) e empilha
                        int deslocamentoLoad = Integer.parseInt(partes[1]);
                        pilhaOperandos.push(memoriaDados.get(ponteiroQuadroExecucao + deslocamentoLoad));
                        ponteiroInstrucao++;
                        break;

                    case "AMREL": // Armazena Relativo (Local): Desempilha e salva relativo ao Frame Pointer (FP)
                        int deslocamentoStore = Integer.parseInt(partes[1]);
                        Double valorStore = pilhaOperandos.pop();
                        int enderecoReal = ponteiroQuadroExecucao + deslocamentoStore;

                        while (memoriaDados.size() <= enderecoReal) {
                            memoriaDados.add(0.0);
                        }
                        memoriaDados.set(enderecoReal, valorStore);
                        ponteiroInstrucao++;
                        break;

                    case "SOMA": // Soma: Desempilha A e B, empilha (A + B)
                        pilhaOperandos.push(pilhaOperandos.pop() + pilhaOperandos.pop());
                        ponteiroInstrucao++;
                        break;

                    case "SUBT": // Subtração: Desempilha B (topo) e A, empilha (A - B)
                        double subOp2 = pilhaOperandos.pop();
                        double subOp1 = pilhaOperandos.pop();
                        pilhaOperandos.push(subOp1 - subOp2);
                        ponteiroInstrucao++;
                        break;

                    case "MULT": // Multiplicação: Desempilha A e B, empilha (A * B)
                        pilhaOperandos.push(pilhaOperandos.pop() * pilhaOperandos.pop());
                        ponteiroInstrucao++;
                        break;

                    case "DIVI": // Divisão: Desempilha B (topo) e A, empilha (A / B)
                        double divOp2 = pilhaOperandos.pop();
                        double divOp1 = pilhaOperandos.pop();
                        pilhaOperandos.push(divOp1 / divOp2);
                        ponteiroInstrucao++;
                        break;

                    case "IMPR": // Imprimir: Desempilha um valor e exibe no console
                        System.out.println("SAIDA: " + pilhaOperandos.pop());
                        ponteiroInstrucao++;
                        break;

                    case "LEIT": // Leitura: Lê valor do console e empilha
                        System.out.print("ENTRADA: ");
                        double inputVal = leitor.nextDouble();
                        pilhaOperandos.push(inputVal);
                        ponteiroInstrucao++;
                        break;

                    case "DSVI": // Desvio Incondicional: Altera o IP para o endereço especificado
                        ponteiroInstrucao = Integer.parseInt(partes[1]);
                        break;

                    case "DSVF": // Desvio Se Falso: Desempilha valor; se for 0.0 (falso), salta para o endereço
                        int destinoSalto = Integer.parseInt(partes[1]);
                        double condicao = pilhaOperandos.pop();
                        if (condicao == 0.0) {
                            ponteiroInstrucao = destinoSalto;
                        } else {
                            ponteiroInstrucao++;
                        }
                        break;

                    case "CMIG": // Comparar Igual (==)
                        pilhaOperandos.push(pilhaOperandos.pop().equals(pilhaOperandos.pop()) ? 1.0 : 0.0);
                        ponteiroInstrucao++;
                        break;
                    case "CMDG": // Comparar Desigual (!=)
                        pilhaOperandos.push(!pilhaOperandos.pop().equals(pilhaOperandos.pop()) ? 1.0 : 0.0);
                        ponteiroInstrucao++;
                        break;
                    case "CMAI": // Comparar Maior ou Igual (>=)
                        double valB = pilhaOperandos.pop();
                        double valA = pilhaOperandos.pop();
                        pilhaOperandos.push(valA >= valB ? 1.0 : 0.0);
                        ponteiroInstrucao++;
                        break;
                    case "CPMI": // Comparar Menor ou Igual (<=)
                        double vB = pilhaOperandos.pop();
                        double vA = pilhaOperandos.pop();
                        pilhaOperandos.push(vA <= vB ? 1.0 : 0.0);
                        ponteiroInstrucao++;
                        break;
                    case "CMMA": // Comparar Maior (>)
                        double vb2 = pilhaOperandos.pop();
                        double va2 = pilhaOperandos.pop();
                        pilhaOperandos.push(va2 > vb2 ? 1.0 : 0.0);
                        ponteiroInstrucao++;
                        break;
                    case "CMME": // Comparar Menor (<)
                        double vb3 = pilhaOperandos.pop();
                        double va3 = pilhaOperandos.pop();
                        pilhaOperandos.push(va3 < vb3 ? 1.0 : 0.0);
                        ponteiroInstrucao++;
                        break;

                    case "PUSHER": // Empilha Endereço de Retorno: Prepara a pilha para saber onde voltar após CHPR
                        pilhaOperandos.push(Double.parseDouble(partes[1]));
                        ponteiroInstrucao++;
                        break;

                    case "CHPR": // Chamada de Procedimento: Salta para o endereço da função
                        ponteiroInstrucao = Integer.parseInt(partes[1]);
                        break;

                    case "ENPR": // Entrar no Procedimento: Salva o contexto atual (FP) na pilha de chamadas e define novo FP
                        pilhaChamadas.push(ponteiroQuadroExecucao);
                        ponteiroQuadroExecucao = memoriaDados.size();
                        ponteiroInstrucao++;
                        break;

                    case "RTPR": // Retornar de Procedimento: Restaura contexto anterior (FP), limpa memória local e salta de volta
                        while (memoriaDados.size() > ponteiroQuadroExecucao) {
                            memoriaDados.remove(memoriaDados.size() - 1);
                        }
                        ponteiroQuadroExecucao = pilhaChamadas.pop();
                        int enderecoRetorno = pilhaOperandos.pop().intValue();
                        ponteiroInstrucao = enderecoRetorno;
                        break;

                    default:
                        System.err.println("Instrução desconhecida: " + operacao);
                        executando = false;
                }
            } catch (Exception e) {
                System.err.println("Erro na execução da linha " + ponteiroInstrucao + ": " + linha);
                e.printStackTrace();
                executando = false;
            }
        }
        System.out.println("--- Execução Finalizada ---");
    }
}