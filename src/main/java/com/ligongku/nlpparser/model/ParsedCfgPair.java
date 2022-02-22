package com.ligongku.nlpparser.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Pack:       com.ligongku.nlpparser.model
 * File:       ParsedCfgPair
 * Desc:
 * User:       chuangfengwang
 * CreateTime: 2022-02-22 15:42
 */
@Data
public class ParsedCfgPair {
    private Long id;
    private Long sentId;
    private String deprel;
    private String leftCpostag;
    private String rightCpostag;
    private String leftWord;
    private String rightWord;
    private String leftPostag;
    private String rightPostag;
    private Integer leftWordNo;
    private Integer rightWordNo;
    private LocalDateTime createAt;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":")
                .append(id);
        sb.append(",\"sentId\":")
                .append(sentId);
        sb.append(",\"deprel\":\"")
                .append(deprel).append('\"');
        sb.append(",\"leftCpostag\":\"")
                .append(leftCpostag).append('\"');
        sb.append(",\"rightCpostag\":\"")
                .append(rightCpostag).append('\"');
        sb.append(",\"leftWord\":\"")
                .append(leftWord).append('\"');
        sb.append(",\"rightWord\":\"")
                .append(rightWord).append('\"');
        sb.append(",\"leftPostag\":\"")
                .append(leftPostag).append('\"');
        sb.append(",\"rightPostag\":\"")
                .append(rightPostag).append('\"');
        sb.append(",\"leftWordNo\":")
                .append(leftWordNo);
        sb.append(",\"rightWordNo\":")
                .append(rightWordNo);
        sb.append(",\"createAt\":")
                .append(createAt);
        sb.append('}');
        return sb.toString();
    }
}
