package br.com.ufmt.vm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

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

    public void carregarPrograma(String caminhoArquivo) throws IOException {
        this.instrucoes = Files.readAllLines(Paths.get(caminhoArquivo));
        System.out.println("--- Programa Carregado (" + instrucoes.size() + " instruções) ---");
    }

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
                    case "INPP":
                        ponteiroInstrucao++;
                        break;

                    case "PARA":
                        executando = false;
                        break;

                    case "ALME":
                        memoriaDados.add(0.0);
                        ponteiroInstrucao++;
                        break;

                    case "CRCT":
                        double valorConstante = Double.parseDouble(partes[1]);
                        pilhaOperandos.push(valorConstante);
                        ponteiroInstrucao++;
                        break;

                    case "ARMZ":
                        int enderecoGlobal = Integer.parseInt(partes[1]);
                        Double valorArmz = pilhaOperandos.pop();
                        if (enderecoGlobal >= memoriaDados.size()) {
                            while (memoriaDados.size() <= enderecoGlobal) memoriaDados.add(0.0);
                        }
                        memoriaDados.set(enderecoGlobal, valorArmz);
                        ponteiroInstrucao++;
                        break;

                    case "CRVL":
                        int endGlobalCarregar = Integer.parseInt(partes[1]);
                        pilhaOperandos.push(memoriaDados.get(endGlobalCarregar));
                        ponteiroInstrucao++;
                        break;

                    case "CREL":
                        int deslocamentoLoad = Integer.parseInt(partes[1]);
                        pilhaOperandos.push(memoriaDados.get(ponteiroQuadroExecucao + deslocamentoLoad));
                        ponteiroInstrucao++;
                        break;

                    case "AMREL":
                        int deslocamentoStore = Integer.parseInt(partes[1]);
                        Double valorStore = pilhaOperandos.pop();
                        int enderecoReal = ponteiroQuadroExecucao + deslocamentoStore;

                        while (memoriaDados.size() <= enderecoReal) {
                            memoriaDados.add(0.0);
                        }
                        memoriaDados.set(enderecoReal, valorStore);
                        ponteiroInstrucao++;
                        break;

                    case "SOMA":
                        pilhaOperandos.push(pilhaOperandos.pop() + pilhaOperandos.pop());
                        ponteiroInstrucao++;
                        break;

                    case "SUBT":
                        double subOp2 = pilhaOperandos.pop();
                        double subOp1 = pilhaOperandos.pop();
                        pilhaOperandos.push(subOp1 - subOp2);
                        ponteiroInstrucao++;
                        break;

                    case "MULT":
                        pilhaOperandos.push(pilhaOperandos.pop() * pilhaOperandos.pop());
                        ponteiroInstrucao++;
                        break;

                    case "DIVI":
                        double divOp2 = pilhaOperandos.pop();
                        double divOp1 = pilhaOperandos.pop();
                        pilhaOperandos.push(divOp1 / divOp2);
                        ponteiroInstrucao++;
                        break;

                    case "IMPR":
                        System.out.println("SAIDA: " + pilhaOperandos.pop());
                        ponteiroInstrucao++;
                        break;

                    case "LEIT":
                        System.out.print("ENTRADA: ");
                        double inputVal = leitor.nextDouble();
                        pilhaOperandos.push(inputVal);
                        ponteiroInstrucao++;
                        break;

                    case "DSVI":
                        ponteiroInstrucao = Integer.parseInt(partes[1]);
                        break;

                    case "DSVF":
                        int destinoSalto = Integer.parseInt(partes[1]);
                        double condicao = pilhaOperandos.pop();
                        if (condicao == 0.0) {
                            ponteiroInstrucao = destinoSalto;
                        } else {
                            ponteiroInstrucao++;
                        }
                        break;

                    case "CMIG":
                        pilhaOperandos.push(pilhaOperandos.pop().equals(pilhaOperandos.pop()) ? 1.0 : 0.0);
                        ponteiroInstrucao++;
                        break;
                    case "CMDG":
                        pilhaOperandos.push(!pilhaOperandos.pop().equals(pilhaOperandos.pop()) ? 1.0 : 0.0);
                        ponteiroInstrucao++;
                        break;
                    case "CMAI":
                        double valB = pilhaOperandos.pop();
                        double valA = pilhaOperandos.pop();
                        pilhaOperandos.push(valA >= valB ? 1.0 : 0.0);
                        ponteiroInstrucao++;
                        break;
                    case "CPMI":
                        double vB = pilhaOperandos.pop();
                        double vA = pilhaOperandos.pop();
                        pilhaOperandos.push(vA <= vB ? 1.0 : 0.0);
                        ponteiroInstrucao++;
                        break;
                    case "CMMA":
                        double vb2 = pilhaOperandos.pop();
                        double va2 = pilhaOperandos.pop();
                        pilhaOperandos.push(va2 > vb2 ? 1.0 : 0.0);
                        ponteiroInstrucao++;
                        break;
                    case "CMME":
                        double vb3 = pilhaOperandos.pop();
                        double va3 = pilhaOperandos.pop();
                        pilhaOperandos.push(va3 < vb3 ? 1.0 : 0.0);
                        ponteiroInstrucao++;
                        break;

                    case "PUSHER":
                        pilhaOperandos.push(Double.parseDouble(partes[1]));
                        ponteiroInstrucao++;
                        break;

                    case "CHPR":
                        ponteiroInstrucao = Integer.parseInt(partes[1]);
                        break;

                    case "ENPR":
                        pilhaChamadas.push(ponteiroQuadroExecucao);
                        ponteiroQuadroExecucao = memoriaDados.size();
                        ponteiroInstrucao++;
                        break;

                    case "RTPR":
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