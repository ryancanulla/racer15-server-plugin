package com.litl.racer15.gameplugin;

public enum GameState {

    WaitingForPlayers ("wp"),
    CountingDown ("cd"),
    InPlay ("ip"),
    GameOver ("go");

    private final String state;

    private GameState( String state ) {
        this.state = state;
    }
    
    public String getState() {
        return state;
    }
}
