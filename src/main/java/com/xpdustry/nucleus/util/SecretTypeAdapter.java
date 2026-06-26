// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

public final class SecretTypeAdapter extends TypeAdapter<Secret> {
    @Override
    public void write(final JsonWriter out, final Secret value) throws IOException {
        out.value(value.value());
    }

    @Override
    public Secret read(final JsonReader in) throws IOException {
        return new Secret(in.nextString());
    }
}
