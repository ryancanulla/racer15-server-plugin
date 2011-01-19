package com.litl.racer15.gamemanager;

import com.electrotank.electroserver5.extensions.api.value.EsObject;
import com.electrotank.electroserver5.extensions.api.value.EsObjectRO;
import com.electrotank.electroserver5.extensions.api.value.ExtensionComponentConfiguration;
import com.electrotank.electroserver5.extensions.api.value.GameConfiguration;
import com.electrotank.electroserver5.extensions.api.value.RoomConfiguration;
import com.electrotank.electroserver5.extensions.BaseExtensionLifecycleEventHandler;

public class GameInitializer extends BaseExtensionLifecycleEventHandler {

    /**
     * Initializes each minigame's plugin, initial game details, and registers it with
     * the GameManager.
     * 
     * @param ignored could contain the XML parameters from the web admin interface, 
     * but in this case is just ignored
     */
    @Override
    public void init(EsObjectRO ignored) {
        // use the name of the extension that will contain all the game plugins
        String extensionName = getApi().getExtensionName();

        // invoke the initialization method for each of your games
        // initOneGame(extensionName, pluginName, gameType, maxPlayers)
        initOneGame(extensionName, "Racer15Plugin", "Racer15Plugin", 4);
    }

    /**
     * Registers a game with GameManager, using the standard options.  
     * If a game needs custom gameDetails, it is best to just make a new method for that game.
     * 
     * @param extensionName  Name of the extension that this game is in.  Does not have to be the same as the extension GMSInitializer is in.
     * @param pluginName    Name (handle) of the plugin for the game.  It's best to have the handle and name the same.
     * @param gameType      Game type as registered with GameManager.  Does not need to match pluginName.
     * @param maxPlayers    Maximum number of players.
     */
    private void initOneGame(String extensionName, String pluginName, String gameType, int maxPlayers) {
        ExtensionComponentConfiguration gamePlugin = new ExtensionComponentConfiguration();
        gamePlugin.setExtensionName(extensionName);

        // the handle is the name by which the plugin can be addressed
        // when instantiated in the room
        gamePlugin.setHandle(pluginName);//Name by which the plugin can be addressed when instantiated in the room

        // This needs to be the name of the plugin in the Extension.xml file
        // Usually it is less confusing to just use the same name as the handle
        gamePlugin.setName(pluginName);//Name of the plugin in the Extension.xml file

        // Create the room configuration
        RoomConfiguration roomConfig = new RoomConfiguration();
        roomConfig.setCapacity(maxPlayers);
        roomConfig.setDescription(gameType + " Multiplayer game");

        //add the game plugin(s)
        roomConfig.addPlugin(gamePlugin);

        // Create the game configuration

        // When a user joins a room there are many events that user can potentially receive. 
        // The default subscriptions for a user joining the game are defined here.
        GameConfiguration gameRoomConfig = new GameConfiguration();
        gameRoomConfig.setReceivingRoomListUpdates(false);
        gameRoomConfig.setReceivingRoomVariableUpdates(false);
        gameRoomConfig.setReceivingUserListUpdates(true);
        gameRoomConfig.setReceivingUserVariableUpdates(true);
        gameRoomConfig.setReceivingVideoEvents(false);
        gameRoomConfig.setRoomConfiguration(roomConfig);

        //Create the default GameDetails object

        // When a game is created it has a game details EsObject associated with it. 
        // This object is publicly seen in the game list, and can be accessed and modified 
        // by the game itself.

        EsObject esob = new EsObject();
        gameRoomConfig.setInitialGameDetails(esob);

        // Register the game
        // Once the game has been registered, users can create a new instance of this game 
        // using the integrated game manager.
        // Plugins can also create a new game and put users into it.

        getApi().registerGameConfiguration(gameType, gameRoomConfig);

        getApi().getLogger().warn(gameType + " game registered with GameManager.");
    }
}
