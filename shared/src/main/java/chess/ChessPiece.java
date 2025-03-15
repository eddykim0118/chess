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
        
        switch (type) {
            case KING:
                validMoves.addAll(getKingMoves(board, myPosition));
                break;
            case QUEEN:
                validMoves.addAll(getQueenMoves(board, myPosition));
                break;
            case BISHOP:
                validMoves.addAll(getBishopMoves(board, myPosition));
                break;
            case KNIGHT:
                validMoves.addAll(getKnightMoves(board, myPosition));
                break;
            case ROOK:
                validMoves.addAll(getRookMoves(board, myPosition));
                break;
            case PAWN:
                validMoves.addAll(getPawnMoves(board, myPosition));
                break;
        }
        
        return validMoves;
    }

    // Create helper method for directional moves (used by bishop, rook, queen)
    private Collection<ChessMove> getDirectionalMoves(ChessBoard board, ChessPosition position, int[][] directions) {
        Collection<ChessMove> moves = new ArrayList<>();
        
        for (int[] direction : directions) {
            int newRow = position.getRow();
            int newCol = position.getColumn();
            
            while (true) {
                newRow += direction[0];
                newCol += direction[1];
                
                // Check if new position is off the board
                if (newRow < 1 || newRow > 8 || newCol < 1 || newCol > 8) {
                    break;
                }
                
                ChessPosition newPosition = new ChessPosition(newRow, newCol);
                ChessPiece pieceAtNewPosition = board.getPiece(newPosition);
                
                // Empty square - add move and continue in this direction
                if (pieceAtNewPosition == null) {
                    moves.add(new ChessMove(position, newPosition, null));
                } 
                // Occupied square
                else {
                    // If it's an enemy piece, we can capture it
                    if (pieceAtNewPosition.getTeamColor() != this.pieceColor) {
                        moves.add(new ChessMove(position, newPosition, null));
                    }
                    // Either way, we can't move further in this direction
                    break;
                }
            }
        }
        return moves;
    }

    // Simplify bishop moves using the helper
    private Collection<ChessMove> getBishopMoves(ChessBoard board, ChessPosition position) {
        int[][] directions = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        return getDirectionalMoves(board, position, directions);
    }

    // Simplify rook moves using the helper
    private Collection<ChessMove> getRookMoves(ChessBoard board, ChessPosition position) {
        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        return getDirectionalMoves(board, position, directions);
    }

    private void addPawnPromotionMoves(Collection<ChessMove> moves, ChessPosition start, ChessPosition end) {
        moves.add(new ChessMove(start, end, PieceType.QUEEN));
        moves.add(new ChessMove(start, end, PieceType.ROOK));
        moves.add(new ChessMove(start, end, PieceType.BISHOP));
        moves.add(new ChessMove(start, end, PieceType.KNIGHT));
    }

    private Collection<ChessMove> getKingMoves(ChessBoard board, ChessPosition position) {
        Collection<ChessMove> moves = new ArrayList<>();
        int[][] kingMoves = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1},           {0, 1},
            {1, -1},  {1, 0},  {1, 1}
        };
        
        for (int[] move : kingMoves) {
            int newRow = position.getRow() + move[0];
            int newCol = position.getColumn() + move[1];
            
            if (isValidPosition(newRow, newCol)) {
                ChessPosition newPosition = new ChessPosition(newRow, newCol);
                ChessPiece pieceAtNewPosition = board.getPiece(newPosition);
                
                if (pieceAtNewPosition == null || pieceAtNewPosition.getTeamColor() != this.pieceColor) {
                    moves.add(new ChessMove(position, newPosition, null));
                }
            }
        }
        return moves;
    }

    private Collection<ChessMove> getQueenMoves(ChessBoard board, ChessPosition position) {
        // Queen combines bishop and rook moves
        Collection<ChessMove> moves = new ArrayList<>();
        moves.addAll(getBishopMoves(board, position));
        moves.addAll(getRookMoves(board, position));
        return moves;
    }

    private Collection<ChessMove> getKnightMoves(ChessBoard board, ChessPosition position) {
        Collection<ChessMove> moves = new ArrayList<>();
        int[][] knightMoves = {
            {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
            {1, -2}, {1, 2}, {2, -1}, {2, 1}
        };
        
        for (int[] move : knightMoves) {
            int newRow = position.getRow() + move[0];
            int newCol = position.getColumn() + move[1];
            
            if (isValidPosition(newRow, newCol)) {
                ChessPosition newPosition = new ChessPosition(newRow, newCol);
                ChessPiece pieceAtNewPosition = board.getPiece(newPosition);
                
                if (pieceAtNewPosition == null || pieceAtNewPosition.getTeamColor() != this.pieceColor) {
                    moves.add(new ChessMove(position, newPosition, null));
                }
            }
        }
        return moves;
    }

    private Collection<ChessMove> getPawnMoves(ChessBoard board, ChessPosition position) {
        Collection<ChessMove> moves = new ArrayList<>();
        int direction = (this.pieceColor == ChessGame.TeamColor.WHITE) ? 1 : -1;
        int startingRow = (this.pieceColor == ChessGame.TeamColor.WHITE) ? 2 : 7;
        int promotionRow = (this.pieceColor == ChessGame.TeamColor.WHITE) ? 8 : 1;
        
        // Forward move
        addPawnForwardMoves(board, position, direction, startingRow, promotionRow, moves);
        
        // Capture moves
        addPawnCaptureMoves(board, position, direction, promotionRow, moves);
        
        return moves;
    }

    private void addPawnForwardMoves(ChessBoard board, ChessPosition position, int direction, 
                                    int startingRow, int promotionRow, Collection<ChessMove> moves) {
        int oneForward = position.getRow() + direction;
        if (isValidPosition(oneForward, position.getColumn())) {
            ChessPosition newPosition = new ChessPosition(oneForward, position.getColumn());
            if (board.getPiece(newPosition) == null) {
                // Check for promotion
                if (oneForward == promotionRow) {
                    addPawnPromotionMoves(moves, position, newPosition);
                } else {
                    moves.add(new ChessMove(position, newPosition, null));
                }
                
                // Two square advance if pawn is on starting row
                if (position.getRow() == startingRow) {
                    ChessPosition twoForward = new ChessPosition(oneForward + direction, position.getColumn());
                    if (board.getPiece(twoForward) == null) {
                        moves.add(new ChessMove(position, twoForward, null));
                    }
                }
            }
        }
    }

    private void addPawnCaptureMoves(ChessBoard board, ChessPosition position, int direction, 
                                    int promotionRow, Collection<ChessMove> moves) {
        int oneForward = position.getRow() + direction;
        if (isValidPosition(oneForward, position.getColumn())) {
            for (int colOffset : new int[]{-1, 1}) {
                int newCol = position.getColumn() + colOffset;
                if (isValidPosition(oneForward, newCol)) {
                    ChessPosition capturePosition = new ChessPosition(oneForward, newCol);
                    ChessPiece pieceAtCapture = board.getPiece(capturePosition);
                    
                    if (pieceAtCapture != null && pieceAtCapture.getTeamColor() != this.pieceColor) {
                        if (oneForward == promotionRow) {
                            addPawnPromotionMoves(moves, position, capturePosition);
                        } else {
                            moves.add(new ChessMove(position, capturePosition, null));
                        }
                    }
                }
            }
        }
    }

    // Helper method to check if position is valid
    private boolean isValidPosition(int row, int column) {
        return row >= 1 && row <= 8 && column >= 1 && column <= 8;
    }

    @Override
    public boolean equals(Object ob) {
        if (this == ob) { return true; }
        if (ob == null || getClass() != ob.getClass()) { return false; }
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
