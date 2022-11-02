package fr.xpdustry.nucleus.common.event;

public final class ServerChatEvent extends ServerEvent {

    private final String playerName;
    private final String message;

    public ServerChatEvent(final String serverName, final String playerName, final String message) {
        super(serverName);
        this.playerName = playerName;
        this.message = message;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getMessage() {
        return message;
    }
}
