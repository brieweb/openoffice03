package com.sun.star.lib.loader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

/**
 *
 * @author brian
 */
public class StreamGobbler extends Thread {

    InputStream m_istream;

    public StreamGobbler(InputStream istream) {
        m_istream = istream;
    }

    @Override
    public void run() {
        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(m_istream, "UTF-8"));
            // read from input stream
            while (br.readLine() != null) {
                // don't handle line content
            }
            br.close();
        } catch (UnsupportedEncodingException e) {
            // cannot read from input stream
        } catch (IOException e) {
            // stop reading from input stream
        }
    }
}
