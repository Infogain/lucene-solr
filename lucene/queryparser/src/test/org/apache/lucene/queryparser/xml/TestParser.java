package org.apache.lucene.queryparser.xml;

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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.analysis.MockTokenFilter;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.BooleanFilter;
import org.apache.lucene.queries.FilterClause;
import org.apache.lucene.queries.TermFilter;
import org.apache.lucene.queryparser.xml.builders.KeywordNearQueryParser;
import org.apache.lucene.queryparser.xml.builders.WildcardNearQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.FieldedQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsFilter;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.intervals.FieldedBooleanQuery;
import org.apache.lucene.search.intervals.IntervalFilterQuery;
import org.apache.lucene.search.intervals.OrderedNearQuery;
import org.apache.lucene.search.intervals.RangeIntervalFilter;
import org.apache.lucene.search.intervals.UnorderedNearQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.w3c.dom.Element;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


public class TestParser extends LuceneTestCase {

  private static CoreParser builder;
  private static Directory dir;
  private static IndexReader reader;
  private static IndexSearcher searcher;
  private static String ANALYSER_PARAM     = "tests.TestParser.analyser";
  private static String DEFAULT_ANALYSER   = "mock";
  private static String STANDARD_ANALYSER  = "standard";
 
  @BeforeClass
  public static void beforeClass() throws Exception {
    String analyserToUse = System.getProperty(ANALYSER_PARAM, DEFAULT_ANALYSER);
    Analyzer analyzer =  null;
    if (analyserToUse.equals(STANDARD_ANALYSER))
    {
      analyzer = new StandardAnalyzer(TEST_VERSION_CURRENT);
    }
    else
    {
      assertEquals(DEFAULT_ANALYSER, analyserToUse);
      // TODO: rewrite test (this needs to set QueryParser.enablePositionIncrements, too, for work with CURRENT):
      analyzer = new MockAnalyzer(random(), MockTokenizer.WHITESPACE, true, MockTokenFilter.ENGLISH_STOPSET);
    }
    builder = new CorePlusExtensionsParser("contents", analyzer);
    
    //MatchAllDocsFilter is not yet in side the builderFactory
    //Remove this when we have MatchAllDocsFilter within CorePlusExtensionsParser
    builder.filterFactory.addBuilder("MatchAllDocsFilter", new FilterBuilder() {
      
      @Override
      public Filter getFilter(Element e) throws ParserException {
        return new MatchAllDocsFilter();
      }
    });

    BufferedReader d = new BufferedReader(new InputStreamReader(
        TestParser.class.getResourceAsStream("reuters21578.txt"), StandardCharsets.US_ASCII));
    dir = newDirectory();
    IndexWriter writer = new IndexWriter(dir, newIndexWriterConfig(TEST_VERSION_CURRENT, analyzer));
    String line = d.readLine();
    while (line != null) {
      int endOfDate = line.indexOf('\t');
      String date = line.substring(0, endOfDate).trim();
      String content = line.substring(endOfDate).trim();
      Document doc = new Document();
      doc.add(newTextField("date", date, Field.Store.YES));
      doc.add(newTextField("contents", content, Field.Store.YES));
      doc.add(new IntField("date2", Integer.valueOf(date), Field.Store.YES));
      writer.addDocument(doc);
      line = d.readLine();
    }
    d.close();
    writer.close();
    reader = DirectoryReader.open(dir);
    searcher = newSearcher(reader);

  }

  @AfterClass
  public static void afterClass() throws Exception {
    reader.close();
    dir.close();
    reader = null;
    searcher = null;
    dir = null;
    builder = null;
  }

  public void testTermQueryXML() throws ParserException, IOException {
    Query q = parse("TermQuery.xml");
    dumpResults("TermQuery", q, 5);
  }
  
  public void testTermQueryEmptyXML() throws ParserException, IOException {
    parse("TermQueryEmpty.xml", true/*shouldFail*/);
  }
  
  public void testTermQueryStopwordXML() throws ParserException, IOException {
    parse("TermQueryStopwords.xml", true/*shouldFail*/);
  }
  
  public void testTermQueryMultipleTermsXML() throws ParserException, IOException {
    parse("TermQueryMultipleTerms.xml", true/*shouldFail*/);
  }

  public void testSimpleTermsQueryXML() throws ParserException, IOException {
    Query q = parse("TermsQuery.xml");
    assertTrue("Expecting a BooleanQuery, but resulted in " + q.getClass(), q instanceof BooleanQuery);
    dumpResults("TermsQuery", q, 5);
  }

  public void testSimpleTermsQueryWithTermElementXML() throws ParserException, IOException {
    Query q = parse("TermsQueryWithTermElement.xml");
    dumpResults("TermsQuery", q, 5);
  }
  
