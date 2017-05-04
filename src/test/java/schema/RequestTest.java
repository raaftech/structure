package schema;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

/**
 * Created by bardofrings on 04/05/2017.
 */
public class RequestTest extends SchemaTest {
    @Override
    protected Set<String> good() {
        return ImmutableSet.of(
                "schema/requestGood01.json"
        );
    }

    @Override
    protected Set<String> bad() {
        return ImmutableSet.of(
                "schema/requestBad01.json"
        );
    }

    @Override
    protected String schema() {
        return "resource:/schema/base/request.schema.json";
    }
}
