/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2024 huangyuhui <huanghongxun2008@126.com> and contributors
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
package org.jackhuang.hmcl.download.java.foojay;

import org.jackhuang.hmcl.download.java.JavaDistribution;

/**
 * @author Glavo
 */
public enum FoojayJavaDistribution implements JavaDistribution {
    ADOPTIUM("Adoptium", true),
    LIBERICA("Liberica", true),
    GRAALVM("Oracle GraalVM", false);

    private final String displayName;
    private final boolean jreSupported;

    FoojayJavaDistribution(String displayName, boolean jreSupported) {
        this.displayName = displayName;
        this.jreSupported = jreSupported;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public boolean isJreSupported() {
        return jreSupported;
    }
}
