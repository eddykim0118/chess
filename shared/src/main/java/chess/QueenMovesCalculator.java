package chess;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Calculator for queen moves
 */
public class QueenMovesCalculator implements PieceMovesCalculator {
    @Override
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition position, ChessPiece piece) {
        Collection<ChessMove> moves = new ArrayList<>();

        // Queen can move in any direction (combination of rook and bishop moves)
        int[][] directions = {
                {-1, -1}, {-1, 0}, {-1, 1},  // Diagonals and verticals
                {0, -1},           {0, 1},   // Horizontals
                {1, -1},  {1, 0},  {1, 1}    // Diagonals and verticals
        };

        // Check moves in each direction
        for (int[] direction : directions) {
            addMovesInDirection(board, position, piece, direction[0], direction[1], moves);
        }

        return moves;
    }

    // Helper method to add all moves in a given direction until blocked
    private void addMovesInDirection(ChessBoard board, ChessPosition start, ChessPiece piece,
                                     int rowDirection, int colDirection, Collection<ChessMove> moves) {
        int row = start.getRow();
        int col = start.getColumn();

        for (int i = 1; i <= 8; i++) {  // Maximum of 8 squares in any direction
            int newRow = row + i * rowDirection;
            int newCol = col + i * colDirection;

            // Stop if we go off the board
            if (newRow < 1 || newRow > 8 || newCol < 1 || newCol > 8) {
                break;
            }

            ChessPosition newPosition = new ChessPosition(newRow, newCol);
            ChessPiece pieceAtNewPosition = board.getPiece(newPosition);

            if (pieceAtNewPosition == null) {
                // Empty square, we can move here
                moves.add(new ChessMove(start, newPosition, null));
            } else if (pieceAtNewPosition.getTeamColor() != piece.getTeamColor()) {
                // Enemy piece, we can capture it and then stop
                moves.add(new ChessMove(start, newPosition, null));
                break;
            } else {
                // Friendly piece, we can't move here or beyond
                break;
            }
        }
    }
}