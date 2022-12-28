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
package fr.xpdustry.nucleus.discord.interaction;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.interaction.InteractionCreateEvent;
import org.javacord.api.interaction.ApplicationCommand;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.listener.interaction.InteractionCreateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO Clean up this mess
public final class SlashCommandManager {

    private final Map<Class<?>, OptionTypeHandler<?>> handlers = new HashMap<>();
    private final Map<String, SlashCommandInfo> slash = new HashMap<>();
    private final DiscordApi api;

    {
        handlers.put(String.class, new StringTypeHandler());
        handlers.put(Boolean.class, new BooleanTypeHandler());
        handlers.put(Integer.class, new IntegerTypeHandler());
        handlers.put(Long.class, new LongTypeHandler());
        handlers.put(Float.class, new FloatTypeHandler());
        handlers.put(Double.class, new DoubleTypeHandler());
    }

    public SlashCommandManager(final DiscordApi api) {
        this.api = api;
    }

    public void register(final Object instance) {
        final var interaction = instance.getClass().getAnnotation(SlashInteraction.class);
        if (interaction == null) {
            return;
        }
        if (slash.containsKey(interaction.value())) {
            throw new IllegalArgumentException("Slash interaction " + interaction.value() + " already registered.");
        }

        // Collect info about the command
        var root = new SlashCommandNode(interaction.value(), SlashCommandNode.Type.COMMAND);
        slash.put(interaction.value(), new SlashCommandInfo(instance.getClass(), root));

        // Collect the command handlers
        for (final var method : instance.getClass().getDeclaredMethods()) {
            final var annotation = method.getAnnotation(SlashInteraction.Handler.class);
            if (annotation == null) {
                continue;
            }

            if (!annotation.group().isEmpty() && annotation.subcommand().isEmpty()) {
                throw new IllegalArgumentException("Cannot have a group without a subcommand");
            }

            var node = root;
            if (!annotation.group().isEmpty()) {
                node = node.children.computeIfAbsent(
                        annotation.group(),
                        name -> new SlashCommandNode(name, SlashCommandNode.Type.SUB_COMMAND_GROUP));
                if (node.type != SlashCommandNode.Type.SUB_COMMAND_GROUP) {
                    throw new IllegalArgumentException(
                            "Unexpected node type, expected SUB_COMMAND_GROUP, got " + node.type);
                }
            }
            if (!annotation.subcommand().isEmpty()) {
                node = node.children.computeIfAbsent(
                        annotation.subcommand(), name -> new SlashCommandNode(name, SlashCommandNode.Type.SUB_COMMAND));
                if (node.type != SlashCommandNode.Type.SUB_COMMAND) {
                    throw new IllegalArgumentException("Unexpected node type, expected SUB_COMMAND, got " + node.type);
                }
            }
            node.command = method;

            for (final var parameter : method.getParameters()) {
                if (!handlers.containsKey(parameter.getType())
                        && !parameter.getType().equals(InteractionContext.class)) {
                    throw new IllegalArgumentException("Unsupported argument type " + parameter.getType());
                }
                if (!parameter.getType().equals(InteractionContext.class)
                        && !parameter.isAnnotationPresent(Option.class)) {
                    throw new IllegalArgumentException(
                            "Missing @Option annotation on parameter " + parameter.getName());
                }
            }

            var name = interaction.value();
            if (!annotation.group().isEmpty()) {
                name += " " + annotation.group();
            }
            if (!annotation.subcommand().isEmpty()) {
                name += " " + annotation.subcommand();
            }
            api.addInteractionCreateListener(new SlashInteractionListener(method, instance, name));
        }
    }

