package br.com.ufmt.lexico;

/**
 * Enumeração que define todos os tipos de tokens aceitos pela gramática da linguagem.
 * Cada constante representa um símbolo terminal ou categoria léxica.
 */
public enum TipoToken {
    // Palavras Reservadas
    DEF("def"),
    IF("if"),
    ELSE("else"),
    WHILE("while"),
    PRINT("print"),
    INPUT("input"),

    // Operadores Aritméticos
    SOMA("+"),
    SUBTRACAO("-"),
    MULTIPLICACAO("*"),
    DIVISAO("/"),
    ATRIBUICAO("="),

    // Operadores Relacionais
    IGUAL("=="),
    DIFERENTE("!="),
    MAIOR(">"),
    MENOR("<"),
    MAIOR_IGUAL(">="),
    MENOR_IGUAL("<="),

    // Delimitadores
    PARENTESES_ESQ("("),
    PARENTESES_DIR(")"),
    DOIS_PONTOS(":"),
    VIRGULA(","),

    // Identificadores e Literais
    IDENTIFICADOR(null),
    NUMERO(null),

    // Tokens Especiais
    TABULACAO("tab"),
    FIM_ARQUIVO("eof"),
    DESCONHECIDO(null);

    public final String matchString;

    /**
     * Construtor do tipo de token.
     * @param matchString A string literal que representa o token (ex: "if", "+"), ou null se for variável.
     */
    TipoToken(String matchString) {
        this.matchString = matchString;
    }
}