/*
 * rs-rendernode is the worker node software of our RenderService.
 * Copyright (C) 2016 Wertarbyte <https://wertarbyte.com>
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

package de.lemaik.renderservice.renderer.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeadlessRenderer extends RendererApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeadlessRenderer.class);

    public HeadlessRenderer(RendererSettings settings) {
        super(settings);
    }

    @Override
    protected void onUpdateAvailable() {
        LOGGER.error("An update is available. You need to download it in order to use the renderer.");
        System.exit(1);
    }
}
