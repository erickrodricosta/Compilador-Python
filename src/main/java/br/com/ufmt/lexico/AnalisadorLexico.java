package br.com.ufmt.lexico;

/**
 * Responsável por ler o código fonte caractere por caractere e transformá-lo em uma sequência de Tokens.
 * Implementa a lógica de autômatos finitos para reconhecimento de padrões.
 */
public class AnalisadorLexico {
    private String conteudoArquivo;
    private int posicaoAtual;
    private int linha;
    private char caractereAtual;

    /**
     * Inicializa o analisador léxico com o código fonte.
     * @param conteudoArquivo O conteúdo completo do arquivo de código fonte.
     */
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

    /**
     * Avança o cursor para o próximo caractere do arquivo.
     */
    private void avancarCaractere() {
        posicaoAtual++;
        if (posicaoAtual < conteudoArquivo.length()) {
            caractereAtual = conteudoArquivo.charAt(posicaoAtual);
        } else {
            caractereAtual = '\0';
        }
    }

    /**
     * Verifica o próximo caractere sem avançar o cursor (Lookahead de 1 posição).
     * @return O próximo caractere ou '\0' se for fim de arquivo.
     */
    private char espiarProximoCaractere() {
        if (posicaoAtual + 1 < conteudoArquivo.length()) {
            return conteudoArquivo.charAt(posicaoAtual + 1);
        }
        return '\0';
    }

    /**
     * Verifica o caractere a duas posições à frente (Lookahead de 2 posições).
     * Utilizado para identificar tokens longos como comentários de aspas triplas.
     * @return O caractere encontrado ou '\0'.
     */
    private char espiarDoisCaracteresFrente() {
        if (posicaoAtual + 2 < conteudoArquivo.length()) {
            return conteudoArquivo.charAt(posicaoAtual + 2);
        }
        return '\0';
    }

    /**
     * Lê e processa um identificador ou palavra reservada.
     * @return Token do tipo IDENTIFICADOR ou uma palavra reservada (ex: IF, WHILE).
     */
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

    /**
     * Lê e processa um número (inteiro ou real).
     * @return Token do tipo NUMERO contendo o valor.
     */
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

    /**
     * Analisa o fluxo de caracteres atual e extrai o próximo token válido.
     * Ignora espaços em branco (exceto indentação significativa) e comentários.
     * @return O próximo Token encontrado ou FIM_ARQUIVO.
     */
    public Token obterProximoToken() {
        while (caractereAtual != '\0') {

            // Tratamento de Comentários (""" ... """)
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

            // Tabulação Real
            if (caractereAtual == '\t') {
                avancarCaractere();
                return new Token(TipoToken.TABULACAO, "\\t", linha);
            }

            // Tabulação com Espaços (Soft Tab)
            if (caractereAtual == ' ' && espiarProximoCaractere() == ' ') {
                while (caractereAtual == ' ') {
                    avancarCaractere();
                }
                return new Token(TipoToken.TABULACAO, "  ", linha);
            }

            // Espaços em branco não significativos
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

            // Operadores Compostos (==, !=, >=, <=)
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

            // Caracteres Únicos
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
                    String desconhecido = String.valueOf(caractereAtual);
                    avancarCaractere();
                    return new Token(TipoToken.DESCONHECIDO, desconhecido, linha);
            }
        }

        return new Token(TipoToken.FIM_ARQUIVO, "EOF", linha);
    }
}