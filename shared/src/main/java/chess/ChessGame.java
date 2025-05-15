package chess;

import java.util.Collection;
import java.util.ArrayList;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {
    private ChessBoard board;
    private TeamColor currentTurn = TeamColor.WHITE;
    private ChessMove lastMove = null;

    public ChessGame() {

    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return currentTurn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        currentTurn = team;
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
        if (piece == null) return null;

        Collection<ChessMove> potentialMoves;

        if (piece.getPieceType() == ChessPiece.PieceType.PAWN) {
            potentialMoves = piece.pieceMoves(board, startPosition);
        } else {
            potentialMoves = piece.pieceMoves(board, startPosition);
        }

        Collection<ChessMove> validMoves = new ArrayList<>();
        for (ChessMove move : potentialMoves) {
            if (!wouldBeInCheckAfterMove(piece.getTeamColor(), move)) {
                validMoves.add(move);
            }
        }

        return validMoves;
    }

    /**
     * Helper method to chec if making a move would leave the king in check
     */
    private boolean wouldBeInCheckAfterMove(TeamColor teamColor, ChessMove move) {
        ChessBoard tempBoard = new ChessBoard();

        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition pos = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(pos);
                if (piece != null) {
                    tempBoard.addPiece(pos, piece);
                }
            }
        }

        ChessPiece movingPiece = tempBoard.getPiece(move.getStartPosition());

        if (move.getPromotionPiece() != null) {
            movingPiece = new ChessPiece(movingPiece.getTeamColor(), move.getPromotionPiece());
        }

        tempBoard.addPiece(move.getStartPosition(), null);
        tempBoard.addPiece(move.getEndPosition(), movingPiece);

        return isInCheckOnBoard(teamColor, tempBoard);
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        ChessPiece movingPiece = board.getPiece(move.getStartPosition());

        if (movingPiece == null) {
            throw new InvalidMoveException("No piece at the starting position");
        }

        if (movingPiece.getTeamColor() != currentTurn) {
            throw new InvalidMoveException("It's not your turn");
        }

        Collection<ChessMove> validMovesList = validMoves(move.getStartPosition());
        if (!validMovesList.contains(move)) {
            throw new InvalidMoveException("Invalid Move");
        }

        if (move.getPromotionPiece() != null) {
            movingPiece = new ChessPiece(movingPiece.getTeamColor(), move.getPromotionPiece());
        }

        board.addPiece(move.getStartPosition(), null);
        board.addPiece(move.getEndPosition(), movingPiece);

        lastMove = move;

        currentTurn = (currentTurn == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
        
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        return isInCheckOnBoard(teamColor, board);
    }

    private boolean isInCheckOnBoard(TeamColor teamColor, ChessBoard checkBoard) {
        
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        throw new RuntimeException("Not implemented");
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

    /**
     * Gets the last move made in the game
     *
     * @return the last move
     */
    public ChessMove getLastMove() {
        return lastMove;
    }
}
