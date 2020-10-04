package de.lemaik.renderservice.renderer.chunky;

public class RenderException extends Exception {

  public RenderException(String message, Exception inner) {
    super(message, inner);
  }
}
