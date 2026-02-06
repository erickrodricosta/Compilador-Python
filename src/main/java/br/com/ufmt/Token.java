package br.com.ufmt;

public class Token {
    public TokenType type;
    public String lexeme; // O texto original (ex: "variavel", "10.5", "if")
    public int line;      // Para mensagens de erro

    public Token(TokenType type, String lexeme, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
    }

    @Override
    public String toString() {
        return "<" + type + ", \"" + lexeme + "\">";
    }
}