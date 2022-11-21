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

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.javacord.sender.JavacordCommandSender;
import fr.xpdustry.javelin.UserAuthenticator;
import java.awt.*;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.springframework.stereotype.Component;

// TODO Delete messages after 5 seconds
@Component
public final class JavelinCommands implements AnnotationCommand {

    private final UserAuthenticator authenticator;

    public JavelinCommands(final UserAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @CommandPermission("fr.xpdustry.nucleus.javelin.user.add")
    @CommandDescription("Add a new user to the server.")
    @CommandMethod("javelin user add <username> <password>")
    public void addUser(
            final JavacordCommandSender sender,
            final @Argument("username") String username,
            final @Argument("password") String password) {
        if (this.authenticator.existsUser(username)) {
            sender.sendMessage("The user " + username + " has been override.");
        } else {
            sender.sendMessage("The user " + username + " has been added.");
        }
        sender.getMessage().delete();
        this.authenticator.saveUser(username, password.toCharArray());
    }

    @CommandPermission("fr.xpdustry.nucleus.javelin.user.remove")
    @CommandDescription("Removes a user from the server.")
    @CommandMethod("javelin user remove <username>")
    public void removeUser(final JavacordCommandSender sender, final @Argument("username") String username) {
        if (this.authenticator.existsUser(username)) {
            this.authenticator.deleteUser(username);
            sender.sendMessage("The user " + username + " has been removed.");
        } else {
            sender.sendErrorMessage("The user " + username + " does not exists.");
        }
    }

    @CommandPermission("fr.xpdustry.nucleus.javelin.user.remove")
    @CommandDescription("Removes all users from the server.")
    @CommandMethod("javelin user remove-all")
    public void removeAllUsers(final JavacordCommandSender sender) {
        final var count = this.authenticator.countUsers();
        this.authenticator.deleteAllUsers();
        sender.sendMessage(String.format("%d users have been removed.", count));
    }

    @CommandPermission("fr.xpdustry.nucleus.javelin.user.list")
    @CommandDescription("List the users.")
    @CommandMethod("javelin user list")
    public void listUsers(final JavacordCommandSender sender) {
        final var users = this.authenticator.findAllUsers().iterator();
        if (!users.hasNext()) {
            sender.sendMessage("No users...");
        } else {
            final var builder = new StringBuilder();
            while (users.hasNext()) {
                builder.append(" - ").append(users.next());
                if (users.hasNext()) {
                    builder.append('\n');
                }
            }
            sender.getMessage()
                    .reply(new EmbedBuilder()
                            .setColor(Color.CYAN)
                            .setTitle("Registered Javelin users")
                            .setDescription(builder.toString()));
        }
    }
}
