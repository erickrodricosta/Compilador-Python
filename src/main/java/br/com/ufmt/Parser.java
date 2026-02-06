package br.com.ufmt;

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

    // Método auxiliar para verificar se o token atual começa um comando
    private boolean isComando(TokenType type) {
        return type == TokenType.PRINT ||
                type == TokenType.IF ||
                type == TokenType.WHILE ||
                type == TokenType.IDENT;
    }

    // === Regras Gramaticais: Estrutura ===

    // <programa> -> <corpo>
    public void parsePrograma() {
        codeGen.emit("INPP"); // Inicia programa
        parseCorpo();
        codeGen.emit("PARA"); // Finaliza programa
    }

    // <corpo> -> <dc> <comandos>
    private void parseCorpo() {
        parseDc();
        // Contexto FALSE: Estamos no corpo principal, não exige indentação para continuar
        parseComandos(false);
    }

    // <dc> -> <dc_v> <mais_dc> | <dc_f> <mais_dc_f> | λ
    private void parseDc() {
        while (currentToken.type == TokenType.IDENT || currentToken.type == TokenType.DEF) {
            if (currentToken.type == TokenType.IDENT) {
                parseDcV();
            } else if (currentToken.type == TokenType.DEF) {
                parseDcF();
            }
        }
    }

    // <dc_v> -> <ident> = <expressao>
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
        parseListaPar();
        eat(TokenType.RPAREN);
        eat(TokenType.COLON);

        parseBloco();

        codeGen.emit("RTPR");
        codeGen.patch(jumpInstructionIndex, codeGen.getCurrentAddress());
    }

    // <lista_par> -> ident <mais_par>
    private void parseListaPar() {
        if (currentToken.type == TokenType.IDENT) {
            String varName = currentToken.lexeme;
            eat(TokenType.IDENT);
            symbolTable.addVariable(varName);
            parseMaisPar();
        }
    }

    // <mais_par> -> , <lista_par> | λ
    private void parseMaisPar() {
        if (currentToken.type == TokenType.COMMA) {
            eat(TokenType.COMMA);
            parseListaPar();
        }
    }

    // === Regras Gramaticais: Comandos e Blocos ===

    // <bloco> -> tabulacao <comandos>
    private void parseBloco() {
        eat(TokenType.TAB);
        // Contexto TRUE: Estamos dentro de um bloco, exige indentação
        parseComandos(true);
    }

    // <comandos> -> <comando> <mais_comandos>
    private void parseComandos(boolean insideBlock) {
        parseComando();
        parseMaisComandos(insideBlock);
    }

    // <mais_comandos> -> <comandos> | λ
    private void parseMaisComandos(boolean insideBlock) {
        // 1. Critério de Parada Universal: Fim de Arquivo ou Else
        if (currentToken.type == TokenType.EOF || currentToken.type == TokenType.ELSE) {
            return;
        }

        // 2. Lógica para quando estamos DENTRO de um bloco (Função, If, While)
        if (insideBlock) {
            // Se houver TAB, significa que o bloco continua
            if (currentToken.type == TokenType.TAB) {
                eat(TokenType.TAB);

                // Verificação pós-TAB: Se for ELSE ou EOF, para.
                if (currentToken.type == TokenType.ELSE || currentToken.type == TokenType.EOF) {
                    return;
                }

                // Se for um comando válido, continua processando recursivamente
                if (isComando(currentToken.type)) {
                    parseComandos(true);
                }
            }
            // Se NÃO houver TAB (Dedent), o bloco acabou. Retorna para o nível anterior.
            else {
                return;
            }
        }
        // 3. Lógica para quando estamos no MAIN (fora de bloco)
        else {
            // No main, não exigimos TAB. Se for comando, processa.
            if (isComando(currentToken.type)) {
                parseComandos(false);
            }
            // Robustez: Se houver TAB solto no main, consumimos e continuamos
            else if (currentToken.type == TokenType.TAB) {
                eat(TokenType.TAB);
                if (isComando(currentToken.type)) {
                    parseComandos(false);
                }
            }
        }
    }

    // <comando>
    private void parseComando() {
        if (currentToken.type == TokenType.PRINT) {
            eat(TokenType.PRINT);
            eat(TokenType.LPAREN);
            String name = currentToken.lexeme;
            eat(TokenType.IDENT);
            eat(TokenType.RPAREN);

            int addr = symbolTable.getVariableAddress(name);
            codeGen.emit("CRVL", addr);
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

    // ident <restoIdent> -> = <expressao> | <lista_arg>
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

    // <expressao> -> <termo> <outros_termos> | input()
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