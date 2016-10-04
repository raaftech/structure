package com.raaftech.structure.graph.repositories;

import com.raaftech.structure.graph.Profile;
import org.springframework.data.neo4j.annotation.Query;

public interface ProfileRepository extends GenericRepository<Profile> {

    // Used to purge all instruments from the database
    @Query("MATCH (n:Profile) " +
            "OPTIONAL MATCH (n)<-[r]->(m) " +
            "DELETE r, n")
    void purgeProfiles();


}
