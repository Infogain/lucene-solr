package org.apache.lucene.queryparser.xml.builders;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.queryparser.xml.DOMUtils;
import org.apache.lucene.queryparser.xml.ParserException;
import org.apache.lucene.queryparser.xml.QueryBuilder;
import org.apache.lucene.search.FieldedQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.apache.lucene.search.intervals.FieldedBooleanQuery;
import org.apache.lucene.search.intervals.IntervalFilterQuery;
import org.apache.lucene.search.intervals.OrderedNearQuery;
import org.apache.lucene.search.intervals.RangeIntervalFilter;
import org.apache.lucene.search.intervals.UnorderedNearQuery;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class NearQueryBuilder implements QueryBuilder{
  final private QueryBuilder factory;

  public NearQueryBuilder(QueryBuilder factory) {
    super();
    this.factory = factory;
  }
  
  @Override
  public Query getQuery(Element e) throws ParserException {
    int slop = DOMUtils.getAttribute(e, "slop", 0);
    boolean inOrder = DOMUtils.getAttribute(e, "inOrder", false);
    
    List<Query> subQueriesList = new ArrayList<>();
    for (Node kid = e.getFirstChild(); kid != null; kid = kid.getNextSibling()) {
      if (kid.getNodeType() == Node.ELEMENT_NODE) {
        Query q = factory.getQuery((Element) kid);
        if (!(q instanceof MatchAllDocsQuery)) {
          FieldedQuery fq = FieldedBooleanQuery.toFieldedQuery(factory.getQuery((Element) kid));
          subQueriesList.add(fq);
        }
        
      }
    }
    switch (subQueriesList.size())
    {
      case 0:
        return new MatchAllDocsQuery();
      case 1:
        return subQueriesList.get(0);
        default:
          FieldedQuery[] subQueries = subQueriesList.toArray(new FieldedQuery[subQueriesList.size()]);
          if (inOrder)
            return new OrderedNearQuery(slop, subQueries);
          else
            return new UnorderedNearQuery(slop, subQueries);
    }
  }
  
}