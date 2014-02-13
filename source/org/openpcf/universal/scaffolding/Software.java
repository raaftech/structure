/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.scaffolding;

import java.awt.*;

/**
 * Represents software in the most generic sense of the word.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class Software extends Component {
    private String vendor;
    private String version;

    public Software(String name, Image icon, String vendor, String version) {
        super(name, icon);
        this.vendor = vendor;
        this.version = version;
    }
}
