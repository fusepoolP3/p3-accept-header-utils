/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.fusepool.p3.accept.util;

import java.util.*;


import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import javax.activation.MimeType;
import javax.servlet.http.HttpServletRequest;

import static eu.fusepool.p3.accept.util.MimeUtils.mimeType;

public class AcceptPreferenceTest {

    @Test
    public void gettingQualityWithWildCard() {
        List<String> entryStrings = new ArrayList<String>();
        entryStrings.add("*/*;q=.1");
        entryStrings.add("image/png;q=1");
        entryStrings.add("image/jpeg");
        entryStrings.add("image/*;q=.3");
        entryStrings.add("text/*;q=.3");

        AcceptPreference acceptHeader = new AcceptPreference(entryStrings);

        Assert.assertEquals(1000, acceptHeader.getAcceptedQuality(mimeType("image/jpeg")));
        Assert.assertEquals(1000, acceptHeader.getAcceptedQuality(mimeType("image/png")));
        Assert.assertEquals(300, acceptHeader.getAcceptedQuality(mimeType("image/x-foo")));
        Assert.assertEquals(300, acceptHeader.getAcceptedQuality(mimeType("text/plain")));
        Assert.assertEquals(100, acceptHeader.getAcceptedQuality(mimeType("application/pdf")));
    }

    @Test
    public void gettingQualityWithoutWildCard() {
        List<String> entryStrings = new ArrayList<String>();
        entryStrings.add("image/png;q=1");
        entryStrings.add("image/jpeg");
        entryStrings.add("image/*;q=.3");
        entryStrings.add("text/*;q=.3");

        AcceptPreference acceptHeader = new AcceptPreference(entryStrings);
        Assert.assertEquals(1000, acceptHeader.getAcceptedQuality(mimeType("image/jpeg")));
        Assert.assertEquals(1000, acceptHeader.getAcceptedQuality(mimeType("image/png")));
        Assert.assertEquals(300, acceptHeader.getAcceptedQuality(mimeType("image/x-foo")));
        Assert.assertEquals(300, acceptHeader.getAcceptedQuality(mimeType("text/plain")));
        Assert.assertEquals(0, acceptHeader.getAcceptedQuality(mimeType("application/pdf")));
    }
    
    
    /** 
     * Java by default sends: text/html, image/gif, image/jpeg, star; q=.2, star/star; q=.2
     */
    @Test
    public void dealingWithJavaDeault() {
        AcceptPreference acceptHeader = AcceptPreference.fromString("text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2");
        Assert.assertEquals(1000, acceptHeader.getAcceptedQuality(mimeType("image/jpeg")));
        Assert.assertEquals(1000, acceptHeader.getAcceptedQuality(mimeType("image/gif")));
        Assert.assertEquals(200, acceptHeader.getAcceptedQuality(mimeType("image/x-foo")));
        Assert.assertEquals(200, acceptHeader.getAcceptedQuality(mimeType("text/plain")));
        Assert.assertEquals(200, acceptHeader.getAcceptedQuality(mimeType("application/pdf")));
    }
    

    @Test
    public void getPreferredAcceptIsIdempotent() {
        List<String> entryStrings = new ArrayList<String>();
        entryStrings.add("image/png;q=.3");
        entryStrings.add("text/csv;q=.2");

        AcceptPreference acceptHeader = new AcceptPreference(entryStrings);
        Assert.assertEquals("image/png", acceptHeader.getPreferredAccept().toString());
        Assert.assertEquals("image/png", acceptHeader.getPreferredAccept().toString());
    }

    @Test
    public void gettingHighestQuality() {
        List<String> entryStrings = new ArrayList<String>();

        entryStrings.add("image/png;q=1");
        entryStrings.add("image/*;q=.3");
        entryStrings.add("text/*;q=.3");

        AcceptPreference acceptHeader = new AcceptPreference(entryStrings);
        Assert.assertEquals("image/png", acceptHeader.getPreferredAccept().getBaseType());
    }

    @Test
    public void gettingBestFromSupported() {
        List<String> entryStrings = new ArrayList<String>();

        entryStrings.add("image/png;q=1");
        entryStrings.add("image/jpeg");
        entryStrings.add("image/*;q=.3");
        entryStrings.add("text/*;q=.3");

        Set<MimeType> supported = new HashSet<MimeType>() {{
            add(mimeType("application/json"));
            add(mimeType("application/octetstream"));
            add(mimeType("image/tif"));
        }};

        AcceptPreference acceptHeader = new AcceptPreference(entryStrings);
        Assert.assertEquals("image/tif", acceptHeader.getPreferredAccept(supported).getBaseType());

        supported.add(mimeType("image/png"));
        Assert.assertEquals("image/png", acceptHeader.getPreferredAccept(supported).getBaseType());

        supported.clear();
        supported.add(mimeType("text/hmtl"));
        Assert.assertEquals("text/hmtl", acceptHeader.getPreferredAccept(supported).getBaseType());
    }

    @Test
    public void gettingBestFromSupported2() {
        List<String> entryStrings = new ArrayList<String>();

        entryStrings.add("image/*;q=0.1");
        entryStrings.add("text/*;q=.01");
        entryStrings.add("application/rdf+xml;q=.3");

        Set<MimeType> supported = new HashSet<MimeType>() {{
            add(mimeType("text/csv"));
            add(mimeType("text/turtle"));
            add(mimeType("application/rdf+xml"));
        }};

        AcceptPreference acceptHeader = new AcceptPreference(entryStrings);
        Assert.assertEquals("application/rdf+xml", acceptHeader.getPreferredAccept(supported).getBaseType());
    }

    @Test
    public void mergingMultipleHeaders() {
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);

        EasyMock.expect(request.getHeaders(AcceptPreference.RFC7231_HEADER))
                .andReturn(new Vector<String>() {{
                    add("text/*;q=0.5,image/*;q=0.3");
                    add("text/html;q=1,image/png;q=0.4");
                    add("*/*;q=0.1");
                }}.elements())
                .atLeastOnce();

        EasyMock.replay(request);

        AcceptPreference acceptPreference = AcceptPreference.fromRequest(request);

        assertEntriesMatch(acceptPreference, "text/*", "image/*", "text/html", "image/png","*/*");
        
        Set<MimeType> supported = new HashSet<MimeType>() {{
            add(mimeType("application/rdf+xml"));
        }};

        Assert.assertEquals("application/rdf+xml", acceptPreference.getPreferredAccept(supported).getBaseType());

        supported.add(mimeType("image/gif"));
        Assert.assertEquals("image/gif", acceptPreference.getPreferredAccept(supported).getBaseType());

        supported.add(mimeType("text/csv"));
        Assert.assertEquals("text/csv", acceptPreference.getPreferredAccept(supported).getBaseType());

        supported.add(mimeType("text/html"));
        Assert.assertEquals("text/html", acceptPreference.getPreferredAccept(supported).getBaseType());
    }

    private void assertEntriesMatch(AcceptPreference header, String... types) {
        List<AcceptPreference.AcceptHeaderEntry> entries = header.getEntries();
        Set<String> typeSet = new HashSet<String>(Arrays.asList(types));

        for(AcceptPreference.AcceptHeaderEntry entry : entries) {
            Assert.assertTrue(typeSet.remove(entry.mediaType.getBaseType()));
        }

        Assert.assertEquals(0, typeSet.size());
    }
}