package com.ligongku.nlpparser.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ParsedSent {
    private Long id;
    private String fileName;
    private String sent;
    private Integer lineNo;
    private Integer fileOffset;
    private String parserName;
    private LocalDateTime createAt;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":")
                .append(id);
        sb.append(",\"fileName\":\"")
                .append(fileName).append('\"');
        sb.append(",\"sent\":\"")
                .append(sent).append('\"');
        sb.append(",\"lineNo\":")
                .append(lineNo);
        sb.append(",\"fileOffset\":")
                .append(fileOffset);
        sb.append(",\"parserName\":\"")
                .append(parserName).append('\"');
        sb.append(",\"createAt\":")
                .append(createAt);
        sb.append('}');
        return sb.toString();
    }
}
