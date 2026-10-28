package tech.raaf.structure.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.exceptions.IllegalEntityType;
import tech.raaf.structure.helper.Triple;
import tech.raaf.structure.schema.TypeSchemaTest;

/**
 * Checks that toGraph refuses an instance with the same identity
 * values as an other instance of its type.
 */
public class IdentityTest extends TypeSchemaTest {

  // The identity of DnsName is dnsname
  private static final String DNS_NAME = "99c0da37-3599-418d-8ecc-de36d37ae9f4";
  // Ipv4Address gets the identity addr from IpAddress
  private static final String IPV4_ADDRESS = "f7fbe917-9d49-4df5-95f6-6e132067a424";
  // The identity of Group is name, ComponentGroup gets it from Group
  private static final String GROUP = "5f1eb7c5-ec5e-4563-be6a-fc50774fc374";
  private static final String COMPONENT_GROUP = "ee92c308-80f4-40b2-af3a-77d26cd99517";

  @Test
  public void ownIdentity() {
    Entity original = dnsName("identity.example");
    original.toGraph();
    assertDuplicate(original, dnsName("identity.example"));
  }

  @Test
  public void inheritedIdentity() {
    Entity original = ipv4Address("192.0.2.1");
    original.toGraph();
    assertDuplicate(original, ipv4Address("192.0.2.1"));
  }

  @Test
  public void nameIdentity() {
    Entity original = instance(COMPONENT_GROUP, "identity-group", ImmutableMap.of());
    original.toGraph();
    assertDuplicate(original, instance(COMPONENT_GROUP, "identity-group", ImmutableMap.of()));
  }

  @Test
  public void subtypeIdentity() {
    Entity original = instance(GROUP, "identity-group", ImmutableMap.of());
    original.toGraph();
    assertDuplicate(original, instance(COMPONENT_GROUP, "identity-group", ImmutableMap.of()));
  }

  @Test
  public void otherValue() {
    Entity first = ipv4Address("192.0.2.1");
    Entity second = ipv4Address("192.0.2.2");
    first.toGraph();
    second.toGraph();
    assertNotNull(Context.getNodeRepository().findByUuid(first.getUuid()));
    assertNotNull(Context.getNodeRepository().findByUuid(second.getUuid()));
  }

  @Test
  public void update() {
    Entity address = ipv4Address("192.0.2.1");
    address.toGraph();
    address.setDesc("An updated address.");
    address.toGraph();
    assertEquals("An updated address.", Context.getNodeRepository().findByUuid(address.getUuid()).getDesc());
  }

  @Test(expected = IllegalArgumentException.class)
  public void instanceHasNoIdentity() {
    ipv4Address("192.0.2.1").setIdentity(new String[] { "addr" });
  }

  // Assert that toGraph refuses the duplicate and does not write it
  private static void assertDuplicate(Entity original, Entity duplicate) {
    try {
      duplicate.toGraph();
      fail("Wrote a duplicate of " + original.getUuid());
    } catch (IllegalEntityType e) {
      JsonObject error = new JsonObject(e.getMessage());
      assertEquals(409, (int) error.getInteger("code"));
      assertEquals(original.getUuid(), error.getString("conflict"));
    }
    assertNull(Context.getNodeRepository().findByUuid(duplicate.getUuid()));
  }

  private static Entity dnsName(String dnsname) {
    return instance(DNS_NAME, dnsname, ImmutableMap.of("dnsname", dnsname));
  }

  private static Entity ipv4Address(String addr) {
    return instance(IPV4_ADDRESS, addr, ImmutableMap.of("addr", addr, "reachable", true));
  }

  private static Entity instance(String type, String name, Map<String, Object> data) {
    Entity entity = new Entity();
    entity.setUuid(UUID.randomUUID().toString());
    entity.setName(name);
    entity.setDesc("An instance for the identity test.");
    entity.setRelations(ImmutableSet.of(Triple.of("INSTANCE_OF", "outgoing", ImmutableSet.of(type))));
    entity.setData(data);
    return entity;
  }
}
