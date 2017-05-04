package schema;

import com.google.common.collect.ImmutableSet;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

public class EntityTest extends SchemaTest {


    @Override
    protected Set<String> good() {
        return ImmutableSet.of(
                "schema/goodEntity01.json",
                "schema/badEntity01.json"
        );
    }

    @Override
    protected Set<String> bad() {
        return ImmutableSet.of("schema/badEntity01.json");
    }

    @Override
    protected String schema() {
        return "resource:/schema/next/entity.schema.json";
    }
}
