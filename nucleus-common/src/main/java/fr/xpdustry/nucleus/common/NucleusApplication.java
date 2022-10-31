package fr.xpdustry.nucleus.common;

import fr.xpdustry.javelin.JavelinSocket;
import fr.xpdustry.nucleus.common.util.NucleusPlatform;

public interface NucleusApplication {

    NucleusPlatform getPlatform();

    default JavelinSocket getSocket() {
        return JavelinSocket.noop();
    }
}
