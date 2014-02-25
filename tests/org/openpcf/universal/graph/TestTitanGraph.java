/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.graph;

import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.blueprints.Vertex;
import org.junit.Test;
import org.openpcf.universal.initialization.UniversalGraphFactory;
import org.openpcf.universal.initialization.UniversalLoggingFactory;
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
public class TestTitanGraph {

    @Test
    public void quickTest() {

        Logger logger = UniversalLoggingFactory.create(TestTitanGraph.class);

        try{
            UniversalGraphFactory.load();
        }
        catch (Exception e){
            logger.error(e.getMessage());
        }

        TitanGraph g = UniversalGraphFactory.create("/tmp/test-titandb");

        for (Vertex v: g.getVertices("name", "genericUnixLikeOperatingSystem")){
            logger.warn(v.toString());
        }
    }
}
