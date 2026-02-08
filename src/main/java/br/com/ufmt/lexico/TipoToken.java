package br.com.ufmt.lexico;

public enum TipoToken {
    DEF("def"),
    IF("if"),
    ELSE("else"),
    WHILE("while"),
    PRINT("print"),
    INPUT("input"),

    SOMA("+"),
    SUBTRACAO("-"),
    MULTIPLICACAO("*"),
    DIVISAO("/"),
    ATRIBUICAO("="),

    IGUAL("=="),
    DIFERENTE("!="),
    MAIOR(">"),
    MENOR("<"),
    MAIOR_IGUAL(">="),
    MENOR_IGUAL("<="),

    PARENTESES_ESQ("("),
    PARENTESES_DIR(")"),
    DOIS_PONTOS(":"),
    VIRGULA(","),

    IDENTIFICADOR(null),
    NUMERO(null),

    TABULACAO("tab"),

    FIM_ARQUIVO("eof"),

    DESCONHECIDO(null);

    public final String matchString;

    TipoToken(String matchString) {
        this.matchString = matchString;
    }
}