  public void testTermsQueryWithSingleTerm() throws ParserException, IOException {
    Query q = parse("TermsQuerySingleTerm.xml");
    assertTrue("Expecting a TermQuery, but resulted in " + q.getClass(), q instanceof TermQuery);
    dumpResults("TermsQueryWithSingleTerm", q, 5);
  }
  
  
  //term appears like single term but results in two terms when it runs through standard analyzer
  public void testTermsQueryWithStopwords() throws ParserException, IOException {
    Query q = parse("TermsQueryStopwords.xml");
    if (builder.analyzer instanceof StandardAnalyzer)
      assertTrue("Expecting a BooleanQuery, but resulted in " + q.getClass(), q instanceof BooleanQuery);
    dumpResults("TermsQueryWithStopwords", q, 5);
    }
  
  public void testTermsQueryEmpty() throws ParserException, IOException {
    Query q = parse("TermsQueryEmpty.xml");
    assertTrue("Expecting a MatchAllDocsQuery, but resulted in " + q.getClass(), q instanceof MatchAllDocsQuery);
    dumpResults("Empty TermsQuery", q, 5);
  }
  
  public void testTermsQueryWithOnlyStopwords() throws ParserException, IOException {
    Query q = parse("TermsQueryOnlyStopwords.xml");
    if (builder.analyzer instanceof StandardAnalyzer)
      assertTrue("Expecting a MatchAllDocsQuery, but resulted in " + q.getClass(), q instanceof MatchAllDocsQuery);
    dumpResults("TermsQuery with only stopwords", q, 5);
  }
  

  public void testBooleanQueryXML() throws ParserException, IOException {
    Query q = parse("BooleanQuery.xml");
    dumpResults("BooleanQuery", q, 5);
  }

  public void testDisjunctionMaxQueryXML() throws ParserException, IOException {
    Query q = parse("DisjunctionMaxQuery.xml");
    assertTrue(q instanceof DisjunctionMaxQuery);
    DisjunctionMaxQuery d = (DisjunctionMaxQuery)q;
    assertEquals(0.0f, d.getTieBreakerMultiplier(), 0.0001f);
    assertEquals(2, d.getDisjuncts().size());
    DisjunctionMaxQuery ndq = (DisjunctionMaxQuery) d.getDisjuncts().get(1);
    assertEquals(1.2f, ndq.getTieBreakerMultiplier(), 0.0001f);
    assertEquals(1, ndq.getDisjuncts().size());
  }

  public void testRangeFilterQueryXML() throws ParserException, IOException {
    Query q = parse("RangeFilterQuery.xml");
    dumpResults("RangeFilter", q, 5);
  }

  public void testUserQueryXML() throws ParserException, IOException {
    Query q = parse("UserInputQuery.xml");
    dumpResults("UserInput with Filter", q, 5);
  }

  public void testCustomFieldUserQueryXML() throws ParserException, IOException {
    Query q = parse("UserInputQueryCustomField.xml");
    int h = searcher.search(q, null, 1000).totalHits;
    assertEquals("UserInputQueryCustomField should produce 0 result ", 0, h);
  }

  public void testLikeThisQueryXML() throws Exception {
    Query q = parse("LikeThisQuery.xml");
    dumpResults("like this", q, 5);
  }

  public void testBoostingQueryXML() throws Exception {
    Query q = parse("BoostingQuery.xml");
    dumpResults("boosting ", q, 5);
  }

  public void testFuzzyLikeThisQueryXML() throws Exception {
    Query q = parse("FuzzyLikeThisQuery.xml");
    //show rewritten fuzzyLikeThisQuery - see what is being matched on
    if (VERBOSE) {
      System.out.println(q.rewrite(reader));
    }
    dumpResults("FuzzyLikeThis", q, 5);
  }

  public void testTermsFilterXML() throws Exception {
    Query q = parse("TermsFilterQuery.xml");
    dumpResults("Terms Filter", q, 5);
  }
  
  public void testTermsFilterWithSingleTerm() throws Exception {
    Query q = parse("TermsFilterQueryWithSingleTerm.xml");
    dumpResults("TermsFilter With SingleTerm", q, 5);
  }
  
  public void testTermsFilterQueryWithStopword() throws Exception {
    Query q = parse("TermsFilterQueryStopwords.xml");
    dumpResults("TermsFilter with Stopword", q, 5);
  }
  
  public void testTermsFilterQueryWithOnlyStopword() throws Exception {
    Query q = parse("TermsFilterOnlyStopwords.xml");
    dumpResults("TermsFilter with all stop words", q, 5);
  }
  
  public void testBoostingTermQueryXML() throws Exception {
    Query q = parse("BoostingTermQuery.xml");
    dumpResults("BoostingTermQuery", q, 5);
  }

  public void testSpanTermXML() throws Exception {
    Query q = parse("SpanQuery.xml");
    dumpResults("Span Query", q, 5);
  }

  public void testConstantScoreQueryXML() throws Exception {
    Query q = parse("ConstantScoreQuery.xml");
    dumpResults("ConstantScoreQuery", q, 5);
  }
  
