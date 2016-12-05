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
package org.fcrepo.importexport.integration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.jena.rdf.model.Model;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoHttpClientBuilder;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.junit.Before;
import org.slf4j.Logger;

import static org.apache.jena.rdf.model.ResourceFactory.createPlainLiteral;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.riot.RDFDataMgr.loadModel;
import static org.apache.jena.riot.web.HttpOp.setDefaultHttpClient;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author awoods
 * @since 2016-09-01
 */
abstract class AbstractResourceIT {

    private Logger logger = logger();

    private static final int SERVER_PORT = Integer.parseInt(System.getProperty("fcrepo.dynamic.test.port", "8080"));

    private static final String HOSTNAME = "localhost";

    static final String DC_TITLE = "http://purl.org/dc/elements/1.1/title";

    static final String USERNAME = "fedoraAdmin";

    static final String PASSWORD = "password";

    static final String serverAddress = "http://" + HOSTNAME + ":" + SERVER_PORT + "/fcrepo/rest/";

    static final String TARGET_DIR = System.getProperty("project.build.directory");

    static FcrepoClient.FcrepoClientBuilder clientBuilder;

    AbstractResourceIT() {
        clientBuilder = FcrepoClient.client().credentials(USERNAME, PASSWORD).authScope("localhost");
        setDefaultHttpClient(new FcrepoHttpClientBuilder(USERNAME, PASSWORD, "localhost").build());

        final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(Integer.MAX_VALUE);
        connectionManager.setDefaultMaxPerRoute(20);
        connectionManager.closeIdleConnections(3, TimeUnit.SECONDS);
    }

    @Before
    public void before() {
        assertNotNull(TARGET_DIR);
        assertTrue(new File(TARGET_DIR).exists());
    }

    protected FcrepoResponse create(final URI uri) throws FcrepoOperationFailedException {
        logger.debug("Request ------: {}", uri);
        return clientBuilder.build().put(uri).perform();
    }

    protected InputStream insertTitle(final String title) {
        try {
            return new ByteArrayInputStream(("INSERT DATA { <> <" + DC_TITLE + "> '" + title + "' . }")
                    .getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // can't actually happen
            throw new RuntimeException(e);
        }
    }

    protected boolean exists(final URI uri) throws FcrepoOperationFailedException {
        final FcrepoResponse resp = clientBuilder.build().head(uri).disableRedirects().perform();
        return resp.getStatusCode() == 200 || resp.getStatusCode() == 307;
    }

    protected Model getAsModel(final URI uri) throws FcrepoOperationFailedException {
        return loadModel(uri.toString());
    }
    protected void assertHasTitle(final URI uri, final String title) throws FcrepoOperationFailedException {
        final Model model = getAsModel(uri);
        assertTrue(uri + " should have had the dc:title, \"" + title + "\"!",
                model.contains(createResource(uri.toString()), createProperty(DC_TITLE), createPlainLiteral(title)));
    }

    abstract protected Logger logger();
}
