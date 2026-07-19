# ChunkyCloud Render Node

Render node for ChunkyCloud, a distributed rendering service for
[Chunky](https://github.com/chunky-dev/chunky).

This service runs headlessly, connects to a ChunkyCloud API server, polls for
render tasks, downloads the required scene data and resource packs, renders the
assigned tile with Chunky, reports progress, uploads the rendered result, and
marks the task as finished.

## Requirements

- Java 17 or newer
- A render-node API key issued by a [ChunkyCloud server](https://github.com/ChunkyCloud/server)

## Getting started

1. Get a render node API key (also called render node token) from the ChunkyCloud server you want to connect to
2. Download the [latest release](https://github.com/ChunkyCloud/render-node/releases/latest)
3. Run the jar

   ```bash
   java -jar rendernode-v2.0.1.jar \
     --api-key your-render-node-api-key
   ```

> [!IMPORTANT]
> You may only run a single process per API key. If you want to host multiple render nodes, get a different API key for each of them.

## Configuration

| Option                 | Default                             | Description                                                            |
| ---------------------- | ----------------------------------- | ---------------------------------------------------------------------- |
| `--api`                | `https://api.chunkycloud.lemaik.de` | ChunkyCloud API endpoint.                                              |
| `--api-key`            | unset                               | Render-node API key.                                                   |
| `--api-key-file`       | unset                               | File containing the render-node API key. Useful for container secrets. |
| `--cpu-load`           | `100`                               | Maximum Chunky CPU load.                                               |
| `-t`, `--thread-count` | `2`                                 | Number of render threads used by Chunky.                               |
| `--job-path`           | `./rs_jobs`                         | Directory for temporary per-task data.                                 |
| `--texturepacks-path`  | `./rs_texturepacks`                 | Directory for downloaded resource packs.                               |
| `--cache-directory`    | `./rs_cache`                        | HTTP cache directory for downloaded scene resources.                   |
| `--max-cache-size`     | `512`                               | Maximum HTTP cache size, in MB.                                        |

The API key can also be provided through the `API_KEY` environment variable.

## Runtime directories

When no custom paths are provided, the render node creates these directories in
the current working directory:

| Directory         | Purpose                                                 |
| ----------------- | ------------------------------------------------------- |
| `rs_jobs`         | Temporary task working directories                      |
| `rs_texturepacks` | Downloaded resource packs                               |
| `rs_cache`        | HTTP cache for API and scene downloads                  |
| `rs_chunky`       | Chunky settings directory used by the headless renderer |

For long-running nodes, mount these directories on persistent storage so resource
packs and cached scene files survive container restarts.

## Docker

```bash
docker run --rm \
  -e API_KEY=your-render-node-api-key \
  -v chunkycloud-render-node-data:/opt/cc-rendernode/data \
  ghcr.io/chunkycloud/render-node:latest
```

The container runs from `/opt/cc-rendernode/data`, so the default runtime
directories are created inside the mounted data volume. Mounting a volume is recommended so that cached data (e.g. resource packs and scene files) are persisted across container restarts.

You can use a Docker secret to specify the API key, just launch with `--api-key-file /run/secrets/chunkycloud-node-token` instead of using an environment variable.

## Development

> [!CAUTION]
> Please **do not run customized render nodes against leMaik's ChunkyCloud server**. Your contributions are very welcome, but please test them against a local ChunkyCloud server instance.

The project builds a self-contained jar file with its dependencies. You'll need Maven 3.

```bash
mvn package
```

The runnable jar file is written to:

```text
target/rendernode-jar-with-dependencies.jar
```

The main entry point is
`de.lemaik.renderservice.renderer.Main`.

## License

This project is licensed under the GNU General Public License v3.0 or later.
See [LICENSE.txt](LICENSE.txt).