  public void testPhraseQueryXML() throws Exception {
    Query q = parse("PhraseQuery.xml");
    assertTrue("Expecting a PhraseQuery, but resulted in " + q.getClass(), q instanceof PhraseQuery);
    dumpResults("PhraseQuery", q, 5);
  }
  
  public void testPhraseQueryXMLWithStopwordsXML() throws Exception {
    if (builder.analyzer instanceof StandardAnalyzer) {
      parse("PhraseQueryStopwords.xml", true/*shouldfail*/);
    }
  }
  
  public void testPhraseQueryXMLWithNoTextXML() throws Exception {
    parse("PhraseQueryEmpty.xml", true/*shouldFail*/);
  }

  public void testGenericTextQueryXML() throws Exception {
    Query q = parse("GenericTextQuery.xml");
    assertTrue("Expecting a PhraseQuery, but resulted in " + q.getClass(), q instanceof PhraseQuery);
    dumpResults("GenericTextQuery", q, 5);
  }
  
  public void testGenericTextQuerySingleTermXML() throws Exception {
    Query q = parse("GenericTextQuerySingleTerm.xml");
    assertTrue("Expecting a TermQuery, but resulted in " + q.getClass(), q instanceof TermQuery);
    dumpResults("GenericTextQuery", q, 5);
  }
  
  public void testGenericTextQueryWithStopwordsXML() throws Exception {
    Query q = parse("GenericTextQueryStopwords.xml");
    assertTrue("Expecting a PhraseQuery, but resulted in " + q.getClass(), q instanceof PhraseQuery);
    dumpResults("GenericTextQuery with stopwords", q, 5);
  }
  
  public void testGenericTextQueryWithAllStopwordsXML() throws Exception {
    Query q = parse("GenericTextQueryAllStopwords.xml");
    if (builder.analyzer instanceof StandardAnalyzer)
      assertTrue("Expecting a MatchAllDocsQuery, but resulted in " + q.getClass(), q instanceof MatchAllDocsQuery);
    dumpResults("GenericTextQuery with just stopwords", q, 5);
  }
  
  public void testGenericTextQueryWithNoTextXML() throws Exception {
    Query q = parse("GenericTextQueryEmpty.xml");
    assertTrue("Expecting a MatchAllDocsQuery, but resulted in " + q.getClass(), q instanceof MatchAllDocsQuery);
    dumpResults("GenericTextQuery with no text", q, 5);
  }
  
  public void testGenericTextQueryPhraseWildcardXML() throws Exception {
    Query q = parse("GenericTextQueryPhraseWildcard.xml");
    dumpResults("GenericTextQuery with a phrase wildcard", q, 5);
  }
  
  public void testGenericTextQueryTrailingWildcardXML() throws Exception {
    Query q = parse("GenericTextQueryTrailingWildcard.xml");
    dumpResults("GenericTextQuery with a trailing wildcard", q, 5);
  }

  public void testGenericTextQueryMultiWildcardXML() throws Exception {
    Query q = parse("GenericTextQueryMultiWildcard.xml");
    dumpResults("GenericTextQuery with multiple terms containing wildcards", q, 5);
  }
  
  public void testGenericTextQueryPhraseWildcard2XML() throws Exception {
    Query q = parse("GenericTextQueryPhraseWildcard2.xml");
    dumpResults("GenericTextQuery with a phrase wildcard", q, 5);
  }
  
  public void testGenericTextQueryTrailingWildcard2XML() throws Exception {
    Query q = parse("GenericTextQueryTrailingWildcard2.xml");
    dumpResults("GenericTextQuery with a trailing wildcard", q, 5);
  }

  public void testGenericTextQueryMultiWildcard2XML() throws Exception {
    Query q = parse("GenericTextQueryMultiWildcard2.xml");
    dumpResults("GenericTextQuery with multiple terms containing wildcards", q, 5);
  }

  public void testGenericTextQueryMultiClauseXML() throws Exception {
    Query q = parse("GenericTextQueryMultiClause.xml");
    dumpResults("GenericTextQuery. BooleanQuery containing multiple GenericTextQuery clauses with different boost factors", q, 5);
  }

  public void testComplexPhraseQueryXML() throws Exception {
    Query q = parse("ComplexPhraseQuery.xml");
    dumpResults("ComplexPhraseQuery", q, 5);
  }
  
  public void testComplexPhraseQueryPrefixQueryXML() throws Exception {
    Query q = parse("ComplexPhraseQueryPrefixQuery.xml");
    dumpResults("ComplexPhraseQuery with a single prefix query term", q, 5);
  }
  
  public void testComplexPhraseNearQueryXML() throws Exception {
    Query q = parse("ComplexPhraseNearQuery.xml");
    dumpResults("ComplexPhraseNearQuery", q, 5);
  }
  
  /* test cases for keyword near query */
  public void testKWNearQuery() throws Exception {
    Query q = parse("KeywordNear.xml");
    dumpResults("KeywordNear query", q, 5);
  }
  
