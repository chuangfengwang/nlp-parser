package com.ligongku.nlpparser;


import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.CharSink;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLSentence;
import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLWord;

import com.hankcs.hanlp.dependency.nnparser.NeuralNetworkDependencyParser;
import com.hankcs.hanlp.utility.SentencesUtil;
import com.ligongku.nlpparser.dao.NlpParserSqliteDao;
import com.ligongku.nlpparser.model.ParsedCfgPair;
import com.ligongku.nlpparser.model.ParsedSent;
import com.ligongku.nlpparser.model.ParsedWord;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DependencyParser {

    private NlpParserSqliteDao nlpParserSqliteDao;

    private NeuralNetworkDependencyParser neuralNetworkDependencyParser = new NeuralNetworkDependencyParser();

    private List<Pattern> filterPatternList = Lists.newLinkedList();

    private CharSink sink;


    public void parseFile(String filePath) {
        File file = new File(filePath);
        try {
            Files.asCharSource(file, Charsets.UTF_8).readLines(new LineProcessor<Object>() {
                private int lineNum = 1;
                private int fileOffset = 0;

                @Override
                public boolean processLine(String line) throws IOException {
                    parseLine(line, fileOffset, lineNum, filePath);
                    if (lineNum % 100 == 0) {
                        System.out.println(String.format("finished file: %s , lineNum %d", filePath, lineNum));
                    }

                    fileOffset += line.length();
                    ++lineNum;
                    return true;
                }

                @Override
                public Object getResult() {
                    return lineNum;
                }
            });
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public String filterLine(String line) {
        for (Pattern pattern : filterPatternList) {
            Matcher matcher = pattern.matcher(line);
            line = matcher.replaceAll("");
        }
        return line;
    }

    public void parseLine(String line, int fileOffset, int lineNo, String fileName) {
        line = filterLine(line);
        if (Strings.isNullOrEmpty(line)) {
            return;
        }

        List<String> sentList = SentencesUtil.toSentenceList(line, false);
        LocalDateTime now = LocalDateTime.now();
        for (int idx = 0; idx < sentList.size(); ++idx) {
            String sent = sentList.get(idx);
            ParsedSent parsedSent = new ParsedSent();
            parsedSent.setFileName(fileName);
            parsedSent.setSent(sent);
            parsedSent.setFileOffset(fileOffset);
            parsedSent.setLineNo(lineNo);
            parsedSent.setParserName(neuralNetworkDependencyParser.getClass().getSimpleName());
            parsedSent.setCreateAt(now);

            Long sentId = nlpParserSqliteDao.insertSent(parsedSent);
            parsedSent.setId(sentId);

            // System.out.println("\n\n" + parsedSent);
            parseSentence(sent, sentId, fileName);

            fileOffset += sent.length();
        }
    }

    public void parseSentence(String sent, Long sentId, String fileName) {
        List<ParsedWord> wordList = Lists.newLinkedList();
        CoNLLSentence sentence = neuralNetworkDependencyParser.parse(sent);
        LocalDateTime now = LocalDateTime.now();
        for (CoNLLWord word : sentence) {
            ParsedWord parsedWord = new ParsedWord();
            parsedWord.setSentId(sentId);
            parsedWord.setWord(word.LEMMA);
            parsedWord.setWordId(word.ID);
            parsedWord.setLemma(word.LEMMA);
            parsedWord.setCpostag(word.CPOSTAG);
            parsedWord.setPostag(word.POSTAG);
            parsedWord.setHeadNo(word.HEAD.ID);
            parsedWord.setDeprel(word.DEPREL);
            parsedWord.setConllwordName(word.NAME);
            parsedWord.setCreateAt(now);

            // System.out.printf("%s/%s --(%s)--> %s/%s\n", word.LEMMA, word.POSTAG, word.DEPREL, word.HEAD.LEMMA, word.HEAD.POSTAG);
            wordList.add(parsedWord);
        }
        parseCfgList(sentence, sentId);
        appendOutputConll(sentence, sent, sentId, fileName);
        nlpParserSqliteDao.insertWordList(wordList);
    }

    public void parseCfgList(CoNLLSentence coNLLSentence, Long sentId) {
        LocalDateTime now = LocalDateTime.now();
        List<ParsedCfgPair> cfgPairList = Lists.newLinkedList();
        for (CoNLLWord word : coNLLSentence) {
            ParsedCfgPair cfgPair = new ParsedCfgPair();
            if (word != CoNLLWord.ROOT) {
                cfgPair.setSentId(sentId);
                cfgPair.setDeprel(word.DEPREL);
                cfgPair.setLeftCpostag(word.CPOSTAG);
                cfgPair.setRightCpostag(word.HEAD.CPOSTAG);
                cfgPair.setLeftWord(word.LEMMA);
                cfgPair.setRightWord(word.HEAD.LEMMA);
                cfgPair.setLeftPostag(word.POSTAG);
                cfgPair.setRightPostag(word.HEAD.POSTAG);
                cfgPair.setLeftWordNo(word.ID);
                cfgPair.setRightWordNo(word.HEAD.ID);
                cfgPair.setCreateAt(now);

                cfgPairList.add(cfgPair);
            }
        }
        nlpParserSqliteDao.insertCfgList(cfgPairList);
    }

    public void appendOutputConll(CoNLLSentence coNLLSentence, String sent, Long sentId, String fileName) {
        String result = printToConllFormat(coNLLSentence, sent, sentId, fileName);
        try {
            sink.write(result);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public String printToConllFormat(CoNLLSentence coNLLSentence, String sent, Long sentId, String fileName) {
        String result = String.format("# fileName = %s\n# sent_id = %d\n# text = %s\n%s\n",
                fileName, sentId, sent, coNLLSentence.toString());
        // System.out.print(result);
        return result;
    }

    public void close() {
        nlpParserSqliteDao.initIndex();
        nlpParserSqliteDao.close();
    }

    private void initLineFilterPatternList(String filterRegexFilePath) {
        File file = new File(filterRegexFilePath);
        try {
            Files.asCharSource(file, Charsets.UTF_8).readLines(new LineProcessor<Object>() {
                private int lineNum = 1;

                @Override
                public boolean processLine(String line) throws IOException {
                    line = line.trim();
                    if (Strings.isNullOrEmpty(line)) {
                        return true;
                    }
                    Pattern pattern = Pattern.compile(line);
                    filterPatternList.add(pattern);

                    ++lineNum;
                    return true;
                }

                @Override
                public Object getResult() {
                    return lineNum;
                }
            });
        } catch (IOException e) {

            System.err.println(e.getMessage());
        }
    }

    public DependencyParser(String conllOutputFilePath, String filterRegexFilePath, String dbPath) {
        this.sink = Files.asCharSink(new File(conllOutputFilePath), Charsets.UTF_8, FileWriteMode.APPEND);
        this.nlpParserSqliteDao = new NlpParserSqliteDao(dbPath);
        initLineFilterPatternList(filterRegexFilePath);
    }

    public static void printHelpMsg(Options options) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("用法: java -jar <this_file>.jar <options> files...\n" +
                "模型文件需要放在 ./hanlp-data/ ", options);
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Option opt1 = Option.builder("r").longOpt("regex").argName("filterRegex").hasArg()
                .desc("过滤无效字符的正则文件路径. 默认： ./filterRegex.txt").required(false).numberOfArgs(1).build();
        Option opt2 = Option.builder("c").longOpt("conll").argName("conllFile").hasArg()
                .desc("中间结果coNLL文件输出路径. 默认: ./conll_output.txt").required(false).numberOfArgs(1).build();
        Option opt3 = Option.builder("d").longOpt("database").argName("dbFile").hasArg()
                .desc("结果写入的数据库路径. 默认: ./nlpparsed.sqlite3").required(false).numberOfArgs(1).build();
        Option opt4 = Option.builder("h").longOpt("help").hasArg(false)
                .desc("显示帮助").required(false).build();

        Options options = new Options();
        options.addOption(opt1);
        options.addOption(opt2);
        options.addOption(opt3);
        options.addOption(opt4);

        CommandLine cli = null;
        CommandLineParser cliParser = new DefaultParser();


        String outputFilePath;
        String filterPattern;
        String dbPath;
        List<String> inputFileList;
        try {
            cli = cliParser.parse(options, args);

            outputFilePath = cli.getOptionValue("c", "conll_output.txt");
            filterPattern = cli.getOptionValue("r", "filterRegex.txt");
            dbPath = cli.getOptionValue("d", "nlpparsed.sqlite3");
            inputFileList = cli.getArgList();
            if (cli.hasOption("h")) {
                printHelpMsg(options);
                return;
            }
        } catch (ParseException | NullPointerException e) {
            // 解析失败时用 HelpFormatter 打印 帮助信息
            printHelpMsg(options);
            e.printStackTrace();
            return;
        }
        DependencyParser parser = new DependencyParser(outputFilePath, filterPattern, dbPath);
        for (String inputFile : inputFileList) {
            parser.parseFile(inputFile);
        }
        parser.close();


//        String outputFilePath = Joiner.on(File.separator).join("data", "conll_output.txt");
//        String filterPattern = Joiner.on(File.separator).join("data", "filterRegex.txt");
//        DependencyParser parser = new DependencyParser(outputFilePath, filterPattern);
//
//        parser.parseFile("data/lz-data/shentiyundongxunlian.txt");
//        parser.parseFile("data/lz-data/tushouxunlian.txt");
//        parser.parseFile("data/lz-data/yujia.txt");
//
//        parser.close();
    }
}
