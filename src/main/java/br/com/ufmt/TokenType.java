package br.com.ufmt;

public enum TokenType {
    // === Palavras Reservadas ===
    DEF("def"),
    IF("if"),
    ELSE("else"),
    WHILE("while"),
    PRINT("print"),
    INPUT("input"),

    // === Operadores Aritméticos ===
    PLUS("+"),
    MINUS("-"),
    MULT("*"),
    DIV("/"),
    ASSIGN("="), // Usado para atribuição

    // === Operadores Relacionais ===
    EQUALS("=="),
    DIFF("!="),
    GREATER(">"),
    LESS("<"),
    GREATER_EQ(">="),
    LESS_EQ("<="),

    // === Delimitadores ===
    LPAREN("("),
    RPAREN(")"),
    COLON(":"),
    COMMA(","),

    // === Identificadores e Literais ===
    // Estes não têm string fixa, pois o valor varia (ex: nome da variável ou valor numérico)
    IDENT(null),
    NUMBER(null), // Na gramática serve para inteiros e reais

    // === Estruturais ===
    // A gramática especifica <bloco> -> tabulacao <comandos>
    // Logo, a tabulação é um token sintático
    TAB("tab"),

    // Fim de arquivo
    EOF("eof"),

    // Token de erro ou desconhecido
    UNKNOWN(null);

    public final String matchString;

    TokenType(String matchString) {
        this.matchString = matchString;
    }
}