  public void testKWNearQueryWildcard() throws Exception {
    Query q = parse("KeywordNearWildcard.xml");
    dumpResults("KeywordNear with wildcard terms", q, 5);
  }
  
  public void testKWNearQuerySpecialChar() throws Exception {
    Query q = parse("KeywordNearSpecialChars.xml");
    dumpResults("KeywordNear with special characters", q, 5);
  }
  
  public void testKWNearQuerywithStopwords() throws Exception {
    Query q = parse("KeywordNearStopwords.xml");
    dumpResults("KeywordNear with stopwords", q, 5);
  }
  
  public void testKWNearViaGenericTextQuery() throws Exception {
    Query q = parse("KeywordNearThroughGenericTextQuery.xml");
    dumpResults("GenericTextQuery with multiple terms containing wildcards", q, 5);
  }
  
  public void testKWNearQuerywithEmptytokens() throws Exception {
    Query q = parse("KeywordNearEmptyQuery.xml");
    dumpResults("Keyword Near with empty tokens", q, 5);
  }
  
  //TODO: move this test along with the KeywordNearQueryParser to an appropriate parser names space
  public void testKeywordNearQueryParser() throws Exception {
    
    KeywordNearQueryParser p = new KeywordNearQueryParser("contents", builder.analyzer);
    Query q = p.parse("to");
    dumpResults("KeywordNearQueryParser stop word", q, 5);
    q = p.parse("");
    assertTrue("Expecting a MatchAllDocsQuery, but resulted in " + q.getClass(), q instanceof MatchAllDocsQuery);
    dumpResults("KeywordNearQueryParser empty query", q, 5);
    q = p.parse("<TRUMP PLAZA>");
    dumpResults("KeywordNearQueryParser special char1", q, 5);
    q = p.parse("7/8");
    dumpResults("KeywordNearQueryParser special char2", q, 5);
    q = p.parse("ACQUIR* BY ZENEX <Zenex Oil Pty Ltd> said it acquired the interests of E?so S*h Africa, the local subsidiary");
    dumpResults("KeywordNearQueryParser wildcard", q, 5);
  }

  /* end of keyword near query test cases*/

  private static void tryQuery(WildcardNearQueryParser p, String s) throws Exception {
    Query q = p.parse(s);
    if (VERBOSE) {
      System.out.println("'" + s + "' => '" + q + "'");
    }
  }

  public void testWildcardNearQueryParser() throws Exception {

    WildcardNearQueryParser p = new WildcardNearQueryParser("contents", builder.analyzer);
    Query q = p.parse("to");
    dumpResults("WildcardNearQueryParser stop word", q, 5);
    q = p.parse("");
    assertTrue("Expecting a MatchAllDocsQuery, but resulted in " + q.getClass(), q instanceof MatchAllDocsQuery);
    dumpResults("WildcardNearQueryParser empty query", q, 5);
    q = p.parse("<TRUMP PLAZA>");
    dumpResults("WildcardNearQueryParser special char1", q, 5);
    q = p.parse("7/8");
    dumpResults("WildcardNearQueryParser special char2", q, 5);

    // Made up:
    tryQuery(p, "London-City-Airport");
    tryQuery(p, "London-Cit*-Airport");
    tryQuery(p, "London-*ty-Airport");
    tryQuery(p, "London-*-Airport");
    tryQuery(p, "London-* Airport");
    tryQuery(p, "London *-Airport");
    tryQuery(p, "London * Airport");
    tryQuery(p, "London City * Airport");
    tryQuery(p, "London ? Airport");
    tryQuery(p, "Lon*-* *port");
    tryQuery(p, "Lon*-* Airport");
    tryQuery(p, "Lon*-City Airport");

    tryQuery(p, "trick-or-treat");
    tryQuery(p, "trick-??-treat");
    tryQuery(p, "trick-?*?-treat");
    tryQuery(p, "trick-**-treat");
    tryQuery(p, "trick-??-?????");
    tryQuery(p, "trick-*-?????");
    tryQuery(p, "trick-??-* candy");
    tryQuery(p, "trick-*-treat");
    tryQuery(p, "trick-*-*-treat");
    tryQuery(p, "trick * * treat");
    tryQuery(p, "i love trick-*-treat candy");

    tryQuery(p, "slow up?-side down?");
    tryQuery(p, "slow up??-side down??");
    tryQuery(p, "slow-up??side down");
    tryQuery(p, "slow-up-??side down");

    // Based on real queries:
    tryQuery(p, "8-k*");
    tryQuery(p, "re-domesticat*");
    tryQuery(p, "re-domicile*");
    tryQuery(p, "ipc-fipe*");
    tryQuery(p, "leo-mesdag b.v.*");
    tryQuery(p, "spin-off*");
    tryQuery(p, "skb-bank*");
    tryQuery(p, "gonzalez-paramo*");
    tryQuery(p, "jenaro cardona-fox*");
    tryQuery(p, "t-note*");
    tryQuery(p, "non-bank*");
    tryQuery(p, "conversion to open-end*");
    tryQuery(p, "estate uk-3*");
    tryQuery(p, "sc-to-* sec filing");
    tryQuery(p, "ВСМПО-АВИСМА*");
    tryQuery(p, "jean-franc* dubos");
    tryQuery(p, "vietnam-singapore industrial* park*");
    tryQuery(p, "prorated* or pro-rated");

    // Not great with standard analyzer that throws away stop words:

    // Made up:
    tryQuery(p, "stopword-fail0 *or-me");
    tryQuery(p, "stopword-fail1 or*-me");

    // Based on real queries:
    tryQuery(p, "stopword-fail2 sc-to* sec filing");
    tryQuery(p, "stopword-fail3 throw-in*");
  }



