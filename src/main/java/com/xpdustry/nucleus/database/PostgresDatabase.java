// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.intellij.lang.annotations.Language;

public interface PostgresDatabase {

    <R> R withTransaction(final SQLFunction<Transaction, R> function);

    Connection newOrphanConnection() throws SQLException;

    interface Transaction {
        StatementBuilder prepareStatement(final @Language("SQL") String statement) throws SQLException;
    }

    interface StatementValueBinder<B extends StatementValueBinder<B>> {

        B bind(final String value) throws SQLException;

        B bind(final int value) throws SQLException;

        B bind(final long value) throws SQLException;

        B bind(final boolean value) throws SQLException;

        B bind(final float value) throws SQLException;

        B bind(final double value) throws SQLException;

        B bind(final byte[] value) throws SQLException;

        B bind(final Instant value) throws SQLException;
    }

    interface StatementBuilder extends StatementValueBinder<StatementBuilderExecute>, StatementBuilderExecute {

        <T> StatementBuilderExecute batch(
                final Iterable<T> values, final SQLBiConsumer<StatementValueBinder<?>, T> consumer) throws SQLException;
    }

    interface StatementBuilderExecute extends StatementValueBinder<StatementBuilderExecute> {

        <T> List<T> executeSelect(final SQLFunction<ResultSet, T> mapper) throws SQLException;

        <T> Optional<T> executeSingleSelect(final SQLFunction<ResultSet, T> mapper) throws SQLException;

        int executeUpdate() throws SQLException;

        boolean executeSingleUpdate() throws SQLException;
    }
}
