package com.litl.racer15.gameplugin;

import com.electrotank.electroserver5.extensions.api.PluginApi;
import com.electrotank.electroserver5.extensions.api.value.EsObject;

public class PlayerInfo implements Comparable<PlayerInfo> {

    private String playerName;
    private int score;
    private int callBackId;
    private int x = -1;
    private int y = -1;

    public PlayerInfo(String playerName) {
        this.playerName = playerName;
        score = 0;
        callBackId = -1;
    }
    
    public EsObject toEsObject() {
        EsObject obj = new EsObject();
        obj.setString(PluginConstants.NAME, playerName);
        obj.setInteger(PluginConstants.SCORE, score);
        return obj;
    }
    
    public void userExit(PluginApi api) {
        score = 0;
        cancelCallback(api);
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getScore() {
        return score;
    }

    public int addToScore(int newPoints) {
        score += newPoints;
        return score;
    }

    public int getCallBackId() {
        return callBackId;
    }

    public void setCallBackId(int callBackId) {
        this.callBackId = callBackId;
    }

    public void cancelCallback(PluginApi api) {
        if (callBackId != -1) {
            api.cancelScheduledExecution(callBackId);
            callBackId = -1;
        }
    }

    public int compareTo( PlayerInfo playerInfo  ) {
        return score - playerInfo.getScore();
    }

}
