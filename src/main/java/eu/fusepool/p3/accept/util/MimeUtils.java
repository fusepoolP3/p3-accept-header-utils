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

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

/**
 * {@link MimeUtils} contains utility methods and constants useful when
 * dealing with {@link javax.activation.MimeType}s.
 *
 * @author Giuliano Mega
 */
public class MimeUtils {

    public static final MimeType WILDCARD_TYPE = mimeType("*/*");

    public static final String MIME_TYPE_WILDCARD = "*";

    /**
     * Creates a new {@link javax.activation.MimeType} from a string, but issues a
     * {@link RuntimeException} instead of a checked {@link MimeTypeParseException}
     * in case the string cannot be parsed. Useful when creating constants for which
     * we are certain that no exception should ensue. E.g.:
     * <code>
     *     public class MyClass {
     *         public final MimeType MY_MIME = mimeType("application/my-mime");
     *
     *         ...
     *     }
     * </code>
     *
     * @param type a MIME type as a {@link String} (e.g. "text/html").
     *
     * @return the corresponding {@link MimeType}.
     *
     * @throws java.lang.RuntimeException if the {@link MimeType} constructor
     *         throws {@link MimeTypeParseException}.
     */
    public static MimeType mimeType(String type) {
        try {
            return new MimeType(type);
        } catch (MimeTypeParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @param t1
     * @param t2
     * @return true if t1 is the same or a subtype ot t2 such as when t1 is
     * text/plain and t2 is text/*
     */
    public static boolean isSameOrSubtype(MimeType t1, MimeType t2) {
        String type1 = t1.getPrimaryType();
        String subtype1 = t1.getSubType();
        String type2 = t2.getPrimaryType();
        String subtype2 = t2.getSubType();

        if (type2.equals(MimeUtils.MIME_TYPE_WILDCARD) && subtype2.equals(MimeUtils.MIME_TYPE_WILDCARD)) {
            return true;
        } else if (type1.equalsIgnoreCase(type2) && subtype2.equals(MimeUtils.MIME_TYPE_WILDCARD)) {
            return true;
        } else {
            return type1.equalsIgnoreCase(type2) && subtype1.equalsIgnoreCase(subtype2);
        }
    }
}
