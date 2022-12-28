package fr.xpdustry.nucleus.core.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class NucleusConfigurationUpgrader {

    public void upgrade(final Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        final var properties = new Properties();
        try (final var reader = Files.newBufferedReader(path)) {
            properties.load(reader);
        }
        upgrade(properties);
        try (final var writer = Files.newBufferedWriter(path)) {
            properties.store(writer, null);
        }
    }

    private void upgrade(final Properties properties) {
        move(properties, "fr.xpdustry.nucleus.mindustry.translation.token", "fr.xpdustry.nucleus.translation.token");
        move(properties, "fr.xpdustry.nucleus.discord.channel.reports", "fr.xpdustry.nucleus.discord.channel.report");
    }

    private void move(final Properties properties, final String oldKey, final String newKey) {
        final var value = properties.getProperty(oldKey);
        if (value != null) {
            properties.setProperty(newKey, value);
            properties.remove(oldKey);
        }
    }
}
