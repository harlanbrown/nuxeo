/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.core.convert.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.ecm.core.convert.api.ConverterNotAvailable;
import org.nuxeo.ecm.core.convert.api.ConverterNotRegistered;
import org.nuxeo.ecm.core.convert.extension.ChainedConverter;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.model.impl.DefaultRuntimeContext;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(ConvertFeature.class)
@Deploy("org.nuxeo.ecm.core.mimetype")
public class TestService {

    RuntimeContext rc;

    @Inject
    protected ConversionService cs;

    @Before
    public void init() {
        rc = new DefaultRuntimeContext(Framework.getRuntime());
    }

    @After
    public void undeploy() {
        rc.destroy();
        rc = null;
    }

    protected void deploy(String path) throws Exception {
        URL location = TestService.class.getResource("/".concat(path));
        rc.deploy(location);
    }

    @Test
    public void testServiceRegistration() {
        assertNotNull(cs);
    }

    @Test
    public void testServiceContrib() throws Exception {
        deploy("OSGI-INF/converters-test-contrib1.xml");
        assertNotNull(cs);

        Converter cv1 = ConversionServiceImpl.getConverter("dummy1");
        assertNotNull(cv1);

        ConverterDescriptor desc1 = ConversionServiceImpl.getConverterDescriptor("dummy1");
        assertNotNull(desc1);

        assertEquals("test/me", desc1.getDestinationMimeType());
        assertSame(2, desc1.getSourceMimeTypes().size());
        assertTrue(desc1.getSourceMimeTypes().contains("text/plain"));
        assertTrue(desc1.getSourceMimeTypes().contains("text/xml"));
    }

    @Test
    public void testConverterLookup() throws Exception {
        deploy("OSGI-INF/converters-test-contrib1.xml");

        String converterName = cs.getConverterName("text/plain", "test/me");
        assertEquals("dummy1", converterName);

        converterName = cs.getConverterName("text/plain2", "test/me");
        assertNull(converterName);

        deploy("OSGI-INF/converters-test-contrib2.xml");

        converterName = cs.getConverterName("test/me", "foo/bar");
        assertEquals("dummy2", converterName);

        converterName = cs.getConverterName("text/plain", "foo/bar");
        assertEquals("dummyChain", converterName);

        Converter cv = ConversionServiceImpl.getConverter("dummyChain");
        assertNotNull(cv);
        boolean isChain = false;
        if (cv instanceof ChainedConverter) {
            ChainedConverter ccv = (ChainedConverter) cv;
            List<String> steps = ccv.getSteps();
            assertNotNull(steps);
            assertSame(2, steps.size());
            assertTrue(steps.contains("test/me"));
            assertTrue(steps.contains("foo/bar"));
            isChain = true;

        }
        assertTrue(isChain);

        converterName = cs.getConverterName("something", "somethingelse");
        assertEquals("custom", converterName);

        converterName = cs.getConverterName("any", "somethingelse");
        assertEquals("wildcard", converterName);

        converterName = cs.getConverterName("text/plain", "jacky/chan");
        assertEquals("dummyChain2", converterName);
        Converter cv2 = ConversionServiceImpl.getConverter("dummyChain2");
        assertNotNull(cv2);
        isChain = false;
        if (cv2 instanceof ChainedConverter) {
            ChainedConverter ccv = (ChainedConverter) cv2;
            List<String> steps = ccv.getSteps();
            assertNull(steps);
            isChain = true;

        }
        assertTrue(isChain);
    }

