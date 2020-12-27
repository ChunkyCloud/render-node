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

import com.lexicalscope.jewel.cli.Option;
import java.io.File;

/**
 * Commandline arguments, parsed by {@link com.lexicalscope.jewel.cli.CliFactory}.
 */
public interface CommandlineArguments {
    @Option(longName = "headless",
            description = "Run without GUI",
            defaultValue = "false")
    boolean getHeadless();

    @Option(longName = "cpu-load",
            description = "Maximum CPU load",
            defaultValue = "100")
    int getCpuLoad();

    @Option(shortName = "t",
            longName = "thread-count",
            description = "Number of threads per processes",
            defaultValue = "2")
    int getThreads();

    @Option(longName = "chunky-xms",
            description = "minimum memory for chunky processes, in MB",
            defaultValue = "1024")
    int getXms();

    @Option(longName = "chunky-xmx",
            description = "maximum memory for chunky processes, in MB",
            defaultValue = "2048")
    int getXmx();

    @Option(longName = "texturepacks-path",
        description = "path for texturepacks",
        defaultToNull = true)
    File getTexturepacksPath();

    @Option(longName = "job-path",
            description = "path for temporary job data",
            defaultToNull = true)
    File getJobPath();

    @Option(longName = "upload-rate",
            description = "maximum upload rate in KB/s",
            defaultToNull = true)
    Integer getMaxUploadRate();

    @Option(longName = "master",
            description = "URL of the master server API endpoint",
            defaultValue = "https://api.chunkycloud.lemaik.de")
    String getMasterServer();

    @Option(longName = "cache-directory",
            description = "cache directory for scene files",
            defaultToNull = true)
    File getCacheDirectory();

    @Option(longName = "max-cache-size",
            description = "maximum cache size, in mb",
            defaultToNull = true)
    Long getMaxCacheSize();

    @Option(longName = "name",
            defaultToNull = true)
    String getName();

    @Option(longName="api-key",
            description="API Key",
            defaultToNull = true)
    String getApiKey();
}
