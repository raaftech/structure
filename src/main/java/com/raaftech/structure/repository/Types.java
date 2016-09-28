package com.raaftech.structure.repository;

import com.raaftech.structure.graph.Type;
import com.raaftech.structure.graph.repositories.TypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Types extends Repository<Type> {

    private TypeRepository repository;

    private Logger log = LoggerFactory.getLogger(Types.class);

    public Types(TypeRepository repository) {
        super(repository);
        this.repository = repository;
    }

}
