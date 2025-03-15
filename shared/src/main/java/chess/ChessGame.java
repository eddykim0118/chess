package chess;

import java.util.Collection;
import java.util.HashSet;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {
    private ChessBoard board;
    private TeamColor teamTurn;

    public ChessGame() {
        this.board = new ChessBoard();
        this.board.resetBoard();
        this.teamTurn = TeamColor.WHITE;
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return teamTurn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        this.teamTurn = team;
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    /**
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);
        if (piece == null) {
            return null;
        }

        Collection<ChessMove> possibleMoves = piece.pieceMoves(board, startPosition);
        Collection<ChessMove> validMoves = new HashSet<>();

        for (ChessMove move : possibleMoves) {
            ChessBoard tempBoard = cloneBoard();
            tempBoard.addPiece(move.getEndPosition(), piece);
            tempBoard.removePiece(startPosition);

            if (!isInCheck(piece.getTeamColor(), tempBoard)) {
                validMoves.add(move);
            }
        }
        return validMoves;
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to preform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        ChessPosition start = move.getStartPosition();
        ChessPiece piece = board.getPiece(start);

        if (piece == null || piece.getTeamColor() != teamTurn) {
            throw new InvalidMoveException("Invalid move: No Piece or wrong team's turn");
        }

        Collection<ChessMove> validMoves = validMoves(start);
        if (validMoves == null || !validMoves.contains(move)) {
            throw new InvalidMoveException("Invalid move: Not a valid move");
        }

        ChessPosition end = move.getEndPosition();
        
        // Make the move
        board.removePiece(start);
        
        // Handle pawn promotion
        if (piece.getPieceType() == ChessPiece.PieceType.PAWN && 
            (end.getRow() == 8 || end.getRow() == 1) && 
            move.getPromotionPiece() != null) {
            board.addPiece(end, new ChessPiece(piece.getTeamColor(), move.getPromotionPiece()));
        } else {
            board.addPiece(end, piece);
        }

        // Check if the move puts the king in check
        if (isInCheck(piece.getTeamColor())) {
            // Revert the move
            board.addPiece(start, piece);
            board.removePiece(end);
            throw new InvalidMoveException("Invalid move: This move leaves the king in check.");
        }

        teamTurn = (teamTurn == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
    }

    

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        return isInCheck(teamColor, board);
    }

    private boolean isInCheck(TeamColor teamColor, ChessBoard board) {
        ChessPosition kingPosition = findKingPosition(teamColor, board);
        return isPositionUnderAttack(kingPosition, teamColor, board);
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        return isInCheck(teamColor) && !canMoveWithoutCheck(teamColor, board);
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        return !isInCheck(teamColor) && !canMoveWithoutCheck(teamColor, board);
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.board = board;
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return board;
    }


private ChessPosition findKingPosition(TeamColor teamColor, ChessBoard board) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition pos = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(pos);
                if (piece != null && piece.getPieceType() == ChessPiece.PieceType.KING &&
                        piece.getTeamColor() == teamColor) {
                    return pos;
                }
            }
        }
        return null;
    }

    private boolean isPositionUnderAttack(ChessPosition position, TeamColor teamColor, ChessBoard board) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition pos = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(pos);
                
                // Skip empty squares or pieces of the same team
                if (piece == null || piece.getTeamColor() == teamColor) {
                    continue;
                }
                
                // Check if this opponent piece can attack the position
                if (canPieceAttackPosition(piece, board, pos, position)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canPieceAttackPosition(ChessPiece piece, ChessBoard board, 
                                     ChessPosition piecePosition, ChessPosition targetPosition) {
        Collection<ChessMove> moves = piece.pieceMoves(board, piecePosition);
        for (ChessMove move : moves) {
            if (move.getEndPosition().equals(targetPosition)) {
                return true;
            }
        }
        return false;
    }

    private ChessBoard cloneBoard() {
        return cloneBoard(this.board);
    }

    private ChessBoard cloneBoard(ChessBoard boardToClone) {
        ChessBoard newBoard = new ChessBoard();
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition pos = new ChessPosition(row, col);
                ChessPiece piece = boardToClone.getPiece(pos);
                if (piece != null) {
                    newBoard.addPiece(pos, new ChessPiece(piece.getTeamColor(), piece.getPieceType()));
                } else {
                    newBoard.removePiece(pos);
                }
            }
        }
        return newBoard;
    }

    private boolean canMoveWithoutCheck(TeamColor teamColor, ChessBoard boardToCheck) {
        // Find king position
        ChessPosition kingPosition = findKingPosition(teamColor, boardToCheck);
        if (kingPosition == null) {
            return false;
        }
        
        // Check if any piece can make a valid move
        return canAnyPieceMakeValidMove(teamColor, boardToCheck);
    }

    // New helper method to reduce nesting
    private boolean canAnyPieceMakeValidMove(TeamColor teamColor, ChessBoard boardToCheck) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = boardToCheck.getPiece(position);
                
                if (piece == null || piece.getTeamColor() != teamColor) {
                    continue;
                }
                
                // Get potential moves for this piece
                if (canPieceMakeValidMove(piece, position, teamColor, boardToCheck)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Additional helper method to further reduce nesting
    private boolean canPieceMakeValidMove(ChessPiece piece, ChessPosition position, 
                                       TeamColor teamColor, ChessBoard boardToCheck) {
        Collection<ChessMove> pieceMoves = piece.pieceMoves(boardToCheck, position);
        
        for (ChessMove move : pieceMoves) {
            // Try the move on a temporary board
            ChessBoard tempBoard = cloneBoard(boardToCheck);
            tempBoard.addPiece(move.getEndPosition(), piece);
            tempBoard.removePiece(position);
            
            // Check if king is still in check after this move
            if (!isInCheck(teamColor, tempBoard)) {
                return true; // Found a valid move
            }
        }
        return false;
    }
}