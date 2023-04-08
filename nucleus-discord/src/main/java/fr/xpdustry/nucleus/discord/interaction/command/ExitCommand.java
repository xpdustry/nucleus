/*
 * Nucleus, the software collection powering Xpdustry.
 * Copyright (C) 2022  Xpdustry
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.xpdustry.nucleus.discord.interaction.command;

import fr.xpdustry.nucleus.api.application.ShutdownEvent;
import fr.xpdustry.nucleus.api.application.ShutdownEvent.Cause;
import fr.xpdustry.nucleus.api.event.EventService;
import fr.xpdustry.nucleus.discord.interaction.InteractionContext;
import fr.xpdustry.nucleus.discord.interaction.InteractionDescription;
import fr.xpdustry.nucleus.discord.interaction.InteractionListener;
import fr.xpdustry.nucleus.discord.interaction.InteractionPermission;
import fr.xpdustry.nucleus.discord.interaction.SlashInteraction;
import javax.inject.Inject;
import org.javacord.api.entity.permission.PermissionType;

@SlashInteraction("exit")
@InteractionDescription("Exits the bot.")
@InteractionPermission(PermissionType.ADMINISTRATOR)
public class ExitCommand implements InteractionListener {

    private final EventService eventService;

    @Inject
    public ExitCommand(final EventService eventService) {
        this.eventService = eventService;
    }

    @SlashInteraction.Handler
    public void onExit(final InteractionContext context) {
        context.sendEphemeralMessage("Exiting...");
        eventService.publish(ShutdownEvent.of(Cause.EXIT));
    }
}
