package chess;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Calculator for pawn moves
 */
public class PawnMovesCalculator implements PieceMovesCalculator {
    @Override
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition position, ChessPiece piece) {
        Collection<ChessMove> moves = new ArrayList<>();

        int row = position.getRow();
        int col = position.getColumn();
        ChessGame.TeamColor color = piece.getTeamColor();

        // Determine direction based on team color
        int direction = (color == ChessGame.TeamColor.WHITE) ? 1 : -1;

        // Forward move (1 square)
        addForwardMove(board, position, piece, direction, 1, moves);

        // Forward move (2 squares) from starting position
        if ((color == ChessGame.TeamColor.WHITE && row == 2) ||
                (color == ChessGame.TeamColor.BLACK && row == 7)) {
            // Check if the square directly in front is empty first
            ChessPosition singleMovePos = new ChessPosition(row + direction, col);
            if (board.getPiece(singleMovePos) == null) {
                // Now check the two-square move
                addForwardMove(board, position, piece, direction, 2, moves);
            }
        }

        // Diagonal captures
        addDiagonalCaptures(board, position, piece, direction, moves);

        return moves;
    }

    // Helper method to add forward moves
    private void addForwardMove(ChessBoard board, ChessPosition start, ChessPiece piece,
                                int direction, int squares, Collection<ChessMove> moves) {
        int row = start.getRow();
        int col = start.getColumn();
        int newRow = row + (direction * squares);

        // Check if new position is on the board
        if (newRow < 1 || newRow > 8) {
            return;
        }

        ChessPosition newPosition = new ChessPosition(newRow, col);
        ChessPiece pieceAtNewPosition = board.getPiece(newPosition);

        // Forward moves are only valid if the square is empty
        if (pieceAtNewPosition == null) {
            // Check if pawn reaches the end of the board (promotion)
            if (newRow == 1 || newRow == 8) {
                // Add moves for all possible promotion pieces
                addPromotionMoves(start, newPosition, moves);
            } else {
                // Normal move
                moves.add(new ChessMove(start, newPosition, null));
            }
        }
    }

    // Helper method to add diagonal capture moves
    private void addDiagonalCaptures(ChessBoard board, ChessPosition start, ChessPiece piece,
                                     int direction, Collection<ChessMove> moves) {
        int row = start.getRow();
        int col = start.getColumn();
        int newRow = row + direction;

        // Check if new position is on the board
        if (newRow < 1 || newRow > 8) {
            return;
        }

        // Check left diagonal
        if (col > 1) {
            ChessPosition leftCapture = new ChessPosition(newRow, col - 1);
            ChessPiece leftPiece = board.getPiece(leftCapture);

            // Can capture if there's an enemy piece
            if (leftPiece != null && leftPiece.getTeamColor() != piece.getTeamColor()) {
                // Check for promotion
                if (newRow == 1 || newRow == 8) {
                    addPromotionMoves(start, leftCapture, moves);
                } else {
                    moves.add(new ChessMove(start, leftCapture, null));
                }
            }
        }

        // Check right diagonal
        if (col < 8) {
            ChessPosition rightCapture = new ChessPosition(newRow, col + 1);
            ChessPiece rightPiece = board.getPiece(rightCapture);

            // Can capture if there's an enemy piece
            if (rightPiece != null && rightPiece.getTeamColor() != piece.getTeamColor()) {
                // Check for promotion
                if (newRow == 1 || newRow == 8) {
                    addPromotionMoves(start, rightCapture, moves);
                } else {
                    moves.add(new ChessMove(start, rightCapture, null));
                }
            }
        }
    }

    // Helper method to add promotion moves
    private void addPromotionMoves(ChessPosition start, ChessPosition end, Collection<ChessMove> moves) {
        // Add a move for each possible promotion type
        moves.add(new ChessMove(start, end, ChessPiece.PieceType.QUEEN));
        moves.add(new ChessMove(start, end, ChessPiece.PieceType.ROOK));
        moves.add(new ChessMove(start, end, ChessPiece.PieceType.BISHOP));
        moves.add(new ChessMove(start, end, ChessPiece.PieceType.KNIGHT));
    }
}