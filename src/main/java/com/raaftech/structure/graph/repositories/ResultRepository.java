package com.raaftech.structure.graph.repositories;

import com.raaftech.structure.graph.Result;
import org.springframework.data.neo4j.annotation.Query;

public interface ResultRepository extends GenericRepository<Result> {

    @Query("MATCH (n:Result) RETURN count(n)")
    long count();
}
