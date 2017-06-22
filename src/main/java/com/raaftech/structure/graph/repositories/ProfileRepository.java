package com.raaftech.structure.graph.repositories;

import com.raaftech.structure.graph.Profile;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface ProfileRepository extends GenericRepository<Profile> {

    // Used to purge all instruments from the database
    @Query("MATCH (n:Profile) " +
            "OPTIONAL MATCH (n)<-[r]->(m) " +
            "DELETE r, n")
    void purgeProfiles();

    // Return all profiles that apply to a type tree
    @Query("MATCH (start:Type {uuid:{0}})<-[:ISA*..]-(subtypes)<-[:APPLIES_TO]-(profile:Profile) " +
            "RETURN DISTINCT profile")
    Set<Profile> findAllProfilesForType(String typeUuid);

}
