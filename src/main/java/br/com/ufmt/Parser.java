package br.com.ufmt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Parser {
    private Lexer lexer;
    private SymbolTable symbolTable;
    private CodeGenerator codeGen;
    private Token currentToken;

    public Parser(Lexer lexer, SymbolTable symbolTable, CodeGenerator codeGen) {
        this.lexer = lexer;
        this.symbolTable = symbolTable;
        this.codeGen = codeGen;
        this.currentToken = lexer.nextToken(); // Lê o primeiro token
    }

    // === Métodos Auxiliares ===

    private void eat(TokenType type) {
        if (currentToken.type == type) {
            currentToken = lexer.nextToken();
        } else {
            error("Esperado " + type + " mas encontrado " + currentToken);
        }
    }

    private void error(String msg) {
        throw new RuntimeException("Erro de Sintaxe na linha " + currentToken.line + ": " + msg);
    }

    private boolean isComando(TokenType type) {
        return type == TokenType.PRINT ||
                type == TokenType.IF ||
                type == TokenType.WHILE ||
                type == TokenType.IDENT;
    }

    // === Regras Gramaticais: Estrutura ===

    public void parsePrograma() {
        codeGen.emit("INPP");
        parseCorpo();
        codeGen.emit("PARA");
    }

    private void parseCorpo() {
        parseDc();
        parseComandos(false);
    }

    // MÉTODO CORRIGIDO
    private void parseDc() {
        while (currentToken.type == TokenType.IDENT || currentToken.type == TokenType.DEF) {
            if (currentToken.type == TokenType.DEF) {
                parseDcF();
            }
            else if (currentToken.type == TokenType.IDENT) {
                // Se o identificador é uma função conhecida, paramos as declarações.
                // Isso permite que o parseComandos assuma o controle para fazer a chamada.
                if (symbolTable.getFunctionAddress(currentToken.lexeme) != null) {
                    break;
                }
                parseDcV();
            }
        }
    }

    private void parseDcV() {
        String varName = currentToken.lexeme;
        eat(TokenType.IDENT);

        int addr = symbolTable.addVariable(varName);
        codeGen.emit("ALME", 1);

        eat(TokenType.ASSIGN);
        parseExpressao();
        codeGen.emit("ARMZ", addr);
    }

    // === Regras Gramaticais: Declaração de Funções ===

    // <dc_f> -> def ident <parametros> : <corpo_f>
    private void parseDcF() {
        eat(TokenType.DEF);
        String funcName = currentToken.lexeme;
        eat(TokenType.IDENT);

        int jumpInstructionIndex = codeGen.emit("DSVI", 0);
        symbolTable.addFunction(funcName, codeGen.getCurrentAddress());

        eat(TokenType.LPAREN);

        // CORREÇÃO: Capturar endereços dos parâmetros
        List<Integer> paramAddresses = new ArrayList<>();
        parseListaPar(paramAddresses);

        eat(TokenType.RPAREN);
        eat(TokenType.COLON);

        // CORREÇÃO: Gerar ARMZ reverso para tirar valores da pilha
        // A pilha tem [..., arg1, arg2]. O topo é arg2.
        // Precisamos fazer ARMZ arg2, depois ARMZ arg1.
        Collections.reverse(paramAddresses);
        for (Integer addr : paramAddresses) {
            codeGen.emit("ARMZ", addr);
        }

        parseBloco();

        codeGen.emit("RTPR");
        codeGen.patch(jumpInstructionIndex, codeGen.getCurrentAddress());
    }

    // Agora recebe a lista para preencher
    private void parseListaPar(List<Integer> paramAddresses) {
        if (currentToken.type == TokenType.IDENT) {
            String varName = currentToken.lexeme;
            eat(TokenType.IDENT);

            // Adiciona variável e guarda o endereço na lista
            int addr = symbolTable.addVariable(varName);
            paramAddresses.add(addr);

            parseMaisPar(paramAddresses);
        }
    }

    // Agora recebe a lista para repassar
    private void parseMaisPar(List<Integer> paramAddresses) {
        if (currentToken.type == TokenType.COMMA) {
            eat(TokenType.COMMA);
            parseListaPar(paramAddresses);
        }
    }

    // === Regras Gramaticais: Comandos e Blocos ===

    private void parseBloco() {
        eat(TokenType.TAB);
        parseComandos(true);
    }

    // Substitua este método na classe Parser.java
    private void parseComandos(boolean insideBlock) {
        // NOVO LOOP: Consome linhas vazias ou de comentários (Tokens TAB soltos)
        // antes de tentar ler o primeiro comando.
        while (currentToken.type == TokenType.TAB) {
            eat(TokenType.TAB);

            // Se após consumir o TAB encontrarmos Fim de Arquivo ou Else,
            // significa que o bloco era só comentários/vazio. Retornamos.
            if (currentToken.type == TokenType.EOF || currentToken.type == TokenType.ELSE) {
                return;
            }
        }

        // Agora que limpamos os TABs extras, esperamos um comando real.
        if (isComando(currentToken.type)) {
            parseComando();
            parseMaisComandos(insideBlock);
        }
        // Se não for comando (ex: fechamento de bloco sem comandos), não fazemos nada.
    }

    private void parseMaisComandos(boolean insideBlock) {
        // 1. Critério de Parada Universal
        if (currentToken.type == TokenType.EOF || currentToken.type == TokenType.ELSE) {
            return;
        }

        // 2. Lógica DENTRO de um bloco
        if (insideBlock) {
            // Se houver TAB, significa que pode vir um comando
            if (currentToken.type == TokenType.TAB) {
                eat(TokenType.TAB);

                // Se depois do TAB vier ELSE ou EOF, acabou.
                if (currentToken.type == TokenType.ELSE || currentToken.type == TokenType.EOF) {
                    return;
                }

                // CASO NORMAL: Tem TAB e depois um Comando
                if (isComando(currentToken.type)) {
                    parseComandos(true);
                }
                // CORREÇÃO CRÍTICA:
                // Se tem TAB mas NÃO é comando (ex: encontrou outro TAB de uma linha vazia,
                // ou o comentário consumido deixou um TAB órfão), ignoramos e tentamos de novo.
                else if (currentToken.type == TokenType.TAB) {
                    // Chamamos recursivamente para processar o próximo TAB sem sair do bloco
                    parseMaisComandos(true);
                }
                // Se tem TAB mas vem algo que não é comando nem TAB (ex: DEF), é fim de bloco.
            }
            // Se NÃO houver TAB (Dedent), o bloco acabou.
            else {
                return;
            }
        }
        // 3. Lógica NO MAIN
        else {
            if (isComando(currentToken.type)) {
                parseComandos(false);
            }
            else if (currentToken.type == TokenType.TAB) {
                eat(TokenType.TAB);
                if (isComando(currentToken.type)) {
                    parseComandos(false);
                } else {
                    // Se achou TAB solto no main sem comando, ignora e continua
                    parseMaisComandos(false);
                }
            }
        }
    }

    private void parseComando() {
        if (currentToken.type == TokenType.PRINT) {
            eat(TokenType.PRINT);
            eat(TokenType.LPAREN);

            // Aceita expressões dentro do print (ex: print(a), print(10))
            parseExpressao();

            eat(TokenType.RPAREN);
            codeGen.emit("IMPR");
        }
        else if (currentToken.type == TokenType.IF) {
            parseIf();
        }
        else if (currentToken.type == TokenType.WHILE) {
            parseWhile();
        }
        else if (currentToken.type == TokenType.IDENT) {
            String name = currentToken.lexeme;
            eat(TokenType.IDENT);
            parseRestoIdent(name);
        }
        else {
            error("Comando desconhecido: " + currentToken);
        }
    }

    private void parseRestoIdent(String name) {
        if (currentToken.type == TokenType.ASSIGN) {
            eat(TokenType.ASSIGN);

            if (!symbolTable.containsVariable(name)) {
                symbolTable.addVariable(name);
                codeGen.emit("ALME", 1);
            }

            parseExpressao();
            int addr = symbolTable.getVariableAddress(name);
            codeGen.emit("ARMZ", addr);
        }
        else if (currentToken.type == TokenType.LPAREN) {
            if (symbolTable.getFunctionAddress(name) == null) {
                error("Funcao nao declarada: " + name);
            }

            codeGen.emit("PUSHER", 0);
            int paramIndex = codeGen.getCurrentAddress() - 1;

            eat(TokenType.LPAREN);
            parseArgumentos();
            eat(TokenType.RPAREN);

            int funcAddr = symbolTable.getFunctionAddress(name);
            codeGen.emit("CHPR", funcAddr);
            codeGen.patch(paramIndex, codeGen.getCurrentAddress());
        }
    }

    // === Expressões ===

    private void parseCondicao() {
        parseExpressao();
        TokenType op = currentToken.type;
        eat(op);
        parseExpressao();

        switch (op) {
            case EQUALS:      codeGen.emit("CMIG"); break;
            case DIFF:        codeGen.emit("CMDG"); break;
            case GREATER_EQ:  codeGen.emit("CMAI"); break;
            case LESS_EQ:     codeGen.emit("CPMI"); break;
            case GREATER:     codeGen.emit("CMMA"); break;
            case LESS:        codeGen.emit("CMME"); break;
            default: error("Operador relacional inválido");
        }
    }

    private void parseIf() {
        eat(TokenType.IF);
        parseCondicao();
        eat(TokenType.COLON);

        int jumpAddr = codeGen.emit("DSVF", 0);
        parseBloco();

        if (currentToken.type == TokenType.ELSE) {
            int jumpElseAddr = codeGen.emit("DSVI", 0);
            codeGen.patch(jumpAddr, codeGen.getCurrentAddress());

            eat(TokenType.ELSE);
            eat(TokenType.COLON);
            parseBloco();

            codeGen.patch(jumpElseAddr, codeGen.getCurrentAddress());
        } else {
            codeGen.patch(jumpAddr, codeGen.getCurrentAddress());
        }
    }

    private void parseWhile() {
        eat(TokenType.WHILE);
        int startAddr = codeGen.getCurrentAddress();
        parseCondicao();
        eat(TokenType.COLON);

        int jumpEndAddr = codeGen.emit("DSVF", 0);
        parseBloco();
        codeGen.emit("DSVI", startAddr);
        codeGen.patch(jumpEndAddr, codeGen.getCurrentAddress());
    }

    private void parseArgumentos() {
        if (currentToken.type != TokenType.RPAREN) {
            parseExpressao();
            parseMaisArgumentos();
        }
    }

    private void parseMaisArgumentos() {
        if (currentToken.type == TokenType.COMMA) {
            eat(TokenType.COMMA);
            parseArgumentos();
        }
    }

    private void parseExpressao() {
        if (currentToken.type == TokenType.INPUT) {
            eat(TokenType.INPUT);
            eat(TokenType.LPAREN);
            eat(TokenType.RPAREN);
            codeGen.emit("LEIT");
        } else {
            parseTermo();
            parseOutrosTermos();
        }
    }

    private void parseOutrosTermos() {
        while (currentToken.type == TokenType.PLUS || currentToken.type == TokenType.MINUS) {
            Token op = currentToken;
            eat(op.type);
            parseTermo();

            if (op.type == TokenType.PLUS) {
                codeGen.emit("SOMA");
            } else {
                codeGen.emit("SUBT");
            }
        }
    }

    private void parseTermo() {
        parseFator();
        parseMaisFatores();
    }

    private void parseMaisFatores() {
        while (currentToken.type == TokenType.MULT || currentToken.type == TokenType.DIV) {
            Token op = currentToken;
            eat(op.type);

            parseFator();

            if (op.type == TokenType.MULT) {
                codeGen.emit("MULT");
            } else {
                codeGen.emit("DIVI");
            }
        }
    }

    private void parseFator() {
        if (currentToken.type == TokenType.IDENT) {
            String name = currentToken.lexeme;
            eat(TokenType.IDENT);
            if (!symbolTable.containsVariable(name)) {
                error("Variavel nao declarada: " + name);
            }
            int addr = symbolTable.getVariableAddress(name);
            codeGen.emit("CRVL", addr);
        }
        else if (currentToken.type == TokenType.NUMBER) {
            double val = Double.parseDouble(currentToken.lexeme);
            eat(TokenType.NUMBER);
            codeGen.emit("CRCT", val);
        }
        else if (currentToken.type == TokenType.LPAREN) {
            eat(TokenType.LPAREN);
            parseExpressao();
            eat(TokenType.RPAREN);
        }
        else {
            error("Fator inesperado: " + currentToken);
        }
    }
}