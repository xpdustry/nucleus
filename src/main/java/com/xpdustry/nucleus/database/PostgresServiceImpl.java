// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.database;

import com.xpdustry.foundation.plugin.PluginListener;
import com.xpdustry.nucleus.config.ConfigAutoRegister;
import com.xpdustry.nucleus.config.ConfigKey;
import com.xpdustry.nucleus.config.ConfigKeyRegistry;
import com.xpdustry.nucleus.dependency.Inject;
import com.xpdustry.nucleus.dependency.Named;
import com.xpdustry.nucleus.util.Secret;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
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
import mindustry.Vars;
import org.jspecify.annotations.Nullable;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PostgresServiceImpl implements PostgresService, PluginListener {

    @ConfigAutoRegister
    static final ConfigKey<PostgresConfig> CONFIG_KEY = new ConfigKey<>(
            PostgresConfig.class,
            "nucleus.database",
            "The database config",
            new PostgresConfig("localhost", 5432, "postgres", "postgres", new Secret("postgres"), true),
            ConfigKey.Flag.SENSITIVE);

    private static final ScopedValue<HandleImpl> HANDLE = ScopedValue.newInstance();
    private static final Logger log = LoggerFactory.getLogger(PostgresServiceImpl.class);

    private final ConfigKeyRegistry registry;
    private final Path directory;
    private @Nullable PostgresDataSourceFactory factory = null;
    private @Nullable HikariDataSource source = null;

    @Inject
    PostgresServiceImpl(final ConfigKeyRegistry registry, final @Named("home") Path directory) {
        this.registry = registry;
        this.directory = directory;
    }

    @Override
    public void onInit() {
        {
            final var config = this.registry.get(CONFIG_KEY);
            if (config.local()) {
                if (Vars.mods.getMod("sql4md-postgresql-embedded") == null) {
                    throw new IllegalStateException(
                            "The 'sql4md-postgresql-embedded' is missing, cannot use a local postgres instance without it");
                }
                this.factory = new EmbeddedPostgresDataSourceFactory(config, this.directory);
            } else {
                this.factory = new ExternalPostgresDataSourceFactory(config);
            }
            try {
                this.factory.init();
            } catch (final IOException e) {
                throw new RuntimeException("Failed to init the postgres data source factory", e);
            }
        }

        {
            final var config = new HikariConfig();
            config.setDataSource(this.factory.create());
            config.setPoolName("sql-transaction-pool");
            config.setMaximumPoolSize(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));
            config.setMinimumIdle(2);
            config.setAutoCommit(false);
            this.source = new HikariDataSource(config);
        }

        this.withHandle(handle -> {
            log.debug("Running the SQL setup script");
            final var stream =
                    this.getClass().getClassLoader().getResourceAsStream("com/xpdustry/nucleus/database/setup.sql");
            if (stream == null) {
                throw new IllegalStateException("The sql setup script is missing");
            }
            final var connection = ((HandleImpl) handle).connection;
            try (final var batch = connection.createStatement();
                    stream;
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
            return Boolean.TRUE;
        });
    }

    @Override
    public void onExit() {
        Objects.requireNonNull(this.source, "source").close();
        try {
            Objects.requireNonNull(this.factory, "factory").exit();
        } catch (final IOException e) {
            throw new RuntimeException("Failed to exit the postgres data source factory", e);
        }
    }

    @Override
    public <R> R withHandle(final SQLFunction<Handle, R> function) {
        Objects.requireNonNull(this.source, "source");

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

    @Override
    public Connection newOrphanConnection() throws SQLException {
        return Objects.requireNonNull(this.source, "source").getConnection();
    }

    private record HandleImpl(PostgresServiceImpl database, Connection connection) implements Handle {

        @SuppressWarnings("SqlSourceToSinkFlow")
        @Override
        public StatementBuilder prepareStatement(final String statement) throws SQLException {
            return new StatementBuilderImpl(this.connection.prepareStatement(statement));
        }
    }

    private static final class StatementBuilderImpl implements PostgresService.StatementBuilder {

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
        public <T> List<T> executeSelect(final SQLFunction<ResultSet, T> mapper) throws SQLException {
            try (this.statement;
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
            try (this.statement) {
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

    private interface PostgresDataSourceFactory {

        default void init() throws IOException {}

        default void exit() throws IOException {}

        PGSimpleDataSource create();
    }

    private record ExternalPostgresDataSourceFactory(PostgresConfig config) implements PostgresDataSourceFactory {

        @Override
        public PGSimpleDataSource create() {
            final var url =
                    "jdbc:postgresql://" + this.config.host() + ":" + this.config.port() + "/" + this.config.database();
            final var source = new PGSimpleDataSource();
            source.setUrl(url);
            source.setUser(this.config.username());
            source.setPassword(this.config.password().value());
            return source;
        }
    }

    private static final class EmbeddedPostgresDataSourceFactory implements PostgresDataSourceFactory {

        private final PostgresConfig config;
        private final Path directory;
        private @Nullable EmbeddedPostgres embedded;

        public EmbeddedPostgresDataSourceFactory(final PostgresConfig config, final Path directory) {
            this.config = config;
            this.directory = directory;
        }

        @Override
        public void init() throws IOException {
            this.embedded = EmbeddedPostgres.builder()
                    .setDataDirectory(this.directory.resolve("postgres"))
                    .setCleanDataDirectory(false)
                    .setRegisterShutdownHook(true)
                    .start();
        }

        @Override
        public void exit() throws IOException {
            Objects.requireNonNull(this.embedded, "embedded").close();
        }

        @Override
        public PGSimpleDataSource create() {
            final var source = new PGSimpleDataSource();
            source.setUrl(Objects.requireNonNull(this.embedded, "embedded")
                    .getJdbcUrl(this.config.username(), this.config.database()));
            return source;
        }
    }
}
