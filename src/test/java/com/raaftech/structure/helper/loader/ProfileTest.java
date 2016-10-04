package com.raaftech.structure.helper.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.raaftech.structure.base.SpringTestConfig;
import com.raaftech.structure.graph.Type;
import com.raaftech.structure.graph.repositories.TypeRepository;
import com.raaftech.structure.helper.JsonHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = SpringTestConfig.class)
public class ProfileTest {

    @Autowired
    TypeRepository typeRepository;

    private static final ObjectMapper mapper = new ObjectMapper();
    final JsonNode profileSchema = JsonHelper.jsonFromResource("schema/helper/loader/profile.schema.json");

    @Before
    @Transactional
    @Rollback
    public void setUp() throws Exception {
        Type centos = new Type("CentOS", "CentOS", "620b6614-5ef6-4cb4-9923-82becf46c438");
        Type fedora = new Type("Fedora", "Fedora", "620b6614-5ef6-4cb4-9923-82becf46c439");
        Type rhel = new Type("RHEL", "Red Hat Enterprise Linux", "620b6614-5ef6-4cb4-9923-82becf46c439");
        Type sles = new Type("SLES", "SuSE Linux Enterprise Server", "620b6614-5ef6-4cb4-9923-82becf46c43a");
        Type apache = new Type("ApacheHTTPD", "The Apache HTTP server", "620b6614-5ef6-4cb4-9923-82becf46c43b");
        centos.save();
        fedora.save();
        rhel.save();
        sles.save();
        apache.save();
    }

    @Test
    public void deserialize() throws IOException {
        final String example = "{\n" +
                "    \"name\" : \"somemap\",\n" +
                "    \"desc\" : \"A profile of Apache HTTPD that makes it possible to recognize itself on various Operating Systems.\",\n" +
                "    \"providedBy\": \"ApacheHTTPD\",\n" +
                "    \"appliesTo\": [\"CentOS\", \"Fedora\", \"SLES\", \"RHEL\"],\n" +
                "    \"serviceNames\": [\"httpd\"],\n" +
                "    \"packageNames\": [\"httpd\"],\n" +
                "    \"files\": {\n" +
                "        \"logFiles\": [\"access.log\", \"error.log\"],\n" +
                "        \"dataFiles\": [\"index.html\"],\n" +
                "        \"binaryFiles\": [\"httpd\"],\n" +
                "        \"libraryFiles\": [\"libapache.so\"],\n" +
                "        \"configurationFiles\": [\"httpd.conf\"]\n" +
                "        },\n" +
                "    \"directories\": {\n" +
                "        \"logDirectories\": [\"/var/log/httpd\"],\n" +
                "        \"dataDirectories\": [\"/var/www\"],\n" +
                "        \"binaryDirectories\": [\"/usr/sbin\"],\n" +
                "        \"libraryDirectories\": [\"/usr/lib\"],\n" +
                "        \"configurationDirectories\": [\"/etc/httpd\"]\n" +
                "    },\n" +
                "    \"commands\": {\n" +
                "        \"versionCheck\": {\n" +
                "            \"command\": [\"/usr/bin/httpd\", \"--version\"],\n" +
                "            \"matches\": \"^Server version: Apache/[0-9]\\\\.[0-9]\\\\.[0-9]{2}.*$\"\n" +
                "        }\n" +
                "    }\n" +
                "}";

        JsonHelper.validateToSchema(profileSchema, mapper.readTree(example));
        final Profile profile = mapper.readValue(example, Profile.class);
        Set<String> testSet;

        final Type apache = typeRepository.findOneByName("ApacheHTTPD");
        final Type centos = typeRepository.findOneByName("CentOS");
        final Type fedora = typeRepository.findOneByName("Fedora");
        final Type sles = typeRepository.findOneByName("SLES");
        final Type rhel = typeRepository.findOneByName("RHEL");

        assertNotNull(apache);
        assertNotNull(centos);
        assertNotNull(fedora);
        assertNotNull(sles);
        assertNotNull(rhel);

        assertEquals("somemap", profile.getName());
        assertEquals(
                "A profile of Apache HTTPD that makes it possible to recognize itself on various Operating Systems.",
                profile.getDesc());

        assertEquals(apache.getUuid(), profile.getProvidedBy().getUuid());

        assertEquals(4, profile.getAppliesTo().size());
        profile.getAppliesTo().forEach(t -> assertTrue(t.getUuid() == centos.getUuid()
                || t.getUuid() == fedora.getUuid() || t.getUuid() == sles.getUuid()
                || t.getUuid() == rhel.getUuid()));

        assertEquals(1, profile.getPackageNames().size());
        assertEquals("httpd", profile.getPackageNames().iterator().next());

        assertEquals(1, profile.getServiceNames().size());
        assertEquals("httpd", profile.getServiceNames().iterator().next());

        assertEquals(2, profile.getLogFiles().size());
        testSet = Sets.newHashSet("access.log", "error.log");
        profile.getLogFiles().forEach(f -> assertTrue(testSet.contains(f)));

        assertEquals(1, profile.getDataFiles().size());
        assertEquals("index.html", profile.getDataFiles().iterator().next());

        assertEquals(1, profile.getBinaryFiles().size());
        assertEquals("httpd", profile.getBinaryFiles().iterator().next());

        assertEquals(1, profile.getLibraryFiles().size());
        assertEquals("libapache.so", profile.getLibraryFiles().iterator().next());

        assertEquals(1, profile.getConfigurationFiles().size());
        assertEquals("httpd.conf", profile.getConfigurationFiles().iterator().next());

        assertEquals(1, profile.getLogDirectories().size());
        assertEquals("/var/log/httpd", profile.getLogDirectories().iterator().next());

        assertEquals(1, profile.getDataDirectories().size());
        assertEquals("/var/www", profile.getDataDirectories().iterator().next());

        assertEquals(1, profile.getBinaryDirectories().size());
        assertEquals("/usr/sbin", profile.getBinaryDirectories().iterator().next());

        assertEquals(1, profile.getLibraryDirectories().size());
        assertEquals("/usr/lib", profile.getLibraryDirectories().iterator().next());

        assertEquals(1, profile.getConfigurationDirectories().size());
        assertEquals("/etc/httpd", profile.getConfigurationDirectories().iterator().next());

        assertEquals(1, profile.getCommands().size());
        Profile.Command command = profile.getCommands().get("versionCheck");
        assertNotNull(command);
        assertNotNull(command.match);
        assertEquals("^Server version: Apache/[0-9]\\.[0-9]\\.[0-9]{2}.*$", command.match.toString());
        assertNotNull(command.command);
        assertEquals("/usr/bin/httpd", command.command.get(0));
        assertEquals("--version", command.command.get(1));
    }

}