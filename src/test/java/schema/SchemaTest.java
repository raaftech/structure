package schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.raaftech.structure.helper.JsonHelper;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Tests to check whether JSON schemas in Structure are correct.
 */
public abstract class SchemaTest {

    private Set<JsonNode> good;
    private Set<JsonNode> bad;
    private URI schema;

    @Before
    public void loadJson() {
        good = good().stream().map(JsonHelper::jsonFromResourceOrFile).collect(Collectors.toSet());
        bad = bad().stream().map(JsonHelper::jsonFromResourceOrFile).collect(Collectors.toSet());
        schema = URI.create(schema());
    }

    @Test
    public void testGoodJson() throws Exception {
        good.forEach(json -> assertTrue(JsonHelper.validateToSchema(schema, json)));
    }

    @Test
    public void testBadJson() throws Exception {
        bad.forEach(json -> {
            assertFalse(JsonHelper.validateToSchema(schema, json));
        });
    }

    protected abstract Set<String> good();
    protected abstract Set<String> bad();
    protected abstract String schema();
}
