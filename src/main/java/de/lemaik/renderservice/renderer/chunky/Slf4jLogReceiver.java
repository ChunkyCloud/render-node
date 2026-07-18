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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.llbit.log.Level;
import se.llbit.log.Receiver;

/**
 * An adapter from Chunky's logger to Slf4J.
 */
public class Slf4jLogReceiver extends Receiver {

  private static final Logger LOGGER = LoggerFactory.getLogger("Chunky");

  @Override
  public void logEvent(Level level, String message) {
    switch (level) {
      case ERROR:
        LOGGER.error(message);
        break;
      case WARNING:
        LOGGER.warn(message);
        break;
      case INFO:
      default:
        LOGGER.info(message);
        break;
    }
  }

  @Override
  public void logEvent(Level level, String message, Throwable thrown) {
    switch (level) {
      case ERROR:
        LOGGER.error(message, thrown);
        break;
      case WARNING:
        LOGGER.warn(message, thrown);
        break;
      case INFO:
      default:
        LOGGER.info(message, thrown);
        break;
    }
  }

  @Override
  public void logEvent(Level level, Throwable thrown) {
    switch (level) {
      case ERROR:
        LOGGER.error(thrown.getMessage(), thrown);
        break;
      case WARNING:
        LOGGER.warn(thrown.getMessage(), thrown);
        break;
      case INFO:
      default:
        LOGGER.info(thrown.getMessage(), thrown);
    }
  }
}
