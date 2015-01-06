/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package eu.fusepool.p3.accept.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;

import javax.activation.MimeType;

public class MimeComparatorTest {

    @Test
    public void testInSet() {
        SortedSet<MimeType> set = new TreeSet<MimeType>(
                new MimeTypeComparator());
        Map<String, String> attrib = new HashMap<String, String>();
        attrib.put("q", ".4");

        MimeType type1 = MimeUtils.mimeType("image/jpeg;q=.4");
        MimeType type2 = MimeUtils.mimeType("image/jpeg");
        MimeType type3 = MimeUtils.mimeType("image/*");

        set.add(type1);
        set.add(type2);
        set.add(type3);

        Iterator<MimeType> iter = set.iterator();

        Assert.assertEquals(type2, iter.next());
        Assert.assertEquals(type1, iter.next());
        Assert.assertEquals(type3, iter.next());
    }

}
