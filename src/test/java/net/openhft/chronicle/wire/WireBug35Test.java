package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

/**
 * @author ryanlea
 */
public class WireBug35Test {

    @Test
    public void objectsInSequence() {
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

        final Wire wire = WireType.TEXT.apply(bytes);
        wire.write(() -> "seq").sequence(seq -> {
            seq.marshallable(obj -> obj.write(() -> "key").text("value"));
            seq.marshallable(obj -> obj.write(() -> "key").text("value"));
        });

        final String text = wire.asText().toString();
        Object load = new Yaml().load(text);

        assertEquals("{seq=[{key=value}, {key=value}]}", load.toString());
    }
}