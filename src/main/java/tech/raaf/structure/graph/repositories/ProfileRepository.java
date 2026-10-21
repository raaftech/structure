package tech.raaf.structure.graph.repositories;

import com.google.common.collect.ImmutableMap;
import tech.raaf.structure.graph.Graph;
import tech.raaf.structure.graph.NodeMapper;
import tech.raaf.structure.graph.Profile;

import java.util.List;

public class ProfileRepository extends GenericRepository<Profile> {

    public ProfileRepository() {
        super("Profile");
    }

    // Used to purge all profiles from the database
    public void purgeProfiles() {
        Graph.query("MATCH (n:Profile) DETACH DELETE n");
    }

    // Return all profiles that apply to a type tree. They will be sorted ASC based on distance to the root type.
    public List<Profile> findAllProfilesForType(String typeUuid) {
        return NodeMapper.mapAll(Graph.query("MATCH (start:Type {uuid: $uuid})<-[:ISA*0..]-(child:Type)" +
                "<-[:APPLIES_TO]-(profile:Profile) " +
                "MATCH distanceToRoot = shortestPath((child)-[:ISA]->(t:Type {name: 'Type'})) " +
                "WITH distanceToRoot, profile ORDER BY length(distanceToRoot) ASC " +
                "RETURN DISTINCT profile AS n", ImmutableMap.of("uuid", typeUuid)), "n");
    }
}
