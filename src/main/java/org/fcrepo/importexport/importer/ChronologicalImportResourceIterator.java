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
package org.fcrepo.importexport.importer;

import static java.nio.file.FileVisitResult.CONTINUE;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.fcrepo.importexport.common.FcrepoConstants.NON_RDF_SOURCE;
import static org.fcrepo.importexport.common.FcrepoConstants.RDF_TYPE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RDFDataMgr;
import org.fcrepo.importexport.common.Config;
import org.fcrepo.importexport.common.FcrepoConstants;
import org.fcrepo.importexport.common.URITranslationUtil;
import org.fcrepo.importexport.importer.VersionImporter.ImportResource;

/**
 * Iterates through resources in chronological order based on last modified timestamp 
 * 
 * @author bbpennel
 *
 */
public class ChronologicalImportResourceIterator implements Iterator<ImportResource> {

    private final Config config;
    private final List<URI> sortedUris;
    private Iterator<URI> uriIt;

    private ImportResourceFactory rescFactory;

    public ChronologicalImportResourceIterator(final Config config, final ImportResourceFactory rescFactory)
            throws IOException {
        this.config = config;
        this.rescFactory = rescFactory;

        ChronologicalUriExtractingFileVisitor treeWalker = new ChronologicalUriExtractingFileVisitor(this.config);
        Files.walkFileTree(config.getBaseDirectory().toPath(), treeWalker);
        sortedUris = treeWalker.getSortedUris();
    }

    @Override
    public boolean hasNext() {
        if (uriIt == null) {
            uriIt = sortedUris.iterator();
        }
        return uriIt.hasNext();
    }

    @Override
    public ImportResource next() {
        URI nextUri = uriIt.next();

        return rescFactory.createFromUri(nextUri);
    }

    private static Model parseStream(final InputStream in, final Config config) throws IOException {
        final SubjectMappingStreamRDF mapper = new SubjectMappingStreamRDF(config.getSource(),
                                                                           config.getDestination());
        try (final InputStream in2 = in) {
            RDFDataMgr.parse(mapper, in2, contentTypeToLang(config.getRdfLanguage()));
        }
        return mapper.getModel();
    }

    private class ChronologicalUriExtractingFileVisitor extends SimpleFileVisitor<Path> {
        private List<Entry<Long, URI>> createUriList;
        private final Config config;

        public ChronologicalUriExtractingFileVisitor(final Config config) {
            createUriList = new ArrayList<>();
            this.config = config;
        }

        public List<URI> getSortedUris() {
            return createUriList.stream()
                .sorted(new Comparator<Entry<Long, URI>>() {
                    @Override
                    public int compare(Entry<Long, URI> o1, Entry<Long, URI> o2) {
                        return o1.getKey().compareTo(o2.getKey());
                    }
                })
                .map(entry -> entry.getValue())
                .collect(Collectors.toList());
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (!file.toString().endsWith(config.getRdfExtension())) {
                return CONTINUE;
            }

            if (file.toString().endsWith("fcr%3Aversions" + config.getRdfExtension())) {
                return CONTINUE;
            }

            File rdfFile = file.toFile();
            Model model = parseStream(new FileInputStream(rdfFile), config);

            final URI resourceUri;
            final ResIterator binaryResources = model.listResourcesWithProperty(RDF_TYPE, NON_RDF_SOURCE);
            if (binaryResources.hasNext()) {
                resourceUri = URI.create(binaryResources.next().getURI());
            } else {
                resourceUri = URITranslationUtil.uriForFile(rdfFile, config);
            }

            Resource resc = model.getResource(resourceUri.toString());
            Statement stmt = resc.getProperty(FcrepoConstants.LAST_MODIFIED_DATE);
            if (stmt == null) {
                createUriList.add(new SimpleEntry<>(0L, resourceUri));
            } else {
                final XSDDateTime created = (XSDDateTime) stmt.getLiteral().getValue();
                final long createdMillis = created.asCalendar().getTimeInMillis();
                createUriList.add(new SimpleEntry<>(createdMillis, resourceUri));
            }

            return CONTINUE;
        }
    }
}
