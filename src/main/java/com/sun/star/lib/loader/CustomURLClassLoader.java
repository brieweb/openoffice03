package com.sun.star.lib.loader;

import java.net.URL;
import java.net.URLClassLoader;

/**
 *
 * @author brian
 */
public class CustomURLClassLoader extends URLClassLoader {
 
        public CustomURLClassLoader( URL[] urls ) {
            super( urls );
        }
 
        @Override
        protected Class<?> findClass( String name ) throws ClassNotFoundException {
            // This is only called via this.loadClass -> super.loadClass ->
            // this.findClass, after this.loadClass has already called
            // super.findClass, so no need to call super.findClass again:
            throw new ClassNotFoundException( name );
        }
 
        @Override
        protected synchronized Class<?> loadClass( String name, boolean resolve )
            throws ClassNotFoundException
        {
            Class c = findLoadedClass( name );
            if ( c == null ) {
                try {
                    c = super.findClass( name );
                } catch ( ClassNotFoundException e ) {
                    return super.loadClass( name, resolve );
                } catch ( SecurityException e ) {
                    // A SecurityException "Prohibited package name: java.lang"
                    // may occur when the user added the JVM's rt.jar to the
                    // java.class.path:
                    return super.loadClass( name, resolve );
                }
            }
            if ( resolve ) {
                resolveClass( c );
            }
            return c;
        }
    }