    @Test
    public void testAvailability() throws Exception {
        deploy("OSGI-INF/converters-test-contrib1.xml");
        deploy("OSGI-INF/converters-test-contrib2.xml");
        deploy("OSGI-INF/converters-test-contrib4.xml");

        ConverterCheckResult result = null;

        // ** not existing converter
        // check registration check
        boolean notRegistred = false;

        try {
            result = cs.isConverterAvailable("toto");
        } catch (ConverterNotRegistered e) {
            notRegistred = true;
        }
        assertTrue(notRegistred);

        // check call
        notRegistred = false;
        try {
            cs.convert("toto", new SimpleBlobHolder(Blobs.createBlob("")), null);
        } catch (ConverterNotRegistered e) {
            notRegistred = true;
        }
        assertTrue(notRegistred);

        // not available converter
        notRegistred = false;
        try {
            result = cs.isConverterAvailable("NotAvailableConverter");
        } catch (ConverterNotRegistered e) {
            notRegistred = true;
        }
        assertFalse(notRegistred);
        assertFalse(result.isAvailable());
        assertNotNull(result.getErrorMessage());
        assertNotNull(result.getInstallationMessage());

        notRegistred = false;
        boolean notAvailable = false;
        try {
            cs.convert("NotAvailableConverter", new SimpleBlobHolder(Blobs.createBlob("")), null);
        } catch (ConverterNotRegistered e) {
            notRegistred = true;
        } catch (ConverterNotAvailable e) {
            notAvailable = true;
        }
        assertFalse(notRegistred);
        assertTrue(notAvailable);

        // ** available converter
        notRegistred = false;
        notAvailable = false;
        try {
            result = cs.isConverterAvailable("dummy2");
        } catch (ConverterNotRegistered e) {
            notRegistred = true;
        }
        assertFalse(notRegistred);
        assertTrue(result.isAvailable());
        assertNull(result.getErrorMessage());
        assertNull(result.getInstallationMessage());
        assertSame(2, result.getSupportedInputMimeTypes().size());

        notRegistred = false;
        try {
            cs.convert("dummy2", new SimpleBlobHolder(Blobs.createBlob("")), null);
        } catch (ConverterNotRegistered e) {
            notRegistred = true;
        } catch (ConverterNotAvailable e) {
            notAvailable = true;
        }
        assertFalse(notRegistred);
        assertFalse(notAvailable);
    }

    @Test
    public void testChainConverterAvailability() throws Exception {
        deploy("OSGI-INF/converters-test-contrib3.xml");
        deploy("OSGI-INF/converters-test-contrib4.xml");
        deploy("OSGI-INF/converters-test-contrib5.xml");

        ConverterCheckResult result = cs.isConverterAvailable("chainAvailable");
        assertNotNull(result);
        assertTrue(result.isAvailable());

        result = cs.isConverterAvailable("chainNotAvailable");
        assertNotNull(result);
        assertFalse(result.isAvailable());
        assertNotNull(result.getErrorMessage());
        assertNotNull(result.getInstallationMessage());
    }

    @Test
    public void testServiceConfig() throws Exception {
        deploy("OSGI-INF/convert-service-config-test.xml");
        assertNotNull(cs);

        assertEquals(12, ConversionServiceImpl.getGCIntervalInMinutes());
        assertEquals(132, ConversionServiceImpl.getMaxCacheSizeInKB());
        assertFalse(ConversionServiceImpl.isCacheEnabled());
    }

    @Test
    public void testSupportedSourceMimeType() throws Exception {
        deploy("OSGI-INF/converters-test-contrib1.xml");

        assertTrue(cs.isSourceMimeTypeSupported("dummy1", "text/plain"));
        assertTrue(cs.isSourceMimeTypeSupported("dummy1", "text/xml"));
        assertFalse(cs.isSourceMimeTypeSupported("dummy1", "application/pdf"));
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.convert:OSGI-INF/converters-test-pdf-contrib.xml")
    public void testUpdateMimeTypeAndFileName() {
        Map<String, Serializable> parameters = new HashMap<>();
        Blob blob = Blobs.createBlob("dummy text", "text/plain");
        BlobHolder result = cs.convert("dummyPdf", new SimpleBlobHolder(blob), parameters);
        Blob resultBlob = result.getBlob();
        assertNotNull(resultBlob);
        assertEquals("application/pdf", resultBlob.getMimeType());
        // cannot compute any filename
        assertNull(resultBlob.getFilename());

        blob.setFilename("dummy.txt");
        result = cs.convert("dummyPdf", new SimpleBlobHolder(blob), parameters);
        resultBlob = result.getBlob();
        assertNotNull(resultBlob);
        assertEquals("application/pdf", resultBlob.getMimeType());
        assertEquals("dummy.pdf", resultBlob.getFilename());

        parameters.put("setMimeType", false);
        result = cs.convert("dummyPdf", new SimpleBlobHolder(blob), parameters);
        resultBlob = result.getBlob();
        assertNotNull(resultBlob);
        assertEquals("application/pdf", resultBlob.getMimeType());
        assertEquals("dummy.pdf", resultBlob.getFilename());

        parameters = new HashMap<>();
        parameters.put("tempFilename", true);
        result = cs.convert("dummyPdf", new SimpleBlobHolder(blob), parameters);
        resultBlob = result.getBlob();
        assertNotNull(resultBlob);
        assertEquals("application/pdf", resultBlob.getMimeType());
        assertEquals("dummy.pdf", resultBlob.getFilename());
    }

}
