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
        this.currentToken = lexer.nextToken();
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

    private void parseDc() {
        while (currentToken.type == TokenType.IDENT || currentToken.type == TokenType.DEF) {
            if (currentToken.type == TokenType.DEF) {
                parseDcF();
            }
            else if (currentToken.type == TokenType.IDENT) {
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

        int[] info = symbolTable.getVariableInfo(varName);
        if (info[1] == 1) {
            codeGen.emit("AMREL", info[0]);
        } else {
            codeGen.emit("ARMZ", info[0]);
        }
    }

    // === Regras Gramaticais: Funções ===

    private void parseDcF() {
        eat(TokenType.DEF);
        String funcName = currentToken.lexeme;
        eat(TokenType.IDENT);

        int jumpIndex = codeGen.emit("DSVI", 0);
        symbolTable.addFunction(funcName, codeGen.getCurrentAddress());

        symbolTable.enterFunction();
        codeGen.emit("ENPR");

        eat(TokenType.LPAREN);
        List<Integer> params = new ArrayList<>();
        parseListaPar(params);
        eat(TokenType.RPAREN);
        eat(TokenType.COLON);

        Collections.reverse(params);
        for (Integer addr : params) {
            codeGen.emit("AMREL", addr);
        }

        parseBloco();

        symbolTable.exitFunction();
        codeGen.emit("RTPR");
        codeGen.patch(jumpIndex, codeGen.getCurrentAddress());
    }

    private void parseListaPar(List<Integer> paramAddresses) {
        if (currentToken.type == TokenType.IDENT) {
            String varName = currentToken.lexeme;
            eat(TokenType.IDENT);
            int addr = symbolTable.addVariable(varName);
            paramAddresses.add(addr);
            parseMaisPar(paramAddresses);
        }
    }

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

    private void parseComandos(boolean insideBlock) {
        while (currentToken.type == TokenType.TAB) {
            eat(TokenType.TAB);
            if (currentToken.type == TokenType.EOF || currentToken.type == TokenType.ELSE) {
                return;
            }
        }

        if (isComando(currentToken.type)) {
            parseComando();
            parseMaisComandos(insideBlock);
        }
    }

    private void parseMaisComandos(boolean insideBlock) {
        if (currentToken.type == TokenType.EOF || currentToken.type == TokenType.ELSE) {
            return;
        }

        if (insideBlock) {
            if (currentToken.type == TokenType.TAB) {
                eat(TokenType.TAB);
                if (currentToken.type == TokenType.ELSE || currentToken.type == TokenType.EOF) {
                    return;
                }
                if (isComando(currentToken.type)) {
                    parseComandos(true);
                }
                else if (currentToken.type == TokenType.TAB) {
                    parseMaisComandos(true);
                }
            } else {
                return;
            }
        } else {
            if (isComando(currentToken.type)) {
                parseComandos(false);
            }
            else if (currentToken.type == TokenType.TAB) {
                eat(TokenType.TAB);
                if (isComando(currentToken.type)) {
                    parseComandos(false);
                } else {
                    parseMaisComandos(false);
                }
            }
        }
    }

    // === MÉTODO CORRIGIDO PARA 100% LALG ===
    private void parseComando() {
        if (currentToken.type == TokenType.PRINT) {
            eat(TokenType.PRINT);
            eat(TokenType.LPAREN);
            String varName = currentToken.lexeme;
            eat(TokenType.IDENT);

            // Gera carregamento da variável
            int[] info = symbolTable.getVariableInfo(varName);
            if (info == null) error("Variavel nao declarada: " + varName);

            if (info[1] == 1) { // Local
                codeGen.emit("CREL", info[0]);
            } else { // Global
                codeGen.emit("CRVL", info[0]);
            }
            // -------------------------------------

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

            int[] info = symbolTable.getVariableInfo(name);
            if (info[1] == 1) {
                codeGen.emit("AMREL", info[0]);
            } else {
                codeGen.emit("ARMZ", info[0]);
            }
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

            int[] info = symbolTable.getVariableInfo(name);
            if (info == null) error("Variavel nao declarada: " + name);

            if (info[1] == 1) {
                codeGen.emit("CREL", info[0]);
            } else {
                codeGen.emit("CRVL", info[0]);
            }
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