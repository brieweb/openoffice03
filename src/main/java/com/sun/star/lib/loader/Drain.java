package com.sun.star.lib.loader;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author brian
 */
public class Drain extends Thread {

    public Drain(InputStream stream) {
        super("unoinfo stderr drain");
        this.stream = stream;
    }

    @Override
    public void run() {
        try {
            while (stream.read() != -1) {
            }
        } catch (IOException e) {
            /* ignored */ }
    }

    private final InputStream stream;
}
