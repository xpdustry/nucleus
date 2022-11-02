package fr.xpdustry.nucleus.common.event;

public final class ServerPlayerEvent extends ServerEvent {

    private final String playerName;
    private final Type type;

    public ServerPlayerEvent(final String serverName, final String playerName, Type type) {
        super(serverName);
        this.playerName = playerName;
        this.type = type;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        JOIN, KICK, BAN
    }
}
