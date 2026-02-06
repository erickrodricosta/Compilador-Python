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

    // Verifica se o token atual é o esperado e avança para o próximo
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
        parseComandos();
    }

    // <dc> -> <dc_v> <mais_dc> | <dc_f> <mais_dc_f> | λ
    // Analisa se é declaração de variável, função ou se acabou as declarações
    private void parseDc() {
        // Loop para processar declarações mistas ou sequenciais conforme lookahead
        while (currentToken.type == TokenType.IDENT || currentToken.type == TokenType.DEF) {
            if (currentToken.type == TokenType.IDENT) {
                parseDcV(); // Declaração de variável
            } else if (currentToken.type == TokenType.DEF) {
                parseDcF(); // Declaração de função (Implementaremos na Parte B)
            }
        }
    }

    // <dc_v> -> <ident> = <expressao>
    private void parseDcV() {
        String varName = currentToken.lexeme;
        eat(TokenType.IDENT);

        // Aloca espaço na memória
        // Nota: Se a variável já existe no escopo global, teoricamente não precisaria de ALME novo,
        // mas a gramática sugere declarações aqui. Vamos assumir alocação.
        int addr = symbolTable.addVariable(varName);
        codeGen.emit("ALME", 1);

        eat(TokenType.ASSIGN);

        parseExpressao(); // Gera código que deixa o resultado no topo da pilha

        // Armazena o valor da expressão na variável recém alocada
        codeGen.emit("ARMZ", addr);
    }

    // === Regras Gramaticais: Expressões (Aritmética) ===

    // <expressao> -> <termo> <outros_termos> | input()
    // Resolvendo ambiguidade: input() é especial
    private void parseExpressao() {
        if (currentToken.type == TokenType.INPUT) {
            eat(TokenType.INPUT);
            eat(TokenType.LPAREN);
            eat(TokenType.RPAREN);
            codeGen.emit("LEIT"); // Lê do teclado e põe na pilha
        } else {
            parseTermo();
            parseOutrosTermos();
        }
    }

    // <outros_termos> -> <op_ad> <termo> <outros_termos> | λ
    private void parseOutrosTermos() {
        while (currentToken.type == TokenType.PLUS || currentToken.type == TokenType.MINUS) {
            Token op = currentToken;
            eat(op.type); // Consome + ou -

            parseTermo();

            if (op.type == TokenType.PLUS) {
                codeGen.emit("SOMA");
            } else {
                codeGen.emit("SUBT");
            }
        }
    }

    // <termo> -> <fator> <mais_fatores>
    private void parseTermo() {
        parseFator();
        parseMaisFatores();
    }

    // <mais_fatores> -> <op_mul> <fator> <mais_fatores> | λ
    private void parseMaisFatores() {
        while (currentToken.type == TokenType.MULT || currentToken.type == TokenType.DIV) {
            Token op = currentToken;
            eat(op.type); // Consome * ou /

            parseFator();

            if (op.type == TokenType.MULT) {
                codeGen.emit("MULT");
            } else {
                codeGen.emit("DIVI");
            }
        }
    }

    // <fator> -> ident | numero | ( <expressao> )
    private void parseFator() {
        if (currentToken.type == TokenType.IDENT) {
            String name = currentToken.lexeme;
            eat(TokenType.IDENT);
            if (!symbolTable.containsVariable(name)) {
                error("Variavel nao declarada: " + name);
            }
            int addr = symbolTable.getVariableAddress(name);
            codeGen.emit("CRVL", addr); // Carrega valor da variável na pilha
        }
        else if (currentToken.type == TokenType.NUMBER) {
            double val = Double.parseDouble(currentToken.lexeme);
            eat(TokenType.NUMBER);
            codeGen.emit("CRCT", val); // Carrega constante numérica na pilha
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
    // === Regras Gramaticais: Declaração de Funções ===

    // <dc_f> -> def ident <parametros> : <corpo_f>
    private void parseDcF() {
        eat(TokenType.DEF);
        String funcName = currentToken.lexeme;
        eat(TokenType.IDENT);

        // 1. Pular a definição da função durante a execução linear
        // Emitimos um DSVI (Desvio Incondicional) com um "buraco" (0)
        int jumpInstructionIndex = codeGen.emit("DSVI", 0);

        // 2. Registrar o início real da função na Tabela de Símbolos
        // O endereço da função é a próxima instrução que será gerada
        symbolTable.addFunction(funcName, codeGen.getCurrentAddress());

        // <parametros>
        eat(TokenType.LPAREN);
        parseListaPar(); // Adiciona parâmetros como variáveis
        eat(TokenType.RPAREN);

        eat(TokenType.COLON);

        // <corpo_f> -> <bloco>
        parseBloco();

        // 3. Fim da função: Retorno de Procedimento
        codeGen.emit("RTPR");

        // 4. Backpatching: Corrigir o DSVI lá do passo 1 para pular para CÁ (fim da função)
        codeGen.patch(jumpInstructionIndex, codeGen.getCurrentAddress());
    }

    // <lista_par> -> ident <mais_par>
    private void parseListaPar() {
        if (currentToken.type == TokenType.IDENT) {
            String varName = currentToken.lexeme;
            eat(TokenType.IDENT);

            // Parâmetros são tratados como variáveis locais.
            // Na nossa VM simplificada, assumimos que eles ocupam espaço de memória.
            int addr = symbolTable.addVariable(varName);
            // Nota: Em compiladores reais, parâmetros são copiados da pilha para local,
            // ou acessados via offset do frame. Aqui, vamos apenas registrar.

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
        // Obrigatório ter tabulação para definir bloco (estilo Python)
        eat(TokenType.TAB);
        parseComandos();
    }

    // <comandos> -> <comando> <mais_comandos>
    private void parseComandos() {
        parseComando();
        parseMaisComandos();
    }

    // <mais_comandos> -> <comandos> | λ
    private void parseMaisComandos() {
        // Verifica se o próximo token inicia um comando válido para continuar o loop
        if (currentToken.type == TokenType.PRINT ||
                currentToken.type == TokenType.IF ||
                currentToken.type == TokenType.WHILE ||
                currentToken.type == TokenType.IDENT) {
            parseComandos();
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
            codeGen.emit("CRVL", addr); // Carrega variável
            codeGen.emit("IMPR");       // Imprime topo
        }
        else if (currentToken.type == TokenType.IF) {
            parseIf();
        }
        else if (currentToken.type == TokenType.WHILE) {
            parseWhile();
        }
        else if (currentToken.type == TokenType.IDENT) {
            // Pode ser atribuição (x = 1) ou chamada de função (x())
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
        // 1. Atribuição
        if (currentToken.type == TokenType.ASSIGN) {
            eat(TokenType.ASSIGN);

            if (!symbolTable.containsVariable(name)) {
                // Se a variável não existe, em Python cria-se na hora.
                // Aqui vamos alocar se não existir.
                symbolTable.addVariable(name);
                codeGen.emit("ALME", 1);
            }

            parseExpressao(); // Gera valor no topo da pilha

            int addr = symbolTable.getVariableAddress(name);
            codeGen.emit("ARMZ", addr);
        }
        // 2. Chamada de Função
        else if (currentToken.type == TokenType.LPAREN) {
            // name é o nome da função
            if (symbolTable.getFunctionAddress(name) == null) {
                error("Funcao nao declarada: " + name);
            }

            // Empilha endereço de retorno (simplificação: PUSHER linha_atual + offset)
            // No exemplo do professor: PUSHER <linha_retorno>
            // Como não sabemos a linha exata ainda, usamos um placeholder ou deixamos a VM gerenciar
            // Pelo exemplo dado: PUSHER 94 (hardcoded no exemplo, mas precisa ser dinâmico)
            codeGen.emit("PUSHER", 0); // Placeholder, difícil calcular exato em 1 passo
            int paramIndex = codeGen.getCurrentAddress() - 1; // Indice do Pusher

            eat(TokenType.LPAREN);
            parseArgumentos(); // Gera instruções PARAM ou empilha valores
            eat(TokenType.RPAREN);

            int funcAddr = symbolTable.getFunctionAddress(name);
            codeGen.emit("CHPR", funcAddr);

            // Correção simplista do PUSHER: aponta para a instrução APÓS o CHPR
            codeGen.patch(paramIndex, codeGen.getCurrentAddress());
        }
    }

    // <condicao> -> <expressao> <relacao> <expressao>
    private void parseCondicao() {
        parseExpressao(); // Exp1 na pilha

        TokenType op = currentToken.type;
        eat(op); // Consome operador relacional (==, !=, >, etc)

        parseExpressao(); // Exp2 na pilha

        // Mapeia token para instrução da VM
        // Baseado no exemplo: >= é CMAI, <= é CPMI
        switch (op) {
            case EQUALS:      codeGen.emit("CMIG"); break; // Igual
            case DIFF:        codeGen.emit("CMDG"); break; // Desigual
            case GREATER_EQ:  codeGen.emit("CMAI"); break; // Maior Igual (Exemplo)
            case LESS_EQ:     codeGen.emit("CPMI"); break; // Menor Igual (Exemplo)
            case GREATER:     codeGen.emit("CMMA"); break; // Maior
            case LESS:        codeGen.emit("CMME"); break; // Menor
            default: error("Operador relacional inválido");
        }
    }

    // if <condicao> : <bloco> <pfalsa>
    private void parseIf() {
        eat(TokenType.IF);
        parseCondicao(); // Gera comparação. Topo da pilha: 1 (True) ou 0 (False)

        eat(TokenType.COLON);

        // Desvio Se Falso (pula o bloco se for 0)
        int jumpAddr = codeGen.emit("DSVF", 0);

        parseBloco();

        // <pfalsa> -> else : <bloco> | λ
        if (currentToken.type == TokenType.ELSE) {
            // Se tem Else, o bloco If precisa pular o bloco Else no final
            int jumpElseAddr = codeGen.emit("DSVI", 0);

            // Corrige o DSVF do If para pular para o começo do Else
            codeGen.patch(jumpAddr, codeGen.getCurrentAddress());

            eat(TokenType.ELSE);
            eat(TokenType.COLON);
            parseBloco();

            // Corrige o pulo do fim do If para o fim do Else
            codeGen.patch(jumpElseAddr, codeGen.getCurrentAddress());
        } else {
            // Se não tem Else, corrige o DSVF para o final do bloco If
            codeGen.patch(jumpAddr, codeGen.getCurrentAddress());
        }
    }

    // while <condicao> : <bloco>
    private void parseWhile() {
        eat(TokenType.WHILE);

        int startAddr = codeGen.getCurrentAddress(); // Salva onde começa a condição

        parseCondicao();

        eat(TokenType.COLON);

        // Se condição falsa, sai do loop
        int jumpEndAddr = codeGen.emit("DSVF", 0);

        parseBloco();

        // Volta para o início para testar condição novamente
        codeGen.emit("DSVI", startAddr);

        // Corrige o DSVF para sair do loop
        codeGen.patch(jumpEndAddr, codeGen.getCurrentAddress());
    }

    // <argumentos> -> <expressao> <mais_argumentos>
    private void parseArgumentos() {
        // Verifica se há argumentos (lookahead não é RPAREN)
        if (currentToken.type != TokenType.RPAREN) {
            parseExpressao();
            // No exemplo do professor, argumentos viram instrução PARAM
            // Mas parseExpressao gera CRVL/SOMA na pilha.
            // Vamos assumir que PARAM pega do topo da pilha e move para escopo local
            // Ou o simples fato de estar na pilha já serve (depende da VM).
            // Seguindo o exemplo:
            // codeGen.emit("PARAM"); // Se necessário

            parseMaisArgumentos();
        }
    }

    private void parseMaisArgumentos() {
        if (currentToken.type == TokenType.COMMA) {
            eat(TokenType.COMMA);
            parseArgumentos();
        }
    }
}