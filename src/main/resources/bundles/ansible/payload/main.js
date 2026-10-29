"use strict";

const cmd = require("command-line-args");
const net = require("node:net");
const ansible = require("./ansibleLib");
const Helper = require("./helper");
const graphLib = require("./graphLib");
const settings = require("settings");
const BusConnector = require("busConnector");
const persist = require("persist");

const options = cmd([{ name: "ip", alias: "i", type: String }]);

const main = async () => {
  if (net.isIP(options.ip || "") !== 4) throw new Error("An IPv4 address is required");
  const client = new BusConnector(settings.structureUrl("eventbus/websocket").replace(/^http/, "ws"));
  try {
    const helper = new Helper(await client.getAllTypes());
    const found = await client.getInstanceByPredicate("addr", options.ip);
    const address = found.meta ? found : helper.ipData(options.ip);
    const existing = found.meta ? await client.getOperatingSystem(found.meta.uuid) : {};
    const local = Boolean(
      process.env.STRUCTURE_SELF_ID && existing.meta?.relations.MEMBER_OF?.to.includes(process.env.STRUCTURE_SELF_ID),
    );
    const graph = helper.transform(await ansible.setup(options.ip, local), address, local);
    const entities = found.meta ? [] : [address];
    const children = existing.meta
      ? await Promise.all((existing.meta.relations.HAS?.to || []).map((uuid) => client.getInstance(uuid)))
      : [];

    for (const entity of graph.entities) {
      const type = entity.meta.relations.INSTANCE_OF.to[0];
      const previous = children.find((child) => {
        const current = child.meta.relations.INSTANCE_OF.to[0];
        return current === type || helper.type(current).meta.uuid === helper.type(type).meta.uuid;
      });
      if (previous) {
        const uuid = entity.meta.uuid;
        entity.meta.uuid = previous.meta.uuid;
        graph.os.meta.relations.HAS.to = graph.os.meta.relations.HAS.to.map((id) =>
          id === uuid ? previous.meta.uuid : id,
        );
      } else {
        entities.push(entity);
      }
    }

    if (!existing.meta) {
      console.log(await persist(client, [...entities, graph.root, graph.os]));
      return;
    }

    const before = JSON.stringify(existing);
    await persist(client, entities, [graph.os]);
    const selected = helper.type(graph.os.meta.relations.INSTANCE_OF.to[0]);
    const current = helper.type(existing.meta.relations.INSTANCE_OF.to[0]);
    if (current.meta.uuid !== selected.meta.uuid && helper.isType(selected, current.name)) {
      await client.specialize(existing.meta.uuid, selected.meta.uuid);
      existing.meta.relations.INSTANCE_OF.to = [selected.meta.uuid];
    }
    const type = helper.type(existing.meta.relations.INSTANCE_OF.to[0]);
    existing.data = { ...existing.data, ...helper.software(type, graph.os.data.ver).data };
    for (const uuid of graph.os.meta.relations.HAS.to) {
      if (!(existing.meta.relations.HAS?.to || []).includes(uuid)) {
        graphLib.relateEntity(existing, "HAS", "outgoing", { meta: { uuid } });
      }
    }
    if (JSON.stringify(existing) === before) {
      console.log([existing.meta.uuid]);
      return;
    }
    const updated = await client.updateInstance({ entities: [existing] });
    if (!Array.isArray(updated) || !updated.includes(existing.meta.uuid)) {
      throw new Error("Could not update operating system: " + JSON.stringify(updated));
    }
    console.log(updated);
  } finally {
    client.close();
  }
};

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
