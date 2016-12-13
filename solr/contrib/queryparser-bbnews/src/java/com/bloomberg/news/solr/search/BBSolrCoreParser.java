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
package com.bloomberg.news.solr.search;

import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.xml.QueryBuilder;
import org.apache.lucene.queryparser.xml.builders.SpanQueryBuilder;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.SolrQueryBuilder;

import com.bloomberg.news.lucene.queryparser.xml.BBCoreParser;
import com.bloomberg.news.solr.search.xml.*;
import org.apache.solr.util.plugin.NamedListInitializedPlugin;

/**
 * Assembles a QueryBuilder which uses Query objects from Solr's <code>search</code> module
 * in addition to Query objects supported by the Lucene <code>BBCoreParser</code>.
 */
public class BBSolrCoreParser extends BBCoreParser implements NamedListInitializedPlugin {

  protected final SolrQueryRequest req;

  public BBSolrCoreParser(String defaultField, Analyzer analyzer,
      SolrQueryRequest req) {
    super(defaultField, analyzer);
    this.req = req;

    queryFactory.addBuilder("DisjunctionMaxQuery", new DisjunctionMaxQueryBuilder(defaultField, analyzer, req, queryFactory));

    {
      SpanQueryBuilder builder =  new BooleanQueryBuilder(defaultField, analyzer, req, spanFactory);
      queryFactory.addBuilder("BooleanQuery", builder);
      spanFactory.addBuilder("BooleanQuery", builder);
    }

    queryFactory.addBuilder("RangeQuery", new RangeQueryBuilder(defaultField, analyzer, req, queryFactory));

    queryFactory.addBuilder("WildcardQuery", new WildcardQueryBuilder(defaultField, analyzer, req, queryFactory));

    queryFactory.addBuilder("BoostedQuery", new BoostedQueryBuilder(defaultField, analyzer, req, queryFactory));
  }

  @Override
  public void init(@SuppressWarnings("rawtypes") NamedList initArgs) {
    if (initArgs == null || initArgs.size() == 0) {
      return;
    }
    final SolrResourceLoader loader;
    if (req == null) {
      loader = new SolrResourceLoader();
    } else {
      loader = req.getCore().getResourceLoader();
    }

    @SuppressWarnings("unchecked")
    final Iterable<Map.Entry<String,Object>> args = initArgs;
    for (final Map.Entry<String,Object> entry : args) {
      final String queryName = entry.getKey();
      final String queryBuilderClassName = (String)entry.getValue();

      final SolrQueryBuilder queryBuilder = loader.newInstance(
          queryBuilderClassName,
          SolrQueryBuilder.class,
          null,
          new Class[] {String.class, Analyzer.class, SolrQueryRequest.class, QueryBuilder.class},
          new Object[] {defaultField, analyzer, req, this});

      this.queryFactory.addBuilder(queryName, queryBuilder);
    }
  }

}
