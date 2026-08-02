// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.database;

import com.xpdustry.foundation.plugin.PluginListener;
import com.xpdustry.nucleus.config.ConfigManager;
import com.xpdustry.nucleus.config.ConfigPropertyKey;
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
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import mindustry.Vars;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PostgresDatabaseImpl implements PluginListener, PostgresDatabase {

    private static final ScopedValue<TransactionImpl> HANDLE = ScopedValue.newInstance();
    private static final Logger log = LoggerFactory.getLogger(PostgresDatabaseImpl.class);

    private final ConfigManager configManager;
    private final Path directory;
    private @Nullable PostgresDataSourceFactory factory = null;
    private @Nullable HikariDataSource source = null;

    public PostgresDatabaseImpl(final ConfigManager configManager, final Path directory) {
        this.configManager = configManager;
        this.directory = directory;
    }

    @Override
    public <R> R withTransaction(final SQLFunction<Transaction, R> function) {
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
            return ScopedValue.where(HANDLE, new TransactionImpl(this, connection))
                    .call(() -> {
                        final var handle = HANDLE.get();
                        try {
                            handle.connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
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

    @Override
    public void onInit() {
        if (this.configManager.get(ConfigPropertyKey.DATABASE_EMBEDDED)) {
            if (Vars.mods != null && Vars.mods.getMod("sql4md-postgresql-embedded") == null) {
                throw new IllegalStateException(
                        "The 'sql4md-postgresql-embedded' is missing, cannot use a local postgres instance without it");
            }
            this.factory = new EmbeddedPostgresDataSourceFactory(this.directory);
        } else {
            this.factory = new ExternalPostgresDataSourceFactory(
                    this.configManager.get(ConfigPropertyKey.DATABASE_HOST),
                    this.configManager.get(ConfigPropertyKey.DATABASE_PORT),
                    this.configManager.get(ConfigPropertyKey.DATABASE_NAME),
                    this.configManager.get(ConfigPropertyKey.DATABASE_USERNAME),
                    this.configManager.get(ConfigPropertyKey.DATABASE_PASSWORD));
        }
        try {
            this.factory.init();
        } catch (final IOException e) {
            throw new RuntimeException("Failed to init the postgres data source factory", e);
        }

        final var config = new HikariConfig();
        config.setDataSource(this.factory.create());
        config.setPoolName("sql-transaction-pool");
        config.setMaximumPoolSize(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));
        config.setMinimumIdle(2);
        config.setAutoCommit(false);
        this.source = new HikariDataSource(config);

        this.withTransaction(transaction -> {
            log.debug("Running the SQL setup script");
            final var stream =
                    this.getClass().getClassLoader().getResourceAsStream("com/xpdustry/nucleus/database/setup.sql");
            if (stream == null) {
                throw new IllegalStateException("The sql setup script is missing");
            }
            try (final var batch = ((TransactionImpl) transaction).connection.createStatement();
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

    public static final class TransactionImpl implements Transaction {

        private final PostgresDatabase database;
        private final Connection connection;

        private TransactionImpl(final PostgresDatabase database, final Connection connection) {
            this.database = database;
            this.connection = connection;
        }

        @SuppressWarnings("SqlSourceToSinkFlow")
        @Override
        public StatementBuilderImpl prepareStatement(final @Language("SQL") String statement) throws SQLException {
            return new StatementBuilderImpl(this.connection.prepareStatement(statement));
        }
    }

    public static final class StatementBuilderImpl implements StatementBuilder, StatementBuilderExecute {

        private final PreparedStatement statement;
        private int index = 1;
        private boolean batched = false;

        private StatementBuilderImpl(final PreparedStatement statement) {
            this.statement = statement;
        }

        @Override
        public StatementBuilderImpl bind(final String value) throws SQLException {
            this.statement.setString(this.index, value);
            this.index++;
            return this;
        }

        @Override
        public StatementBuilderImpl bind(final int value) throws SQLException {
            this.statement.setInt(this.index, value);
            this.index++;
            return this;
        }

        @Override
        public StatementBuilderImpl bind(final long value) throws SQLException {
            this.statement.setLong(this.index, value);
            this.index++;
            return this;
        }

        @Override
        public StatementBuilderImpl bind(final boolean value) throws SQLException {
            this.statement.setBoolean(this.index, value);
            this.index++;
            return this;
        }

        @Override
        public StatementBuilderImpl bind(final float value) throws SQLException {
            this.statement.setFloat(this.index, value);
            this.index++;
            return this;
        }

        @Override
        public StatementBuilderImpl bind(final double value) throws SQLException {
            this.statement.setDouble(this.index, value);
            this.index++;
            return this;
        }

        @Override
        public StatementBuilderImpl bind(final byte[] value) throws SQLException {
            this.statement.setBytes(this.index, value);
            this.index++;
            return this;
        }

        @Override
        public StatementBuilderImpl bind(final Instant value) throws SQLException {
            this.statement.setTimestamp(this.index, Timestamp.from(value));
            this.index++;
            return this;
        }

        @Override
        public <T> StatementBuilderExecute batch(
                final Iterable<T> values, final SQLBiConsumer<StatementValueBinder<?>, T> consumer)
                throws SQLException {
            this.batched = true;
            for (final var value : values) {
                consumer.accept(this, value);
                this.index = 0;
                this.statement.addBatch();
            }
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
        public <T> Optional<T> executeSingleSelect(final SQLFunction<ResultSet, T> mapper) throws SQLException {
            try (this.statement;
                    final var result = this.statement.executeQuery()) {
                if (result.next()) {
                    final var value = mapper.apply(result);
                    if (result.next()) {
                        throw new IllegalStateException("Got more than one result");
                    }
                    return Optional.of(value);
                } else {
                    return Optional.empty();
                }
            }
        }

        @Override
        public int executeUpdate() throws SQLException {
            try (this.statement) {
                return this.batched
                        ? IntStream.of(this.statement.executeBatch()).sum()
                        : this.statement.executeUpdate();
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

        DataSource create();
    }

    private record ExternalPostgresDataSourceFactory(
            String host, int port, String database, String username, String password)
            implements PostgresDataSourceFactory {

        @Override
        public DataSource create() {
            final var source = new PGSimpleDataSource();
            source.setUrl("jdbc:postgresql://" + this.host + ":" + this.port + "/" + this.database);
            source.setUser(this.username);
            source.setPassword(this.password);
            return source;
        }
    }

    private static final class EmbeddedPostgresDataSourceFactory implements PostgresDataSourceFactory {

        private final Path directory;
        private @Nullable EmbeddedPostgres embedded;

        private EmbeddedPostgresDataSourceFactory(final Path directory) {
            this.directory = directory;
        }

        @Override
        public void init() throws IOException {
            this.embedded = EmbeddedPostgres.builder()
                    .setDataDirectory(this.directory)
                    .setCleanDataDirectory(false)
                    .setRegisterShutdownHook(true)
                    .start();
        }

        @Override
        public void exit() throws IOException {
            Objects.requireNonNull(this.embedded, "embedded").close();
        }

        @Override
        public DataSource create() {
            return Objects.requireNonNull(this.embedded, "embedded").getPostgresDatabase();
        }
    }
}
