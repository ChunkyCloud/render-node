package de.lemaik.renderservice.renderer.chunky;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.llbit.log.Level;
import se.llbit.log.Receiver;

/**
 * An adapter between Log4J and Chunky's logger.
 */
public class Log4jLogReceiver extends Receiver {

  private static final Logger LOGGER = LogManager.getLogger("Chunky");

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
