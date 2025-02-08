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
    
        switch (this.type) {
            case KNIGHT:
                // Define the 8 possible L-shaped moves a knight can make
                int[][] knightMoves = {
                    {-2, -1}, {-2, 1},  // Two up, one left/right
                    {2, -1}, {2, 1},    // Two down, one left/right
                    {-1, -2}, {1, -2},  // One up/down, two left
                    {-1, 2}, {1, 2}     // One up/down, two right
                };
                
                for (int[] move : knightMoves) {
                    int newRow = myPosition.getRow() + move[0];
                    int newCol = myPosition.getColumn() + move[1];
                    
                    // Check if new position is on board
                    if (newRow >= 1 && newRow <= 8 && newCol >= 1 && newCol <= 8) {
                        ChessPosition newPosition = new ChessPosition(newRow, newCol);
                        ChessPiece pieceAtNewPosition = board.getPiece(newPosition);
                        
                        // Add move if square is empty or contains enemy piece
                        if (pieceAtNewPosition == null || pieceAtNewPosition.getTeamColor() != this.pieceColor) {
                            validMoves.add(new ChessMove(myPosition, newPosition, null));
                        }
                    }
                }
                break;
                
            case PAWN:
                int direction = (this.pieceColor == ChessGame.TeamColor.WHITE) ? 1 : -1;
                int startingRow = (this.pieceColor == ChessGame.TeamColor.WHITE) ? 2 : 7;
                int promotionRow = (this.pieceColor == ChessGame.TeamColor.WHITE) ? 8 : 1;
                
                // Forward move
                int oneForward = myPosition.getRow() + direction;
                if (oneForward >= 1 && oneForward <= 8) {
                    ChessPosition newPosition = new ChessPosition(oneForward, myPosition.getColumn());
                    if (board.getPiece(newPosition) == null) {
                        // Check if pawn reaches promotion row
                        if (oneForward == promotionRow) {
                            addPawnPromotionMoves(validMoves, myPosition, newPosition);
                        } else {
                            validMoves.add(new ChessMove(myPosition, newPosition, null));
                        }
                        
                        // Two square advance if pawn is on starting row
                        if (myPosition.getRow() == startingRow) {
                            ChessPosition twoForward = new ChessPosition(oneForward + direction, myPosition.getColumn());
                            if (board.getPiece(twoForward) == null) {
                                validMoves.add(new ChessMove(myPosition, twoForward, null));
                            }
                        }
                    }
                }
                
                // Capture moves
                for (int colOffset : new int[]{-1, 1}) {
                    if (myPosition.getColumn() + colOffset >= 1 && myPosition.getColumn() + colOffset <= 8) {
                        ChessPosition capturePosition = new ChessPosition(oneForward, myPosition.getColumn() + colOffset);
                        ChessPiece pieceAtCapture = board.getPiece(capturePosition);
                        
                        if (pieceAtCapture != null && pieceAtCapture.getTeamColor() != this.pieceColor) {
                            if (oneForward == promotionRow) {
                                addPawnPromotionMoves(validMoves, myPosition, capturePosition);
                            } else {
                                validMoves.add(new ChessMove(myPosition, capturePosition, null));
                            }
                        }
                    }
                }
                break;
                
            case QUEEN:
                // Queen combines rook and bishop moves
                validMoves.addAll(getRookMoves(board, myPosition));
                validMoves.addAll(getBishopMoves(board, myPosition));
                break;
                
            case KING:
                // King can move one square in any direction
                int[][] kingMoves = {
                    {-1, -1}, {-1, 0}, {-1, 1},
                    {0, -1},           {0, 1},
                    {1, -1},  {1, 0},  {1, 1}
                };
                
                for (int[] move : kingMoves) {
                    int newRow = myPosition.getRow() + move[0];
                    int newCol = myPosition.getColumn() + move[1];
                    
                    if (newRow >= 1 && newRow <= 8 && newCol >= 1 && newCol <= 8) {
                        ChessPosition newPosition = new ChessPosition(newRow, newCol);
                        ChessPiece pieceAtNewPosition = board.getPiece(newPosition);
                        
                        if (pieceAtNewPosition == null || pieceAtNewPosition.getTeamColor() != this.pieceColor) {
                            validMoves.add(new ChessMove(myPosition, newPosition, null));
                        }
                    }
                }
                break;
                
            case BISHOP:
                validMoves.addAll(getBishopMoves(board, myPosition));
                break;
                
            case ROOK:
                validMoves.addAll(getRookMoves(board, myPosition));
                break;
        }
        
        return validMoves;
    }
    private Collection<ChessMove> getBishopMoves(ChessBoard board, ChessPosition myPosition) {
        Collection<ChessMove> moves = new ArrayList<>();
        int[][] directions = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        
        for (int[] direction : directions) {
            int newRow = myPosition.getRow();
            int newCol = myPosition.getColumn();
            
            while (true) {
                newRow += direction[0];
                newCol += direction[1];
                
                if (newRow < 1 || newRow > 8 || newCol < 1 || newCol > 8) {
                    break;
                }
                
                ChessPosition newPosition = new ChessPosition(newRow, newCol);
                ChessPiece pieceAtNewPosition = board.getPiece(newPosition);
                
                if (pieceAtNewPosition == null) {
                    moves.add(new ChessMove(myPosition, newPosition, null));
                } else {
                    if (pieceAtNewPosition.getTeamColor() != this.pieceColor) {
                        moves.add(new ChessMove(myPosition, newPosition, null));
                    }
                    break;
                }
            }
        }
        return moves;
    }

    private Collection<ChessMove> getRookMoves(ChessBoard board, ChessPosition myPosition) {
        Collection<ChessMove> moves = new ArrayList<>();
        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        
        for (int[] direction : directions) {
            int newRow = myPosition.getRow();
            int newCol = myPosition.getColumn();
            
            while (true) {
                newRow += direction[0];
                newCol += direction[1];
                
                if (newRow < 1 || newRow > 8 || newCol < 1 || newCol > 8) {
                    break;
                }
                
                ChessPosition newPosition = new ChessPosition(newRow, newCol);
                ChessPiece pieceAtNewPosition = board.getPiece(newPosition);
                
                if (pieceAtNewPosition == null) {
                    moves.add(new ChessMove(myPosition, newPosition, null));
                } else {
                    if (pieceAtNewPosition.getTeamColor() != this.pieceColor) {
                        moves.add(new ChessMove(myPosition, newPosition, null));
                    }
                    break;
                }
            }
        }
        return moves;
    }

    private void addPawnPromotionMoves(Collection<ChessMove> moves, ChessPosition start, ChessPosition end) {
        moves.add(new ChessMove(start, end, PieceType.QUEEN));
        moves.add(new ChessMove(start, end, PieceType.ROOK));
        moves.add(new ChessMove(start, end, PieceType.BISHOP));
        moves.add(new ChessMove(start, end, PieceType.KNIGHT));
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