  public void testMatchAllDocsPlusFilterXML() throws ParserException, IOException {
    Query q = parse("MatchAllDocsQuery.xml");
    dumpResults("MatchAllDocsQuery with range filter", q, 5);
  }

  public void testBooleanFilterXML() throws ParserException, IOException {
    Query q = parse("BooleanFilter.xml");
    dumpResults("Boolean filter", q, 5);
  }

  public void testNestedBooleanQuery() throws ParserException, IOException {
    Query q = parse("NestedBooleanQuery.xml");
    dumpResults("Nested Boolean query", q, 5);
  }

  public void testCachedFilterXML() throws ParserException, IOException {
    Query q = parse("CachedFilter.xml");
    dumpResults("Cached filter", q, 5);
  }

  public void testDuplicateFilterQueryXML() throws ParserException, IOException {
    List<AtomicReaderContext> leaves = searcher.getTopReaderContext().leaves();
    Assume.assumeTrue(leaves.size() == 1);
    Query q = parse("DuplicateFilterQuery.xml");
    int h = searcher.search(q, null, 1000).totalHits;
    assertEquals("DuplicateFilterQuery should produce 1 result ", 1, h);
  }

  public void testNumericRangeFilterQueryXML() throws ParserException, IOException {
    Query q = parse("NumericRangeFilterQuery.xml");
    dumpResults("NumericRangeFilter", q, 5);
  }
  
  public void testNumericRangeQuery() throws IOException {
    String text = "<NumericRangeQuery fieldName='date2' lowerTerm='19870409' upperTerm='19870412'/>";
    Query q = parseText(text, false);
    dumpResults("NumericRangeQuery1", q, 5);
    text = "<NumericRangeQuery fieldName='date2' lowerTerm='19870602' />";
    q = parseText(text, false);
    dumpResults("NumericRangeQuery2", q, 5);
    text = "<NumericRangeQuery fieldName='date2' upperTerm='19870408'/>";
    q = parseText(text, false);
    dumpResults("NumericRangeQuery3", q, 5);
  }
  
  public void testNumericRangeFilter() throws IOException {
    String text = "<ConstantScoreQuery><NumericRangeFilter fieldName='date2' lowerTerm='19870410' upperTerm='19870531'/></ConstantScoreQuery>";
    Query q = parseText(text, false);
    dumpResults("NumericRangeFilter1", q, 5);
    text = "<ConstantScoreQuery><NumericRangeFilter fieldName='date2' lowerTerm='19870601' /></ConstantScoreQuery>";
    q = parseText(text, false);
    dumpResults("NumericRangeFilter2", q, 5);
    text = "<ConstantScoreQuery><NumericRangeFilter fieldName='date2' upperTerm='19870408'/></ConstantScoreQuery>";
    q = parseText(text, false);
    dumpResults("NumericRangeFilter3", q, 5);
  }
  
  public void testDisjunctionMaxQuery_MatchAllDocsQuery() throws IOException {
    String text = "<DisjunctionMaxQuery fieldName='content'>"
        + "<KeywordNearQuery>rio de janeiro</KeywordNearQuery>"
        + "<KeywordNearQuery>summit</KeywordNearQuery>"
        + "<KeywordNearQuery> </KeywordNearQuery></DisjunctionMaxQuery>";
    Query q = parseText(text, false);
    int size = ((DisjunctionMaxQuery)q).getDisjuncts().size();
    assertTrue("Expecting 2 clauses, but resulted in " + size, size == 2);
    DisjunctionMaxQuery dm = (DisjunctionMaxQuery)q;
    for(Query q1 : dm.getDisjuncts())
    {
      assertFalse("Not expecting MatchAllDocsQuery ",q1 instanceof MatchAllDocsQuery);
    }
    
    text = "<DisjunctionMaxQuery fieldName='content' >"
        + "<MatchAllDocsQuery/>"
        + "<KeywordNearQuery> </KeywordNearQuery></DisjunctionMaxQuery>";
    q = parseText(text, false);
    assertTrue("Expecting a MatchAllDocsQuery, but resulted in " + q.getClass(), q instanceof MatchAllDocsQuery);

  }
  
