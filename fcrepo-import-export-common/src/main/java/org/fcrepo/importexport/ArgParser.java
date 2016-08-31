/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.importexport;

/**
 * Command-line arguments parser.
 *
 * @author awoods
 * @author escowles
 * @since 2016-08-29
 */
public interface ArgParser {

    public String DEFAULT_RDF_EXT = ".ttl";
    public String DEFAULT_RDF_LANG = "text/turtle";

    /**
     * This method parses the command line options, returning the Import/Export configuration
     *
     * @param args from the command line
     * @return Import/Export configuration
     */
    public Config parse(final String[] args);

}
