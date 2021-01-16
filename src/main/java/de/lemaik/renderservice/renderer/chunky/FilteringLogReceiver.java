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
    if (!this.ignoreMessage(level, message, null)) {
      receiver.logEvent(level, message, null);
    }
  }

  @Override
  public void logEvent(Level level, String message, Throwable thrown) {
    if (!this.ignoreMessage(level, message, thrown)) {
      receiver.logEvent(level, message, thrown);
    }
  }

  @Override
  public void logEvent(Level level, Throwable thrown) {
    if (!this.ignoreMessage(level, null, thrown)) {
      receiver.logEvent(level, thrown);
    }
  }

  protected boolean ignoreMessage(Level level, String message, Throwable thrown) {
    if (message == null) {
      return false;
    }
    if (message.startsWith("Warning: Could not load settings from")) {
      // this is intended; the render node is not supposed to use any local settings
      return true;
    }
    return false;
  }
}
