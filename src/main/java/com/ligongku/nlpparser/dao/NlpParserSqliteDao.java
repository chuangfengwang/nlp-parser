package com.ligongku.nlpparser.dao;

import com.ligongku.nlpparser.model.ParsedCfgPair;
import com.ligongku.nlpparser.model.ParsedSent;
import com.ligongku.nlpparser.model.ParsedWord;
import com.ligongku.nlpparser.util.FileUtil;

import java.sql.*;
import java.util.List;

public class NlpParserSqliteDao {

    private Connection connection = null;

    private String dbFilePath;

    // 初始化数据库
    public void initTables() {
        beginTransaction();

        String createTable = "";

        // 依存分析范围表: 句子-分析器
        createTable = "create table if not exists parsed_sent\n" +
                "(\n" +
                "    id          integer      not null primary key autoincrement,                     -- 主键\n" +
                "    file_name   varchar(255) not null default '',                                    -- 文件名\n" +
                "    sent        text         not null default '',                                    -- 句子\n" +
                "    file_offset int          not null default 0,                                     -- 句子首字母在文件中的偏移位置\n" +
                "    line_no     int          not null default 0,                                     -- 句子在文件中的行号\n" +
                "    parser_name varchar(50)  not null default 'hanlp-NeuralNetworkDependencyParser', -- 分析器名称\n" +
                "    create_at   datetime     not null default (datetime('now', 'localtime'))         -- 创建时间\n" +
                ");";
        update(createTable);

        // 分析结果表
        createTable = "create table if not exists parsed_word\n" +
                "(\n" +
                "    id             integer      not null primary key autoincrement,             -- 主键\n" +
                "    sent_id        integer      not null default 0,                             -- 句子 id\n" +
                "    word           varchar(255) not null default '',                            -- 词语\n" +
                "    word_no        int          not null default 0,                             -- 词语在句子中的序号,从1开始, 0是语法树的根节点, -1是空白节点\n" +
                "    lemma          varchar(255) not null default '',                            -- 当前词语（或标点）的原型或词干，在中文中，此列与FORM相同\n" +
                "    cpostag        varchar(50)  not null default '',                            -- 当前词语的词性（粗粒度）\n" +
                "    postag         varchar(50)  not null default '',                            -- 当前词语的词性（细粒度）\n" +
                "    head_no        int          not null default 0,                             -- 当前词语的中心词序号\n" +
                "    deprel         varchar(50)  not null default '',                            -- 当前词语与中心词的依存关系\n" +
                "    conllword_name varchar(50)  not null default '',                            -- 等效字符串\n" +
                "    create_at      datetime     not null default (datetime('now', 'localtime')) -- 创建时间\n" +
                ");";
        update(createTable);

        // cfg 规则表
        createTable = "create table if not exists parsed_cfg_pair\n" +
                "(\n" +
                "    id            integer      not null primary key autoincrement,             -- 主键\n" +
                "    sent_id       integer      not null default 0,                             -- 句子 id\n" +
                "    deprel        varchar(255) not null default '',                            -- 当前词语与中心词的依存关系\n" +
                "    left_cpostag  varchar(50)  not null default '',                            -- 左边词语的词性（粗粒度）\n" +
                "    right_cpostag varchar(50)  not null default '',                            -- 右边词语的词性（粗粒度）\n" +
                "    left_word     varchar(255) not null default '',                            -- 左边词语\n" +
                "    right_word    varchar(255) not null default '',                            -- 右边词语\n" +
                "    left_postag   varchar(50)  not null default '',                            -- 左边词语的词性（细粒度）\n" +
                "    right_postag  varchar(50)  not null default '',                            -- 右边词语的词性（细粒度）\n" +
                "    left_word_no  int          not null default 0,                             -- 左边词语序号\n" +
                "    right_word_no int          not null default 0,                             -- 右边词语序号\n" +
                "    create_at     datetime     not null default (datetime('now', 'localtime')) -- 创建时间\n" +
                ");";
        update(createTable);

        commitTransaction();
    }

    // 添加索引
    public void initIndex() {
        beginTransaction();

        String createIndex = "";

        // 依存分析范围表: 句子-分析器
        createIndex = "create index if not exists idx_create_at on parsed_sent (create_at);";
        update(createIndex);

        // 分析结果表
        createIndex = "create index if not exists idx_sent_word_id on parsed_word (sent_id, word_no);\n" +
                "create index if not exists idx_word on parsed_word (word);\n" +
                "create index if not exists idx_cpostag on parsed_word (cpostag);\n" +
                "create index if not exists idx_postag on parsed_word (postag);\n" +
                "create index if not exists idx_deprel on parsed_word (deprel);\n" +
                "create index if not exists idx_sent_head_no on parsed_word (sent_id, head_no);\n" +
                "create index if not exists idx_create_at on parsed_word (create_at);";
        update(createIndex);

        // cfg 规则表
        createIndex = "create index if not exists idx_sent_id_word_no_lr on parsed_cfg_pair (sent_id, left_word_no, right_word_no);\n" +
                "create index if not exists idx_sent_id_word_no_rl on parsed_cfg_pair (sent_id, right_word_no, left_word_no);\n" +
                "create index if not exists idx_deprel_cpostag on parsed_cfg_pair (deprel, left_cpostag, right_cpostag);\n" +
                "create index if not exists idx_deprel_postag on parsed_cfg_pair (deprel, left_postag, right_postag);\n" +
                "create index if not exists idx_create_at on parsed_cfg_pair (create_at);";
        update(createIndex);

        commitTransaction();
    }

