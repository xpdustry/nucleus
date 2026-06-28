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
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Stream;
import mindustry.Vars;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DatabaseImpl implements Database, PluginListener {

    @ConfigAutoRegister
    static final ConfigKey<DatabaseConfig> CONFIG_KEY = new ConfigKey<>(
            DatabaseConfig.class,
            "nucleus.database",
            "The database config",
            new DatabaseConfig("localhost", 5432, "postgres", "postgres", new Secret("postgres"), true),
            ConfigKey.Flag.SENSITIVE);

    private static final ScopedValue<HandleImpl> HANDLE = ScopedValue.newInstance();
    private static final Logger log = LoggerFactory.getLogger(DatabaseImpl.class);

    private final DatabaseConfig config;
    private final Path directory;
    private @Nullable PostgresProcess process = null;
    private @Nullable HikariDataSource source = null;

    @Inject
    public DatabaseImpl(final ConfigKeyRegistry config, final @Named("home") Path directory) {
        this.config = config.get(CONFIG_KEY);
        this.directory = directory;
    }

    @Override
    public void onInit() {
        final var hikari = new HikariConfig();
        hikari.setDriverClassName("org.postgresql.Driver");
        hikari.setUsername(this.config.username());
        hikari.setPassword(this.config.password().value());
        hikari.setPoolName("sql-pool");
        hikari.setMaximumPoolSize(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));
        hikari.setMinimumIdle(2);
        hikari.addDataSourceProperty("createDatabaseIfNotExist", "true");

        if (this.config.local()) {
            if (Vars.mods.getMod("sql4md-postgresql-embedded") == null) {
                throw new IllegalStateException(
                        "The 'sql4md-postgresql-embedded' is missing, cannot use a local database without it");
            }
            this.process = new EmbeddedPostgresProcess(this.config, this.directory);
        } else {
            this.process = new RemotePostgresProcess(this.config);
        }

        try {
            this.process.start();
        } catch (final IOException e) {
            throw new RuntimeException("Failed to start postgres process", e);
        }

        hikari.setJdbcUrl(this.process.url());

        try {
            this.source = new HikariDataSource(hikari);
        } catch (final Exception e1) {
            try {
                this.process.close();
            } catch (final IOException e2) {
                e1.addSuppressed(e2);
            }
            throw e1;
        }
    }

    @Override
    public void onExit() {
        Objects.requireNonNull(this.source, "source").close();
        try {
            Objects.requireNonNull(this.process, "process").close();
        } catch (final IOException e) {
            log.error("Failed to close postgres process", e);
        }
    }

    @SuppressWarnings("SqlSourceToSinkFlow")
    @Override
    public void executeScript(final String script) {
        this.withConsumerHandle(handle -> {
            try (final var statement = ((HandleImpl) handle).connection.createStatement()) {
                for (var line : script.split(";", -1)) {
                    line = line.trim();
                    if (line.isBlank() || line.startsWith("--")) continue;
                    statement.addBatch(line);
                }
                statement.executeBatch();
            }
        });
    }

    @Override
    public <R extends @Nullable Object> R withFunctionHandle(final ThrowingFunction<Handle, R, SQLException> function) {
        Objects.requireNonNull(this.source);

        if (HANDLE.isBound()) {
            final var handle = HANDLE.get();
            if (handle.database == this) {
                try {
                    return function.apply(handle);
                } catch (final SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        try (final var connection = this.source.getConnection()) {
            return ScopedValue.where(HANDLE, new HandleImpl(this, connection)).call(() -> {
                final var handle = HANDLE.get();
                try {
                    handle.connection.setAutoCommit(false);
                    handle.connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
                    final var result = function.apply(handle);
                    handle.connection.commit();
                    return result;
                } catch (final SQLException e) {
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

    private record HandleImpl(DatabaseImpl database, Connection connection) implements Handle {

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
        public <T extends @Nullable Object> Stream<T> executeSelect(
                final ThrowingFunction<ResultSet, T, SQLException> mapper) throws SQLException {
            try {
                final var result = this.statement.executeQuery();
                final var list = new ArrayList<T>();
                while (result.next()) {
                    list.add(mapper.apply(result));
                }
                return list.stream();
            } finally {
                this.statement.close();
            }
        }

        @Override
        public int executeUpdate() throws SQLException {
            try {
                return this.statement.executeUpdate();
            } finally {
                this.statement.close();
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

    private interface PostgresProcess extends Closeable {

        void start() throws IOException;

        String url();
    }

    private record RemotePostgresProcess(DatabaseConfig config) implements PostgresProcess {

        @Override
        public void start() {}

        @Override
        public String url() {
            return "jdbc:postgresql://" + this.config.host() + ":" + this.config.port() + "/" + this.config.database();
        }

        @Override
        public void close() {}
    }

    private static final class EmbeddedPostgresProcess implements PostgresProcess {

        private final DatabaseConfig config;
        private final Path directory;
        private @Nullable EmbeddedPostgres postgres;

        public EmbeddedPostgresProcess(final DatabaseConfig config, final Path directory) {
            this.config = config;
            this.directory = directory;
        }

        public void start() throws IOException {
            this.postgres = EmbeddedPostgres.builder()
                    .setDataDirectory(this.directory.resolve("postgres"))
                    .setCleanDataDirectory(false)
                    .setRegisterShutdownHook(true)
                    .start();
        }

        @Override
        public String url() {
            return this.postgres().getJdbcUrl(this.config.username(), this.config.database());
        }

        @Override
        public void close() throws IOException {
            this.postgres().close();
        }

        private EmbeddedPostgres postgres() {
            return Objects.requireNonNull(this.postgres, "postgres is not initialized");
        }
    }
}
