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

import com.wertarbyte.consoleapplication.ConsoleApplication;
import com.wertarbyte.consoleapplication.commands.Command;
import com.wertarbyte.consoleapplication.commands.CommandListener;
import com.wertarbyte.consoleapplication.commands.annotation.CommandHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HeadlessRenderer extends RendererApplication implements CommandListener {
    private static final Logger LOGGER = LogManager.getLogger(HeadlessRenderer.class);
    private final ConsoleApplication consoleApplication;

    public HeadlessRenderer(RendererSettings settings) {
        super(settings);
        consoleApplication = new ConsoleApplication() {
            @Override
            @CommandHandler(value = "help", description = "Show all available commands")
            public void printHelp() {
                LOGGER.info("Available commands:");
                for (CommandHandler handler : getCommandHandlers()) {
                    LOGGER.info(handler.value() + "  -  " + handler.description());
                }
            }

            @Override
            protected void onCommandUnknown(Command command) {
                LOGGER.info("Unknown command. Use \"help\" to get a list of all commands.");
            }
        };
        consoleApplication.addCommandHandler(this);
    }

    @Override
    public void start() {
        consoleApplication.start();
        super.start();
    }

    @Override
    @CommandHandler(value = "kill", description = "Stop the renderer immediately")
    public void stop() {
        consoleApplication.stop();
        super.stop();
        LOGGER.info("Renderer should stop now...");
    }

    @CommandHandler(value = "stop", description = "Finish all running tasks and stop the server")
    public void slowStop() {
        LOGGER.info("No new assignments will be started but running assignments will be completed.");
        LOGGER.info("Use 'kill' if you want to stop the renderer immediately.");
        // TODO
        stop();
    }

    @Override
    protected void onUpdateAvailable() {
        LOGGER.error("An update is available. You need to download it in order to use the renderer.");
        System.exit(1);
    }

    /*
    @CommandHandler(value = "list", description = "Show running tasks")
    public void showRunningTasks() {
        LOGGER.info(getRenderer().getRenderingInstancesCount() + " tasks running");
        for (RemoteRendererClient p : getRenderer().getRenderingInstances()) {
            LOGGER.info(String.format("Task %s (Job %s): %s", p.getAdditionalData().get("taskId"), p.getAdditionalData().get("jobId"), p.getChunky().getStatus()));
        }
    }

    @CommandHandler(value = "sps", description = "Show current samples per second")
    public void showSpp() {
        double sps = 0;
        for (RemoteRendererClient p : getRenderer().getRenderingInstances()) {
            sps += p.getChunky().getSamplesPerSecond();
        }
        LOGGER.info(String.format("Rendering with a total of %.2f samples per second", sps));
    }

    @CommandHandler(value = "whoami", description = "Show the unique ID of this renderer")
    public void showId() {
        LOGGER.info("Renderer ID: " + getId());
    }
    */
}