    // 插入一个句子
    public Long insertSent(ParsedSent sent) {
        String sql = "insert into parsed_sent(file_name, sent, file_offset, line_no, parser_name, create_at)\n" +
                "values (?, ?, ?, ?, ?, ?);";
        PreparedStatement ps = null;
        try {
            beginTransaction();
            ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, sent.getFileName());
            ps.setString(2, sent.getSent());
            ps.setInt(3, sent.getFileOffset());
            ps.setInt(4, sent.getLineNo());
            ps.setString(5, sent.getParserName());
            // ps.setDate(6, new java.sql.Date(Date.from(sent.getCreateAt().atZone(ZoneId.systemDefault()).toInstant()).getTime()));
            ps.setDate(6, java.sql.Date.valueOf(sent.getCreateAt().toLocalDate()));
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            long id = 0;
            if (rs.next()) {
                id = rs.getLong(1);
            }
            commitTransaction();
            return id;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException("DB error when insert into parsed_sent");
        }
    }

    // 插入一个句子的依存分析结果
    public int insertWordList(List<ParsedWord> wordList) {

        String sql = "insert into parsed_word(sent_id,word,word_no,lemma,cpostag,postag,head_no,deprel,conllword_name,create_at)\n" +
                "values (?,?,?,?,?,?,?,?,?,?);";
        try {
            beginTransaction();

            PreparedStatement ps = connection.prepareStatement(sql);
            for (ParsedWord word : wordList) {
                ps.setLong(1, word.getSentId());
                ps.setString(2, word.getWord());
                ps.setInt(3, word.getWordId());
                ps.setString(4, word.getLemma());
                ps.setString(5, word.getCpostag());
                ps.setString(6, word.getPostag());
                ps.setInt(7, word.getHeadNo());
                ps.setString(8, word.getDeprel());
                ps.setString(9, word.getConllwordName());
                ps.setDate(10, java.sql.Date.valueOf(word.getCreateAt().toLocalDate()));
                ps.addBatch();
            }

            ps.executeBatch();
            commitTransaction();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            try {
                connection.rollback();
            } catch (SQLException ex) {
                System.err.println(e.getMessage());
            }
            return -1;
        }

        return 0;
    }

    // 插入一批三元组
    public int insertCfgList(List<ParsedCfgPair> cfgPairList) {

        String sql = "insert into parsed_cfg_pair(sent_id, deprel, left_cpostag, right_cpostag, left_word, right_word, left_postag, right_postag, left_word_no, right_word_no, create_at)\n" +
                "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try {
            beginTransaction();

            PreparedStatement ps = connection.prepareStatement(sql);
            for (ParsedCfgPair word : cfgPairList) {
                ps.setLong(1, word.getSentId());
                ps.setString(2, word.getDeprel());
                ps.setString(3, word.getLeftCpostag());
                ps.setString(4, word.getRightCpostag());
                ps.setString(5, word.getLeftWord());
                ps.setString(6, word.getRightWord());
                ps.setString(7, word.getLeftPostag());
                ps.setString(8, word.getRightPostag());
                ps.setInt(9, word.getLeftWordNo());
                ps.setInt(10, word.getRightWordNo());
                ps.setDate(11, java.sql.Date.valueOf(word.getCreateAt().toLocalDate()));
                ps.addBatch();
            }

            ps.executeBatch();
            commitTransaction();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            try {
                connection.rollback();
            } catch (SQLException ex) {
                System.err.println(e.getMessage());
            }
            return -1;
        }

        return 0;
    }

    // 执行更新 sql
    public void update(String sql) {
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setQueryTimeout(0);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    // 开始一个事务
    public void beginTransaction() {
        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    // 提交一个事务
    public void commitTransaction() {
        try {
            connection.commit();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    // 初始化数据库
    public void init() {
        initTables();
    }

    // 初始化连接
    public NlpParserSqliteDao(String dbFile) {
        this.dbFilePath = dbFile;

        // 文件不存在就创建
        FileUtil.makeFileIfNotExist(this.dbFilePath);

        String url = "jdbc:sqlite:" + dbFile;
        try {
            connection = DriverManager.getConnection(url);
            // 初始化数据库
            init();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException("Error when open sqlite DB:" + this.dbFilePath);
        }
    }

    // 关闭连接
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                throw new RuntimeException("Error when close sqlite DB:" + this.dbFilePath);
            }
        }
    }

}