    public CompletableFuture<Set<ApplicationCommand>> compile() {
        final Set<SlashCommandBuilder> compiled = new HashSet<>();

        for (final var command : slash.entrySet()) {
            final var builder = new SlashCommandBuilder()
                    .setName(command.getValue().root().name)
                    .setDescription("No description.");
            final var clazz = command.getValue().clazz();

            if (clazz.isAnnotationPresent(InteractionDescription.class)) {
                builder.setDescription(
                        clazz.getAnnotation(InteractionDescription.class).value());
            }
            if (clazz.isAnnotationPresent(InteractionPermission.class)) {
                builder.setDefaultEnabledForPermissions(
                        clazz.getAnnotation(InteractionPermission.class).value());
            }

            for (final var child : command.getValue().root().children.values()) {
                compile(builder::addOption, child);
            }
            if (command.getValue().root().command != null) {
                if (command.getValue().root().command.isAnnotationPresent(InteractionDescription.class)) {
                    builder.setDescription(command.getValue()
                            .root()
                            .command
                            .getAnnotation(InteractionDescription.class)
                            .value());
                }
                compile(builder::addOption, command.getValue().root().command);
            }

            compiled.add(builder);
        }

        return api.bulkOverwriteServerApplicationCommands(
                api.getServers().iterator().next(), compiled);
    }

    private void compile(final Consumer<SlashCommandOption> options, final SlashCommandNode node) {
        if (node.type == SlashCommandNode.Type.COMMAND) {
            throw new IllegalStateException(
                    "Unexpected node type, expected SUB_COMMAND or SUB_COMMAND_GROUP, got COMMAND");
        }

        final var builder = new SlashCommandOptionBuilder()
                .setName(node.name)
                .setDescription("No description.")
                .setType(
                        node.type == SlashCommandNode.Type.SUB_COMMAND
                                ? SlashCommandOptionType.SUB_COMMAND
                                : SlashCommandOptionType.SUB_COMMAND_GROUP);

        for (final var child : node.children.values()) {
            compile(builder::addOption, child);
        }
        if (node.command != null) {
            if (node.command.isAnnotationPresent(InteractionDescription.class)) {
                builder.setDescription(
                        node.command.getAnnotation(InteractionDescription.class).value());
            }
            compile(builder::addOption, node.command);
        }

        options.accept(builder.build());
    }

    private void compile(final Consumer<SlashCommandOption> options, final Method method) {
        for (final var parameter : method.getParameters()) {
            if (parameter.getType().equals(InteractionContext.class)) {
                continue;
            }

            final var annotation = parameter.getAnnotation(Option.class);
            final var option = new SlashCommandOptionBuilder()
                    .setName(annotation.value())
                    .setDescription(annotation.description().isBlank() ? "No description." : annotation.description())
                    .setRequired(annotation.required());

            final var handler = handlers.get(parameter.getType());
            if (handler == null) {
                throw new IllegalArgumentException("Unsupported argument type " + parameter.getType());
            }

            handler.setType(option, parameter);
            options.accept(option.build());
        }
    }

    private interface OptionTypeHandler<T> {

        void setType(final SlashCommandOptionBuilder builder, final Parameter parameter);

        @Nullable T getArgument(final InteractionContext context, final String name);
    }

    private record SlashCommandInfo(Class<?> clazz, SlashCommandNode root) {}

    private static final class SlashCommandNode {

        private final Map<String, SlashCommandNode> children = new HashMap<>();
        private final String name;
        private final Type type;
        private @MonotonicNonNull Method command;

        private SlashCommandNode(final String name, final Type type) {
            this.name = name;
            this.type = type;
        }

        public enum Type {
            COMMAND,
            SUB_COMMAND_GROUP,
            SUB_COMMAND
        }
    }

    private static final class StringTypeHandler implements OptionTypeHandler<String> {

        @Override
        public void setType(final SlashCommandOptionBuilder builder, final Parameter parameter) {
            builder.setType(SlashCommandOptionType.STRING);
        }

        @Override
        public @Nullable String getArgument(final InteractionContext context, final String name) {
            return context.interaction().getArgumentStringValueByName(name).orElse(null);
        }
    }

    private static final class BooleanTypeHandler implements OptionTypeHandler<Boolean> {

