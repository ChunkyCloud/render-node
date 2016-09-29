package com.wertarbyte.renderservice.renderer.application;

import com.wertarbyte.renderservice.libchunky.ChunkyWrapper;

/**
 * A factory for {@link ChunkyWrapper}s.
 */
public interface ChunkyWrapperFactory {
    ChunkyWrapper getChunkyInstance();
}
