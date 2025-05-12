package chess;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Calculator for knight moves
 */
public class KnightMovesCalculator implements PieceMovesCalculator {
    @Override
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition position, ChessPiece piece) {
        Collection<ChessMove> moves = new ArrayList<>();

        // Knight moves in an L-shape: 2 squares in one direction, then 1 square perpendicular
        int[][] knightMoves = {
                {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
                {1, -2}, {1, 2}, {2, -1}, {2, 1}
        };

        int row = position.getRow();
        int col = position.getColumn();

        // Check each potential knight move
        for (int[] move : knightMoves) {
            int newRow = row + move[0];
            int newCol = col + move[1];

            // Ensure the new position is on the board
            if (newRow >= 1 && newRow <= 8 && newCol >= 1 && newCol <= 8) {
                ChessPosition newPosition = new ChessPosition(newRow, newCol);
                ChessPiece pieceAtNewPosition = board.getPiece(newPosition);

                // Can move if the destination is empty or contains an enemy piece
                if (pieceAtNewPosition == null ||
                        pieceAtNewPosition.getTeamColor() != piece.getTeamColor()) {
                    moves.add(new ChessMove(position, newPosition, null));
                }
            }
        }

        return moves;
    }
}