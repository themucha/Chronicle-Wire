/*
 *
 *  *     Copyright (C) 2016  higherfrequencytrading.com
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU Lesser General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU Lesser General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU Lesser General Public License
 *  *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static net.openhft.chronicle.wire.WireType.TEXT;
import static org.junit.Assert.assertEquals;

/**
 * Created by peter on 17/03/16.
 */
public class ReadmePojoTest {
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(MyPojos.class);
    }

    @Test
    public void testFromString() throws IOException {
        MyPojos mps = new MyPojos("test-list");
        mps.myPojos.add(new MyPojo("text1", 1, 1.1));
        mps.myPojos.add(new MyPojo("text2", 2, 2.2));

        System.out.println(mps);
        MyPojos mps2 = Marshallable.fromString(mps.toString());
        assertEquals(mps, mps2);

        String text = "!MyPojos {\n" +
                "  name: test-list,\n" +
                "  myPojos: [\n" +
                "    { text: text1, num: 1, factor: 1.1 },\n" +
                "    { text: text2, num: 2, factor: 2.2 }\n" +
                "  ]\n" +
                "}\n";
        MyPojos mps3 = Marshallable.fromString(text);
        assertEquals(mps, mps3);

        MyPojos mps4 = Marshallable.fromFile("my-pojos.yaml");
        assertEquals(mps, mps4);
    }

    @Test
    public void testMapDump() throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("text", "words");
        map.put("number", 1);
        map.put("factor", 1.1);
        map.put("list", Arrays.asList(1L, 2L, 3L, 4L));

        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("a", 1L);
        inner.put("b", "Hello World");
        inner.put("c", "bye");
        map.put("inner", inner);

        String text = TEXT.asString(map);
        assertEquals("text: words\n" +
                "number: !int 1\n" +
                "factor: 1.1\n" +
                "list: [\n" +
                "  1,\n" +
                "  2,\n" +
                "  3,\n" +
                "  4\n" +
                "]\n" +
                "inner: {\n" +
                "  a: 1,\n" +
                "  b: Hello World,\n" +
                "  c: bye\n" +
                "}\n", text);
        Map<String, Object> map2 = TEXT.asMap(text);
        assertEquals(map, map2);
    }

    static class MyPojo extends AbstractMarshallable {
        String text;
        int num;
        double factor;

        public MyPojo(String text, int num, double factor) {
            this.text = text;
            this.num = num;
            this.factor = factor;
        }
    }

    static class MyPojos extends AbstractMarshallable {
        String name;
        List<MyPojo> myPojos = new ArrayList<>();

        public MyPojos(String name) {
            this.name = name;
        }
    }
}
