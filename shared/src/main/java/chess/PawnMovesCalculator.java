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

        int direction = (color == ChessGame.TeamColor.WHITE) ? 1 : -1;

        addForwardMove(board, position, piece, direction, 1, moves);

        if ((color == ChessGame.TeamColor.WHITE && row == 2) ||
                (color == ChessGame.TeamColor.BLACK && row == 7)) {
            ChessPosition singleMovePos = new ChessPosition(row + direction, col);
            if (board.getPiece(singleMovePos) == null) {
                addForwardMove(board, position, piece, direction, 2, moves);
            }
        }

        addDiagonalCaptures(board, position, piece, direction, moves);

        return moves;
    }

    private void addForwardMove(ChessBoard board, ChessPosition start, ChessPiece piece,
                                int direction, int squares, Collection<ChessMove> moves) {
        int row = start.getRow();
        int col = start.getColumn();
        int newRow = row + (direction * squares);

        if (newRow < 1 || newRow > 8) {
            return;
        }

        ChessPosition newPosition = new ChessPosition(newRow, col);
        ChessPiece pieceAtNewPosition = board.getPiece(newPosition);

        if (pieceAtNewPosition == null) {
            if (newRow == 1 || newRow == 8) {
                addPromotionMoves(start, newPosition, moves);
            } else {
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