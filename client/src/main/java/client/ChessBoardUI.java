package client;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;
import ui.EscapeSequences;

public class ChessBoardUI {

    public static void drawBoard(ChessBoard board, boolean whiteOnBottom) {
        if (whiteOnBottom) {
            drawBoardWhitePerspective(board);
        } else {
            drawBoardBlackPerspective(board);
        }
    }

    private static void drawBoardWhitePerspective(ChessBoard board) {
        // Draw from white's perspective (a1 in bottom-left)
        System.out.println();

        // Top border with column labels
        System.out.print("    ");
        for (char col = 'a'; col <= 'h'; col++) {
            System.out.print(" " + col + "  ");
        }
        System.out.println();

        // Draw rows from 8 to 1 (top to bottom)
        for (int row = 8; row >= 1; row--) {
            // Row number on left
            System.out.print(" " + row + " ");

            // Draw squares for this row
            for (int col = 1; col <= 8; col++) {
                boolean isLightSquare = (row + col) % 2 == 0;
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);

                drawSquare(piece, isLightSquare);
            }

            // Row number on right
            System.out.println(" " + row);
        }

        // Bottom border with column labels
        System.out.print("    ");
        for (char col = 'a'; col <= 'h'; col++) {
            System.out.print(" " + col + "  ");
        }
        System.out.println();
        System.out.println();
    }

    private static void drawBoardBlackPerspective(ChessBoard board) {
        // Draw from black's perspective (a1 in top-right)
        System.out.println();

        // Top border with column labels (h to a)
        System.out.print("    ");
        for (char col = 'h'; col >= 'a'; col--) {
            System.out.print(" " + col + "  ");
        }
        System.out.println();

        // Draw rows from 1 to 8 (top to bottom)
        for (int row = 1; row <= 8; row++) {
            // Row number on left
            System.out.print(" " + row + " ");

            // Draw squares for this row (h to a)
            for (int col = 8; col >= 1; col--) {
                boolean isLightSquare = (row + col) % 2 == 0;
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);

                drawSquare(piece, isLightSquare);
            }

            // Row number on right
            System.out.println(" " + row);
        }

        // Bottom border with column labels (h to a)
        System.out.print("    ");
        for (char col = 'h'; col >= 'a'; col--) {
            System.out.print(" " + col + "  ");
        }
        System.out.println();
        System.out.println();
    }

    private static void drawSquare(ChessPiece piece, boolean isLightSquare) {
        String bgColor = isLightSquare ? EscapeSequences.SET_BG_COLOR_WHITE : EscapeSequences.SET_BG_COLOR_DARK_GREEN;
        String textColor = EscapeSequences.SET_TEXT_COLOR_BLACK;

        System.out.print(bgColor + textColor);

        if (piece == null) {
            System.out.print(EscapeSequences.EMPTY);
        } else {
            System.out.print(getPieceSymbol(piece));
        }

        System.out.print(EscapeSequences.RESET_BG_COLOR + EscapeSequences.RESET_TEXT_COLOR);
    }

    private static String getPieceSymbol(ChessPiece piece) {
        ChessPiece.PieceType type = piece.getPieceType();
        ChessGame.TeamColor color = piece.getTeamColor();

        if (color == ChessGame.TeamColor.WHITE) {
            return switch (type) {
                case KING -> EscapeSequences.WHITE_KING;
                case QUEEN -> EscapeSequences.WHITE_QUEEN;
                case BISHOP -> EscapeSequences.WHITE_BISHOP;
                case KNIGHT -> EscapeSequences.WHITE_KNIGHT;
                case ROOK -> EscapeSequences.WHITE_ROOK;
                case PAWN -> EscapeSequences.WHITE_PAWN;
            };
        } else {
            return switch (type) {
                case KING -> EscapeSequences.BLACK_KING;
                case QUEEN -> EscapeSequences.BLACK_QUEEN;
                case BISHOP -> EscapeSequences.BLACK_BISHOP;
                case KNIGHT -> EscapeSequences.BLACK_KNIGHT;
                case ROOK -> EscapeSequences.BLACK_ROOK;
                case PAWN -> EscapeSequences.BLACK_PAWN;
            };
        }
    }
}