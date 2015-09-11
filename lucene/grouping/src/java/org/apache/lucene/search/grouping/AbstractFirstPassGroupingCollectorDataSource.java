package org.apache.lucene.search.grouping;

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

abstract interface AbstractFirstPassGroupingCollectorDataSource<GROUP_VALUE_TYPE> {

  /**
   * Returns the group value for the specified doc.
   *
   * @param doc The specified doc
   * @return the group value for the specified doc
   */
  GROUP_VALUE_TYPE getDocGroupValue(int doc);

  /**
   * Returns a copy of the specified group value by creating a new instance and copying the value from the specified
   * groupValue in the new instance. Or optionally the reuse argument can be used to copy the group value in.
   *
   * @param groupValue The group value to copy
   * @param reuse Optionally a reuse instance to prevent a new instance creation
   * @return a copy of the specified group value
   */
  GROUP_VALUE_TYPE copyDocGroupValue(GROUP_VALUE_TYPE groupValue, GROUP_VALUE_TYPE reuse);

}
