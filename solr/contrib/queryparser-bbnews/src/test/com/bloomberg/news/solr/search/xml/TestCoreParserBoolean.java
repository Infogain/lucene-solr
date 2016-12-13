/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bloomberg.news.solr.search.xml;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.xml.CoreParser;
import org.apache.lucene.queryparser.xml.ParserException;
import org.apache.lucene.queryparser.xml.TestCoreParser;
import org.apache.lucene.queryparser.xml.builders.SpanQueryBuilder;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanQuery;

import com.bloomberg.news.lucene.queryparser.xml.TermBuilder;
import com.bloomberg.news.lucene.queryparser.xml.builders.BBTermQueryBuilder;
import com.bloomberg.news.lucene.queryparser.xml.builders.GenericTextQueryBuilder;
import com.bloomberg.news.lucene.queryparser.xml.builders.NearFirstQueryBuilder;
import com.bloomberg.news.lucene.queryparser.xml.builders.NearQueryBuilder;
import com.bloomberg.news.lucene.queryparser.xml.builders.TermFreqBuilder;
import com.bloomberg.news.lucene.queryparser.xml.builders.WildcardNearQueryBuilder;

import java.io.IOException;
import java.io.InputStream;


public class TestCoreParserBoolean extends TestCoreParser {

  private class CoreParserBooleanQuery extends CoreParser {
    public CoreParserBooleanQuery(String defaultField, Analyzer analyzer) {
      super(defaultField, analyzer);

      // the query builder to be tested
      {
        String queryName = "BooleanQuery";
        BooleanQueryBuilder builder = new BooleanQueryBuilder(defaultField, analyzer, null, spanFactory);
        queryFactory.addBuilder(queryName, builder);
        spanFactory.addBuilder(queryName, builder);
      }
      {
        SpanQueryBuilder termQueryBuilder = new BBTermQueryBuilder(new TermBuilder (analyzer));
        queryFactory.addBuilder("TermQuery", termQueryBuilder);
        spanFactory.addBuilder("TermQuery", termQueryBuilder);
        queryFactory.addBuilder("TermFreqQuery", new TermFreqBuilder(termQueryBuilder));
      }
      {
        String queryName = "NearQuery";
        NearQueryBuilder builder = new NearQueryBuilder(spanFactory);
        queryFactory.addBuilder(queryName, builder);
        spanFactory.addBuilder(queryName, builder);
      }
      {
        String queryName = "NearFirstQuery";
        NearFirstQueryBuilder builder = new NearFirstQueryBuilder(spanFactory);
        queryFactory.addBuilder(queryName, builder);
        spanFactory.addBuilder(queryName, builder);
      }
    }
  }

  protected CoreParser newCoreParser(String defaultField, Analyzer analyzer) {
    final CoreParser coreParser = new CoreParserBooleanQuery(defaultField, analyzer);

    // some additional builders to help
    {
      String queryName = "GenericTextQuery";
      GenericTextQueryBuilder builder = new GenericTextQueryBuilder(analyzer);
      coreParser.addQueryBuilder(queryName, builder);
      // This does not implement SpanQueryBuilder yet
      //coreParser.addSpanBuilder(queryName, builder);
    }
    {
      String queryName = "WildcardNearQuery";
      WildcardNearQueryBuilder builder = new WildcardNearQueryBuilder(analyzer);
      coreParser.addQueryBuilder(queryName, builder);
      coreParser.addSpanBuilder(queryName, builder);
    }

    return coreParser;
  }

  @Override
  protected Query parse(String xmlFileName) throws ParserException, IOException {
    try (InputStream xmlStream = TestCoreParserBoolean.class.getResourceAsStream(xmlFileName)) {
      if (xmlStream == null) {
        return super.parse(xmlFileName);
      }
      assertNotNull("Test XML file " + xmlFileName + " cannot be found", xmlStream);
      Query result = coreParser().parse(xmlStream);
      return result;
    }
  }

  public void testBooleanQueryTripleShouldWildcardNearQuery() throws Exception {
    final Query q = parse("BooleanQueryTripleShouldWildcardNearQuery.xml");
    final int size = ((BooleanQuery)q).clauses().size();
    assertTrue("Expecting 2 clauses, but resulted in " + size, size == 2);
    final BooleanQuery bq = (BooleanQuery)q;
    for(BooleanClause bc : bq.clauses())
    {
      assertFalse("Not expecting MatchAllDocsQuery ",bc.getQuery() instanceof MatchAllDocsQuery);
    }
  }

  public void testBooleanQueryMustShouldWildcardNearQuery() throws ParserException, IOException {
    final Query q = parse("BooleanQueryMustShouldWildcardNearQuery.xml");
    assertTrue("Expecting a SpanQuery, but resulted in " + q.getClass(), q instanceof SpanQuery);
  }

  public void testBooleanQueryMustMustShouldWildcardNearQuery() throws Exception {
    final Query q = parse("BooleanQueryMustMustShouldWildcardNearQuery.xml");
    assertTrue("Expecting a BooleanQuery, but resulted in " + q.getClass(), q instanceof BooleanQuery);
    final BooleanQuery bq = (BooleanQuery)q;
    final int size = bq.clauses().size();
    assertTrue("Expecting 2 clauses, but resulted in " + size, size == 2);
    for(BooleanClause bc : bq.clauses())
    {
      assertFalse("Not expecting MatchAllDocsQuery ", bc.getQuery() instanceof MatchAllDocsQuery);
    }
  }

  public void testBooleanQueryMatchAllDocsQueryWildcardNearQuery() throws Exception {
    final Query q = parse("BooleanQueryMatchAllDocsQueryWildcardNearQuery.xml");
    assertTrue("Expecting a MatchAllDocsQuery, but resulted in " + q.getClass(), q instanceof MatchAllDocsQuery);
  }

  public void testBooleanQueryMatchAllDocsQueryTermQuery() throws Exception {
    final Query q = parse("BooleanQueryMatchAllDocsQueryTermQuery.xml");
    assertTrue("Expecting a BooleanQuery, but resulted in " + q.getClass(), q instanceof BooleanQuery);
    final BooleanQuery bq = (BooleanQuery)q;
    final int size = bq.clauses().size();
    assertTrue("Expecting 2 clauses, but resulted in " + size, size == 2);
    boolean bMatchAllDocsFound = false;
    for(BooleanClause bc : bq.clauses())
    {
      bMatchAllDocsFound |= bc.getQuery() instanceof MatchAllDocsQuery;
    }
    assertTrue("Expecting MatchAllDocsQuery ", bMatchAllDocsFound);
  }

  public void testBooleanQueryDedupe() throws ParserException, IOException {
    Query query = parse("BooleanQueryDedupe.xml");
    Query resultQuery = parse("BooleanQueryDedupeResult.xml");
    assertEquals(resultQuery, query);
  }

  public void testNearBooleanNear() throws IOException, ParserException {
    final Query q = parse("NearBooleanNear.xml");
    dumpResults("testNearBooleanNear", q, 5);
  }

  public void testNearFirstBooleanMustXml() throws IOException, ParserException {
    final Query q = parse("NearFirstBooleanMust.xml");
    dumpResults("testNearFirstBooleanMustXml", q, 50);
  }
}
