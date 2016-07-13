package org.apache.lucene.search.intervals;

/**
 * Copyright (c) 2012 Lemur Consulting Ltd.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.search.FieldedQuery;

/**
 * A query that matches if a set of subqueries also match, and are within
 * a given distance of each other within the document.  The subqueries
 * may appear in the document in any order.
 *
 * N.B. Positions must be included in the index for this query to work
 *
 * Implements the LOWPASS<sub>k</sub> operator as defined in <a href=
 * "http://vigna.dsi.unimi.it/ftp/papers/EfficientAlgorithmsMinimalIntervalSemantics"
 * >"Efficient Optimally Lazy Algorithms for Minimal-Interval Semantics"</a>
 *
 * @lucene.experimental
 */

public class UnorderedNearQuery extends IntervalFilterQuery {

  private final int slop;

  /**
   * Constructs an OrderedNearQuery
   * @param slop the maximum distance between the subquery matches
   * @param collectLeaves false if only the parent interval should be collected
   * @param subqueries the subqueries to match.
   */
  public UnorderedNearQuery(int slop, boolean collectLeaves, FieldedQuery... subqueries) {
    this(slop, collectLeaves, new FieldedConjunctionQuery(subqueries));
  }

  /**
   * Constructs an OrderedNearQuery
   * @param slop the maximum distance between the subquery matches
   * @param subqueries the subqueries to match.
   */
  public UnorderedNearQuery(int slop, FieldedQuery... subqueries) {
    this(slop, true, new FieldedConjunctionQuery(subqueries));
  }

  public UnorderedNearQuery(int slop, boolean collectLeaves, FieldedConjunctionQuery subqueries) {
    super(subqueries, new WithinUnorderedFilter(slop + subqueries.queryCount() - 2, collectLeaves));
    this.slop = slop;
  }

  @Override
  public String toString(String field) {
    return "UnorderedNear/" + slop + ":" + super.toString("");
  }

  public static class WithinUnorderedFilter implements IntervalFilter {

    final IntervalFilter innerFilter;
    final boolean collectLeaves;

    public WithinUnorderedFilter(int slop, boolean collectLeaves) {
      this.innerFilter = new WithinIntervalFilter(slop);
      this.collectLeaves = collectLeaves;
    }

    @Override
    public IntervalIterator filter(boolean collectIntervals, IntervalIterator iter) {
      return innerFilter.filter(collectIntervals,
          new ConjunctionIntervalIterator(iter.scorer, collectIntervals, collectLeaves, iter.subs(false)));
    }
    
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (innerFilter == null? 0: innerFilter.hashCode());
      result = prime * result + (collectLeaves? 1: 0);
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (getClass() != obj.getClass()) return false;
      WithinUnorderedFilter other = (WithinUnorderedFilter) obj;
      if (collectLeaves != other.collectLeaves) return false;
      if (innerFilter == null){ 
        if (other.innerFilter != null) return false;
      }
      else if(!innerFilter.equals(other.innerFilter)) return false;
      return true;
    }
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + slop;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    UnorderedNearQuery other = (UnorderedNearQuery) obj;
    if (slop != other.slop) return false;
    return true;
  }

}