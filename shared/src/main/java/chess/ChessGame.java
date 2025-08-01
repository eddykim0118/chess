package chess;

import java.util.*;
import java.util.Objects;
import chess.InvalidMoveException;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {
    private TeamColor currentTeam;
    private ChessBoard gameBoard;

    public ChessGame() {
        this.currentTeam = TeamColor.WHITE;
        this.gameBoard = new ChessBoard();
        this.gameBoard.resetBoard();
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return currentTeam;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        this.currentTeam = team;
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
        ChessPiece piece = gameBoard.getPiece(startPosition);
        if (piece == null) {
            return null;
        }

        Collection<ChessMove> pieceMoves = piece.pieceMoves(gameBoard, startPosition);
        Collection<ChessMove> validMoves = new ArrayList<>();

        for (ChessMove move : pieceMoves) {
            if (!wouldLeaveKingInCheck(move, piece.getTeamColor())) {
                validMoves.add(move);
            }
        }

        return validMoves;
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        ChessPiece piece = gameBoard.getPiece(move.getStartPosition());

        if (piece == null) {
            throw new InvalidMoveException("No piece at starting position");
        }

        if (piece.getTeamColor() != currentTeam) {
            throw new InvalidMoveException("Not your turn");
        }

        Collection<ChessMove> validMoves = validMoves(move.getStartPosition());
        if (validMoves == null || !validMoves.contains(move)) {
            throw new InvalidMoveException("Invalid move");
        }

        gameBoard.addPiece(move.getStartPosition(), null);

        if (move.getPromotionPiece() != null) {
            ChessPiece promotedPiece = new ChessPiece(piece.getTeamColor(), move.getPromotionPiece());
            gameBoard.addPiece(move.getEndPosition(), promotedPiece);
        } else {
            gameBoard.addPiece(move.getEndPosition(), piece);
        }

        // Switch turns
        currentTeam = (currentTeam == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        ChessPosition kingPosition = findKing(teamColor);
        if (kingPosition == null) {
            return false;
        }

        TeamColor oppositeTeam = (teamColor == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;

        // Extract nested logic into helper method to reduce nesting
        return isKingUnderAttack(kingPosition, oppositeTeam);
    }

    /**
     * Helper method to check if king is under attack by any opponent piece
     */
    private boolean isKingUnderAttack(ChessPosition kingPosition, TeamColor attackingTeam) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = gameBoard.getPiece(position);

                if (piece == null || piece.getTeamColor() != attackingTeam) {
                    continue;
                }

                if (canPieceAttackKing(piece, position, kingPosition)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Helper method to check if a specific piece can attack the king
     */
    private boolean canPieceAttackKing(ChessPiece piece, ChessPosition piecePosition, ChessPosition kingPosition) {
        Collection<ChessMove> moves = piece.pieceMoves(gameBoard, piecePosition);
        for (ChessMove move : moves) {
            if (move.getEndPosition().equals(kingPosition)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        if (!isInCheck(teamColor)) {
            return false;
        }

        return hasNoValidMoves(teamColor);
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves while not in check.
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        if (isInCheck(teamColor)) {
            return false;
        }

        return hasNoValidMoves(teamColor);
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.gameBoard = board;
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return gameBoard;
    }

    // Helper Functions
    private boolean wouldLeaveKingInCheck(ChessMove move, TeamColor teamColor) {
        ChessPiece originalPiece = gameBoard.getPiece(move.getStartPosition());
        ChessPiece capturedPiece = gameBoard.getPiece(move.getEndPosition());

        gameBoard.addPiece(move.getStartPosition(), null);
        if (move.getPromotionPiece() != null) {
            ChessPiece promotedPiece = new ChessPiece(teamColor, move.getPromotionPiece());
            gameBoard.addPiece(move.getEndPosition(), promotedPiece);
        } else {
            gameBoard.addPiece(move.getEndPosition(), originalPiece);
        }

        boolean inCheck = isInCheck(teamColor);

        gameBoard.addPiece(move.getStartPosition(), originalPiece);
        gameBoard.addPiece(move.getEndPosition(), capturedPiece);

        return inCheck;
    }

    private ChessPosition findKing(TeamColor teamColor) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = gameBoard.getPiece(position);

                if (piece != null &&
                        piece.getTeamColor() == teamColor &&
                        piece.getPieceType() == ChessPiece.PieceType.KING) {
                    return position;
                }
            }
        }
        return null;
    }

    private boolean hasNoValidMoves(TeamColor teamColor) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = gameBoard.getPiece(position);

                if (piece != null && piece.getTeamColor() == teamColor) {
                    Collection<ChessMove> moves = validMoves(position);
                    if (moves != null && !moves.isEmpty()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    public boolean equals(Object ob) {
        if (this == ob) { return true; }
        if (ob == null || getClass() != ob.getClass()) { return false; }

        ChessGame chessGame = (ChessGame) ob;
        return currentTeam == chessGame.currentTeam &&
                Objects.equals(gameBoard, chessGame.gameBoard);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentTeam, gameBoard);
    }
}