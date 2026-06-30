// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.network;

import com.xpdustry.nucleus.database.PostgresDatabase;
import java.net.InetAddress;

public final class InetAddressWhitelist {

    private final PostgresDatabase database;

    public InetAddressWhitelist(final PostgresDatabase database) {
        this.database = database;
    }

    public boolean contains(final InetAddress address) {
        return this.database.withHandle(handle -> handle.prepareStatement("""
                        SELECT 1 FROM "address_whitelist" "a"
                        WHERE ?::inet <<= "a"."address" LIMIT 1
                        """)
                .push(address.getHostAddress())
                .executeSingleSelect(_ -> Boolean.TRUE)
                .orElse(false));
    }

    public void insert(final InetAddress address, final String reason) {
        this.database.withHandle(handle -> handle.prepareStatement("""
                        INSERT INTO "address_whitelist"("address", "reason") VALUES (?, ?)
                        """)
                .push(address.getHostAddress())
                .push(reason)
                .executeSingleUpdate());
    }
}
