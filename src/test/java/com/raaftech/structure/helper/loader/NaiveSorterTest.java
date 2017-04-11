package com.raaftech.structure.helper.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.Objects;

import static org.junit.Assert.*;

public class NaiveSorterTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void calculateChecksum() throws Exception {
        /*
         Objects a and b only differ in the order of their keys. This should never trigger an update, so our checksum
         calculation should return the same checksums for both objects.
          */

        ObjectNode a = mapper.createObjectNode()
                .put("name", "Foo")
                .put("type", "Component")
                .put("desc", "Foo.");
        ObjectNode meta = a
                .putObject("meta")
                .put("uuid", "abba2750-7e05-4e70-8b36-1a56665dccd6");
        meta.putObject("relations");

        ObjectNode b = mapper.createObjectNode()
                .put("type", "Component")
                .put("desc", "Foo.")
                .put("name", "Foo");
        ObjectNode metb = b
                .putObject("meta")
                .put("uuid", "abba2750-7e05-4e70-8b36-1a56665dccd6");
        metb.putObject("relations");
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