/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.model;

/**
 * Component represents the most abstract part within the Domain Model.
 * Essentially, everything is a component and consists itself of various
 * specialized components. The application will mostly interact with
 * Components and more usefully, collections of components.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-01-06
 */
abstract class Component {
    private String name;
    private String desc;
    private String uuid;

    public Component() {}
}
