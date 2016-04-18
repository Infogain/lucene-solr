/*
 * Created on 25-Jan-2006
 */
package org.apache.lucene.queryparser.xml.builders;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.queryparser.xml.DOMUtils;
import org.apache.lucene.queryparser.xml.ParserException;
import org.apache.lucene.queryparser.xml.QueryBuilder;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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

/**
 * Builder for {@link BooleanQuery}
 */
public class BooleanQueryBuilder implements QueryBuilder {

  private final QueryBuilder factory;

  public BooleanQueryBuilder(QueryBuilder factory) {
    this.factory = factory;
  }

  /* (non-Javadoc)
    * @see org.apache.lucene.xmlparser.QueryObjectBuilder#process(org.w3c.dom.Element)
    */

  @Override
  public Query getQuery(Element e) throws ParserException {
    BooleanQuery.Builder bq = new BooleanQuery.Builder();
    bq.setDisableCoord(DOMUtils.getAttribute(e, "disableCoord", false));
    bq.setMinimumNumberShouldMatch(DOMUtils.getAttribute(e, "minimumNumberShouldMatch", 0));

    NodeList nl = e.getChildNodes();
    final int nl_len = nl.getLength();
    
    boolean matchAllDocsExists = false; 
    boolean shouldClauseExists = false;
    boolean mustClauseExists = false;
    
    for (int i = 0; i < nl_len; i++) {
      Node node = nl.item(i);
      if (node.getNodeName().equals("Clause")) {
        Element clauseElem = (Element) node;
        BooleanClause.Occur occurs = getOccursValue(clauseElem);

        Element clauseQuery = DOMUtils.getFirstChildOrFail(clauseElem);
        Query q = factory.getQuery(clauseQuery);
        //trace for any MatchAllDocsQuery and dedupe them and check for the existence of must or should clause
        if (q instanceof MatchAllDocsQuery) {
          if (matchAllDocsExists) continue;
          matchAllDocsExists = true;
        }
        else {
          shouldClauseExists |= (occurs == BooleanClause.Occur.SHOULD);
          mustClauseExists   |= (occurs == BooleanClause.Occur.MUST);
        }
        bq.add(new BooleanClause(q, occurs));
      }
    }
    //if there is matchalldocs query
    //all should clauses should be removed
    //if there is a must clause then matchalldocs query clauses will be removed
    if (matchAllDocsExists && (mustClauseExists || shouldClauseExists)) {
        BooleanQuery bqRewritten = new BooleanQuery(bq.isCoordDisabled());
        bqRewritten.setMinimumNumberShouldMatch(bq.getMinimumNumberShouldMatch());
        bqRewritten.setBoost(bq.getBoost());
        for(int i = 0; i < bq.clauses().size();i++) {
          BooleanClause bc = bq.clauses().get(i);
          Query q = bc.getQuery();
          if (q instanceof MatchAllDocsQuery) {
            if (mustClauseExists) continue;
          } else if (bc.getOccur() == BooleanClause.Occur.SHOULD)
            continue;
          
          bqRewritten.add(bc);
        }
        bq = bqRewritten;
    }
    if(bq.clauses().size() == 1)
      return bq.clauses().get(0).getQuery();
    else
      return bq;
  }

  static BooleanClause.Occur getOccursValue(Element clauseElem) throws ParserException {
    String occs = clauseElem.getAttribute("occurs");
    if (occs == null || "should".equalsIgnoreCase(occs)) {
      return BooleanClause.Occur.SHOULD;
    } else if ("must".equalsIgnoreCase(occs)) {
      return BooleanClause.Occur.MUST;
    } else if ("mustNot".equalsIgnoreCase(occs)) {
      return BooleanClause.Occur.MUST_NOT;
    } else if ("filter".equals(occs)) {
      return BooleanClause.Occur.FILTER;
    }
    throw new ParserException("Invalid value for \"occurs\" attribute of clause:" + occs);
  }

}
