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
package fr.xpdustry.nucleus.discord.commands;

import fr.xpdustry.javelin.UserAuthenticator;
import fr.xpdustry.nucleus.discord.interaction.InteractionContext;
import fr.xpdustry.nucleus.discord.interaction.InteractionDescription;
import fr.xpdustry.nucleus.discord.interaction.InteractionPermission;
import fr.xpdustry.nucleus.discord.interaction.Option;
import fr.xpdustry.nucleus.discord.interaction.SlashInteraction;
import java.awt.Color;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.springframework.stereotype.Component;

@SlashInteraction("javelin")
@InteractionDescription("Manage the javelin network.")
@InteractionPermission(PermissionType.ADMINISTRATOR)
@Component
public final class JavCommands implements AnnotationCommand {

    private final UserAuthenticator authenticator;

    public JavCommands(final UserAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @SlashInteraction.Handler(group = "user", subcommand = "add")
    @InteractionDescription("Authenticate a user.")
    public void addUser(
            final InteractionContext context,
            final @Option("username") String username,
            final @Option("password") String password) {
        final var responder = context.interaction().createImmediateResponder().setFlags(MessageFlag.EPHEMERAL);
        if (this.authenticator.existsUser(username)) {
            responder.setContent("The user " + username + " has been override.");
        } else {
            responder.setContent("The user " + username + " has been added.");
        }
        this.authenticator.saveUser(username, password.toCharArray());
        responder.respond();
    }

    @SlashInteraction.Handler(group = "user", subcommand = "remove")
    @InteractionDescription("Removes a user from the server.")
    public void removeUser(final InteractionContext context, final @Option("username") String username) {
        final var responder = context.interaction().createImmediateResponder();
        if (this.authenticator.existsUser(username)) {
            this.authenticator.deleteUser(username);
            responder.setContent("The user " + username + " has been removed.");
        } else {
            responder.setContent("The user " + username + " does not exists.");
        }
        responder.setFlags(MessageFlag.EPHEMERAL).respond();
    }

    @SlashInteraction.Handler(group = "user", subcommand = "remove-all")
    @InteractionDescription("Removes all users from the server.")
    public void removeAllUsers(final InteractionContext context) {
        final var count = this.authenticator.countUsers();
        this.authenticator.deleteAllUsers();
        context.interaction()
                .createImmediateResponder()
                .setContent(String.format("%d users have been removed.", count))
                .setFlags(MessageFlag.EPHEMERAL)
                .respond();
    }

    @SlashInteraction.Handler(group = "user", subcommand = "list")
    @InteractionDescription("List the users.")
    public void listUsers(final InteractionContext context) {
        final var responder = context.responder();
        final var users = this.authenticator.findAllUsers().iterator();
        if (!users.hasNext()) {
            responder.setContent("No users...");
        } else {
            final var builder = new StringBuilder();
            while (users.hasNext()) {
                builder.append(" - ").append(users.next());
                if (users.hasNext()) {
                    builder.append('\n');
                }
            }
            responder.addEmbed(new EmbedBuilder()
                    .setColor(Color.CYAN)
                    .setTitle("Registered Javelin users")
                    .setDescription(builder.toString()));
        }
        responder.setFlags(MessageFlag.EPHEMERAL).respond();
    }
}
