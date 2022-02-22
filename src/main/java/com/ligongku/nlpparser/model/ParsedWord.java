package com.ligongku.nlpparser.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ParsedWord {
    private Long id;
    private Long sentId;
    private String word;
    private Integer wordId;
    private String lemma;
    private String cpostag;
    private String postag;
    private Integer headNo;
    private String deprel;
    private String conllwordName;
    private LocalDateTime createAt;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":")
                .append(id);
        sb.append(",\"sentId\":")
                .append(sentId);
        sb.append(",\"word\":\"")
                .append(word).append('\"');
        sb.append(",\"wordId\":")
                .append(wordId);
        sb.append(",\"lemma\":\"")
                .append(lemma).append('\"');
        sb.append(",\"cpostag\":\"")
                .append(cpostag).append('\"');
        sb.append(",\"postag\":\"")
                .append(postag).append('\"');
        sb.append(",\"headNo\":")
                .append(headNo);
        sb.append(",\"deprel\":\"")
                .append(deprel).append('\"');
        sb.append(",\"conllwordName\":\"")
                .append(conllwordName).append('\"');
        sb.append(",\"createAt\":")
                .append(createAt);
        sb.append('}');
        return sb.toString();
    }
}
