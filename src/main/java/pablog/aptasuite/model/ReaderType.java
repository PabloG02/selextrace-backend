package pablog.aptasuite.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ReaderType {
    FASTA,
    FASTQ;

    @JsonCreator
    public static ReaderType fromString(String key) {
        return key == null ? null : ReaderType.valueOf(key.toUpperCase());
    }
}
