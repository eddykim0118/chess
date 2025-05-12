package chess;

/**
 * A chessboard that can hold and rearrange chess pieces.
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessBoard {
    private ChessPiece[][] squares = new ChessPiece[8][8];
    public ChessBoard() {
        
    }

    /**
     * Adds a chess piece to the chessboard
     *
     * @param position where to add the piece to
     * @param piece    the piece to add
     */
    public void addPiece(ChessPosition position, ChessPiece piece) {
        squares[position.getRow() - 1][position.getColumn() - 1] = piece;
    }

    /**
     * Gets a chess piece on the chessboard
     *
     * @param position The position to get the piece from
     * @return Either the piece at the position, or null if no piece is at that
     * position
     */
    public ChessPiece getPiece(ChessPosition position) {
        return squares[position.getRow() - 1][position.getColumn() - 1];
    }

    /**
     * Sets the board to the default starting board
     * (How the game of chess normally starts)
     */
    public void resetBoard() {
        // Standard starting position in FEN notation
        setPositionFromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }

    private void setPositionFromFEN(String fen) {
        squares = new ChessPiece[8][8];
        String[] parts = fen.split(" ");
        String boardPart = parts[0];
        String[] rows = boardPart.split("/");
        
        for (int row = 0; row < 8; row++) {
            int col = 0;
            for (int i = 0; i < rows[row].length(); i++) {
                char c = rows[row].charAt(i);
                if (Character.isDigit(c)) {
                    col += Character.getNumericValue(c);
                } else {
                    ChessGame.TeamColor color = Character.isUpperCase(c) ? 
                        ChessGame.TeamColor.WHITE : ChessGame.TeamColor.BLACK;
                    ChessPiece.PieceType type = getPieceTypeFromChar(c);
                    addPiece(new ChessPosition(8-row, col+1), new ChessPiece(color, type));
                    col++;
                }
            }
        }
    }

    private ChessPiece.PieceType getPieceTypeFromChar(char c) {
        switch (Character.toLowerCase(c)) {
            case 'k': return ChessPiece.PieceType.KING;
            case 'q': return ChessPiece.PieceType.QUEEN;
            case 'r': return ChessPiece.PieceType.ROOK;
            case 'b': return ChessPiece.PieceType.BISHOP;
            case 'n': return ChessPiece.PieceType.KNIGHT;
            case 'p': return ChessPiece.PieceType.PAWN;
            default: throw new IllegalArgumentException("Unknown piece: " + c);
        }
    }
}
