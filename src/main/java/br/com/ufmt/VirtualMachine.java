package br.com.ufmt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class VirtualMachine {
    private List<String> instructions; // O código carregado
    private Stack<Double> stack;       // Pilha de operandos
    private ArrayList<Double> memory;  // Memória de variáveis (D)
    private int ip;                    // Instruction Pointer (Linha atual)
    private boolean running;

    public VirtualMachine() {
        this.stack = new Stack<>();
        this.memory = new ArrayList<>();
        this.ip = 0;
    }

    // Carrega o arquivo gerado pelo compilador
    public void loadProgram(String filePath) throws IOException {
        this.instructions = Files.readAllLines(Paths.get(filePath));
        System.out.println("--- Programa Carregado (" + instructions.size() + " instruções) ---");
    }

    public void execute() {
        if (instructions == null || instructions.isEmpty()) {
            System.out.println("Nenhum programa carregado.");
            return;
        }

        running = true;
        Scanner scanner = new Scanner(System.in);
        scanner.useLocale(Locale.US);

        ip = 0;

        while (running && ip < instructions.size()) {
            // Pega instrução atual e divide (Ex: "CRCT 10" -> ["CRCT", "10"])
            String line = instructions.get(ip).trim();
            if (line.isEmpty()) { ip++; continue; }

            String[] parts = line.split("\\s+");
            String opCode = parts[0];

            // Debug opcional (para ver o que está acontecendo)
            // System.out.println("Executando [" + ip + "]: " + line + " | Pilha: " + stack);

            try {
                switch (opCode) {
                    case "INPP": // Iniciar Programa
                        ip++;
                        break;

                    case "PARA": // Parar Programa
                        running = false;
                        break;

                    case "ALME": // Alocar Memória
                        // Ex: ALME 1 -> adiciona 1 espaço na memória (inicia com 0.0)
                        // Ignoramos o argumento de quantidade no loop simples, apenas adicionamos
                        memory.add(0.0);
                        ip++;
                        break;

                    case "CRCT": // Carrega Constante
                        double val = Double.parseDouble(parts[1]);
                        stack.push(val);
                        ip++;
                        break;

                    case "CRVL": // Carrega Valor (Variavel)
                        int addr = Integer.parseInt(parts[1]);
                        // Ajuste de índice: O exemplo INPP começa variáveis do 1?
                        // Vamos assumir índice 0-based da lista memory, mas se o compilador gera 1-based, subtraímos 1.
                        // Assumindo mapeamento direto do SymbolTable (que começou em 1):
                        stack.push(memory.get(addr - 1));
                        ip++;
                        break;

                    case "ARMZ": // Armazena (Topo da pilha -> Memória)
                        int targetAddr = Integer.parseInt(parts[1]);
                        Double valueToStore = stack.pop();
                        // Se o endereço ainda não existe (extensão dinâmica), preenchemos
                        while (memory.size() < targetAddr) {
                            memory.add(0.0);
                        }
                        memory.set(targetAddr - 1, valueToStore);
                        ip++;
                        break;

                    case "SOMA":
                        stack.push(stack.pop() + stack.pop());
                        ip++;
                        break;

                    case "SUBT":
                        double subOp2 = stack.pop();
                        double subOp1 = stack.pop();
                        stack.push(subOp1 - subOp2);
                        ip++;
                        break;

                    case "MULT":
                        stack.push(stack.pop() * stack.pop());
                        ip++;
                        break;

                    case "DIVI":
                        double divOp2 = stack.pop();
                        double divOp1 = stack.pop();
                        stack.push(divOp1 / divOp2);
                        ip++;
                        break;

                    case "IMPR": // Imprimir
                        System.out.println("SAIDA: " + stack.pop());
                        ip++;
                        break;

                    case "LEIT": // Ler Input
                        System.out.print("ENTRADA: ");
                        double inputVal = scanner.nextDouble();
                        stack.push(inputVal);
                        ip++;
                        break;

                    // === Desvios ===
                    case "DSVI": // Desvio Incondicional
                        ip = Integer.parseInt(parts[1]);
                        break;

                    case "DSVF": // Desvio Se Falso
                        int jumpTo = Integer.parseInt(parts[1]);
                        double condition = stack.pop();
                        if (condition == 0.0) { // Falso
                            ip = jumpTo;
                        } else {
                            ip++;
                        }
                        break;

                    // === Comparadores (Retornam 1.0 ou 0.0) ===
                    case "CMIG": // ==
                        stack.push(stack.pop().equals(stack.pop()) ? 1.0 : 0.0);
                        ip++;
                        break;
                    case "CMDG": // !=
                        stack.push(!stack.pop().equals(stack.pop()) ? 1.0 : 0.0);
                        ip++;
                        break;
                    case "CMAI": // >= (Cuidado: pilha inverte ordem)
                        double valB = stack.pop();
                        double valA = stack.pop();
                        stack.push(valA >= valB ? 1.0 : 0.0);
                        ip++;
                        break;
                    case "CPMI": // <=
                        double vB = stack.pop();
                        double vA = stack.pop();
                        stack.push(vA <= vB ? 1.0 : 0.0);
                        ip++;
                        break;
                    case "CMMA": // >
                        double vb2 = stack.pop();
                        double va2 = stack.pop();
                        stack.push(va2 > vb2 ? 1.0 : 0.0);
                        ip++;
                        break;
                    case "CMME": // <
                        double vb3 = stack.pop();
                        double va3 = stack.pop();
                        stack.push(va3 < vb3 ? 1.0 : 0.0);
                        ip++;
                        break;

                    // === Funções ===
                    case "PUSHER": // Empilha endereço de retorno
                        // Normalmente empilha o valor passado no argumento
                        stack.push(Double.parseDouble(parts[1]));
                        ip++;
                        break;

                    case "CHPR": // Chamada de Procedimento
                        ip = Integer.parseInt(parts[1]);
                        break;

                    case "RTPR": // Retorno de Procedimento
                        // O endereço de retorno deve estar no topo da pilha (colocado pelo PUSHER antes do CHPR)
                        // Nota: Se a função retornou valor, o endereço de retorno está ABAIXO do valor retornado.
                        // Como sua gramática não tem 'return valor', assumimos que o topo é o endereço de retorno.
                        int returnAddr = stack.pop().intValue();
                        ip = returnAddr;
                        break;

                    case "PARAM":
                        // No seu exemplo: PARAM n.
                        // O comportamento depende da implementação da pilha de ativação.
                        // Como simplificação, tratamos como CRVL (Carregar valor da var n para passar como parametro)
                        // ou ignoramos se o parser já colocou o valor na pilha.
                        // Vamos tratar como carregar variável para a pilha, caso o código objeto use isso.
                        int paramAddr = Integer.parseInt(parts[1]);
                        stack.push(memory.get(paramAddr - 1));
                        ip++;
                        break;

                    default:
                        System.err.println("Instrução desconhecida: " + opCode);
                        running = false;
                }
            } catch (Exception e) {
                System.err.println("Erro na execução da linha " + ip + ": " + line);
                e.printStackTrace();
                running = false;
            }
        }
        System.out.println("--- Execução Finalizada ---");
    }
}