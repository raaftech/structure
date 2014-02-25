/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.initialization;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.graphdb.configuration.GraphDatabaseConfiguration;
import com.tinkerpop.gremlin.groovy.jsr223.GremlinGroovyScriptEngineFactory;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static com.thinkaurelius.titan.graphdb.configuration.GraphDatabaseConfiguration.INDEX_BACKEND_KEY;
import static com.thinkaurelius.titan.graphdb.configuration.GraphDatabaseConfiguration.STORAGE_DIRECTORY_KEY;


/**
 * A preliminary Universal Graph Database Factory.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-02-14
 */
public class UniversalGraphFactory {

    public static final String INDEX_TYPE = "es";
    public static final String INDEX_NAME = "search";

    public static TitanGraph create(final String directory) {
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

        TitanGraph graph = TitanFactory.open(config);
        return graph;
    }

    public static void load() throws IOException, InterruptedException, ScriptException {

        ScriptEngine engine = new GremlinGroovyScriptEngineFactory().getScriptEngine();
        engine.eval(new FileReader("scripts/init.grm"));

    }

}
