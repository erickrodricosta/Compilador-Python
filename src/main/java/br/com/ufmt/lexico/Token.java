package br.com.ufmt.lexico;

/**
 * Representa uma unidade léxica encontrada no código fonte.
 * Armazena o tipo, o valor textual (lexema) e a localização (linha).
 */
public class Token {
    public TipoToken tipo;
    public String lexema;
    public int linha;

    /**
     * Cria um novo token.
     * @param tipo O tipo do token (categoria).
     * @param lexema O texto original encontrado no código.
     * @param linha O número da linha onde o token foi encontrado.
     */
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