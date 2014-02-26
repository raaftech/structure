/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.initialization;

import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.blueprints.Vertex;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;

/**
 * A quick test with Titan as graph backend, using TinkerPop Blueprints,
 * all through gremlin as a JSR223 scripting handler.
 * Testing persistence behaviour.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class TestUniversalGraphSingleton {

    @Test
    public void testGraphObjectCreation() {

        Logger logger = UniversalLogger.INSTANCE.getLogger();

        TitanGraph g = UniversalGraph.INSTANCE.getGraph();
        TitanGraph h = UniversalGraph.INSTANCE.getGraph();

        Assert.assertEquals("Graphs are not the same object", g, h);

        for (Vertex v : g.getVertices("name", "genericUnixLikeOperatingSystem")) {
            logger.info(v.toString());
        }
        for (Vertex v : h.getVertices("name", "genericWindowsLikeOperatingSystem")) {
            logger.info(v.toString());
        }
    }
}
