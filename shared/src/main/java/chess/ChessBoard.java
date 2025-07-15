package chess;

import java.util.Arrays;
import java.util.Objects;

/**
 * A chessboard that can hold and rearrange chess pieces.
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessBoard {
    private ChessPiece[][] board;

    public ChessBoard() {
        board = new ChessPiece[8][8];
    }

    /**
     * Adds a chess piece to the chessboard
     *
     * @param position where to add the piece to
     * @param piece    the piece to add
     */
    public void addPiece(ChessPosition position, ChessPiece piece) {
        int row = position.getRow() - 1;
        int col = position.getColumn() - 1;

        board[row][col] = piece;
    }

    /**
     * Gets a chess piece on the chessboard
     *
     * @param position The position to get the piece from
     * @return Either the piece at the position, or null if no piece is at that
     * position
     */
    public ChessPiece getPiece(ChessPosition position) {
        int row = position.getRow() - 1;
        int col = position.getColumn() - 1;

        if (row < 0 || row > 7 || col < 0 || col > 7) {
            return null;
        }

        return board[row][col];
    }

    /**
     * Sets the board to the default starting board
     * (How the game of chess normally starts)
     */
    public void resetBoard() {
        board = new ChessPiece[8][8];

        setupBackRow(1, ChessGame.TeamColor.WHITE);
        setupPawnRow(2, ChessGame.TeamColor.WHITE);

        setupPawnRow(7, ChessGame.TeamColor.BLACK);
        setupBackRow(8, ChessGame.TeamColor.BLACK);
    }

    private void setupBackRow(int row, ChessGame.TeamColor color) {
        addPiece(new ChessPosition(row, 1), new ChessPiece(color, ChessPiece.PieceType.ROOK));
        addPiece(new ChessPosition(row, 2), new ChessPiece(color, ChessPiece.PieceType.KNIGHT));
        addPiece(new ChessPosition(row, 3), new ChessPiece(color, ChessPiece.PieceType.BISHOP));
        addPiece(new ChessPosition(row, 4), new ChessPiece(color, ChessPiece.PieceType.QUEEN));
        addPiece(new ChessPosition(row, 5), new ChessPiece(color, ChessPiece.PieceType.KING));
        addPiece(new ChessPosition(row, 6), new ChessPiece(color, ChessPiece.PieceType.BISHOP));
        addPiece(new ChessPosition(row, 7), new ChessPiece(color, ChessPiece.PieceType.KNIGHT));
        addPiece(new ChessPosition(row, 8), new ChessPiece(color, ChessPiece.PieceType.ROOK));
    }

    private void setupPawnRow(int row, ChessGame.TeamColor color) {
        for (int col = 1; col <= 8; col++) {
            addPiece(new ChessPosition(row, col), new ChessPiece(color, ChessPiece.PieceType.PAWN));
        }
    }

    @Override
    public boolean equals(Object ob) {
        if (this == ob) return true;
        if (ob == null || getClass() != ob.getClass()) return false;

        ChessBoard that = (ChessBoard) ob;
        return Arrays.deepEquals(board, that.board);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(board);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (int row = 7; row >= 0; row--) {
            sb.append(row + 1).append(" |");

            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board[row][col];
                if (piece == null) {
                    sb.append(" . ");
                } else {
                    char colorChar = piece.getTeamColor() == ChessGame.TeamColor.WHITE ? 'W' : 'B';
                    char pieceChar = switch (piece.getPieceType()) {
                        case KING -> 'K';
                        case QUEEN -> 'Q';
                        case ROOK -> 'R';
                        case BISHOP -> 'B';
                        case KNIGHT -> 'N';
                        case PAWN -> 'P';
                    };
                    sb.append(" ").append(colorChar).append(pieceChar);
                }
                sb.append(" |");
            }
            sb.append("\n");
        }
        sb.append("   ");
        for (char col = 'a'; col <= 'h'; col++) {
            sb.append("  ").append(col).append(" ");
        }
        sb.append("\n");

        return sb.toString();
    }

}
