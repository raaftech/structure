package schema;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

public class ProfileTest extends SchemaTest {
    @Override
    protected Set<String> good() {
        return ImmutableSet.of(
                "schema/profileGood01.json"
        );
    }

    @Override
    protected Set<String> bad() {
        return ImmutableSet.of(
                "schema/profileBad01.json", // testing with typo under #/files/logfiles
                "schema/profileBad02.json", // testing with integer (where only strings are allowed) under #/directories/logDirectories
                "schema/profileBad03.json", // testing with bad regex pattern under #/packageNames
                "schema/profileBad04.json" // testing with bad regex pattern under #/providedBy
        );
    }

    @Override
    protected String schema() {
        return "resource:/schema/helper/loader/profile.schema.json";
    }
}
