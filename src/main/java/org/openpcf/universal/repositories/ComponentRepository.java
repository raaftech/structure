/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.repositories;

import org.openpcf.universal.model.Component;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.Set;

/**
 * Give a description of ComponentRepository here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-11-12
 */
public interface ComponentRepository extends GraphRepository<Component> {

    public Component findByName(String name);
}
