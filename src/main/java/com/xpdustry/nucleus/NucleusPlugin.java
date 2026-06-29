// SPDX-License-Identifier: GPL-3.0-only
package com.xpdustry.nucleus;

import com.xpdustry.foundation.plugin.BaseMindustryPlugin;

public final class NucleusPlugin extends BaseMindustryPlugin {

    public final FoundationAPIFromNucleus foundation = new FoundationAPIFromNucleus(this);

    @Override
    public void onInit() {
        this.logger().info("Hello world");
    }
}
