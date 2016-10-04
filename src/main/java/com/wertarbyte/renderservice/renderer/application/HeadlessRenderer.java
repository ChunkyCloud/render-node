/*
 * Copyright (c) 2013-2015 Wertarbyte <http://wertarbyte.com>
 *
 * This file is part of Wertarbyte RenderService.
 *
 * Wertarbyte RenderService is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wertarbyte RenderService is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Wertarbyte RenderService.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.wertarbyte.renderservice.renderer.application;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HeadlessRenderer extends RendererApplication {
    private static final Logger LOGGER = LogManager.getLogger(HeadlessRenderer.class);

    public HeadlessRenderer(RendererSettings settings) {
        super(settings);
    }

    @Override
    protected void onUpdateAvailable() {
        LOGGER.error("An update is available. You need to download it in order to use the renderer.");
        System.exit(1);
    }
}
