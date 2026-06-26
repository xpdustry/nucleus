// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.database;

import com.xpdustry.foundation.plugin.PluginListener;
import com.xpdustry.nucleus.config.ConfigAutoRegister;
import com.xpdustry.nucleus.config.ConfigKey;
import com.xpdustry.nucleus.config.ConfigKeyRegistry;
import com.xpdustry.nucleus.dependency.Inject;
import com.xpdustry.nucleus.dependency.Named;
import com.xpdustry.nucleus.function.ThrowingFunction;
import com.xpdustry.nucleus.util.Secret;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DatabaseImpl implements Database, PluginListener {

    @ConfigAutoRegister
    static final ConfigKey<DatabaseConfig> CONFIG_KEY = new ConfigKey<>(
            DatabaseConfig.class,
            "nucleus.database",
            "The database config",
            new DatabaseConfig("localhost", 5432, "nucleus", "root", new Secret("root"), true),
            ConfigKey.Flag.SENSITIVE);

    private static final ScopedValue<HandleImpl> HANDLE = ScopedValue.newInstance();
    private static final Logger log = LoggerFactory.getLogger(DatabaseImpl.class);

    private final DatabaseConfig config;
    private final Path directory;
    private @Nullable HikariDataSource source = null;

    @Inject
    public DatabaseImpl(final ConfigKeyRegistry config, final @Named("home") Path directory) {
        this.config = config.get(CONFIG_KEY);
        this.directory = directory;
    }

    @Override
    public void onInit() {
        final var hikari = new HikariConfig();
        hikari.setPoolName("sql-connection-pool");
        hikari.setMaximumPoolSize(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));
        hikari.setMinimumIdle(2);
        hikari.addDataSourceProperty("createDatabaseIfNotExist", "true");

        if (this.config.local()) {
            hikari.setDriverClassName("org.h2.Driver");
            hikari.setJdbcUrl("jdbc:h2:file:"
                    + this.directory.resolve(this.config.database() + ".h2").toAbsolutePath()
                    + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;AUTO_SERVER=TRUE");
        } else {
            hikari.setDriverClassName("org.postgresql.Driver");
            hikari.setJdbcUrl("jdbc:postgresql://" + this.config.host() + ":" + this.config.port() + "/"
                    + this.config.database());
            hikari.setUsername(this.config.username());
            hikari.setPassword(this.config.password().value());
        }

        this.source = new HikariDataSource(hikari);

        this.withConsumerHandle(handle -> {
            log.debug("Running the SQL setup script");
            final var stream =
                    this.getClass().getClassLoader().getResourceAsStream("com/xpdustry/nucleus/database/setup.sql");
            if (stream == null) {
                throw new IllegalStateException("The sql setup script is missing");
            }
            final var connection = ((HandleImpl) handle).connection;
            try (final var batch = connection.createStatement();
                    final var _ = stream;
                    final var scanner = new Scanner(stream, StandardCharsets.UTF_8)) {
                scanner.useDelimiter(";");
                while (scanner.hasNext()) {
                    final var statement = scanner.next().trim();
                    if (!statement.isBlank()) {
                        batch.addBatch(statement);
                        log.debug("Adding statement to batch: {}", statement);
                    }
                }
                batch.executeBatch();
                log.debug("Executed the setup script batch");
            } catch (final IOException e) {
                throw new IllegalStateException("Failed to stream the sql setup script", e);
            }
        });
    }

    @Override
    public void onExit() {
        Objects.requireNonNull(this.source).close();
    }

    @Override
    public <R extends @Nullable Object> R withFunctionHandle(final ThrowingFunction<Handle, R, SQLException> function) {
        Objects.requireNonNull(this.source);

        if (HANDLE.isBound()) {
            try {
                return function.apply(HANDLE.get());
            } catch (final SQLException e) {
                throw new RuntimeException(e);
            }
        }

        try (final var connection = this.source.getConnection()) {
            return ScopedValue.where(HANDLE, new HandleImpl(connection)).call(() -> {
                final var handle = HANDLE.get();
                try {
                    handle.connection.setAutoCommit(false);
                    handle.connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
                    final var result = function.apply(handle);
                    handle.connection.commit();
                    return result;
                } catch (final Exception e) {
                    handle.connection.rollback();
                    throw new RuntimeException(e);
                } finally {
                    handle.connection.close();
                }
            });
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private record HandleImpl(Connection connection) implements Handle {

        @SuppressWarnings("SqlSourceToSinkFlow")
        @Override
        public StatementBuilder prepareStatement(final String statement) throws SQLException {
            return new StatementBuilderImpl(this.connection.prepareStatement(statement));
        }
    }

    private static final class StatementBuilderImpl implements Database.StatementBuilder {

        private final PreparedStatement statement;
        private int index = 1;

        private StatementBuilderImpl(final PreparedStatement statement) {
            this.statement = statement;
        }

        @Override
        public StatementBuilder push(final String value) throws SQLException {
            this.statement.setString(this.index, value);
            this.index++;
            return this;
        }

        @Override
        public StatementBuilder push(final int value) throws SQLException {
            this.statement.setInt(this.index, value);
            this.index++;
            return this;
        }

        @Override
        public StatementBuilder push(final long value) throws SQLException {
            this.statement.setLong(this.index, value);
            this.index++;
            return this;
        }

        @Override
        public StatementBuilder push(final boolean value) throws SQLException {
            this.statement.setBoolean(this.index, value);
            this.index++;
            return this;
        }

        @Override
        public StatementBuilder push(final float value) throws SQLException {
            this.statement.setFloat(this.index, value);
            this.index++;
            return this;
        }

        @Override
        public StatementBuilder push(final byte[] value) throws SQLException {
            this.statement.setBytes(this.index, value);
            this.index++;
            return this;
        }

        @Override
        public StatementBuilder push(final Instant value) throws SQLException {
            this.statement.setTimestamp(this.index, Timestamp.from(value));
            this.index++;
            return this;
        }

        @Override
        public <T extends @Nullable Object> List<T> executeSelect(
                final ThrowingFunction<ResultSet, T, SQLException> mapper) throws SQLException {
            try (final var _ = this.statement;
                    final var result = this.statement.executeQuery()) {
                final var list = new ArrayList<T>();
                while (result.next()) {
                    list.add(mapper.apply(result));
                }
                return list;
            }
        }

        @Override
        public int executeUpdate() throws SQLException {
            try (final var _ = this.statement) {
                return this.statement.executeUpdate();
            }
        }

        @Override
        public boolean executeSingleUpdate() throws SQLException {
            final var result = this.executeUpdate();
            return switch (result) {
                case 0 -> false;
                case 1 -> true;
                default -> throw new IllegalStateException("Multiple rows updated, expected 0 or 1, got " + result);
            };
        }
    }
}
