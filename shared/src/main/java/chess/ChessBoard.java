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
    private final ChessPiece[][] board;

    public ChessBoard() {
        this.board = new ChessPiece[8][8];
    }

    /**
     * Adds a chess piece to the chessboard
     *
     * @param position where to add the piece to
     * @param piece    the piece to add
     */
    public void addPiece(ChessPosition position, ChessPiece piece) {
        if (isValidPosition(position)) {
            board[position.getRow() - 1][position.getColumn() - 1] = piece;
        }
    }

    /**
     * Gets a chess piece on the chessboard
     *
     * @param position The position to get the piece from
     * @return Either the piece at the position, or null if no piece is at that
     * position
     */
    public ChessPiece getPiece(ChessPosition position) {
        if (isValidPosition(position)) {
            return board[position.getRow() - 1][position.getColumn() - 1];
        }
        return null;
    }

    /**
     * Sets the board to the default starting board
     * (How the game of chess normally starts)
     */
    public void resetBoard() {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Checks if a position is within the valid bounds of the chess board
     *
     * @param position The position to check
     * @return true if the position is valid, false otherwise
     */
    private boolean isValidPosition(ChessPosition position) {
        return position != null &&
               position.getRow() >= 1 && position.getRow() <= 8 &&
               position.getColumn() >= 1 && position.getColumn() <= 8;
    }

    
    @Override
    public boolean equals(Object ob) {
        if (this == ob) return true;
        if (ob == null || getClass() != ob.getClass()) return false;
        ChessBoard chessBoard = (ChessBoard) ob;
        return Arrays.deepEquals(board, chessBoard.board);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(board);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ChessBoard:\n");
        for (ChessPiece[] row : board) {
            sb.append(Arrays.toString(row)).append("\n");
        }
        return sb.toString();
    }
}
