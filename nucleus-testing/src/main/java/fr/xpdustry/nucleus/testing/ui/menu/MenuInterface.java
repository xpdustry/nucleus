/*
 * Nucleus, the software collection powering Xpdustry.
 * Copyright (C) 2022  Xpdustry
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.xpdustry.nucleus.testing.ui.menu;

import fr.xpdustry.nucleus.testing.ui.Action;
import fr.xpdustry.nucleus.testing.ui.Interface;

/**
 * Nice class to make menus :).
 *
 * <pre> {@code
 *      final var COUNT = StateKey.of("count", Integer.class);
 *      final var menu = MenuInterface.create();
 *
 *      menu.addTransformer(ctx -> {
 *          ctx.getState().put(COUNT, ctx.getState().get(count, 0) + 1);
 *          ctx.getPane().setTitle("Yes");
 *          ctx.getPane().addOptionRow(MenuOption.of("click " + ctx.getState().get(COUNT), Action.open()));
 *      });
 *
 *      MoreEvents.subscribe(EventType.PlayerJoin.class, event -> {
 *          menu.open(event.player);
 *      });
 * } </pre>
 */
public interface MenuInterface extends Interface<MenuInterface, MenuView, MenuPane, MenuPane.Mutable> {

    static MenuInterface create() {
        return new MenuInterfaceImpl();
    }

    void setCloseAction(final Action<MenuView> action);
}