        @Override
        public void setType(final SlashCommandOptionBuilder builder, final Parameter parameter) {
            builder.setType(SlashCommandOptionType.BOOLEAN);
        }

        @Override
        public @Nullable Boolean getArgument(final InteractionContext context, final String name) {
            return context.interaction().getArgumentBooleanValueByName(name).orElse(null);
        }
    }

    private static final class IntegerTypeHandler implements OptionTypeHandler<Integer> {

        @Override
        public void setType(final SlashCommandOptionBuilder builder, final Parameter parameter) {
            builder.setLongMinValue(Integer.MIN_VALUE);
            builder.setLongMaxValue(Integer.MAX_VALUE);
            builder.setType(SlashCommandOptionType.LONG);
        }

        @Override
        public @Nullable Integer getArgument(final InteractionContext context, final String name) {
            return context.interaction()
                    .getArgumentLongValueByName(name)
                    .map(Long::intValue)
                    .orElse(null);
        }
    }

    private static final class LongTypeHandler implements OptionTypeHandler<Long> {

        @Override
        public void setType(final SlashCommandOptionBuilder builder, final Parameter parameter) {
            builder.setType(SlashCommandOptionType.LONG);
        }

        @Override
        public @Nullable Long getArgument(final InteractionContext context, final String name) {
            return context.interaction().getArgumentLongValueByName(name).orElse(null);
        }
    }

    private static final class FloatTypeHandler implements OptionTypeHandler<Float> {

        @Override
        public void setType(final SlashCommandOptionBuilder builder, final Parameter parameter) {
            builder.setDecimalMinValue(Float.MIN_VALUE);
            builder.setDecimalMaxValue(Float.MAX_VALUE);
            builder.setType(SlashCommandOptionType.DECIMAL);
        }

        @Override
        public @Nullable Float getArgument(final InteractionContext context, final String name) {
            return context.interaction()
                    .getArgumentDecimalValueByName(name)
                    .map(Double::floatValue)
                    .orElse(null);
        }
    }

    private static final class DoubleTypeHandler implements OptionTypeHandler<Double> {

        @Override
        public void setType(final SlashCommandOptionBuilder builder, final Parameter parameter) {
            builder.setType(SlashCommandOptionType.DECIMAL);
        }

        @Override
        public @Nullable Double getArgument(final InteractionContext context, final String name) {
            return context.interaction().getArgumentDecimalValueByName(name).orElse(null);
        }
    }

    private final class SlashInteractionListener implements InteractionCreateListener {

        private static final Logger logger = LoggerFactory.getLogger(SlashInteractionListener.class);

        private final Method method;
        private final String name;
        private final MethodHandle handle;

        private SlashInteractionListener(final Method method, final Object instance, final String name) {
            this.method = method;
            this.name = name;
            try {
                this.handle = MethodHandles.lookup().unreflect(method).bindTo(instance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onInteractionCreate(final InteractionCreateEvent event) {
            if (event.getSlashCommandInteraction().isEmpty()) {
                return;
            } else if (!event.getSlashCommandInteraction()
                    .get()
                    .getFullCommandName()
                    .equals(name)) {
                return;
            }

            final var context =
                    new InteractionContext(event.getSlashCommandInteraction().get());
            final List<Object> arguments = new ArrayList<>();

            for (final var parameter : method.getParameters()) {
                if (parameter.getType().equals(InteractionContext.class)) {
                    arguments.add(context);
                    continue;
                }

                final var option = parameter.getAnnotation(Option.class);
                final var helper = handlers.get(parameter.getType());
                if (helper == null) {
                    throw new IllegalArgumentException("Unsupported argument type " + parameter.getType());
                }
                final var argument = helper.getArgument(context, option.value());
                if (option.required() && argument == null) {
                    throw new IllegalArgumentException("Required argument " + option.value() + " is missing");
                }
                arguments.add(argument);
            }

            try {
                handle.invokeWithArguments(arguments);
            } catch (final Throwable throwable) {
                logger.error("Error while executing slash command", throwable);
            }
        }
    }
}
