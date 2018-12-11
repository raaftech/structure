package tech.raaf.structure.graph.repositories;

import org.springframework.data.neo4j.annotation.Query;
import tech.raaf.structure.graph.Result;

public interface ResultRepository extends GenericRepository<Result> {

    @Query("MATCH (n:Result {uuid: {0}}) RETURN n")
    Result findByUuid(String uuid);

    @Query("MATCH (n:Result) RETURN count(n)")
    long count();
}
