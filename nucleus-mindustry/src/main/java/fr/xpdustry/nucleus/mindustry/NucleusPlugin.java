package fr.xpdustry.nucleus.mindustry;

import arc.util.CommandHandler;
import cloud.commandframework.meta.CommandMeta;
import fr.xpdustry.distributor.api.command.ArcCommandManager;
import fr.xpdustry.distributor.api.command.sender.CommandSender;
import fr.xpdustry.distributor.api.plugin.ExtendedPlugin;
import fr.xpdustry.javelin.JavelinPlugin;
import fr.xpdustry.javelin.JavelinSocket;
import fr.xpdustry.nucleus.common.NucleusApplication;
import fr.xpdustry.nucleus.common.NucleusApplicationProvider;
import fr.xpdustry.nucleus.common.util.NucleusPlatform;
import mindustry.gen.Call;

public final class NucleusPlugin extends ExtendedPlugin implements NucleusApplication {

    private final ArcCommandManager<CommandSender> clientCommands = ArcCommandManager.standard(this);
    private final ArcCommandManager<CommandSender> serverCommands = ArcCommandManager.standard(this);

    @Override
    public void onInit() {
        NucleusApplicationProvider.set(this);
    }

    @Override
    public void onClientCommandsRegistration(final CommandHandler handler) {
        this.clientCommands.initialize(handler);
        this.clientCommands.command(this.clientCommands
                .commandBuilder("discord")
                .meta(CommandMeta.DESCRIPTION, "Send you our discord invitation link.")
                .handler(ctx -> {
                    Call.openURI(ctx.getSender().getPlayer().con(), "https://discord.xpdustry.fr");
                }));
    }

    @Override
    public JavelinSocket getSocket() {
        return JavelinPlugin.getJavelinSocket();
    }

    @Override
    public NucleusPlatform getPlatform() {
        return NucleusPlatform.MINDUSTRY;
    }
}
