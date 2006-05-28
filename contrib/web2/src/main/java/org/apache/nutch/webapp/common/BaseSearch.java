/*
 * Copyright 2006 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nutch.webapp.common;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.LogFormatter;
import org.apache.nutch.plugin.Extension;
import org.apache.nutch.plugin.ExtensionPoint;
import org.apache.nutch.webapp.extension.PostSearchExtensionPoint;
import org.apache.nutch.webapp.extension.PreSearchExtensionPoint;
import org.apache.nutch.webapp.extension.SearchExtensionPoint;

public class BaseSearch {

  public static Logger LOG = LogFormatter.getLogger(BaseSearch.class.getName());

  protected PreSearchExtensionPoint[] presearch;

  protected SearchExtensionPoint[] search;

  protected PostSearchExtensionPoint[] postsearch;

  protected Collection setup(String xPoint, Configuration conf) {
    LOG.info("setting up:" + xPoint);

    HashMap filters = new HashMap();
    try {
      ExtensionPoint point = serviceLocator.getPluginRepository()
          .getExtensionPoint(xPoint);
      if (point != null) {
        Extension[] extensions = point.getExtensions();
        for (int i = 0; i < extensions.length; i++) {
          Extension extension = extensions[i];
          Object extensionInstance = extension.getExtensionInstance();
          if (!filters.containsKey(extensionInstance.getClass().getName())) {
            filters.put(extensionInstance.getClass().getName(),
                extensionInstance);
          }
        }
        return filters.values();
      }
    } catch (Exception e) {
      LOG.info("Error setting up extensions :" + e);
    }
    return Collections.EMPTY_LIST;

  }

  private SearchContextImpl context;

  private ServiceLocator serviceLocator;

  /**
   * Construct new BaseSearch object
   */
  public BaseSearch(ServiceLocator locator) {
    this.serviceLocator = locator;
    Collection pre = getPreSearchExtensions(serviceLocator.getConfiguration());
    presearch = new PreSearchExtensionPoint[pre.size()];
    pre.toArray(presearch);

    Collection searchC = getSearchExtensions(serviceLocator.getConfiguration());
    search = new SearchExtensionPoint[searchC.size()];
    searchC.toArray(search);

    Collection post = getPostSearchExtensions(serviceLocator.getConfiguration());
    postsearch = new PostSearchExtensionPoint[post.size()];
    post.toArray(postsearch);
  }

  public Collection getPreSearchExtensions(Configuration conf) {
    if (conf.getObject(PreSearchExtensionPoint.X_POINT_ID) == null) {
      conf.setObject(PreSearchExtensionPoint.X_POINT_ID, setup(
          PreSearchExtensionPoint.X_POINT_ID, conf));
    }

    return (Collection) conf.getObject(PreSearchExtensionPoint.X_POINT_ID);
  }

  public Collection getSearchExtensions(Configuration conf) {
    if (conf.getObject(SearchExtensionPoint.X_POINT_ID) == null) {
      conf.setObject(SearchExtensionPoint.X_POINT_ID, setup(
          SearchExtensionPoint.X_POINT_ID, conf));
    }
    return (Collection) conf.getObject(SearchExtensionPoint.X_POINT_ID);
  }

  public Collection getPostSearchExtensions(Configuration conf) {
    if (conf.getObject(PostSearchExtensionPoint.X_POINT_ID) == null) {
      conf.setObject(PostSearchExtensionPoint.X_POINT_ID, setup(
          PostSearchExtensionPoint.X_POINT_ID, conf));
    }
    return (Collection) conf.getObject(PostSearchExtensionPoint.X_POINT_ID);
  }

  /**
   * Call plugins participating PreSearch activities
   */
  void callPreSearch() {
    for (int i = 0; i < presearch.length; i++) {
      presearch[i].doPreSearch(context);
    }
  }

  /**
   * Call plugins participating Search activities
   */
  void callSearch() {
    for (int i = 0; i < search.length; i++) {
      search[i].doSearch(context);
    }
  }

  /**
   * Call plugins participating postSearch activities
   */
  void callPostSearch() {
    for (int i = 0; i < postsearch.length; i++) {
      postsearch[i].doPostSearch(context);
    }
  }

  /**
   * Entry point to execute the search
   */
  public void doSearch() {
    // create context
    context = new SearchContextImpl(serviceLocator);
    callPreSearch();
    serviceLocator.getSearch().performSearch();
    // callSearch();
    callPostSearch();
  }
}
