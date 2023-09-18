package com.github.wolray.seq;

import java.util.NoSuchElementException;

/**
 * @author wolray
 */
public final class StopException extends NoSuchElementException {
    static final StopException INSTANCE = new StopException();

    private StopException() {}

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
