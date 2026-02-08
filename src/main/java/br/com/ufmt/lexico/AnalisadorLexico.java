package br.com.ufmt.lexico;

public class AnalisadorLexico {
    private String conteudoArquivo;
    private int posicaoAtual;        // Posição atual no arquivo (char)
    private int linha;               // Linha atual (para debug)
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

    // Avança para o próximo caractere
    private void avancarCaractere() {
        posicaoAtual++;
        if (posicaoAtual < conteudoArquivo.length()) {
            caractereAtual = conteudoArquivo.charAt(posicaoAtual);
        } else {
            caractereAtual = '\0'; // Indica fim do arquivo
        }
    }

    // Olha o próximo caractere sem avançar (Lookahead 1)
    private char espiarProximoCaractere() {
        if (posicaoAtual + 1 < conteudoArquivo.length()) {
            return conteudoArquivo.charAt(posicaoAtual + 1);
        }
        return '\0';
    }

    // Olha dois caracteres à frente (Lookahead 2 - usado para """ ou ==)
    private char espiarDoisCaracteresFrente() {
        if (posicaoAtual + 2 < conteudoArquivo.length()) {
            return conteudoArquivo.charAt(posicaoAtual + 2);
        }
        return '\0';
    }

    // Lê identificadores (variaveis) ou palavras reservadas (if, while...)
    private Token lerIdentificador() {
        StringBuilder sb = new StringBuilder();
        while (Character.isLetterOrDigit(caractereAtual) || caractereAtual == '_') {
            sb.append(caractereAtual);
            avancarCaractere();
        }
        String texto = sb.toString();

        // Verifica se é palavra reservada percorrendo o Enum
        for (TipoToken t : TipoToken.values()) {
            if (t.matchString != null && t.matchString.equals(texto)) {
                return new Token(t, texto, linha);
            }
        }
        // Se não for reservada, é identificador comum
        return new Token(TipoToken.IDENTIFICADOR, texto, linha);
    }

    // Lê números (inteiros ou reais)
    private Token lerNumero() {
        StringBuilder sb = new StringBuilder();
        while (Character.isDigit(caractereAtual)) {
            sb.append(caractereAtual);
            avancarCaractere();
        }
        // Se tiver ponto, é número real (float/double)
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

    // Método principal que retorna o próximo token
    public Token obterProximoToken() {
        while (caractereAtual != '\0') {

            // 1. Tratamento de Comentários """ ... """
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

            // 2. Tabulação Real (\t)
            if (caractereAtual == '\t') {
                avancarCaractere();
                return new Token(TipoToken.TABULACAO, "\\t", linha);
            }

            // 3. Tabulação com Espaços (Soft Tab - 2 espaços)
            if (caractereAtual == ' ' && espiarProximoCaractere() == ' ') {
                while (caractereAtual == ' ') {
                    avancarCaractere();
                }
                return new Token(TipoToken.TABULACAO, "  ", linha);
            }

            // 4. Quebras de linha e espaços simples (ignorados)
            if (Character.isWhitespace(caractereAtual)) {
                if (caractereAtual == '\n') {
                    linha++;
                }
                avancarCaractere();
                continue;
            }

            // 5. Números
            if (Character.isDigit(caractereAtual)) {
                return lerNumero();
            }

            // 6. Identificadores e Palavras Reservadas
            if (Character.isLetter(caractereAtual)) {
                return lerIdentificador();
            }

            // 7. Operadores e Pontuação
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

            // Caracteres únicos
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