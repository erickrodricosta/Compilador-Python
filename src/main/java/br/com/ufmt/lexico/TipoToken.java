package br.com.ufmt.lexico;

public enum TipoToken {
    // === Palavras Reservadas ===
    DEF("def"),
    IF("if"),
    ELSE("else"),
    WHILE("while"),
    PRINT("print"),
    INPUT("input"),

    // === Operadores Aritméticos ===
    SOMA("+"),
    SUBTRACAO("-"),
    MULTIPLICACAO("*"),
    DIVISAO("/"),
    ATRIBUICAO("="),

    // === Operadores Relacionais ===
    IGUAL("=="),
    DIFERENTE("!="),
    MAIOR(">"),
    MENOR("<"),
    MAIOR_IGUAL(">="),
    MENOR_IGUAL("<="),

    // === Delimitadores ===
    PARENTESES_ESQ("("),
    PARENTESES_DIR(")"),
    DOIS_PONTOS(":"),
    VIRGULA(","),

    // === Identificadores e Literais ===
    IDENTIFICADOR(null),
    NUMERO(null),

    // === Estruturais ===
    TABULACAO("tab"), // Representa a indentação

    // Fim de arquivo
    FIM_ARQUIVO("eof"),

    // Token de erro ou desconhecido
    DESCONHECIDO(null);

    public final String matchString;

    TipoToken(String matchString) {
        this.matchString = matchString;
    }
}