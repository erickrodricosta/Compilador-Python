package br.com.ufmt.lexico;

public class Token {
    public TipoToken tipo;
    public String lexema; // O texto original (ex: "variavel", "10.5", "if")
    public int linha;     // Para mensagens de erro

    public Token(TipoToken tipo, String lexema, int linha) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linha = linha;
    }

    @Override
    public String toString() {
        return "<" + tipo + ", \"" + lexema + "\">";
    }
}