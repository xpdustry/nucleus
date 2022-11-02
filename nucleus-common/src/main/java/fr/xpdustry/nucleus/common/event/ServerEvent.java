package fr.xpdustry.nucleus.common.event;

import fr.xpdustry.javelin.JavelinEvent;

public abstract class ServerEvent implements JavelinEvent {

    private final String serverName;

    protected ServerEvent(final String serverName) {
        this.serverName = serverName;
    }

    public String getServerName() {
        return serverName;
    }
}
