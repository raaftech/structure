package tech.raaf.structure.graph.repositories;

import tech.raaf.structure.graph.Result;

public class ResultRepository extends GenericRepository<Result> {

  @Override
  protected String label() {
    return "Result";
  }
}
