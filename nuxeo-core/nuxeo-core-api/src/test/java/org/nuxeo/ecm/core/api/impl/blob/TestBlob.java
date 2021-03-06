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
 * $Id: TestBlob.java 28924 2008-01-10 14:04:05Z sfermigier $
 */

package org.nuxeo.ecm.core.api.impl.blob;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:sf@nuxeo.com">Stefane Fermigier</a>
 */
public class TestBlob extends NXRuntimeTestCase {

    private URL url;

    private int length;

    private byte[] blobContent;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        url = Thread.currentThread().getContextClassLoader().getResource("test.blob");
        File file = new File(url.toURI());
        length = (int) file.length();
        blobContent = new byte[length];
        try (InputStream is = new FileInputStream(file)) {
            int bytesRead = is.read(blobContent);
            assertTrue(bytesRead > 0);
        }
    }

    @After
    public void tearDown() throws Exception {
        blobContent = null;
        super.tearDown();
    }

    private void checkFileBlob(Blob blob) throws Exception {
        assertNull(blob.getMimeType());
        assertNull(blob.getEncoding());
        assertEquals(length, blob.getLength());

        String s1 = blob.getString();
        String s2 = blob.getString();
        assertEquals(s1, s2);
        s1 = null;
        s2 = null;

        byte[] blobContent2 = blob.getByteArray();
        assertEquals(blobContent.length, blobContent2.length);
        assertTrue(Arrays.equals(blobContent, blobContent2));
    }

    @Test
    public void testFileBlobFromFile() throws Exception {
        Blob blob = Blobs.createBlob(new File(url.toURI()));
        checkFileBlob(blob);
    }

    @Test
    public void testFileBlobFromStream() throws Exception {
        try (InputStream in = new FileInputStream(new File(url.toURI()))) {
            Blob blob = Blobs.createBlob(in);
            checkFileBlob(blob);
        }
    }

    @Test
    public void testStreamingFromString() throws Exception {
        String nonAsciiString = "String with non ASCII chars: \u00e9";
        Blob blob = new StringBlob(nonAsciiString);
        assertEquals(nonAsciiString, blob.getString());
        assertEquals(blob.getByteArray().length, blob.getLength());
        assertArrayEquals(nonAsciiString.getBytes("utf-8"), blob.getByteArray());
    }

    @Test
    public void testURLBlob() throws Exception {
        Blob blob = new URLBlob(url);
        assertNull(blob.getMimeType());
        assertNull(blob.getEncoding());
        assertEquals(-1, blob.getLength());

        String s1 = blob.getString();
        String s2 = blob.getString();
        assertEquals(s1, s2);
        s1 = null;
        s2 = null;

        byte[] blobContent2 = blob.getByteArray();
        assertEquals(blobContent.length, blobContent2.length);
        assertTrue(Arrays.equals(blobContent, blobContent2));
    }

    @Test
    public void testStringBlob() throws Exception {
        // Use random string for this test.
        StringBuilder buff = new StringBuilder();
        Random rand = new Random();
        for (int i = 0; i < 1000000; i++) {
            buff.append((char) rand.nextInt());
        }
        String s = buff.toString();

        Blob blob = new StringBlob(s);

        assertEquals(blob.getMimeType(), "text/plain");
        assertEquals("UTF-8", blob.getEncoding());
        assertEquals(1000000, blob.getString().length());

        String s1 = blob.getString();
        assertEquals(s, s1);

        String s2 = blob.getString();
        assertEquals(s1, s2);
        s1 = null;
        s2 = null;

        byte[] blobContent2 = blob.getByteArray();
        byte[] blobContent3 = blob.getByteArray();
        assertTrue(Arrays.equals(blobContent3, blobContent2));
    }

    @Test
    public void testStringBlobLength() throws Exception {
        Blob blob = new StringBlob("\u00e9");
        assertEquals("UTF-8", blob.getEncoding());
        assertEquals(2, blob.getLength());
    }

    @Test
    public void testByteArrayBlob() throws Exception {
        Blob blob = new ByteArrayBlob(blobContent);

        assertNull(blob.getMimeType());
        assertNull(blob.getEncoding());
        assertEquals(length, blob.getLength());

        String s1 = blob.getString();
        String s2 = blob.getString();
        assertEquals(s1, s2);
        s1 = null;
        s2 = null;

        byte[] blobContent2 = blob.getByteArray();
        assertEquals(blobContent.length, blobContent2.length);
        assertTrue(Arrays.equals(blobContent, blobContent2));
    }

}
