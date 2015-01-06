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

import javax.activation.MimeType;
import javax.activation.MimeTypeParameterList;
import javax.activation.MimeTypeParseException;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static eu.fusepool.p3.accept.util.MimeUtils.isSameOrSubtype;
import static eu.fusepool.p3.accept.util.MimeUtils.mimeType;

/**
 * This class represents the media-type acceptance preference as expressed by
 * the values of HTTP Accept headers,
 * 
 * @author reto
 * @author Giuliano Mega
 */
public class AcceptPreference {

    private static final Logger logger = LoggerFactory.getLogger(AcceptPreference.class);

    public static final String RFC7231_HEADER = "Accept";

    public static final String RFC7231_MEDIA_SEPARATOR = ",";


    public static class AcceptHeaderEntry implements Comparable<AcceptHeaderEntry> {

        private final MimeTypeComparator mediaTypeComparator = new MimeTypeComparator();
        final MimeType mediaType;
        final int quality; //from 0 to 1000

        AcceptHeaderEntry(MimeType mediaType) {
            MimeTypeParameterList parametersWithoutQ = mediaType.getParameters();

            String qValue = parametersWithoutQ.get("q");
            parametersWithoutQ.remove("q");

            this.mediaType = mimeType(mediaType.getBaseType() + parametersWithoutQ.toString());

            if (qValue == null) {
                quality = 1000;
            } else {
                quality = (int) (Float.parseFloat(qValue) * 1000);
            }
        }

        @Override
        public int compareTo(AcceptHeaderEntry o) {
            if (equals(o)) {
                return 0;
            }
            if (quality == o.quality) {
                return mediaTypeComparator.compare(mediaType, o.mediaType);
            }
            return (o.quality - quality);
        }

        @Override
        public String toString() {
            return mediaType + " with q=" + quality + ";";
        }

    }

    /**
     * Constructs an {@link AcceptPreference} array from an {@link javax.servlet.http.HttpServletRequest}.
     *
     * @param request the request to extract the {@link AcceptPreference} from.
     * @return the {@link AcceptPreference}s reflecting all Accept-Headers in 
     *         the request, in case the request contains no header an 
     *         AcceptPreference quivalent to a single "*&#47;*" header value is returned.
     */
    public static AcceptPreference fromRequest(HttpServletRequest request) {
        ArrayList<AcceptPreference> headers = new ArrayList<AcceptPreference>();
        Enumeration<String> strHeaders = request.getHeaders(RFC7231_HEADER);
        while (strHeaders.hasMoreElements()) {
            headers.add(fromString(strHeaders.nextElement()));
        }
        if (headers.isEmpty()) {
            return fromString("*/*");
        } else {
            return fromHeaders(headers);
        }
    }

    /**
     * @return a new {@link AcceptPreference} from a RFC7231 media/quality list. Example:
     * <code>
     *      fromString("image/png;q=1.0,image/*;q=0.7,text/plain;q=0.5");
     * </code>
     */
    public static AcceptPreference fromString(String header) {
        if (header == null) {
            throw new NullPointerException("Header string can't be null.");
        }

        List<String> entries = new ArrayList<String>();
        for (String entry : header.split(RFC7231_MEDIA_SEPARATOR)) {
            entries.add(entry);
        }

        return new AcceptPreference(entries);
    }

    /**
     * Constructs an {@link AcceptPreference} that is equivalent to 
     * the union of the <code>AcceptPreference</code>s passed as argument.
     *
     * @param headers a collection of accept headers that are part of single request.
     *
     * @return an {@link AcceptPreference} that corresponds to the merge of all headers in
     * the parameter list. Example:
     *
     * <code>
     AcceptPreference merged = fromHeaders(fromString("text/html;q=1.0"),
                                       fromString("image/png;q=0.5"));
 </code>
     *
     * is equivalent to:
     *
     * <code>
     AcceptPreference merged = fromString("text/html;q=1.0,image/png;q=0.5");
 </code>
     */
    public static AcceptPreference fromHeaders(Collection<AcceptPreference> headers) {
        if (headers.isEmpty()) {
            throw new IllegalArgumentException("Header list must contain at least one element.");
        }

        TreeSet<AcceptHeaderEntry> entries = new TreeSet<>();
        for (AcceptPreference header : headers) {
            // It's OK to do this as AcceptHeaderEntry is immutable.
            entries.addAll(header.entries);
        }

        return new AcceptPreference(entries);
    }

    private final TreeSet<AcceptHeaderEntry> entries;

    protected AcceptPreference(List<String> entryStrings) {
        entries = new TreeSet<AcceptHeaderEntry>();
        if ((entryStrings == null) || (entryStrings.size() == 0)) {
            entries.add(new AcceptHeaderEntry(MimeUtils.WILDCARD_TYPE));
        } else {
            for (String string : entryStrings) {
                try {
                    entries.add(new AcceptHeaderEntry(new MimeType(string)));
                } catch (MimeTypeParseException ex) {
                    logger.warn("The string \"" + string + "\" is not a valid mediatype", ex);
                }
            }
        }
    }

    protected AcceptPreference(TreeSet<AcceptHeaderEntry> entries) {
        this.entries = entries;
    }

    /**
     * @return a sorted list of the {@link AcceptHeaderEntry} that compose this
     * {@link AcceptPreference}.
     */
    public List<AcceptHeaderEntry> getEntries() {
        List<AcceptHeaderEntry> result = new ArrayList<AcceptHeaderEntry>();
        for (AcceptHeaderEntry entry : entries) {
            result.add(entry);
        }
        return result;
    }

    /**
     * @return the {@link MimeType} with the highest quality parameter amongst the ones
     * specified in this {@link AcceptPreference}.
     */
    public MimeType getPreferredAccept() {
        return entries.pollFirst().mediaType;
    }

    /**
     * Given a set of supported {@link MimeType}s, returns the one that best
     * satisfies this accept header.
     *
     * @param supportedTypes a set of supported {@link MimeType}s.
     * @return the best candidate in the set, or <code>null</code> if the
     * header does not allow any of supported {@link MimeType}s.
     */
    public MimeType getPreferredAccept(Set<MimeType> supportedTypes) {
        // Starts from the highest.
        for (AcceptHeaderEntry clientSupported : entries) {
            for (MimeType serverSupported : supportedTypes) {
                if (isSameOrSubtype(serverSupported, clientSupported.mediaType)) {
                    return serverSupported;
                }
            }
        }

        return null;
    }

    /**
     * @param type
     * @return a value from 0 to 1000 to indicate the quality in which type is accepted
     */
    public int getAcceptedQuality(MimeType type) {
        for (AcceptHeaderEntry acceptHeaderEntry : entries) {
            if (isSameOrSubtype(type, acceptHeaderEntry.mediaType)) {
                return acceptHeaderEntry.quality;
            }
        }

        Object[] reverseEntries = entries.toArray();
        for (int i = entries.size() - 1; i >= 0; i--) {
            AcceptHeaderEntry entry = (AcceptHeaderEntry) reverseEntries[i];
            if (isSameOrSubtype(entry.mediaType, type)) {
                return entry.quality;
            }
        }

        return 0;
    }
    
    @Override
    public String toString() {
        return entries.toString();
    }

}