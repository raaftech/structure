/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.initialization;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.graphdb.configuration.GraphDatabaseConfiguration;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.groovy.jsr223.GremlinGroovyScriptEngineFactory;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;

import javax.script.ScriptEngine;
import java.io.File;
import java.io.FileReader;
import java.util.Properties;

import static com.thinkaurelius.titan.graphdb.configuration.GraphDatabaseConfiguration.INDEX_BACKEND_KEY;
import static com.thinkaurelius.titan.graphdb.configuration.GraphDatabaseConfiguration.STORAGE_DIRECTORY_KEY;


/**
 * A preliminary Universal Graph Singleton.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-02-14
 */
public enum UniversalGraph {
    INSTANCE;

    private TitanGraph graph;

    private UniversalGraph() {

        Logger logger = UniversalLogger.INSTANCE.getLogger();
        Properties settings = UniversalSettings.INSTANCE.getSettings();

        String INDEX_TYPE = settings.getProperty("universal.titandb.indexer");
        String INDEX_NAME = "search";
        String directory = settings.getProperty("universal.titandb.location");

        BaseConfiguration config = new BaseConfiguration();
        Configuration storage = config.subset(GraphDatabaseConfiguration.STORAGE_NAMESPACE);

        // Disable automatic type (property, attribute, etc)creation.
        config.setProperty("autotype", "none");

        // Configuring local backend.
        storage.setProperty(GraphDatabaseConfiguration.STORAGE_BACKEND_KEY, "local");
        storage.setProperty(GraphDatabaseConfiguration.STORAGE_DIRECTORY_KEY, directory);

        // Configuring elastic search index.
        Configuration index = storage.subset(GraphDatabaseConfiguration.INDEX_NAMESPACE).subset(INDEX_NAME);
        index.setProperty(INDEX_BACKEND_KEY, INDEX_TYPE);
        index.setProperty("local-mode", true);
        index.setProperty("client-only", false);
        index.setProperty(STORAGE_DIRECTORY_KEY, directory + File.separator + INDEX_TYPE);

        graph = TitanFactory.open(config);

        Iterable<Vertex> Vs = graph.getVertices("name", "genericUnixLikeOperatingSystem");

        if (!Vs.iterator().hasNext()){
            logger.info("Default data not found, loading default dataset.");
            ScriptEngine engine = new GremlinGroovyScriptEngineFactory().getScriptEngine();
            try{
                engine.eval(new FileReader("scripts/init.grm"));
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }


    }

    public TitanGraph getGraph(){
        return graph;
    }
}
