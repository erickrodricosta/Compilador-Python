package br.com.ufmt.lexico;

public class AnalisadorLexico {
    private String conteudoArquivo;
    private int posicaoAtual;
    private int linha;
    private char caractereAtual;

    public AnalisadorLexico(String conteudoArquivo) {
        this.conteudoArquivo = conteudoArquivo;
        this.posicaoAtual = 0;
        this.linha = 1;

        if (conteudoArquivo.length() > 0) {
            this.caractereAtual = conteudoArquivo.charAt(0);
        } else {
            this.caractereAtual = '\0';
        }
    }

    private void avancarCaractere() {
        posicaoAtual++;
        if (posicaoAtual < conteudoArquivo.length()) {
            caractereAtual = conteudoArquivo.charAt(posicaoAtual);
        } else {
            caractereAtual = '\0';
        }
    }

    private char espiarProximoCaractere() {
        if (posicaoAtual + 1 < conteudoArquivo.length()) {
            return conteudoArquivo.charAt(posicaoAtual + 1);
        }
        return '\0';
    }

    private char espiarDoisCaracteresFrente() {
        if (posicaoAtual + 2 < conteudoArquivo.length()) {
            return conteudoArquivo.charAt(posicaoAtual + 2);
        }
        return '\0';
    }

    private Token lerIdentificador() {
        StringBuilder sb = new StringBuilder();
        while (Character.isLetterOrDigit(caractereAtual) || caractereAtual == '_') {
            sb.append(caractereAtual);
            avancarCaractere();
        }
        String texto = sb.toString();

        for (TipoToken t : TipoToken.values()) {
            if (t.matchString != null && t.matchString.equals(texto)) {
                return new Token(t, texto, linha);
            }
        }
        return new Token(TipoToken.IDENTIFICADOR, texto, linha);
    }

    private Token lerNumero() {
        StringBuilder sb = new StringBuilder();
        while (Character.isDigit(caractereAtual)) {
            sb.append(caractereAtual);
            avancarCaractere();
        }
        if (caractereAtual == '.') {
            sb.append(caractereAtual);
            avancarCaractere();
            while (Character.isDigit(caractereAtual)) {
                sb.append(caractereAtual);
                avancarCaractere();
            }
        }
        return new Token(TipoToken.NUMERO, sb.toString(), linha);
    }

    public Token obterProximoToken() {
        while (caractereAtual != '\0') {

            if (caractereAtual == '"' && espiarProximoCaractere() == '"' && espiarDoisCaracteresFrente() == '"') {
                avancarCaractere(); avancarCaractere(); avancarCaractere();
                while (caractereAtual != '\0') {
                    if (caractereAtual == '"' && espiarProximoCaractere() == '"' && espiarDoisCaracteresFrente() == '"') {
                        avancarCaractere(); avancarCaractere(); avancarCaractere();
                        break;
                    }
                    if (caractereAtual == '\n') linha++;
                    avancarCaractere();
                }
                continue;
            }

            if (caractereAtual == '\t') {
                avancarCaractere();
                return new Token(TipoToken.TABULACAO, "\\t", linha);
            }

            if (caractereAtual == ' ' && espiarProximoCaractere() == ' ') {
                while (caractereAtual == ' ') {
                    avancarCaractere();
                }
                return new Token(TipoToken.TABULACAO, "  ", linha);
            }

            if (Character.isWhitespace(caractereAtual)) {
                if (caractereAtual == '\n') {
                    linha++;
                }
                avancarCaractere();
                continue;
            }

            if (Character.isDigit(caractereAtual)) {
                return lerNumero();
            }

            if (Character.isLetter(caractereAtual)) {
                return lerIdentificador();
            }

            if (caractereAtual == '=' && espiarProximoCaractere() == '=') {
                avancarCaractere(); avancarCaractere();
                return new Token(TipoToken.IGUAL, "==", linha);
            }
            if (caractereAtual == '!' && espiarProximoCaractere() == '=') {
                avancarCaractere(); avancarCaractere();
                return new Token(TipoToken.DIFERENTE, "!=", linha);
            }
            if (caractereAtual == '>' && espiarProximoCaractere() == '=') {
                avancarCaractere(); avancarCaractere();
                return new Token(TipoToken.MAIOR_IGUAL, ">=", linha);
            }
            if (caractereAtual == '<' && espiarProximoCaractere() == '=') {
                avancarCaractere(); avancarCaractere();
                return new Token(TipoToken.MENOR_IGUAL, "<=", linha);
            }

            switch (caractereAtual) {
                case '+': avancarCaractere(); return new Token(TipoToken.SOMA, "+", linha);
                case '-': avancarCaractere(); return new Token(TipoToken.SUBTRACAO, "-", linha);
                case '*': avancarCaractere(); return new Token(TipoToken.MULTIPLICACAO, "*", linha);
                case '/': avancarCaractere(); return new Token(TipoToken.DIVISAO, "/", linha);
                case '(': avancarCaractere(); return new Token(TipoToken.PARENTESES_ESQ, "(", linha);
                case ')': avancarCaractere(); return new Token(TipoToken.PARENTESES_DIR, ")", linha);
                case ':': avancarCaractere(); return new Token(TipoToken.DOIS_PONTOS, ":", linha);
                case ',': avancarCaractere(); return new Token(TipoToken.VIRGULA, ",", linha);
                case '=': avancarCaractere(); return new Token(TipoToken.ATRIBUICAO, "=", linha);
                case '>': avancarCaractere(); return new Token(TipoToken.MAIOR, ">", linha);
                case '<': avancarCaractere(); return new Token(TipoToken.MENOR, "<", linha);
                default:
                    System.err.println("Caractere inesperado na linha " + linha + ": " + caractereAtual);
                    String desconhecido = String.valueOf(caractereAtual);
                    avancarCaractere();
                    return new Token(TipoToken.DESCONHECIDO, desconhecido, linha);
            }
        }

        return new Token(TipoToken.FIM_ARQUIVO, "EOF", linha);
    }
}