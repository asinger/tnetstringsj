/* Copyright 2011 Armando Singer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tnetstrings;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.tnetstrings.TNetstring.dump;
import static org.tnetstrings.TNetstring.parse;
import static org.tnetstrings.TNetstring.parseSize;
import static org.tnetstrings.TNetstring.parseWithBytesAsString;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.Test;

public class TNetstringTest {

  Charset ASCII = Charset.forName("US-ASCII");

  @Test public void simpleParse() {
    final byte[] fooTNetstring = "3:foo,".getBytes(ASCII);

    assertArrayEquals("foo".getBytes(ASCII),
      (byte[]) parse(fooTNetstring));

    assertEquals("foo", parseWithBytesAsString(fooTNetstring, ASCII));

    final byte[] tnetnull = "0:~".getBytes(ASCII);
    assertEquals(null, parse(tnetnull));

    final byte[] tnettrue = "4:true!".getBytes(ASCII);
    assertEquals(true, parse(tnettrue));

    final byte[] tnetfalse = "5:false!".getBytes(ASCII);
    assertEquals(false, parse(tnetfalse));

    final byte[] tnetnum = "5:12345#".getBytes(ASCII);
    assertEquals(12345L, parse(tnetnum));

    assertEquals(Collections.emptyList(), parse("0:]".getBytes(ASCII)));
    assertEquals(Collections.emptyMap(), parse("0:}".getBytes(ASCII)));
    assertArrayEquals(new byte[0], (byte[]) parse("0:,".getBytes(ASCII)));
  }

  @SuppressWarnings("unchecked")
  @Test public void complexTypeParse() {
    final byte[] mapBytesLong = "16:5:hello,5:12345#}".getBytes(ASCII);
    final byte[] hello = "hello".getBytes(ASCII);

    assertArrayEquals(hello, ((Map<byte[], Long>) parse(mapBytesLong)).keySet().iterator().next());
    assertEquals(Long.valueOf(12345L), ((Map<byte[], Long>) parse(mapBytesLong)).values().iterator().next());

    final byte[] complex = "32:5:hello,5:12345#5:hello,5:56789#]".getBytes(ASCII);
    final ArrayList<Object> list = new ArrayList<Object>();
    list.add(hello);
    list.add(12345L);
    list.add(hello);
    list.add(56789L);

    final List<?> parsedComplex = parse(complex);
    assertArrayEquals((byte[]) list.get(0), (byte[]) parsedComplex.get(0));
    assertEquals(Long.valueOf(12345L), parsedComplex.get(1));
    assertArrayEquals((byte[]) list.get(0), (byte[]) parsedComplex.get(2));
    assertEquals(Long.valueOf(56789L), parsedComplex.get(3));
    assertEquals(4, parsedComplex.size());

    final byte[] twoTnetstrings = "5:hello,5:12345#".getBytes(ASCII);
    assertArrayEquals(hello, (byte[]) parse(twoTnetstrings));
    assertArrayEquals(hello, (byte[]) parse(twoTnetstrings, 0));
    assertEquals(Long.valueOf(12345L), parse(twoTnetstrings, 8)); // offset to 2nd
  }

  @Test public void dumpAndParse() {
    assertEquals(12345L, parse(dump(12345L)));
    assertEquals(Long.valueOf(12345L), parse(dump(Long.valueOf(12345L))));

    assertEquals(true, parse(dump(true)));
    assertEquals(false, parse(dump(false)));

    final ArrayList<Long> longs = new ArrayList<Long>();
    assertEquals(longs, parse(dump(longs)));

    longs.add(12345L);
    longs.add(45678L);
    assertEquals(longs, parse(dump(longs)));

    assertArrayEquals("foo".getBytes(ASCII), (byte[]) parse(dump("foo", ASCII)));

    final Map<String, String> stringMap = new LinkedHashMap<String, String>(5);
    stringMap.put("key111", "valxyz");
    stringMap.put("keyTwo", "valTwo");
    stringMap.put("keyThree", "valThree");
    stringMap.put("key4", "val4");
    stringMap.put("key5", "val5");
    assertEquals(stringMap, parseWithBytesAsString((dump(stringMap, ASCII)), ASCII));

    final ArrayList<String> strings = new ArrayList<String>();
    assertEquals(strings, parse(dump(strings)));

    strings.add("foo");
    strings.add("bar");
    strings.add("baz");
    assertEquals(strings, parseWithBytesAsString((dump(strings, ASCII)), ASCII));

    final byte[] dict = "127:6:METHOD,4:HEAD,3:URI,17:/virt/tst_11/r503,7:VERSION,8:HTTP/1.1,4:PATH,17:/virt/tst_11/r503,4:host,0:,15:accept-encoding,3:foo,}".getBytes(ASCII);

    assertArrayEquals(dict, dump(parseWithBytesAsString(dict, ASCII), ASCII));
    // empty body tacked on
    final byte[] dict2 = "127:6:METHOD,4:HEAD,3:URI,17:/virt/tst_11/r503,7:VERSION,8:HTTP/1.1,4:PATH,17:/virt/tst_11/r503,4:host,0:,15:accept-encoding,3:foo,}0:,".getBytes(ASCII);
    assertArrayEquals(dict, dump(parseWithBytesAsString(dict2, ASCII), ASCII));

    final byte[] fromOffset24 = "GET 0 /virt/tst_11/r503 127:6:METHOD,4:HEAD,3:URI,17:/virt/tst_11/r503,7:VERSION,8:HTTP/1.1,4:PATH,17:/virt/tst_11/r503,4:host,0:,15:accept-encoding,3:foo,}0:,".getBytes(ASCII);

    assertArrayEquals(dict, dump(parseWithBytesAsString(fromOffset24, 24, ASCII), ASCII));
  }
  
  @Test public void testParseSize() {
    assertEquals(0, parseSize("0".getBytes(ASCII)));
    assertEquals(1, parseSize("1".getBytes(ASCII)));
    assertEquals(100, parseSize("100".getBytes(ASCII)));
    assertEquals(999999999, parseSize("999999999".getBytes(ASCII)));
    assertEquals(123456789, parseSize("123456789".getBytes(ASCII)));

    assertEquals(56789, parseSize("123456789".getBytes(ASCII), 4, 9));
    assertEquals(1234, parseSize("123456789".getBytes(ASCII), 0, 4));
    assertEquals(123456789, parseSize("123456789".getBytes(ASCII), 0, "123456789".length()));
    assertEquals(3456, parseSize("123456789".getBytes(ASCII), 2, 6));
  }

  @Test public void testdigitFrom() {
    assertEquals(1, TNetstring.digitFrom((byte) 49, null, 0, 0));
    assertEquals(2, TNetstring.digitFrom((byte) 50, null, 0, 0));
    assertEquals(3, TNetstring.digitFrom((byte) 51, null, 0, 0));
    assertEquals(4, TNetstring.digitFrom((byte) 52, null, 0, 0));
  }
  
  @Test public void testParseDouble() {
    final byte[] two = "2.0".getBytes(ASCII);
    final byte[] twoAgain = "02.0".getBytes(ASCII);
    final byte[] thirtyTwoNotOcatal26 = "032.0".getBytes(ASCII);
    final byte[] negThirtyTwoNotOcatal26 = "-032.0".getBytes(ASCII);

    assertEquals(2d, TNetstring.parseDouble(two, 0, two.length), 0d);

    assertEquals(2d, TNetstring.parseDouble(twoAgain, 0, twoAgain.length), 0d);
    assertEquals(32d, TNetstring.parseDouble(thirtyTwoNotOcatal26, 0, thirtyTwoNotOcatal26.length), 0d);
    assertEquals(-32d, TNetstring.parseDouble(negThirtyTwoNotOcatal26, 0, negThirtyTwoNotOcatal26.length), 0d);
    final StringBuilder max = new StringBuilder("17976931348623157");
    for (int i = 0; i < 292; i++) {
      max.append('0');
    }
    max.append(".0");
    final byte[] maxDouble = max.toString().getBytes(ASCII);
    assertEquals(Double.MAX_VALUE, TNetstring.parseDouble(maxDouble, 0, maxDouble.length), 0d);
  }

  @Test public void testDumpDouble() {
    assertArrayEquals("12:0.0000001001^".getBytes(ASCII), TNetstring.dump(0.0000001001000d));
    assertArrayEquals("12:0.0020001001^".getBytes(ASCII), TNetstring.dump(.0020001001000d));
    assertArrayEquals("19:10100000122222222.0^".getBytes(ASCII), TNetstring.dump(10100000122222222.0000001001000d));
    assertArrayEquals("16:10000.0000001001^".getBytes(ASCII), TNetstring.dump(10000.0000001001000d));
    
    Locale.setDefault(Locale.GERMANY); // We should get a '.' decimal separator in any locale, never ','
    assertArrayEquals("12:0.0000001001^".getBytes(ASCII), TNetstring.dump(0.0000001001000d));
    assertArrayEquals("12:0.0020001001^".getBytes(ASCII), TNetstring.dump(.0020001001000d));
    assertArrayEquals("19:10100000122222222.0^".getBytes(ASCII), TNetstring.dump(10100000122222222.0000001001000d));
    assertArrayEquals("16:10000.0000001001^".getBytes(ASCII), TNetstring.dump(10000.0000001001000d));

    assertArrayEquals("20:-10100000122222222.0^".getBytes(ASCII), TNetstring.dump(-10100000122222222.0000001001000d));
    assertArrayEquals("17:-10000.0000001001^".getBytes(ASCII), TNetstring.dump(-10000.0000001001000d));

    assertArrayEquals("3:0.0^".getBytes(ASCII), TNetstring.dump(0d));
    assertArrayEquals("4:-0.0^".getBytes(ASCII), TNetstring.dump(-0d));

    assertArrayEquals("311:179769313486231570000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000.0^".getBytes(ASCII),
      TNetstring.dump(Double.MAX_VALUE));
    assertArrayEquals("327:0.0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000049^".getBytes(ASCII),
      TNetstring.dump(Double.MIN_VALUE));
  }

  @Test public void testDumpArrays() {
    final float[] f = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f};
    assertArrayEquals("72:3:0.0^3:0.0^3:1.0^3:0.0^3:0.0^3:1.0^3:0.0^3:0.0^3:1.0^3:0.0^3:0.0^3:1.0^]".getBytes(ASCII),
      TNetstring.dump(f));

    final double[] d = {0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0};
    assertArrayEquals("72:3:0.0^3:0.0^3:1.0^3:0.0^3:0.0^3:1.0^3:0.0^3:0.0^3:1.0^3:0.0^3:0.0^3:1.0^]".getBytes(ASCII),
      TNetstring.dump(d));

    final double[] nullDoubleArray = null;
    assertArrayEquals("0:~".getBytes(ASCII), TNetstring.dump(nullDoubleArray));

    final boolean[] b = {true, true, false, true, false};
    assertArrayEquals("37:4:true!4:true!5:false!4:true!5:false!]".getBytes(ASCII),
      TNetstring.dump(b));

    final long[] l = {1L, 50000000000L, -12020202020202L};
    assertArrayEquals("38:1:1#11:50000000000#15:-12020202020202#]".getBytes(ASCII),
      TNetstring.dump(l));

    final int[] i = {1, 500000, -1202020, 0};
    assertArrayEquals("28:1:1#6:500000#8:-1202020#1:0#]".getBytes(ASCII),
      TNetstring.dump(i));

    final short[] s = {1, 5000, -12020, 0};
    assertArrayEquals("24:1:1#4:5000#6:-12020#1:0#]".getBytes(ASCII),
      TNetstring.dump(s));

    final char[] c = {'a', '0', 'b', 'Z'};
    assertArrayEquals("16:1:a,1:0,1:b,1:Z,]".getBytes(ASCII),
      TNetstring.dump(c, ASCII));

    final byte[] byteIntegers = {1, 50, -120, 0};
    assertArrayEquals("20:1:1#2:50#4:-120#1:0#]".getBytes(ASCII),
      TNetstring.dumpIntegers(byteIntegers));

    final Object floatsAsObject = f;
    assertArrayEquals("72:3:0.0^3:0.0^3:1.0^3:0.0^3:0.0^3:1.0^3:0.0^3:0.0^3:1.0^3:0.0^3:0.0^3:1.0^]".getBytes(ASCII),
      TNetstring.dump(floatsAsObject));

    final float[] nullFloats = null;
    assertArrayEquals("0:~".getBytes(ASCII), TNetstring.dump(nullFloats));
  }

}
