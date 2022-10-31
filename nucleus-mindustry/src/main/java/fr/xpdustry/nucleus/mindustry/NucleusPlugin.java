package fr.xpdustry.nucleus.mindustry;

import fr.xpdustry.distributor.api.command.ArcCommandManager;
import fr.xpdustry.distributor.api.command.sender.CommandSender;
import fr.xpdustry.distributor.api.plugin.ExtendedPlugin;
import fr.xpdustry.nucleus.common.util.NucleusPlatform;

public final class NucleusPlugin extends ExtendedPlugin {

    private final ArcCommandManager<CommandSender> clientCommands = ArcCommandManager.standard(this);
    private final ArcCommandManager<CommandSender> serverCommands = ArcCommandManager.standard(this);

    @Override
    public void onInit() {
        System.out.println("Init nucleus");
    }

    public NucleusPlatform getPlatform() {
        return NucleusPlatform.MINDUSTRY;
    }
}
