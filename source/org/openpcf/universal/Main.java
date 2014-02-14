/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal;

import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.blueprints.Vertex;
import org.openpcf.universal.graph.UniversalGraphFactory;

/**
 * The main class and method, possibly referenced in Ant or Maven.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-01-06
 * @version 0.6
 */
public class Main {

    public static void main(String[] args) {

        TitanGraph g = UniversalGraphFactory.create("target/main-titandb");

        for (Vertex vertex : g.getVertices()) {
            System.out.println(vertex.getProperty("name"));
        }
    }
}
