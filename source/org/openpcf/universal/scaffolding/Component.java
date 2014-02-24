/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.scaffolding;

import java.awt.*;

/**
 * Component represents the most abstract part within the Domain Model.
 * Essentially, everything is a component and consists itself of various
 * specialized components. The application will mostly interact with
 * Components and more usefully, collections of components.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class Component {
    private String name;
    private String icon;

    public Component(String name, String icon) {

        //make sure connection object exists, reuse
        // query for name try catch

    }
}