  //federal N/3 (credit OR (taxes N/1 income))
  public void testNearBooleanNear() throws IOException, ParserException {
    String text = ""
                  +"<NearQuery fieldName=\"contents\" slop=\"4\" inOrder=\"false\">"
                  +"<KeywordNearQuery>bank</KeywordNearQuery>"
                  +"<BooleanQuery disableCoord=\"true\"> "
                  +"<Clause occurs=\"should\"><TermQuery>quarter</TermQuery></Clause>"
                  +"<Clause occurs=\"should\">"
                  +"<NearQuery slop=\"2\" inOrder=\"false\">"
                  +"<KeywordNearQuery>earlier,</KeywordNearQuery>"
                  +"<KeywordNearQuery>april</KeywordNearQuery>"
                  +"</NearQuery>"
                  +"</Clause>"
                  +"</BooleanQuery>"
                  +"</NearQuery>"
                  ;
    Query q = parseText(text, false);
    dumpResults("testNearBooleanNear", q, 5);
  }
  
  
  //working version of (A OR B) N/5 C
  public void testNearBoolean() throws IOException {
    BooleanQuery bq = new BooleanQuery();
    bq.add(new TermQuery(new Term("contents", "iranian")), BooleanClause.Occur.SHOULD);
    bq.add(new TermQuery(new Term("contents", "north")), BooleanClause.Occur.SHOULD);
    
    FieldedQuery[] subQueries = new FieldedQuery[2];
    subQueries[0] = FieldedBooleanQuery.toFieldedQuery(bq);
    subQueries[1] = FieldedBooleanQuery.toFieldedQuery(new TermQuery(new Term("contents", "akbar")));
    FieldedQuery fq = new UnorderedNearQuery(5, subQueries);
    dumpResults("testNearBoolean", fq, 5);
  }
  
  public void testNearFirstBooleanMustXml() throws IOException, ParserException {
    String text = ""
                  +"<NearFirstQuery fieldName=\"contents\" end=\"5\">"
                  +"<BooleanQuery disableCoord=\"true\"> "
                  +"<Clause occurs=\"must\"><KeywordNearQuery>ban*</KeywordNearQuery></Clause>"
                  +"<Clause occurs=\"must\"><KeywordNearQuery>sa*</KeywordNearQuery></Clause>"
                  +"</BooleanQuery>"
                  +"</NearFirstQuery>"
                  ;
    Query q = parseText(text, false);
    dumpResults("testNearFirstBooleanMustXml", q, 50);
  }
  
  public void testNearFirstBooleanMust() throws IOException {
    BooleanQuery bq = new BooleanQuery();
    bq.add(new TermQuery(new Term("contents", "upholds")), BooleanClause.Occur.MUST);
    bq.add(new TermQuery(new Term("contents", "building")), BooleanClause.Occur.MUST);
    
    FieldedQuery[] subQueries = new FieldedQuery[2];
    subQueries[0] = FieldedBooleanQuery.toFieldedQuery(bq);
    subQueries[1] = FieldedBooleanQuery.toFieldedQuery(new TermQuery(new Term("contents", "bank")));
    FieldedQuery fq = new UnorderedNearQuery(7, subQueries);
    dumpResults("testNearFirstBooleanMust", fq, 5);
  }
  
  public void testBooleanQuerywithMatchAllDocsQuery() throws IOException {
    String text = "<BooleanQuery fieldName='content' disableCoord='true'>"
        + "<Clause occurs='should'><KeywordNearQuery>rio de janeiro</KeywordNearQuery></Clause>"
        + "<Clause occurs='should'><KeywordNearQuery>summit</KeywordNearQuery></Clause>"
        + "<Clause occurs='should'><KeywordNearQuery> </KeywordNearQuery></Clause></BooleanQuery>";
    Query q = parseText(text, false);
    int size = ((BooleanQuery)q).clauses().size();
    assertTrue("Expecting 2 clauses, but resulted in " + size, size == 2);
    BooleanQuery bq = (BooleanQuery)q;
    for(BooleanClause bc : bq.clauses())
    {
      assertFalse("Not expecting MatchAllDocsQuery ",bc.getQuery() instanceof MatchAllDocsQuery);
    }
  
    text = "<BooleanQuery fieldName='content' disableCoord='true'>"
        + "<Clause occurs='must'><KeywordNearQuery>rio de janeiro</KeywordNearQuery></Clause>"
        + "<Clause occurs='should'><KeywordNearQuery> </KeywordNearQuery></Clause></BooleanQuery>";
    q = parseText(text, false);
    assertTrue("Expecting a IntervalFilterQuery, but resulted in " + q.getClass(), q instanceof IntervalFilterQuery);
    
    text = "<BooleanQuery fieldName='content' disableCoord='true'>"
        + "<Clause occurs='must'><KeywordNearQuery>rio de janeiro</KeywordNearQuery></Clause>"
        + "<Clause occurs='must'><KeywordNearQuery>summit</KeywordNearQuery></Clause>"
        + "<Clause occurs='should'><KeywordNearQuery> </KeywordNearQuery></Clause></BooleanQuery>";
    q = parseText(text, false);
    assertTrue("Expecting a BooleanQuery, but resulted in " + q.getClass(), q instanceof BooleanQuery);
    bq = (BooleanQuery)q;
    size = bq.clauses().size();
    assertTrue("Expecting 2 clauses, but resulted in " + size, size == 2);
    for(BooleanClause bc : bq.clauses())
    {
      assertFalse("Not expecting MatchAllDocsQuery ", bc.getQuery() instanceof MatchAllDocsQuery);
    }
    
    text = "<BooleanQuery fieldName='content' disableCoord='true'>"
        + "<Clause occurs='must'><MatchAllDocsQuery/></Clause>"
        + "<Clause occurs='should'><KeywordNearQuery> </KeywordNearQuery></Clause></BooleanQuery>";
    q = parseText(text, false);
    assertTrue("Expecting a MatchAllDocsQuery, but resulted in " + q.getClass(), q instanceof MatchAllDocsQuery);
    
    text = "<BooleanQuery fieldName='content' disableCoord='true'>"
        + "<Clause occurs='must'><MatchAllDocsQuery/></Clause>"
        + "<Clause occurs='mustnot'><TermQuery>summit</TermQuery></Clause></BooleanQuery>";
    q = parseText(text, false);
    assertTrue("Expecting a BooleanQuery, but resulted in " + q.getClass(), q instanceof BooleanQuery);
    bq = (BooleanQuery)q;
    size = bq.clauses().size();
    assertTrue("Expecting 2 clauses, but resulted in " + size, size == 2);
    boolean bMatchAllDocsFound = false;
    for(BooleanClause bc : bq.clauses())
    {
      bMatchAllDocsFound |= bc.getQuery() instanceof MatchAllDocsQuery;
    }
    assertTrue("Expecting MatchAllDocsQuery ", bMatchAllDocsFound);
    
  }
  
