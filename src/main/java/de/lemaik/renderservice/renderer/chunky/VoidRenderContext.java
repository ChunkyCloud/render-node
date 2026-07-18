/*
 * Copyright (C) 2022-2026 leMaik and contributors
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.lemaik.renderservice.renderer.chunky;


import se.llbit.chunky.main.Chunky;
import se.llbit.chunky.main.ChunkyOptions;
import se.llbit.chunky.renderer.RenderContext;

import java.io.OutputStream;

/**
 * A {@link RenderContext} for Chunky that does not save any files.
 */
public class VoidRenderContext extends RenderContext {
    public VoidRenderContext() {
        super(new Chunky(ChunkyOptions.getDefaults()));
    }

    @Override
    public OutputStream getSceneFileOutputStream(String fileName) {
        return new OutputStream() {
            @Override
            public void write(int b) {
                // no-op
            }
        };
    }

    public void setRenderThreadCount(int threads) {
        config.renderThreads = threads;
    }

    public void setSppPerPass(int sppPerPass) {
        config.sppPerPass = sppPerPass;
    }
}
