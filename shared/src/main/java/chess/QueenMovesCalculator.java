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

        int[][] directions = {
                {-1, -1}, {-1, 0}, {-1, 1}, 
                {0, -1},           {0, 1}, 
                {1, -1},  {1, 0},  {1, 1}
        };

        for (int[] direction : directions) {
            addMovesInDirection(board, position, piece, direction[0], direction[1], moves);
        }

        return moves;
    }

    private void addMovesInDirection(ChessBoard board, ChessPosition start, ChessPiece piece,
                                     int rowDirection, int colDirection, Collection<ChessMove> moves) {
        int row = start.getRow();
        int col = start.getColumn();

        for (int i = 1; i <= 8; i++) {
            int newRow = row + i * rowDirection;
            int newCol = col + i * colDirection;

            if (newRow < 1 || newRow > 8 || newCol < 1 || newCol > 8) {
                break;
            }

            ChessPosition newPosition = new ChessPosition(newRow, newCol);
            ChessPiece pieceAtNewPosition = board.getPiece(newPosition);

            if (pieceAtNewPosition == null) {
                moves.add(new ChessMove(start, newPosition, null));
            } else if (pieceAtNewPosition.getTeamColor() != piece.getTeamColor()) {
                moves.add(new ChessMove(start, newPosition, null));
                break;
            } else {
                break;
            }
        }
    }
}