  public void testBooleanFilterwithMatchAllDocsFilter() throws ParserException, IOException {
    
    String text = "<BooleanFilter fieldName='content' disableCoord='true'>"
        + "<Clause occurs='should'><TermFilter>janeiro</TermFilter></Clause>"
        + "<Clause occurs='should'><MatchAllDocsFilter/></Clause></BooleanFilter>";
    
    Filter f = builder.filterFactory.getFilter(parseXML(text));
    assertTrue("Expecting a TermFilter, but resulted in " + f.getClass(), f instanceof TermFilter);
  
    text = "<BooleanFilter fieldName='content' disableCoord='true'>"
        + "<Clause occurs='must'><TermFilter>rio</TermFilter></Clause>"
        + "<Clause occurs='should'><MatchAllDocsFilter/></Clause></BooleanFilter>";
    f = builder.filterFactory.getFilter(parseXML(text));
    assertTrue("Expecting a TermFilter, but resulted in " + f.getClass(), f instanceof TermFilter);
    
    text = "<BooleanFilter fieldName='content' disableCoord='true'>"
        + "<Clause occurs='must'><TermFilter>rio</TermFilter></Clause>"
        + "<Clause occurs='must'><TermFilter>janeiro</TermFilter></Clause>"
        + "<Clause occurs='must'><TermFilter>summit</TermFilter></Clause>"
        + "<Clause occurs='should'><MatchAllDocsFilter/></Clause></BooleanFilter>";
    f = builder.filterFactory.getFilter(parseXML(text));
    assertTrue("Expecting a BooleanFilter, but resulted in " + f.getClass(), f instanceof BooleanFilter);
    BooleanFilter bf = (BooleanFilter)f;
    int size = bf.clauses().size();
    assertTrue("Expecting 3 clauses, but resulted in " + size, size == 3);
    for(FilterClause fc : bf.clauses())
    {
      assertFalse("Not expecting MatchAllDocsQuery ", fc.getFilter() instanceof MatchAllDocsFilter);
    }
    
    text = "<BooleanFilter fieldName='content' disableCoord='true'>"
        + "<Clause occurs='must'><MatchAllDocsFilter/></Clause>"
        + "<Clause occurs='should'><MatchAllDocsFilter/></Clause></BooleanFilter>";
    f = builder.filterFactory.getFilter(parseXML(text));
    assertTrue("Expecting a MatchAllDocsFilter, but resulted in " + f.getClass(), f instanceof MatchAllDocsFilter);
    
    text = "<BooleanFilter fieldName='content' disableCoord='true'>"
        + "<Clause occurs='must'><MatchAllDocsFilter/></Clause>"
        + "<Clause occurs='mustnot'><TermFilter>summit</TermFilter></Clause></BooleanFilter>";
    f = builder.filterFactory.getFilter(parseXML(text));
    assertTrue("Expecting a BooleanFilter, but resulted in " + f.getClass(), f instanceof BooleanFilter);
    bf = (BooleanFilter)f;
    size = bf.clauses().size();
    assertTrue("Expecting 2 clauses, but resulted in " + size, size == 2);
    boolean bMatchAllDocsFound = false;
    for(FilterClause fc : bf.clauses())
    {
      bMatchAllDocsFound |= fc.getFilter() instanceof MatchAllDocsFilter;
    }
    assertTrue("Expecting MatchAllDocsFilter ", bMatchAllDocsFound);
    
  }
  
