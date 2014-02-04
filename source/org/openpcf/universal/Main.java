/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal;

/**
 * The main class and method, possibly referenced in Ant or Maven.
 *
 * @author      Rubin Simons <rubin.simons@raaftech.com>
 * @since       2014-01-06
 * @version     0.6
 */
public class Main {

    private static String version = "0.1";

    public static void main(String[] args) {

        System.out.println("Yes,yes..");
    }

    public String getVersion(){
        return version;
    }

}
