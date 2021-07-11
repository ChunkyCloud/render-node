package de.lemaik.renderservice.renderer.chunky;

import se.llbit.log.Level;
import se.llbit.log.Receiver;

public class FilteringLogReceiver extends Receiver {

  private final Receiver receiver;

  public FilteringLogReceiver(Receiver receiver) {
    this.receiver = receiver;
  }

  @Override
  public void logEvent(Level level, String message) {
    if (this.shouldLogMessage(level, message, null)) {
      receiver.logEvent(level, message, null);
    }
  }

  @Override
  public void logEvent(Level level, String message, Throwable thrown) {
    if (this.shouldLogMessage(level, message, thrown)) {
      receiver.logEvent(level, message, thrown);
    }
  }

  @Override
  public void logEvent(Level level, Throwable thrown) {
    if (this.shouldLogMessage(level, null, thrown)) {
      receiver.logEvent(level, thrown);
    }
  }

  protected boolean shouldLogMessage(Level level, String message, Throwable thrown) {
    if (message == null) {
      return true;
    }
    if (message.startsWith("Warning: Could not load settings from")) {
      // this is intended; the render node is not supposed to use any local settings
      return false;
    }
    if (message.startsWith("Render dump not found:")) {
      // happens if the spp in the scene description is not 0 (see #7); the render node intentionally never loads dumps
      return false;
    }
    return true;
  }
}
