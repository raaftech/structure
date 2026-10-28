package tech.raaf.structure.schema;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tech.raaf.structure.helper.JsonHelper;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public class IpSchemaTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void genericAddressAndNetworkAcceptIpv6() {
    assertTrue(valid("ipaddress", "addr", "2001:db8::1"));
    assertTrue(valid("ipnetwork", "cidr", "2001:db8::/32"));
    assertFalse(valid("ipaddress", "addr", 123));
    assertFalse(valid("ipnetwork", "cidr", 123));
  }

  @Test
  public void ipv4AddressChecksOctets() {
    for (String address : new String[] { "0.0.0.0", "192.0.2.1", "255.255.255.255" }) {
      assertTrue(valid("ipv4address", "addr", address), address);
    }
    for (String address : new String[] { "2001:db8::1", "256.0.0.1", "192.0.2", "192.0.2.01" }) {
      assertFalse(valid("ipv4address", "addr", address), address);
    }
  }

  @Test
  public void ipv4NetworkChecksAddressAndPrefix() {
    for (String network : new String[] { "0.0.0.0/0", "192.0.2.0/24", "255.255.255.255/32" }) {
      assertTrue(valid("ipv4network", "cidr", network), network);
    }
    for (String network : new String[] { "2001:db8::/32", "256.0.0.0/24", "192.0.2.0/33", "192.0.2.0" }) {
      assertFalse(valid("ipv4network", "cidr", network), network);
    }
  }

  @Test
  public void listenerPortMustBeAnIntegerInRange() {
    for (int value : new int[] { 1, 65535 }) {
      assertTrue(valid("iplistener", "port", value));
    }
    for (int value : new int[] { 0, 65536 }) {
      assertFalse(valid("iplistener", "port", value));
    }
    assertFalse(valid("iplistener", "port", 1.5));
  }

  private boolean valid(String schema, String field, Object value) {
    ObjectNode reference = mapper.createObjectNode();
    reference.put("$ref", "resource:/bundles/tcpip/schema/" + schema + ".schema.json#/properties/" + field);
    return JsonHelper.validationErrors(reference, mapper.valueToTree(value)).isEmpty();
  }
}
