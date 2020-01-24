/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.tools;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;

/**
 * A CopyFilter which compares Java Regex Patterns to each Path to determine
 * whether a file should be copied.
 */
public class RegexCopyFilter extends CopyFilter {

  private static final Log LOG = LogFactory.getLog(RegexCopyFilter.class);
  private Path filtersPath;
  private List<Pattern> filters;

  /**
   * Constructor, sets up a File object to read filter patterns from and
   * the List to store the patterns.
   */
  protected RegexCopyFilter(Configuration conf, String filtersFilename) {
    filtersPath = new Path(filtersFilename);
    filters = new ArrayList<>();
    setConf(conf);
  }

  /**
   * Loads a list of filter patterns for use in shouldCopy.
   */
  @Override
  public void initialize() {
    BufferedReader reader = null;
    try {
      Configuration conf = getConf();
      FileSystem fs = FileSystem.get(conf);
      InputStream is = fs.open(filtersPath);

      reader = new BufferedReader(new InputStreamReader(is,
          Charset.forName("UTF-8")));
      String line;
      while ((line = reader.readLine()) != null) {
        Pattern pattern = Pattern.compile(line);
        filters.add(pattern);
      }
    } catch (FileNotFoundException notFound) {
      LOG.error("Can't find filters file " + filtersPath);
    } catch (IOException cantRead) {
      LOG.error("An error occurred while attempting to read from " +
          filtersPath);
    } finally {
      IOUtils.cleanup(LOG, reader);
    }
  }

  /**
   * Sets the list of filters to exclude files from copy.
   * Simplifies testing of the filters feature.
   *
   * @param filtersList a list of Patterns to be excluded
   */
  @VisibleForTesting
  protected final void setFilters(List<Pattern> filtersList) {
    this.filters = filtersList;
  }

  @Override
  public boolean shouldCopy(Path path) {
    if (filters.isEmpty()) {
      return true;
    }

    for (Pattern filter : filters) {
      if (filter.matcher(path.toString()).matches()) {
        return true;
      }
    }

    return false;
  }
}
