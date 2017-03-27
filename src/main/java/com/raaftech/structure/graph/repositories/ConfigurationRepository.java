package com.raaftech.structure.graph.repositories;

import com.raaftech.structure.graph.Result;
import org.springframework.data.neo4j.annotation.Query;

public interface ConfigurationRepository extends GenericRepository<Result> {

    @Query("MATCH (n:Configuration) RETURN count(n)")
    long count();
}