  public void testNearFirstXML() throws ParserException, IOException {
    Query q = parse("NearFirst.xml");
    dumpResults("Near First (Interval)", q, 5);
  }
  
  public void testNearNotFirstXML() throws ParserException, IOException {
    Query q = parse("NearNotFirst.xml");
    dumpResults("Near Not First (Interval)", q, 5);
  }
  
  public void testNearNearXML() throws ParserException, IOException {
    Query q = parse("NearNear.xml");
    dumpResults("Near Near (Interval)", q, 5);
  }
  
  public void testNearOrXML() throws ParserException, IOException {
    Query q = parse("NearOr.xml");
    dumpResults("Near Or (Interval)", q, 5);
  }
  
  public void testNearPhraseXML() throws ParserException, IOException {
    Query q = parse("NearPhrase.xml");
    dumpResults("Near Phrase (Interval)", q, 5);
  }
  
  public void testNearWildcardXML() throws ParserException, IOException {
    Query q = parse("NearWildcard.xml");
    dumpResults("NearWildcard (Interval)", q, 5);
  }
  
  public void testNearTermQuery() throws ParserException, IOException {
    int slop = 1;
    FieldedQuery[] subqueries = new FieldedQuery[2];
    subqueries[0] = new TermQuery(new Term("contents", "keihanshin"));
    subqueries[1] = new TermQuery(new Term("contents", "real"));
    Query q = new OrderedNearQuery(slop, true, subqueries);
    dumpResults("NearPrefixQuery", q, 5);
  }
  
  public void testPrefixedNearQuery() throws ParserException, IOException {
    int slop = 1;
    FieldedQuery[] subqueries = new FieldedQuery[2];
    subqueries[0] = new PrefixQuery(new Term("contents", "keihanshi"));
    ((MultiTermQuery)subqueries[0]).setRewriteMethod(MultiTermQuery.CONSTANT_SCORE_BOOLEAN_QUERY_REWRITE);
    subqueries[1] = new PrefixQuery(new Term("contents", "rea"));
    ((MultiTermQuery)subqueries[1]).setRewriteMethod(MultiTermQuery.CONSTANT_SCORE_BOOLEAN_QUERY_REWRITE);
    Query q = new OrderedNearQuery(slop, true, subqueries);
    dumpResults("NearPrefixQuery", q, 5);
  }

  //================= Helper methods ===================================

  private Query parse(String xmlFileName) throws IOException {
    return parse(xmlFileName, false);
  }
  
  private Query parse(String xmlFileName, Boolean shouldFail) throws IOException {
    InputStream xmlStream = TestParser.class.getResourceAsStream(xmlFileName);
    assertTrue("Test XML file " + xmlFileName + " cannot be found", xmlStream != null);
    Query result = parse(xmlStream, shouldFail);
    xmlStream.close();
    return result;
  }
  private Query parseText(String text, Boolean shouldFail) 
  {
    InputStream xmlStream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    return parse(xmlStream, shouldFail);
  }
  
  private Query parse(InputStream xmlStream, Boolean shouldFail)
  {
    Query result = null;
    try {
      result = builder.parse(xmlStream);
    } catch (ParserException ex) {
      assertTrue("Parser exception " + ex, shouldFail);
    }
    if (shouldFail && result != null)
      assertTrue("Expected to fail. But resulted in query: " + result.getClass() + " with value: " + result, false);
    return result;
  }

  private void dumpResults(String qType, Query q, int numDocs) throws IOException {
    if (VERBOSE) {
      System.out.println("=======TEST: " + q.getClass() + " query=" + q);
    }
    TopDocs hits = searcher.search(q, null, numDocs);
    assertTrue(qType + " " + q + " should produce results ", hits.totalHits > 0);
    if (true) {
      System.out.println("=========" + qType + " class=" + q.getClass() + " query=" + q + "============");
      ScoreDoc[] scoreDocs = hits.scoreDocs;
      for (int i = 0; i < Math.min(numDocs, hits.totalHits); i++) {
        Document ldoc = searcher.doc(scoreDocs[i].doc);
        System.out.println("[" + ldoc.get("date") + "]" + ldoc.get("contents"));
      }
      System.out.println();
    }
  }

  //helper
  private static Element parseXML(String text) throws ParserException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = null;
    try {
      db = dbf.newDocumentBuilder();
    }
    catch (Exception se) {
      throw new ParserException("XML Parser configuration error", se);
    }
    org.w3c.dom.Document doc = null;
    InputStream xmlStream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    try {
      doc = db.parse(xmlStream);
    }
    catch (Exception se) {
      throw new ParserException("Error parsing XML stream:" + se, se);
    }
    return doc.getDocumentElement();
  }
}
