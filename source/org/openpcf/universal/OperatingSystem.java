/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.*;

/**
 * Give a description of OperatingSystem here.
 *
 * @author      Rubin Simons <rubin.simons@raaftech.com>
 * @since       2014-01-06
 * @version     0.6
 */
public class OperatingSystem extends Software implements Checkable, Stopable {
    private Service[] assignedServices;
    private Configuration[] configuredDevices;
}
