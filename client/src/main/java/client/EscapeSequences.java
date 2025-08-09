package client;

/**
 * This class contains constants and functions relating to ANSI Escape Sequences that are useful in the Client display
 */
public class EscapeSequences {

    private static final String UNICODE_ESCAPE = "\u001b";
    private static final String ANSI_ESCAPE = "\033";

    public static final String ERASE_SCREEN = UNICODE_ESCAPE + "[2J";
    public static final String ERASE_LINE = UNICODE_ESCAPE + "[2K";

    public static final String SET_TEXT_COLOR_BLACK = UNICODE_ESCAPE + "[30m";
    public static final String SET_TEXT_COLOR_RED = UNICODE_ESCAPE + "[31m";
    public static final String SET_TEXT_COLOR_GREEN = UNICODE_ESCAPE + "[32m";
    public static final String SET_TEXT_COLOR_YELLOW = UNICODE_ESCAPE + "[33m";
    public static final String SET_TEXT_COLOR_BLUE = UNICODE_ESCAPE + "[34m";
    public static final String SET_TEXT_COLOR_MAGENTA = UNICODE_ESCAPE + "[35m";
    public static final String SET_TEXT_COLOR_CYAN = UNICODE_ESCAPE + "[36m";
    public static final String SET_TEXT_COLOR_WHITE = UNICODE_ESCAPE + "[37m";
    public static final String RESET_TEXT_COLOR = UNICODE_ESCAPE + "[0m";

    public static final String SET_BG_COLOR_BLACK = UNICODE_ESCAPE + "[40m";
    public static final String SET_BG_COLOR_RED = UNICODE_ESCAPE + "[41m";
    public static final String SET_BG_COLOR_GREEN = UNICODE_ESCAPE + "[42m";
    public static final String SET_BG_COLOR_YELLOW = UNICODE_ESCAPE + "[43m";
    public static final String SET_BG_COLOR_BLUE = UNICODE_ESCAPE + "[44m";
    public static final String SET_BG_COLOR_MAGENTA = UNICODE_ESCAPE + "[45m";
    public static final String SET_BG_COLOR_CYAN = UNICODE_ESCAPE + "[46m";
    public static final String SET_BG_COLOR_WHITE = UNICODE_ESCAPE + "[47m";
    public static final String RESET_BG_COLOR = UNICODE_ESCAPE + "[0m";

    public static final String SET_TEXT_BOLD = UNICODE_ESCAPE + "[1m";
    public static final String SET_TEXT_FAINT = UNICODE_ESCAPE + "[2m";
    public static final String RESET_TEXT_BOLD_FAINT = UNICODE_ESCAPE + "[22m";
    public static final String SET_TEXT_ITALIC = UNICODE_ESCAPE + "[3m";
    public static final String RESET_TEXT_ITALIC = UNICODE_ESCAPE + "[23m";
    public static final String SET_TEXT_UNDERLINE = UNICODE_ESCAPE + "[4m";
    public static final String RESET_TEXT_UNDERLINE = UNICODE_ESCAPE + "[24m";
}