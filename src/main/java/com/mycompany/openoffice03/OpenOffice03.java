package com.mycompany.openoffice03;

/**
 *
 * @author brian
 */
public class OpenOffice03 {
        public static void fizz(String[] args) {
        try {
            // get the remote office component context
            com.sun.star.uno.XComponentContext xContext  =
                com.sun.star.comp.helper.Bootstrap.bootstrap();

            System.out.println("Connected to a running office ...");

            com.sun.star.lang.XMultiComponentFactory xMCF =
                xContext.getServiceManager();

            String available = (xMCF != null ? "available" : "not available");
            System.out.println( "remote ServiceManager is " + available );
        }
        catch (java.lang.Exception e){
            e.printStackTrace();
        }
        finally {
            System.exit(0);
        }
    }
}
