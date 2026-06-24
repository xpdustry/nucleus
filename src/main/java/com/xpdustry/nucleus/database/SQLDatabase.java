// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.database;

import com.xpdustry.nucleus.function.ThrowingConsumer;
import com.xpdustry.nucleus.function.ThrowingFunction;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

public interface SQLDatabase {

    <R extends @Nullable Object> R withFunctionHandle(final ThrowingFunction<Handle, R, SQLException> function);

    default void withConsumerHandle(final ThrowingConsumer<Handle, SQLException> consumer) {
        this.<@Nullable Void>withFunctionHandle(handle -> {
            consumer.accept(handle);
            return null;
        });
    }

    void executeScript(final String script) throws SQLException;

    interface Handle {

        StatementBuilder prepareStatement(final String statement) throws SQLException;
    }

    interface StatementBuilder {

        StatementBuilder push(final String value) throws SQLException;

        StatementBuilder push(final int value) throws SQLException;

        StatementBuilder push(final long value) throws SQLException;

        StatementBuilder push(final boolean value) throws SQLException;

        StatementBuilder push(final float value) throws SQLException;

        StatementBuilder push(final byte[] value) throws SQLException;

        StatementBuilder push(final Instant value) throws SQLException;

        <T> Stream<T> executeSelect(final ThrowingFunction<ResultSet, T, SQLException> mapper) throws SQLException;

        int executeUpdate() throws SQLException;

        boolean executeSingleUpdate() throws SQLException;
    }
}
