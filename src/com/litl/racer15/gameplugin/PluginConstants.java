package com.litl.racer15.gameplugin;

public class PluginConstants {

    // actions 
    public static final String ACTION          = "a";
    public static final String ADD_PLAYER      = "au";
    public static final String DIG_HERE        = "d";
    public static final String DONE_DIGGING    = "dd";
    public static final String ERROR           = "err";
    public static final String GAME_OVER       = "go";
    public static final String INIT_ME         = "i";
    public static final String POSITION_UPDATE = "pu";
    public static final String REMOVE_PLAYER   = "ru";
    public static final String START_COUNTDOWN = "s";
    public static final String STOP_COUNTDOWN  = "sc";
    public static final String START_GAME      = "sg";
    public static final String PLAYER_LIST     = "ul";
    
    // parameters
    public static final String COUNTDOWN_LEFT  = "cs";
    public static final String ITEM_FOUND      = "f";
    public static final String GAME_STATE      = "gs";
    public static final String ITEM_ID         = "id";
    public static final String NAME            = "n";
    public static final String SCORE           = "s";
    public static final String SUCCESS         = "suc";
    public static final String TIME_STAMP      = "tm";
    public static final String X               = "x";
    public static final String Y               = "y";    
    
    // error messages
    public static final String ALREADY_DIGGING  = "AlreadyDigging";
    public static final String SPOT_ALREADY_DUG = "SpotAlreadyDug";
    public static final String GAME_IS_OVER     = "GameIsOver";
    
    // game flow constants
    public static final int MAXIMUM_PLAYERS     = 4;
    public static final int MINIMUM_PLAYERS     = 2;    
    public static final int COUNTDOWN_SECONDS   = 5;    
    
    // other constants
    public static final int DURATION_MS        = 2000;   // 2 seconds
    public static final int BOARD_WIDTH        = 800;
    public static final int BOARD_HEIGHT       = 600;
    public static final int NUM_ROWS           = 12;
    public static final int NUM_COLS           = 16;
    public static final int POINTS_TO_WIN      = 4000;  
}
