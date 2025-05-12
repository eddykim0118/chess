package chess;

import java.util.Collection;

public interface PieceMovesCalculator {
    /**
     * Calculates all the possible moves for a piece at the given position
     *
     * @param board The current chess board
     * @param position The position of the piece
     * @param piece The chess piece
     * @return Collection of valid moves
     */
    Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition position, ChessPiece piece);
}