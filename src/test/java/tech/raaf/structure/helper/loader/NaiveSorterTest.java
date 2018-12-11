package tech.raaf.structure.helper.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import mockit.Mocked;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;
import tech.raaf.structure.base.Context;

import java.io.ByteArrayInputStream;
import java.util.Objects;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NaiveSorterTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void calculateChecksum(@Mocked Context context) throws Exception {
        /*
         Objects a and b only differ in the order of their keys. This should never trigger an update, so our checksum
         calculation should return the same checksums for both objects.
          */

        String instanceOf = UUID.randomUUID().toString();

        ObjectNode a = mapper.createObjectNode()
                .put("name", "Foo")
                .put("desc", "Foo.");
        ObjectNode meta = a
                .putObject("meta")
                .put("identity", "foo")
                .put("uuid", "abba2750-7e05-4e70-8b36-1a56665dccd6");
        ObjectNode rela = meta.putObject("relations").putObject("INSTANCE_OF");
        rela.put("direction", "outgoing");
        rela.putArray("uuids").add(instanceOf);

        ObjectNode b = mapper.createObjectNode()
                .put("desc", "Foo.")
                .put("name", "Foo");
        ObjectNode metb = b
                .putObject("meta")
                .put("uuid", "abba2750-7e05-4e70-8b36-1a56665dccd6")
                .put("identity", "foo");
        ObjectNode relb = metb.putObject("relations").putObject("INSTANCE_OF");
        relb.put("direction", "outgoing");
        relb.putArray("uuids").add(instanceOf);
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(b));

        // our old way:
        assertFalse(Objects.equals(DigestUtils.md5Hex(a.toString()), DigestUtils.md5Hex(b.toString())));
        assertFalse(Objects.equals(DigestUtils.sha1Hex(a.toString()), DigestUtils.sha1Hex(b.toString())));

        // the new way:
        assertTrue(Objects.equals(NaiveSorter.calculateChecksum(a, NaiveSorter.ChecksumAlgorithm.MD5),
                NaiveSorter.calculateChecksum(b, NaiveSorter.ChecksumAlgorithm.MD5)));
        assertTrue(Objects.equals(NaiveSorter.calculateChecksum(a, NaiveSorter.ChecksumAlgorithm.SHA1),
                NaiveSorter.calculateChecksum(b, NaiveSorter.ChecksumAlgorithm.SHA1)));

        // test the overloaded InputStream version as well
        assertTrue(Objects.equals(
                NaiveSorter.calculateChecksum(
                        new ByteArrayInputStream(a.toString().getBytes()), NaiveSorter.ChecksumAlgorithm.MD5),
                NaiveSorter.calculateChecksum(
                        new ByteArrayInputStream(b.toString().getBytes()), NaiveSorter.ChecksumAlgorithm.MD5)));


        // add some properties that should be filtered from the JSON, see if the checksums still match
        meta.put("path", "foo");
        metb.put("path", "bar");
        assertTrue(Objects.equals(NaiveSorter.calculateChecksum(a, NaiveSorter.ChecksumAlgorithm.SHA1),
                NaiveSorter.calculateChecksum(b, NaiveSorter.ChecksumAlgorithm.SHA1)));
    }

}