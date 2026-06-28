// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.database;

import com.xpdustry.nucleus.function.ThrowingConsumer;
import com.xpdustry.nucleus.function.ThrowingFunction;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Blocking;

@Blocking
public interface Database {

    <R> R withFunctionHandle(final ThrowingFunction<Handle, R, SQLException> function);

    default void withConsumerHandle(final ThrowingConsumer<Handle, SQLException> consumer) {
        this.withFunctionHandle(handle -> {
            consumer.accept(handle);
            return Boolean.TRUE;
        });
    }

    interface Handle {

        StatementBuilder prepareStatement(final @Language("SQL") String statement) throws SQLException;
    }

    interface StatementBuilder {

        StatementBuilder push(final String value) throws SQLException;

        StatementBuilder push(final int value) throws SQLException;

        StatementBuilder push(final long value) throws SQLException;

        StatementBuilder push(final boolean value) throws SQLException;

        StatementBuilder push(final float value) throws SQLException;

        StatementBuilder push(final byte[] value) throws SQLException;

        StatementBuilder push(final Instant value) throws SQLException;

        <T> List<T> executeSelect(final ThrowingFunction<ResultSet, T, SQLException> mapper) throws SQLException;

        int executeUpdate() throws SQLException;

        boolean executeSingleUpdate() throws SQLException;
    }
}
