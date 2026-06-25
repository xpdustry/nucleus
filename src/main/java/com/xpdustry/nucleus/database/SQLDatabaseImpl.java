// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.database;

import com.xpdustry.foundation.plugin.PluginListener;
import com.xpdustry.nucleus.dependency.Inject;
import com.xpdustry.nucleus.dependency.Named;
import com.xpdustry.nucleus.function.ThrowingFunction;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
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
import org.jspecify.annotations.Nullable;

final class SQLDatabaseImpl implements SQLDatabase, PluginListener {

    private static final ScopedValue<HandleImpl> HANDLE = ScopedValue.newInstance();

    private final SQLDatabaseConfig config;
    private final Path directory;
    private @Nullable HikariDataSource source = null;

    @Inject
    public SQLDatabaseImpl(final SQLDatabaseConfig config, final @Named("home") Path directory) {
        this.config = config;
        this.directory = directory;
    }

    @Override
    public void onInit() {
        final var hikari = new HikariConfig();
        hikari.setPoolName("imperium-sql-pool");
        hikari.setMaximumPoolSize(Runtime.getRuntime().availableProcessors());
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
            hikari.setPassword(this.config.password());
        }

        this.source = new HikariDataSource(hikari);
    }

    @Override
    public void onExit() {
        Objects.requireNonNull(this.source).close();
    }

    @SuppressWarnings("SqlSourceToSinkFlow")
    @Override
    public void executeScript(final String script) {
        this.withFunctionHandle(handle -> {
            final var connection = ((HandleImpl) handle).connection;
            try (final var statement = connection.createStatement()) {
                for (var line : script.split(";", -1)) {
                    line = line.trim();
                    if (line.isBlank() || line.startsWith("--")) continue;
                    statement.addBatch(line);
                }
                statement.executeBatch();
            }
            return null;
        });
    }

    @Override
    public <R> R withFunctionHandle(final ThrowingFunction<Handle, R, SQLException> function) {
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

    private record HandleImpl(Connection connection) implements Handle {

        @SuppressWarnings("SqlSourceToSinkFlow")
        @Override
        public StatementBuilder prepareStatement(final String statement) throws SQLException {
            return new StatementBuilderImpl(this.connection.prepareStatement(statement));
        }
    }

    private static final class StatementBuilderImpl implements SQLDatabase.StatementBuilder {

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
        public <T> Stream<T> executeSelect(final ThrowingFunction<ResultSet, T, SQLException> mapper)
                throws SQLException {
            this.ensureIsNotClosed();
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
            this.ensureIsNotClosed();
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

        private void ensureIsNotClosed() throws SQLException {
            if (this.statement.isClosed()) {
                throw new IllegalStateException("The statement has already been consumed.");
            }
        }
    }
}
