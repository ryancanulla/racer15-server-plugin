package com.litl.racer15.gameplugin;

import com.electrotank.electroserver5.extensions.BasePlugin;
import com.electrotank.electroserver5.extensions.ChainAction;
import com.electrotank.electroserver5.extensions.api.ScheduledCallback;
import com.electrotank.electroserver5.extensions.api.value.EsObject;
import com.electrotank.electroserver5.extensions.api.value.EsObjectRO;
import com.electrotank.electroserver5.extensions.api.value.UserEnterContext;
import java.util.AbstractMap;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Racer15Plugin extends BasePlugin {
    // variables
    private AbstractMap<String, PlayerInfo> playerInfoMap;
    private AbstractQueue<PlayerInfo> delayQueue;
    private GameState gameState;
    private int callbackId = -1;
    private long startTime = 0;

    @Override
    public void init(EsObjectRO ignored) {
        playerInfoMap = new ConcurrentHashMap<String, PlayerInfo>();
        delayQueue = new ConcurrentLinkedQueue<PlayerInfo>();
        gameState = GameState.WaitingForPlayers;
    }

    @Override
    public ChainAction userEnter(UserEnterContext context) {
        String playerName = context.getUserName();
        boolean ok = okForPlayerToEnter();
        if (ok) {
            getApi().getLogger().debug("userEnter: " + playerName);
            return ChainAction.OkAndContinue;
        } else {
            getApi().getLogger().debug("Game refused to let " + playerName + " join.");
            return ChainAction.Fail;
        }
    }

    private int getCountdownLeft() {
        long now = System.currentTimeMillis();
        long elapsedMillis = now - startTime;
        long millisLeft = PluginConstants.COUNTDOWN_SECONDS * 1000 - elapsedMillis;
        return (int) (millisLeft / 1000);
    }

    private EsObject[] getScoreObjectList() {
        PlayerInfo[] playerScores = null;
        if (playerInfoMap.size() > 0) {
            playerScores = 
                    new PlayerInfo[playerInfoMap.size()];
            int index = 0;
            for (PlayerInfo pInfo : playerInfoMap.values()) {
                playerScores[index] = pInfo;
                index++;
            }
        } else {
            return null;
        }
        List<PlayerInfo> sortedScores = Arrays.asList( playerScores );
        Collections.sort( sortedScores );

        EsObject[] list = new EsObject[playerScores.length];

        int index = playerScores.length - 1;
        for ( PlayerInfo playerInfo : sortedScores ) {
            list[index] = playerInfo.toEsObject();
            index--;
        }
        
        return list;
    }

    private synchronized boolean okForPlayerToEnter() {
        if (gameState != GameState.WaitingForPlayers
                && gameState != GameState.CountingDown) {
            return false;
        }
        int numPlayers = getApi().getUsers().size();
        if (numPlayers > PluginConstants.MAXIMUM_PLAYERS) {
            getApi().setGameLockState(true);
            // numPlayers includes the player who is trying to enter
            return false;
        } else if (numPlayers == PluginConstants.MAXIMUM_PLAYERS) {
            getApi().setGameLockState(true);
            return true;
        } else {
            return true;
        }
    }

    @Override
    public void request(String playerName, EsObjectRO requestParameters) {
        EsObject messageIn = new EsObject();
        messageIn.addAll(requestParameters);
        getApi().getLogger().debug(playerName + " requests: " + messageIn.toString());

        if (gameState == GameState.GameOver) {
            sendErrorMessage(playerName, PluginConstants.GAME_IS_OVER);
            return;
        }

        String action = messageIn.getString(PluginConstants.ACTION);

        if (action.equals(PluginConstants.INIT_ME)) {
            handlePlayerInitRequest(playerName);
        } else if (action.equals(PluginConstants.POSITION_UPDATE)) {
            relayMessage(playerName, messageIn);
        }
    }

    @Override
    public void userExit(String playerName) {
        if (playerInfoMap.containsKey(playerName)) {
            if (gameState == GameState.InPlay || gameState == GameState.GameOver) {
                PlayerInfo pInfo = playerInfoMap.get(playerName);
                pInfo.userExit(getApi());
            } else {
                playerInfoMap.remove(playerName);
            }
        }
        EsObject message = new EsObject();
        message.setString(PluginConstants.ACTION, PluginConstants.REMOVE_PLAYER);
        message.setString(PluginConstants.NAME, playerName);
        sendAndLog("userExit", message);

        int numUsersInRoom = getApi().getUsers().size();
        getApi().getLogger().debug("numUsersInRoom: " + numUsersInRoom);
        if (numUsersInRoom <= PluginConstants.MINIMUM_PLAYERS) {
            if (gameState == GameState.CountingDown) {
                getApi().getLogger().debug("resetCountdown called");
                resetCountdown();
            } else if (gameState == GameState.InPlay) {
                getApi().getLogger().debug("endGame called");
                endGame();
            }
        }
    }

    @Override
    public void destroy() {
        while (!delayQueue.isEmpty()) {
            PlayerInfo pInfo = delayQueue.poll();
            if (pInfo != null) {
                pInfo.cancelCallback(getApi());
            }
        }
        getApi().getLogger().debug("room destroyed");
    }

    private synchronized EsObject[] getFullPlayerList() {
        EsObject[] list = new EsObject[playerInfoMap.size()];
        int ptr = 0;
        for (PlayerInfo pInfo : playerInfoMap.values()) {
            list[ptr] = pInfo.toEsObject();
            ptr++;
        }
        return list;
    }


    private synchronized void handlePlayerInitRequest(String playerName) {
        EsObject message2 = new EsObject();
        message2.setString(PluginConstants.ACTION, PluginConstants.ADD_PLAYER);
        message2.setString(PluginConstants.NAME, playerName);
        sendAndLog("addUser", message2);

        // add the new user to the user list
        playerInfoMap.put(playerName, new PlayerInfo(playerName));

        // send the user the full user list
        EsObject message = new EsObject();
        message.setString(PluginConstants.ACTION, PluginConstants.PLAYER_LIST);
        EsObject[] list = getFullPlayerList();
        message.setEsObjectArray(PluginConstants.PLAYER_LIST, list);
        message.setString(PluginConstants.GAME_STATE, gameState.getState());
        if (gameState == GameState.CountingDown) {
            message.setInteger(PluginConstants.COUNTDOWN_LEFT, getCountdownLeft());
        }
        getApi().sendPluginMessageToUser(playerName, message);
        getApi().getLogger().debug("Message sent to " + playerName + ": " + message.toString());

        switch (gameState) {
            case WaitingForPlayers:
                if (playerInfoMap.size() >= PluginConstants.MINIMUM_PLAYERS) {
                    startCountdown();
                }
                break;
            case CountingDown:
                startLateJoinerCountdown(playerName);
                if (playerInfoMap.size() >= PluginConstants.MAXIMUM_PLAYERS) {
                    stopCountdown();    // this will start the game
                }
                break;
        }

    }

    private void sendErrorMessage(String playerName, String error) {
        EsObject message = new EsObject();
        message.setString(PluginConstants.ACTION, PluginConstants.ERROR);
        message.setString(PluginConstants.ERROR, error);
        getApi().sendPluginMessageToUser(playerName, message);
        getApi().getLogger().debug("Message sent to " + playerName + ": " + message.toString());
    }

    private synchronized void tickQueue() {
        if (delayQueue.isEmpty()) {
            return;
        }
        try {
            PlayerInfo pInfo = delayQueue.poll();
            if (pInfo != null) {
                //handleDiggingFinished(pInfo);
            }
        } catch (Exception exception) {
            getApi().getLogger().error("Exception while running tickQueue", exception);
        }
    }

    private void relayMessage(String playerName, EsObject messageIn) {
        messageIn.setString(PluginConstants.NAME, playerName);
        messageIn.setString(PluginConstants.TIME_STAMP, String.valueOf(System.currentTimeMillis()));
        sendAndLog("relayMessage", messageIn);
    }
    
    private void sendToOneAndLog(String fromMethod, String playerName, EsObject message) {
        getApi().sendPluginMessageToUser(playerName, message);
        getApi().getLogger().debug(fromMethod + " to " + playerName + ": " + message.toString());
    }

    private void sendAndLog(String fromMethod, EsObject message) {
        List<String> initializedPlayers = new ArrayList<String>();
        for (PlayerInfo pInfo : playerInfoMap.values()) {
            initializedPlayers.add(pInfo.getPlayerName());
        }

        if (initializedPlayers.size() < 1) {
            return;     // nobody to send the message to
        }

        getApi().sendPluginMessageToUsers(initializedPlayers, message);
        getApi().getLogger().debug(fromMethod + ": " + message.toString());
    }
    
    private void startLateJoinerCountdown(String playerName) {
        if (gameState != GameState.CountingDown) {
            return;
        }
        EsObject message = new EsObject();
        message.setString(PluginConstants.ACTION, PluginConstants.START_COUNTDOWN);
        message.setInteger(PluginConstants.COUNTDOWN_LEFT, getCountdownLeft());
        sendToOneAndLog("DiggingPlugin2.startLateJoinerCountdown", playerName, message);
    }

    /**
     * Countdown Classes
     */
    private void startCountdown() {
        if (gameState == GameState.CountingDown) {
            return;
        }
        gameState = GameState.CountingDown;
        EsObject message = new EsObject();
        message.setString(PluginConstants.ACTION, PluginConstants.START_COUNTDOWN);
        message.setInteger(PluginConstants.COUNTDOWN_LEFT, PluginConstants.COUNTDOWN_SECONDS);
        sendAndLog("DiggingPlugin2.startCountdown", message);
        setCountdownCallback(PluginConstants.COUNTDOWN_SECONDS);
    }

    private void resetCountdown() {
        getApi().cancelScheduledExecution(callbackId);
        EsObject message = new EsObject();
        message.setString(PluginConstants.ACTION, PluginConstants.STOP_COUNTDOWN);
        message.setString(PluginConstants.GAME_STATE, GameState.WaitingForPlayers.getState());
        sendAndLog("DiggingPlugin2.resetCountdown", message);
        gameState = GameState.WaitingForPlayers;
        getApi().setGameLockState(false);
    }

    private void stopCountdown() {
        getApi().cancelScheduledExecution(callbackId);
        EsObject message = new EsObject();
        message.setString(PluginConstants.ACTION, PluginConstants.STOP_COUNTDOWN);
        sendAndLog("DiggingPlugin2.stopCountdown", message);

        if (playerInfoMap.size() >= PluginConstants.MINIMUM_PLAYERS) {
            getApi().setGameLockState(true);
            startGame();
        } else {
            gameState = GameState.WaitingForPlayers;
            getApi().setGameLockState(false);
        }
    }

    private void startGame() {
        gameState = GameState.InPlay;
        EsObject message = new EsObject();
        message.setString(PluginConstants.ACTION, PluginConstants.START_GAME);
        sendAndLog("DiggingPlugin2.startGame", message);
    }

    private void endGame() {
        gameState = GameState.GameOver;
        EsObject message = new EsObject();
        message.setString(PluginConstants.ACTION, PluginConstants.GAME_OVER);
        message.setBoolean(PluginConstants.SUCCESS, isTerminal());
        EsObject[] scoreObjectList = getScoreObjectList();
        if (scoreObjectList != null) {
            message.setEsObjectArray(PluginConstants.PLAYER_LIST, getScoreObjectList());
        } else {
            message.setBoolean(PluginConstants.SUCCESS, false);
        }
        sendAndLog("DiggingPlugin2.startGame", message);
    }

    private void setCountdownCallback(int seconds) {
        getApi().cancelScheduledExecution(callbackId);
        startTime = System.currentTimeMillis();
        callbackId = getApi().scheduleExecution(seconds * 1000,
                1,
                new ScheduledCallback() {

                    public void scheduledCallback() {
                        stopCountdown();
                    }
                });
    }

    private boolean isTerminal() {
        for (PlayerInfo pInfo : playerInfoMap.values()) {
            if (pInfo.getScore() >= PluginConstants.POINTS_TO_WIN) {
                return true;
            }
        }

        return false;
    }
}
