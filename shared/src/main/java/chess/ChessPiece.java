package chess;

import java.util.Collection;
import java.util.Objects;
import java.util.ArrayList;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {
    private final ChessGame.TeamColor pieceColor;
    private final PieceType type;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.pieceColor = pieceColor;
        this.type = type;
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return pieceColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        Collection<ChessMove> validMoves = new ArrayList<>();
    
        if (this.type == PieceType.ROOK) {
            // Define the four directions a rook can move (up, down, left, right)
            int[][] directions = {
                {0, 1},   // right
                {0, -1},  // left
                {1, 0},   // up
                {-1, 0}   // down
            };
            
            for (int[] direction : directions) {
                int newRow = myPosition.getRow();
                int newCol = myPosition.getColumn();
                
                while (true) {
                    newRow += direction[0];
                    newCol += direction[1];
                    
                    // Check if we're still on the board
                    if (newRow < 1 || newRow > 8 || newCol < 1 || newCol > 8) {
                        break;
                    }
                    
                    ChessPosition newPosition = new ChessPosition(newRow, newCol);
                    ChessPiece pieceAtNewPosition = board.getPiece(newPosition);
                    
                    if (pieceAtNewPosition == null) {
                        // Empty square - valid move
                        validMoves.add(new ChessMove(myPosition, newPosition, null));
                    } else {
                        // Square has a piece
                        if (pieceAtNewPosition.getTeamColor() != this.pieceColor) {
                            // Enemy piece - can capture
                            validMoves.add(new ChessMove(myPosition, newPosition, null));
                        }
                        // Whether enemy or friendly piece, can't move further in this direction
                        break;
                    }
                }
            }
        }
        
        return validMoves;
    }

    @Override
    public boolean equals(Object ob) {
        if (this == ob) return true;
        if (ob == null || getClass() != ob.getClass()) return false;
        ChessPiece chessPiece = (ChessPiece) ob;
        return pieceColor == chessPiece.pieceColor && type == chessPiece.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pieceColor, type);
    }

    @Override
    public String toString() {
        return "ChessPiece{" + "pieceColor=" + pieceColor + ", type=" + type + "}";
    }
}
