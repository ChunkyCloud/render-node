/*
 * Copyright (C) 2016-2026 leMaik and contributors
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

package de.lemaik.renderservice.renderer;

import com.lexicalscope.jewel.cli.Cli;
import com.lexicalscope.jewel.cli.CliFactory;
import de.lemaik.renderservice.renderer.application.CommandlineArguments;
import de.lemaik.renderservice.renderer.application.HeadlessRenderer;
import de.lemaik.renderservice.renderer.application.RendererSettings;
import de.lemaik.renderservice.renderer.chunky.FilteringLogReceiver;
import de.lemaik.renderservice.renderer.chunky.Slf4jLogReceiver;
import se.llbit.log.Level;
import se.llbit.log.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * The main class.
 */
public class Main {
    public static final String VERSION;
    public static final int VERSION_CODE = 3;

    static {
        String version = Main.class.getPackage().getImplementationVersion();
        VERSION = version == null ? "unknown" : version;
        Log.setReceiver(new FilteringLogReceiver(new Slf4jLogReceiver()), Level.ERROR, Level.WARNING, Level.INFO);
    }

    private Main() {
    }

    public static void main(String[] args) {
        Cli<CommandlineArguments> cli = CliFactory.createCli(CommandlineArguments.class);

        CommandlineArguments arguments;
        try {
            arguments = cli.parseArguments(args);
        } catch (Exception e) {
            System.out.println(cli.getHelpMessage());
            return;
        }

        String apiKey = Optional.ofNullable(arguments.getApiKey()).orElse(System.getenv("API_KEY"));
        if (apiKey == null) {
            String apiKeyFile = arguments.getApiKeyFile();
            if (apiKeyFile != null) {
                try {
                    apiKey = Files.readString(Path.of(apiKeyFile)).trim();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read the API key from " + apiKeyFile, e);
                }
            }
            if (apiKey == null) {
                System.err.println(
                        "Missing API key. Use the --api-key or --api-key-file option or the API_KEY environment variable to specify one.");
                System.exit(-1);
            }
        }

        RendererSettings settings = new RendererSettings(
                arguments.getCpuLoad(),
                arguments.getThreads(),
                arguments.getJobPath(),
                arguments.getTexturepacksPath(),
                arguments.getApiUrl(),
                arguments.getCacheDirectory(),
                arguments.getMaxCacheSize(),
                apiKey
        );
        new HeadlessRenderer(settings).start();
    }
}
