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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.bytes.IORuntimeException;
import net.openhft.chronicle.bytes.StopCharTesters;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.pool.EnumInterner;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.IOException;

enum CcyPair {
    EURUSD, GBPUSD, EURCHF;

    static final EnumInterner<CcyPair> INTERNER = new EnumInterner<>(CcyPair.class);
}

/**
 * Created by peter.lawrey on 03/12/2015.
 */
public class CSVBytesMarshallableTest {
    Bytes bytes = Bytes.from(
            "1.09029,1.090305,EURUSD,2,1,EBS\n" +
                    "1.50935,1.50936,GBPUSD,5,1,RTRS\n" +
                    "1.0906,1.09065,EURCHF,3,1,EBS\n");

    // low level marshalling
    @Test
    public void bytesMarshallable() {
        Bytes bytes2 = Bytes.elasticByteBuffer();
        FXPrice fxPrice = new FXPrice();
        while (bytes.readRemaining() > 0) {
            fxPrice.readMarshallable(bytes);
            fxPrice.writeMarshallable(bytes2);
        }
        System.out.println(bytes2);
    }

    // wire marshalling.
    @Test
    public void marshallable() {
        doTest(WireType.JSON, false);
        doTest(WireType.TEXT, false);

        doTest(WireType.BINARY, true);
        doTest(WireType.FIELDLESS_BINARY, true);
        doTest(WireType.RAW, true);
    }

    private void doTest(WireType wt, boolean binary) {
        bytes.readPosition(0);
        CSVWire in = new CSVWire(bytes);

        Bytes bytes2 = Bytes.elasticByteBuffer();
        Wire out = wt.apply(bytes2);

        FXPrice2 fxPrice = new FXPrice2();
        while (bytes.readRemaining() > 0) {
            fxPrice.readMarshallable(in);
            fxPrice.writeMarshallable(out);
        }

        System.out.println();
        System.out.println(wt);
        System.out.println(binary ? bytes2.toHexString() : bytes2.toString());
    }

}

class FXPrice implements BytesMarshallable {
    public double bidprice;
    public double offerprice;
    //enum
    public CcyPair pair;
    public int size;
    public byte level;
    public String exchangeName;
    public transient double midPrice;

    @Override
    public void readMarshallable(Bytes<?> bytes) {
        bidprice = bytes.parseDouble();
        offerprice = bytes.parseDouble();
        pair = parseEnum(bytes, CcyPair.INTERNER);
        size = Maths.toInt32(bytes.parseLong());
        level = Maths.toInt8(bytes.parseLong());
        exchangeName = bytes.parseUtf8(StopCharTesters.COMMA_STOP);
        midPrice = (bidprice + offerprice) / 2;
    }

    @Override
    public void writeMarshallable(Bytes bytes) {
        try {
            bytes.append(bidprice).append(',');
            bytes.append(offerprice).append(',');
            bytes.append(pair.name()).append(',');
            bytes.append(size).append(',');
            bytes.append(exchangeName).append('\n');
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    private <T extends Enum<T>> T parseEnum(Bytes<?> bytes, EnumInterner<T> interner) {
        StringBuilder sb = Wires.acquireStringBuilder();
        bytes.parseUtf8(sb, StopCharTesters.COMMA_STOP);
        return interner.intern(sb);
    }
}

class FXPrice2 implements Marshallable {
    public double bidprice;
    public double offerprice;
    //enum
    public CcyPair pair;
    public int size;
    public byte level;
    public String exchangeName;
    public transient double midPrice;

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        wire.read(() -> "bidprice").float64(this, (t, v) -> t.bidprice = v)
                .read(() -> "offerprice").float64(this, (t, v) -> t.offerprice = v)
                .read(() -> "pair").asEnum(CcyPair.class, this, (t, v) -> t.pair = v)
                .read(() -> "size").int32(this, (t, v) -> t.size = v)
                .read(() -> "level").int8(this, (t, v) -> t.level = v)
                .read(() -> "exchangeName").text(this, (t, v) -> t.exchangeName = v);
    }

    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        wire.write(() -> "bidprice").float64(bidprice)
                .write(() -> "offerprice").float64(offerprice)
                .write(() -> "pair").asEnum(pair)
                .write(() -> "size").int32(size)
                .write(() -> "level").int8(level)
                .write(() -> "exchangeName").text(exchangeName);
    }
}
