package com.bloomberg.news.lucene.queryparser.xml;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.xml.CoreParser;
import org.apache.lucene.queryparser.xml.QueryBuilder;
import org.apache.lucene.queryparser.xml.builders.SpanQueryBuilder;

import com.bloomberg.news.lucene.queryparser.xml.builders.BBTermQueryBuilder;
import com.bloomberg.news.lucene.queryparser.xml.builders.BBTermsQueryBuilder;
import com.bloomberg.news.lucene.queryparser.xml.builders.GenericTextQueryBuilder;
import com.bloomberg.news.lucene.queryparser.xml.builders.NearFirstQueryBuilder;
import com.bloomberg.news.lucene.queryparser.xml.builders.NearQueryBuilder;
import com.bloomberg.news.lucene.queryparser.xml.builders.PhraseQueryBuilder;
import com.bloomberg.news.lucene.queryparser.xml.builders.TermFreqBuilder;
import com.bloomberg.news.lucene.queryparser.xml.builders.WildcardNearQueryBuilder;

//import com.bloomberg.news.solr.search.xml.*;

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
 * CoreParser + Bloomberg (News) specific custom builders
 */
public class BBCoreParser extends CoreParser {

  protected TermBuilder termBuilder;

  /**
   * Construct an XML parser that uses a single instance QueryParser for handling
   * UserQuery tags - all parse operations are synchronized on this parser
   *
   * @param parser A QueryParser which will be synchronized on during parse calls.
   */
  public BBCoreParser(Analyzer analyzer, QueryParser parser) {
    this(null, analyzer, parser);
  }

  /**
   * Constructs an XML parser that creates a QueryParser for each UserQuery request.
   *
   * @param defaultField The default field name used by QueryParsers constructed for UserQuery tags
   */
  public BBCoreParser(String defaultField, Analyzer analyzer) {
    this(defaultField, analyzer, null);
  }

  protected BBCoreParser(String defaultField, Analyzer analyzer, QueryParser parser) {
    super(defaultField, analyzer, parser);

    this.termBuilder = new TermBuilder(analyzer);

    {
      SpanQueryBuilder termQueryBuilder = new BBTermQueryBuilder(termBuilder);
      queryFactory.addBuilder("TermQuery", termQueryBuilder);
      spanFactory.addBuilder("TermQuery", termQueryBuilder);
      queryFactory.addBuilder("TermFreqQuery", new TermFreqBuilder(termQueryBuilder));
    }
    {
      QueryBuilder termsQueryBuilder = new BBTermsQueryBuilder(termBuilder);
      queryFactory.addBuilder("TermsQuery", termsQueryBuilder);
      queryFactory.addBuilder("TermsFreqQuery", new TermFreqBuilder(termsQueryBuilder));
    }

    queryFactory.addBuilder("PhraseQuery", new PhraseQueryBuilder(analyzer));
    //GenericTextQuery is a error tolerant version of PhraseQuery
    queryFactory.addBuilder("GenericTextQuery", new GenericTextQueryBuilder(analyzer));

    {
      SpanQueryBuilder builder = new NearQueryBuilder(spanFactory);
      queryFactory.addBuilder("NearQuery", builder);
      spanFactory.addBuilder("NearQuery", builder);
    }
    {
      SpanQueryBuilder builder = new NearFirstQueryBuilder(spanFactory);
      queryFactory.addBuilder("NearFirstQuery", builder);
      spanFactory.addBuilder("NearFirstQuery", builder);
    }
    {
      SpanQueryBuilder builder =  new WildcardNearQueryBuilder(analyzer);
      queryFactory.addBuilder("WildcardNearQuery", builder);
      spanFactory.addBuilder("WildcardNearQuery", builder);
    }
  }
}
