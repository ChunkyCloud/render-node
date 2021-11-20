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

package de.lemaik.renderservice.renderer;

import com.lexicalscope.jewel.cli.Cli;
import com.lexicalscope.jewel.cli.CliFactory;
import de.lemaik.renderservice.renderer.application.CommandlineArguments;
import de.lemaik.renderservice.renderer.application.HeadlessRenderer;
import de.lemaik.renderservice.renderer.application.RendererSettings;
import de.lemaik.renderservice.renderer.chunky.FilteringLogReceiver;
import de.lemaik.renderservice.renderer.chunky.Log4jLogReceiver;
import java.util.Optional;
import se.llbit.log.Level;
import se.llbit.log.Log;

/**
 * The main class.
 */
public class Main {

  public static final String VERSION = Main.class.getPackage().getImplementationVersion();
  public static final int API_VERSION = 1;

  static {
    Log.setReceiver(new FilteringLogReceiver(new Log4jLogReceiver()), Level.ERROR, Level.WARNING,
        Level.INFO);
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
      System.err.println(
          "Missing API key. Use the --api-key option or the API_KEY environment variable to specify one.");
      System.exit(-1);
    }

    RendererSettings settings = new RendererSettings(
        arguments.getCpuLoad(),
        arguments.getThreads(),
        arguments.getXms(),
        arguments.getXmx(),
        arguments.getJobPath(),
        arguments.getTexturepacksPath(),
        arguments.getMaxUploadRate(),
        arguments.getMasterServer(),
        arguments.getCacheDirectory(),
        arguments.getMaxCacheSize(),
        arguments.getName(),
        apiKey
    );
    new HeadlessRenderer(settings).start();
  }
}
