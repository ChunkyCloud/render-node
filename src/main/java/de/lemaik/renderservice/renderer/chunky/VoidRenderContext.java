package de.lemaik.renderservice.renderer.chunky;


import se.llbit.chunky.main.Chunky;
import se.llbit.chunky.main.ChunkyOptions;
import se.llbit.chunky.renderer.RenderContext;

import java.io.OutputStream;

/**
 * A {@link RenderContext} for Chunky that does not save any files.
 */
public class VoidRenderContext extends RenderContext {
    public VoidRenderContext() {
        super(new Chunky(ChunkyOptions.getDefaults()));
    }

    @Override
    public OutputStream getSceneFileOutputStream(String fileName) {
        return new OutputStream() {
            @Override
            public void write(int b) {
                // no-op
            }
        };
    }

    public void setRenderThreadCount(int threads) {
        config.renderThreads = threads;
    }

    public void setSppPerPass(int sppPerPass) {
        config.sppPerPass = sppPerPass;
    }
}
