package com.xpdustry.nucleus.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.intellij.lang.annotations.Language;

public interface PostgresDatabase {

    <R> R withHandle(final SQLFunction<Handle, R> function);

    Connection newOrphanConnection() throws SQLException;

    interface Handle {
        StatementBuilder prepareStatement(final @Language("SQL") String statement) throws SQLException;
    }

    interface StatementBuilder {
        StatementBuilder push(final String value) throws SQLException;

        StatementBuilder push(final int value) throws SQLException;

        StatementBuilder push(final long value) throws SQLException;

        StatementBuilder push(final boolean value) throws SQLException;

        StatementBuilder push(final float value) throws SQLException;

        StatementBuilder push(final double value) throws SQLException;

        StatementBuilder push(final byte[] value) throws SQLException;

        StatementBuilder push(final Instant value) throws SQLException;

        StatementBuilder addToBatch() throws SQLException;

        <T> List<T> executeSelect(final SQLFunction<ResultSet, T> mapper) throws SQLException;

        <T> Optional<T> executeSingleSelect(final SQLFunction<ResultSet, T> mapper) throws SQLException;

        int executeUpdate() throws SQLException;

        boolean executeSingleUpdate() throws SQLException;
    }
}
