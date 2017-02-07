"use strict";

/**
 * This class is used to relate one entity to another.
 */
class Relation {
    /**
     * The relation constructor.
     * @param direction {string} The direction of the relation, defaults to 'outgoing'.
     */
    constructor(direction) {
        this.direction = direction ? direction : 'outgoing';
        this.entities = [];
        this.uuids = [];
    }

    /**
     * Add an entity between the entity that has this relation, and a given entity.
     * @param {Entity} entity The given entity to create a relation to.
     * @returns {Relation}
     */
    addEntity(entity) {
        this.uuids.push(entity.meta.uuid);
        this.entities.push(entity);
        return this;
    }
}

/**
 * The Entity class is used the base data structure for Structure.
 */
class Entity {
    /**
     * The entity constructor.
     * @param {string} type The type of the entity.
     * @param {string} name The name of the entity.
     * @param {string} desc The description of the entity.
     * @param {string} uuid The UUID of the entity.
     * @param {object} data A simple object containing keys and values.
     * @param {object} relations An object that maps relation names to Relation objects.
     * @param {object} file An object containing file references.
     */
    constructor(type, name, desc, uuid, data, relations, file) {
        this.type = type;
        this.name = name;
        this.desc = desc;
        this.meta = {};
        this.meta.uuid = uuid;

        if (data) this.data = data;
        if (relations) this.meta.relations = relations;
        if (file) this.file = file;

    }

    /**
     * Create a new relation object on this entity.
     * @param {string} name The name of the relation.
     * @param {string} direction The direction, either 'incoming' or 'outgoing', with the latter being default.
     * @returns {Relation} The created relation object, to which entities can be added, see {@link Relation}.
     */
    relationCreate(name, direction) {
        if (!this.meta.hasOwnProperty('relations')) {
            this.meta.relations = {};
        }
        if(!direction) direction = 'outgoing';
        const relation = new Relation(direction);
        this.meta.relations[name] = relation;
        return relation;
    }
}

/**
 * The SubGraph class is used to hold a graph of Entity objects
 */
class SubGraph {
    constructor(entities) {
        this.entitySet = {};

        if(entities) {
            if(Array.isArray(entities)) {
                this.entitySet = readBundle(entities);
            } else {
                if(typeof(entities) === 'object') {
                    this.entitySet = clone(entities);
                }
            }
        }
    }

    /**
     * insertEntity :: Entity -> String -> String -> Entity -> {k: Entity}
     * Given a subgraph, an entity to insert, the relation name to bind to, the direction of the relation
     * and an Entity to relate to; creates a new subgraph with the Entity, including relation, added to it.
     * @param {Entity} entity The entity to insert.
     * @param {String} relationName The name of the relation to add to the entity.
     * @param {String} relationDirection outgoing | incoming Direction of the relation.
     * @param {Entity} relatedEntity The related Entity.
     * @returns {Object.<string,Entity>} A newly created Subgraph with the entity inserted.
     */
    insertEntity(entity, relationName, relationDirection, relatedEntity) {
        let check = this.findEntity(entity.meta.uuid);
        if(check !== null) return this;

        let check2 = this.findEntity(entity.name);
        if(check2 !== null) return this;

        let relation = entity.relationCreate(relationName, relationDirection);
        relation.addEntity(relatedEntity);
        this.entitySet[entity.meta.uuid] = entity;
        return this;
    }

    /**
     * removeEntity :: String -> {k: Entity}
     * Given a subgraph and a uuid, remove the Entity from the subgraph that corresponds to the uuid.
     * @param {String} uuid The uuid to search for.
     * @returns {Object.<string,Entity>} A newly created subgraph with the entity removed.
     */
    removeEntity(uuid) {
        const isDependency = (subgraph, uuid) => {
            for(const key of Object.keys(subgraph)) {
                const e = subgraph[key];
                for(const k of Object.keys(e.meta.relations)) {
                    const rel = e.meta.relations[k];
                    for(let i=0;i<rel.entities.length;i++) {
                        let u = rel.entities[i];
                        if(u.meta.uuid === uuid) return true;
                    }
                }
            }
            return false;
        };

        let entity = this.findEntity(uuid);
        if(entity !== null) {
            if(!isDependency(this.entitySet, uuid)) delete this.entitySet[uuid];
        }

        return this;
    }

    /**
     * findEntity :: String -> Entity | null
     * Given a subgraph and an id, find the first Entity in the subgraph that matches the predicate
     * @param {String} id Predicate that matches either the name of an entity or the uuid.
     * @returns {Entity|null} The found entity or null if nothing was found.
     */
    findEntity(id) {
        const equals = (predicate,value) => value.toLowerCase() === predicate.toLowerCase();

        for(const key of Object.keys(this.entitySet)) {
            const entity = this.entitySet[key];
            if(equals(id, entity.name) || equals(id, entity.meta.uuid)) return clone(entity);
        }
        return null;
    }

    nodeCount() {
        return Object.keys(this.entitySet).length;
    }

    toMap() {
        return clone(this.entitySet);
    }

    toList() {
        return flatten(this.entitySet);
    }
}

/**
 * Given a flat array of entity definitions that adhere to entity.schema.json, create a hierarchy based on relations.
 * @param {Object[]} entities
 * @returns {Object.<string, Entity>} A map of entity UUID's to the entities themselves.
 */
const readBundle = (entities) => {
    const ents = clone(entities);
    const entityMap = {};
    ents.forEach((e) => entityMap[e.meta.uuid] =
        new Entity(e.type, e.name, e.desc, e.meta.uuid, e.data, e.meta.relations, e.file));

    for (const key of Object.keys(entityMap)) {
        const relations = entityMap[key].meta.relations;
        for (const relName of Object.keys(relations)) {
            const rel = relations[relName];
            rel.uuids.forEach((relatedUuid) => {
                if (relatedUuid in entityMap) {
                    if (rel.entities == null) {
                        rel.entities = [];
                    }
                    rel.entities.push(entityMap[relatedUuid]);
                }
            });
        }
    }
    return entityMap;
};

/**
 * Given an array of {@link Entity entities} that might contain nested entities, flatten and transform them
 * so they validate against entity.schema.json.
 * @param {Entity|Entity[]} entities An array of {@link Entity entities}, or a single {@link Entity entity} that may
 * contain nested entities.
 * @returns {Array} A flat array or {@link Entity entities} where relations are flattened with UUID references.
 */
const flatten = (entities, done, flattened) => {
    if(!done) done = [];
    if(!flattened) flattened = [];

    const objToList = (obj) => {
        let list = [];
        for(const p of Object.keys(obj)) {
            list.push(obj[p]);
        }
        return list;
    };

    const flattenEntity = (e) => {
        if(done.indexOf(e.meta.uuid) === -1) {
            done.push(e.meta.uuid);
            const copy = clone(e);
            for (const key of Object.keys(copy.meta.relations)) {
                const relEntities = copy.meta.relations[key].entities;
                delete copy.meta.relations[key].entities;

                if (relEntities != null) {
                    relEntities.forEach((relEnt) => flatten([relEnt], done, flattened));
                }
            }
            flattened.push(copy);
        }
    };

    if(!Array.isArray(entities)) {
        entities = objToList(entities);
    }
    entities.forEach(flattenEntity);

    return flattened;
};

const clone = (obj) => {
    return JSON.parse(JSON.stringify(obj));
};

module.exports = {
    Entity: Entity,
    SubGraph: SubGraph
};
