/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * This file incorporates work covered by the following license notice:
 *
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements. See the NOTICE file distributed
 *   with this work for additional information regarding copyright
 *   ownership. The ASF licenses this file to you under the Apache
 *   License, Version 2.0 (the "License"); you may not use this file
 *   except in compliance with the License. You may obtain a copy of
 *   the License at http://www.apache.org/licenses/LICENSE-2.0 .
 */
 
package com.sun.star.lib.loader;
 
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.security.AccessController;
import java.security.PrivilegedAction;
 
public final class Loader {
 
    private static ClassLoader m_Loader = null;
 
    private Loader() {}
 
    public static void main( String[] arguments ) throws Exception {
 
        // get the name of the class to be loaded from the manifest
        String className = null;
        Class clazz = Loader.class;
        ClassLoader loader = clazz.getClassLoader();
        ArrayList<URL> res = new ArrayList<>();
        try {
            Enumeration<URL> en = loader.getResources( "META-INF/MANIFEST.MF" );
            while ( en.hasMoreElements() ) {
                res.add( en.nextElement() );
            }
            // the jarfile with the com/sun/star/lib/loader/Loader.class
            // per-entry attribute is most probably the last resource in the
            // list, therefore search backwards
            for ( int i = res.size() - 1; i >= 0; i-- ) {
                URL jarurl = res.get( i );
                try {
                    JarURLConnection jarConnection =
                        (JarURLConnection) jarurl.openConnection();
                    Manifest mf = jarConnection.getManifest();
                    Attributes attrs = (mf != null) ? mf.getAttributes(
                        "com/sun/star/lib/loader/Loader.class") : null;
                    if ( attrs != null ) {
                        className = attrs.getValue( "Application-Class" );
                        if ( className != null )
                            break;
                    }
                } catch ( IOException e ) {
                    // if an I/O error occurs when opening a new
                    // JarURLConnection, ignore this manifest file
                    System.err.println( "com.sun.star.lib.loader.Loader::" +
                                        "main: bad manifest file: " + e );
                }
            }
        } catch ( IOException e ) {
            // if an I/O error occurs when getting the manifest resources,
            // try to get the name of the class to be loaded from the argument
            // list
            System.err.println( "com.sun.star.lib.loader.Loader::" +
                                "main: cannot get manifest resources: " + e );
        }
 
        // if no manifest entry was found, get the name of the class
        // to be loaded from the argument list
        String[] args;
        if ( className == null ) {
            if ( arguments.length > 0 ) {
                className = arguments[0];
                args = new String[arguments.length - 1];
                System.arraycopy( arguments, 1, args, 0, args.length );
            } else {
                throw new IllegalArgumentException(
                    "The name of the class to be loaded must be either " +
                    "specified in the Main-Class attribute of the " +
                    "com/sun/star/lib/loader/Loader.class entry " +
                    "of the manifest file or as a command line argument." );
            }
        } else {
            args = arguments;
        }
 
        // load the class with the customized class loader and
        // invoke the fizz method
        if ( className != null ) {
            ClassLoader cl = getCustomLoader();
            Thread.currentThread().setContextClassLoader(cl);
            Class c = cl.loadClass( className );
            @SuppressWarnings("unchecked")
            Method m = c.getMethod( "fizz", new Class[] { String[].class } );
            m.invoke( null, new Object[] { args } );
        }
    }
 
    public static synchronized ClassLoader getCustomLoader() {
        if ( m_Loader == null ) {
 
            // get the urls from which to load classes and resources
            // from the class path
            ArrayList<URL> vec = new ArrayList<>();
            String classpath = null;
            try {
                classpath = System.getProperty( "java.class.path" );
            } catch ( SecurityException e ) {
                // don't add the class path entries to the list of class
                // loader URLs
                System.err.println( "com.sun.star.lib.loader.Loader::" +
                    "getCustomLoader: cannot get system property " +
                    "java.class.path: " + e );
            }
            if ( classpath != null ) {
                addUrls(vec, classpath, File.pathSeparator);
            }
 
            // get the urls from which to load classes and resources
            // from the UNO installation
            String path = InstallationFinder.getPath();
            if ( path != null ) {
                callUnoinfo(path, vec);
            } else {
                System.err.println( "com.sun.star.lib.loader.Loader::" +
                    "getCustomLoader: no UNO installation found!" );
            }
 
            // copy urls to array
            final URL[] urls = new URL[vec.size()];
            vec.toArray( urls );
 
            // instantiate class loader
            m_Loader = AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> new CustomURLClassLoader(urls));
        }
 
        return m_Loader;
    }
 
    private static void addUrls(ArrayList<URL> urls, String data, String delimiter) {
        StringTokenizer tokens = new StringTokenizer( data, delimiter );
        while ( tokens.hasMoreTokens() ) {
            try {
                URL nextUrl = new File( tokens.nextToken() ).toURI().toURL();
                urls.add( nextUrl );
            } catch ( MalformedURLException e ) {
                // don't add this class path entry to the list of class loader
                // URLs
                System.err.println( "com.sun.star.lib.loader.Loader::" +
                    "getCustomLoader: bad pathname: " + e );
            }
        }
    }
 
    private static void close(InputStream c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 
    private static void callUnoinfo(String path, ArrayList<URL> urls) {
        Process p;
        try {
            String execStr [] = new String[] { new File(path, "unoinfo").getPath(), "java" };
            p = Runtime.getRuntime().exec(execStr);
        } catch (IOException e) {
            System.err.println(
                "com.sun.star.lib.loader.Loader::getCustomLoader: exec" +
                " unoinfo: " + e);
            return;
        }
        Drain myDrain = new Drain(p.getErrorStream());
        myDrain.start();
        int code;
        byte[] buf = new byte[1000];
        int n = 0;
        InputStream is = null;
        try {
            is = p.getInputStream();
            code = is.read();
            for (;;) {
                if (n == buf.length) {
                    if (n > Integer.MAX_VALUE / 2) {
                        System.err.println(
                            "com.sun.star.lib.loader.Loader::getCustomLoader:" +
                            " too much unoinfo output");
                        return;
                    }
                    byte[] buf2 = new byte[2 * n];
                    System.arraycopy(buf, 0, buf2, 0, n);
                    buf = buf2;
                }
                int k = is.read(buf, n, buf.length - n);
                if (k == -1) {
                    break;
                }
                n += k;
            }
        } catch (IOException e) {
            System.err.println(
                "com.sun.star.lib.loader.Loader::getCustomLoader: reading" +
                " unoinfo output: " + e);
            return;
        } finally {
            close(is);
        }
 
        int ev;
        try {
            ev = p.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println(
                "com.sun.star.lib.loader.Loader::getCustomLoader: waiting for" +
                " unoinfo: " + e);
            return;
        }
        if (ev != 0) {
            System.err.println(
                "com.sun.star.lib.loader.Loader::getCustomLoader: unoinfo"
                + " exit value " + n);
            return;
        }
        String s;
        if (code == '0') {
            s = new String(buf);
        } else if (code == '1') {
            try {
                s = new String(buf, "UTF-16LE");
            } catch (UnsupportedEncodingException e) {
                System.err.println(
                    "com.sun.star.lib.loader.Loader::getCustomLoader:" +
                    " transforming unoinfo output: " + e);
                return;
            }
        } else {
            System.err.println(
                "com.sun.star.lib.loader.Loader::getCustomLoader: bad unoinfo"
                + " output");
            return;
        }
        addUrls(urls, s, "\0");
    }
 

 
    
}
 
/* vim:set shiftwidth=4 softtabstop=4 expandtab: */