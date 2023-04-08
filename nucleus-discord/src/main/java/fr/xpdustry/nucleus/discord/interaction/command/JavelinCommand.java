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

import fr.xpdustry.javelin.UserAuthenticator;
import fr.xpdustry.nucleus.discord.interaction.InteractionContext;
import fr.xpdustry.nucleus.discord.interaction.InteractionDescription;
import fr.xpdustry.nucleus.discord.interaction.InteractionListener;
import fr.xpdustry.nucleus.discord.interaction.InteractionPermission;
import fr.xpdustry.nucleus.discord.interaction.Option;
import fr.xpdustry.nucleus.discord.interaction.SlashInteraction;
import java.awt.Color;
import javax.inject.Inject;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;

@SlashInteraction("javelin")
@InteractionDescription("Manage the javelin network.")
@InteractionPermission(PermissionType.ADMINISTRATOR)
public final class JavelinCommand implements InteractionListener {

    private final UserAuthenticator authenticator;

    @Inject
    public JavelinCommand(final UserAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @SlashInteraction.Handler(group = "user", subcommand = "add")
    @InteractionDescription("Authenticate a user.")
    public void onUserAdd(
            final InteractionContext context,
            final @Option("username") String username,
            final @Option("password") String password) {
        if (this.authenticator.existsUser(username)) {
            context.sendEphemeralMessage("The user %s already exists.", username);
        } else {
            this.authenticator.saveUser(username, password.toCharArray());
            context.sendEphemeralMessage("The user %s has been added.", username);
        }
    }

    @SlashInteraction.Handler(group = "user", subcommand = "update")
    @InteractionDescription("Authenticate a user.")
    public void onUserUpdate(
            final InteractionContext context,
            final @Option("username") String username,
            final @Option("password") String password) {
        if (this.authenticator.existsUser(username)) {
            this.authenticator.saveUser(username, password.toCharArray());
            context.sendEphemeralMessage("The user %s has been updated.", username);
        } else {
            context.sendEphemeralMessage("The user %s does not exists.", username);
        }
    }

    @SlashInteraction.Handler(group = "user", subcommand = "delete")
    @InteractionDescription("Deletes a user from the server.")
    public void onUserDelete(final InteractionContext context, final @Option("username") String username) {
        if (this.authenticator.existsUser(username)) {
            this.authenticator.deleteUser(username);
            context.sendEphemeralMessage("The user %s has been deleted.", username);
        } else {
            context.sendEphemeralMessage("The user %s does not exists.", username);
        }
    }

    @SlashInteraction.Handler(group = "user", subcommand = "delete-all")
    @InteractionDescription("Deletes all users from the server.")
    public void onUserDeleteAll(final InteractionContext context) {
        final var count = this.authenticator.countUsers();
        this.authenticator.deleteAllUsers();
        context.sendEphemeralMessage("All %d user(s) have been deleted.", count);
    }

    @SlashInteraction.Handler(group = "user", subcommand = "list")
    @InteractionDescription("List the users.")
    public void onUserList(final InteractionContext context) {
        if (this.authenticator.countUsers() == 0) {
            context.sendEphemeralMessage("No users...");
        } else {
            final var users = this.authenticator.findAllUsers().iterator();
            final var builder = new StringBuilder();
            while (users.hasNext()) {
                builder.append(" - ").append(users.next());
                if (users.hasNext()) {
                    builder.append('\n');
                }
            }
            context.sendEphemeralMessage(new EmbedBuilder()
                    .setColor(Color.CYAN)
                    .setTitle("Registered Javelin users")
                    .setDescription(builder.toString()));
        }
    }
}
