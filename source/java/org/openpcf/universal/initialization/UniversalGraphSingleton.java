/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.initialization;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.graphdb.configuration.GraphDatabaseConfiguration;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;

import java.io.File;
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
public enum UniversalGraphSingleton {
    INSTANCE;

    private TitanGraph graph;

    private UniversalGraphSingleton() {

        Logger logger = UniversalLoggerFactory.getLogger(this.getClass());
        Properties settings = UniversalSettingsSingleton.INSTANCE.getSettings();

        String INDEX_TYPE = settings.getProperty("universal.graph.dbindex");
        String INDEX_NAME = "search";
        String directory = settings.getProperty("universal.graph.dbhome");

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
    }

    public TitanGraph getGraph() {
        return graph;
    }
}
