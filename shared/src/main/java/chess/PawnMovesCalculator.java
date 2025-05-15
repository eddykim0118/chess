package chess;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Calculator for pawn moves
 */
public class PawnMovesCalculator implements PieceMovesCalculator {
    @Override
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition position, ChessPiece piece, ChessMove lastMove) {
        // Get regular moves first
        Collection<ChessMove> moves = pieceMoves(board, position, piece);

        // Now check for en passant
        if (lastMove != null) {
            addEnPassantMove(board, position, piece, lastMove, moves);
        }

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

    private void addEnPassantMove(ChessBoard board, ChessPosition position, 
                                  ChessPiece piece, ChessMove lastMove, Collection<ChessMove> moves) {
        // Check if last move was a pawn double move
        ChessPiece lastMovePiece = board.getPiece(lastMove.getEndPosition());
        
        if (lastMovePiece != null && 
            lastMovePiece.getPieceType() == ChessPiece.PieceType.PAWN &&
            lastMovePiece.getTeamColor() != piece.getTeamColor()) {
            
            int row = position.getRow();
            int col = position.getColumn();
            int lastMoveCol = lastMove.getEndPosition().getColumn();
            
            // Check if pieces are adjacent and on the same row
            if (row == lastMove.getEndPosition().getRow() && 
                Math.abs(col - lastMoveCol) == 1) {
                
                // Verify it was a double move (2 square jump)
                int startRow = lastMove.getStartPosition().getRow();
                int endRow = lastMove.getEndPosition().getRow();
                
                if (Math.abs(startRow - endRow) == 2) {
                    // Direction based on color
                    int direction = (piece.getTeamColor() == ChessGame.TeamColor.WHITE) ? 1 : -1;
                    
                    // Calculate the en passant capture position
                    ChessPosition capturePos = new ChessPosition(row + direction, lastMoveCol);
                    
                    // Add the move
                    moves.add(new ChessMove(position, capturePos, null));
                }
            }
        }
    }
}