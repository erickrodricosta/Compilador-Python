package br.com.ufmt;

public class Lexer {
    private String input;
    private int pos;         // Posição atual no arquivo (char)
    private int line;        // Linha atual (para debug)
    private char currentChar;

    public Lexer(String input) {
        this.input = input;
        this.pos = 0;
        this.line = 1;

        if (input.length() > 0) {
            this.currentChar = input.charAt(0);
        } else {
            this.currentChar = '\0';
        }
    }

    // Avança para o próximo caractere
    private void advance() {
        pos++;
        if (pos < input.length()) {
            currentChar = input.charAt(pos);
        } else {
            currentChar = '\0'; // Indica fim do input
        }
    }

    // Olha o próximo caractere sem avançar (Lookahead)
    private char peek() {
        if (pos + 1 < input.length()) {
            return input.charAt(pos + 1);
        }
        return '\0';
    }

    // Olha dois caracteres à frente (para identificar """ ou ==)
    private char peekWait() {
        if (pos + 2 < input.length()) {
            return input.charAt(pos + 2);
        }
        return '\0';
    }

    // Ignora espaços em branco, mas CUIDADO:
    // A gramática diz que Tabulação é um Token sintático.
    // Então só ignoramos espaços normais e quebras de linha que não sejam indentação.
    private void skipWhitespace() {
        while (currentChar != '\0' && Character.isWhitespace(currentChar)) {
            // Se for tabulação, NÃO ignoramos aqui, pois o nextToken deve pegar
            if (currentChar == '\t') {
                break;
            }

            if (currentChar == '\n') {
                line++;
            }
            advance();
        }
    }

    // Lê identificadores (variaveis) ou palavras reservadas (if, while...)
    private Token readIdentifier() {
        StringBuilder sb = new StringBuilder();
        while (Character.isLetterOrDigit(currentChar) || currentChar == '_') {
            sb.append(currentChar);
            advance();
        }
        String text = sb.toString();

        // Verifica se é palavra reservada
        for (TokenType t : TokenType.values()) {
            if (t.matchString != null && t.matchString.equals(text)) {
                return new Token(t, text, line);
            }
        }
        // Se não for reservada, é identificador
        return new Token(TokenType.IDENT, text, line);
    }

    // Lê números (inteiros ou floats)
    private Token readNumber() {
        StringBuilder sb = new StringBuilder();
        while (Character.isDigit(currentChar)) {
            sb.append(currentChar);
            advance();
        }
        // Se tiver ponto, é float
        if (currentChar == '.') {
            sb.append(currentChar);
            advance();
            while (Character.isDigit(currentChar)) {
                sb.append(currentChar);
                advance();
            }
        }
        return new Token(TokenType.NUMBER, sb.toString(), line);
    }

    // Método principal que retorna o próximo token
    // Adicione/Substitua este método na classe Lexer
    public Token nextToken() {
        while (currentChar != '\0') {

            // 1. Tratamento de Comentários """ ... """
            if (currentChar == '"' && peek() == '"' && peekWait() == '"') {
                advance(); advance(); advance();
                while (currentChar != '\0') {
                    if (currentChar == '"' && peek() == '"' && peekWait() == '"') {
                        advance(); advance(); advance();
                        break;
                    }
                    if (currentChar == '\n') line++;
                    advance();
                }
                continue;
            }

            // 2. Tabulação Real (\t)
            if (currentChar == '\t') {
                advance();
                return new Token(TokenType.TAB, "\\t", line);
            }

            // 3. NOVA LÓGICA: Tabulação com Espaços (Soft Tab)
            // Se encontrar 2 ou mais espaços seguidos, considera como TAB
            if (currentChar == ' ' && peek() == ' ') {
                // Consome todos os espaços contínuos
                while (currentChar == ' ') {
                    advance();
                }
                return new Token(TokenType.TAB, "  ", line);
            }

            // 4. Quebras de linha e espaços simples
            // Espaços únicos são ignorados, quebras de linha incrementam contador
            if (Character.isWhitespace(currentChar)) {
                if (currentChar == '\n') {
                    line++;
                }
                advance();
                continue;
            }

            // 5. Números
            if (Character.isDigit(currentChar)) {
                return readNumber();
            }

            // 6. Identificadores e Palavras Reservadas
            if (Character.isLetter(currentChar)) {
                return readIdentifier();
            }

            // 7. Operadores e Pontuação (Igual ao anterior)
            if (currentChar == '=' && peek() == '=') { advance(); advance(); return new Token(TokenType.EQUALS, "==", line); }
            if (currentChar == '!' && peek() == '=') { advance(); advance(); return new Token(TokenType.DIFF, "!=", line); }
            if (currentChar == '>' && peek() == '=') { advance(); advance(); return new Token(TokenType.GREATER_EQ, ">=", line); }
            if (currentChar == '<' && peek() == '=') { advance(); advance(); return new Token(TokenType.LESS_EQ, "<=", line); }

            switch (currentChar) {
                case '+': advance(); return new Token(TokenType.PLUS, "+", line);
                case '-': advance(); return new Token(TokenType.MINUS, "-", line);
                case '*': advance(); return new Token(TokenType.MULT, "*", line);
                case '/': advance(); return new Token(TokenType.DIV, "/", line);
                case '(': advance(); return new Token(TokenType.LPAREN, "(", line);
                case ')': advance(); return new Token(TokenType.RPAREN, ")", line);
                case ':': advance(); return new Token(TokenType.COLON, ":", line);
                case ',': advance(); return new Token(TokenType.COMMA, ",", line);
                case '=': advance(); return new Token(TokenType.ASSIGN, "=", line);
                case '>': advance(); return new Token(TokenType.GREATER, ">", line);
                case '<': advance(); return new Token(TokenType.LESS, "<", line);
                default:
                    System.err.println("Caractere inesperado na linha " + line + ": " + currentChar);
                    advance();
                    return new Token(TokenType.UNKNOWN, String.valueOf(currentChar), line);
            }
        }

        return new Token(TokenType.EOF, "EOF", line);
    }
}