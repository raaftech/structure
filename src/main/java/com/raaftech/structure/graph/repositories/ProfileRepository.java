package com.raaftech.structure.graph.repositories;

import com.raaftech.structure.graph.Profile;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface ProfileRepository extends GenericRepository<Profile> {

    // Used to purge all instruments from the database
    @Query("MATCH (n:Profile) " +
            "OPTIONAL MATCH (n)<-[r]->(m) " +
            "DELETE r, n")
    void purgeProfiles();

    // Return all profiles that apply to a type tree. They will be sorted ASC based on distance to the root type.
    @Query("MATCH (start:Type {uuid:{0}})<-[:ISA*0..]-(child:Type)<-[:APPLIES_TO]-(profile:Profile), " +
            "distanceToRoot = shortestPath((child)-[:ISA]->(t:Type {name:\"Type\"})) " +
            "WITH distanceToRoot, profile ORDER BY length(distanceToRoot) ASC " +
            "RETURN DISTINCT profile")
    List<Profile> findAllProfilesForType(String typeUuid);

}
