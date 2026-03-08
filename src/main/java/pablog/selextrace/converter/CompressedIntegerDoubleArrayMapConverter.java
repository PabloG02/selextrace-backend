package pablog.selextrace.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

// TODO: Explore alternatives for language-independent, compact binary formats (e.g. Protocol Buffers, FlatBuffers, CBOR, MessagePack, ...).
@Converter
public class CompressedIntegerDoubleArrayMapConverter implements AttributeConverter<Map<Integer, double[]>, byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(Map<Integer, double[]> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return new byte[0];
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzipOut = new GZIPOutputStream(baos);
             ObjectOutputStream oos = new ObjectOutputStream(gzipOut)) {

            oos.writeObject(attribute);
            oos.flush();
            gzipOut.finish();

            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Error compressing profiles data", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Integer, double[]> convertToEntityAttribute(byte[] dbData) {
        if (dbData == null || dbData.length == 0) {
            return new HashMap<>();
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(dbData);
             GZIPInputStream gzipIn = new GZIPInputStream(bais);
             ObjectInputStream ois = new ObjectInputStream(gzipIn)) {

            return (Map<Integer, double[]>) ois.readObject();

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Error decompressing profiles data", e);
        }
    }
}
