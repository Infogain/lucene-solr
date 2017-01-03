package org.apache.lucene.queryparser.xml.builders;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.xml.CoreParser;
import org.apache.lucene.queryparser.xml.FilterBuilder;
import org.apache.lucene.queryparser.xml.ParserException;
import org.apache.lucene.queryparser.xml.QueryBuilder;
import org.apache.lucene.queryparser.xml.TermBuilder;
import org.apache.lucene.search.Query;
import org.w3c.dom.Element;

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
 * Utility class to facilitate configuration of Term[s][Freq](Query|Filter) builders.
 */
public class TermFreqBuildersWrapper implements QueryBuilder {

  public TermFreqBuildersWrapper(String defaultField, Analyzer analyzer,
    QueryBuilder queryFactory) {

    final CoreParser coreParser = (CoreParser)queryFactory;

    final TermBuilder termBuilder = new TermBuilder(analyzer);

    {
      QueryBuilder termQueryBuilder = new BBTermQueryBuilder(termBuilder);
      coreParser.addQueryBuilder("TermQuery", termQueryBuilder);
      coreParser.addQueryBuilder("TermFreqQuery", new TermFreqBuilder(null /* termFilterBuilder */, termQueryBuilder));
    }
    {
      QueryBuilder termsQueryBuilder = new BBTermsQueryBuilder(termBuilder);
      coreParser.addQueryBuilder("TermsQuery", termsQueryBuilder);
      coreParser.addQueryBuilder("TermsFreqQuery", new TermFreqBuilder(null /* termsFilterBuilder */, termsQueryBuilder));
    }
    {
      FilterBuilder termFilterBuilder = new BBTermFilterBuilder(termBuilder);
      coreParser.addFilterBuilder("TermFilter", termFilterBuilder);
      coreParser.addFilterBuilder("TermFreqFilter", new TermFreqBuilder(termFilterBuilder, null /* termQueryBuilder */));
    }
    {
      FilterBuilder termsFilterBuilder = new BBTermsFilterBuilder(termBuilder);
      coreParser.addFilterBuilder("TermsFilter", termsFilterBuilder);
      coreParser.addFilterBuilder("TermsFreqFilter", new TermFreqBuilder(termsFilterBuilder, null /* termsQueryBuilder */));
    }
  }

  @Override
  public Query getQuery(Element e) throws ParserException {
    throw new ParserException("TermFreqBuildersWrapper.getQuery is unsupported");
  }

}
