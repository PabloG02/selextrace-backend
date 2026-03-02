package pablog.selextrace.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import pablog.selextrace.motif.MotifAnalysisProfile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Converter
public class CompressedMotifAnalysisProfileListConverter implements AttributeConverter<List<MotifAnalysisProfile>, byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(List<MotifAnalysisProfile> attribute) {
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
            throw new RuntimeException("Error compressing motif analysis profiles", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MotifAnalysisProfile> convertToEntityAttribute(byte[] dbData) {
        if (dbData == null || dbData.length == 0) {
            return new ArrayList<>();
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(dbData);
             GZIPInputStream gzipIn = new GZIPInputStream(bais);
             ObjectInputStream ois = new ObjectInputStream(gzipIn)) {

            return (List<MotifAnalysisProfile>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Error decompressing motif analysis profiles", e);
        }
    }
}