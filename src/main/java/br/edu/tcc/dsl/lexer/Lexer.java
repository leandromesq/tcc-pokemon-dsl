package br.edu.tcc.dsl.lexer;

import java.util.ArrayList;
import java.util.List;

public final class Lexer {

    private Lexer() {
    }

    private static boolean isLetter(char character) {
        return (character >= 'a' && character <= 'z')
                || (character >= 'A' && character <= 'Z');
    }

    private static boolean isNamePart(char character) {
        return isLetter(character)
                || (character >= '0' && character <= '9');
    }

    public static List<Token> tokenize(String source) {
        List<Token> tokens = new ArrayList<>();
        int position = 0;

        while (position < source.length()) {
            char current = source.charAt(position);

            if (Character.isWhitespace(current)) {
                position++;
                continue;
            }

            if (current == '!') {
                tokens.add(new Token(
                        TokenType.BANG,
                        "!",
                        position,
                        position + 1
                ));

                position++;
                continue;
            }

            if (current == '-'
                    && position + 1 < source.length()
                    && source.charAt(position + 1) == '>') {
                tokens.add(new Token(
                        TokenType.ARROW,
                        "->",
                        position,
                        position + 2
                ));

                position += 2;
                continue;
            }

            if (isLetter(current)) {
                int start = position;
                position++;

                while (position < source.length()) {
                    char next = source.charAt(position);

                    if (isNamePart(next)) {
                        position++;
                        continue;
                    }

                    if (next == '-'
                            && position + 1 < source.length()
                            && isNamePart(source.charAt(position + 1))) {
                        position++;
                        continue;
                    }

                    break;
                }

                tokens.add(new Token(
                        TokenType.NAME,
                        source.substring(start, position),
                        start,
                        position
                ));

                continue;
            }

            throw new IllegalArgumentException(
                    "Caractere inesperado na posição "
                            + position + ": '" + current + "'"
            );
        }

        tokens.add(new Token(
                TokenType.EOF,
                "",
                source.length(),
                source.length()
        ));

        return List.copyOf(tokens);
    }
}
