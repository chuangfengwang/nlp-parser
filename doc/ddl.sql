-- 句子表
create table if not exists parsed_sent
(
    id          integer      not null primary key autoincrement,                     -- 主键
    file_name   varchar(255) not null default '',                                    -- 文件名
    sent        text         not null default '',                                    -- 句子
    file_offset int          not null default 0,                                     -- 句子首字母在文件中的偏移位置
    line_no     int          not null default 0,                                     -- 句子在文件中的行号
    parser_name varchar(50)  not null default 'hanlp-NeuralNetworkDependencyParser', -- 分析器名称
    create_at   datetime     not null default (datetime('now', 'localtime'))         -- 创建时间
);

create index if not exists idx_create_at on parsed_sent (create_at);

insert into parsed_sent(file_name, sent, file_offset, line_no, parser_name)
values (?, ?, ?, ?, ?);

-- 词语表
create table if not exists parsed_word
(
    id             integer      not null primary key autoincrement,             -- 主键
    sent_id        integer      not null default 0,                             -- 句子 id
    word           varchar(255) not null default '',                            -- 词语
    word_no        int          not null default 0,                             -- 词语在句子中的序号,从1开始, 0是语法树的根节点, -1是空白节点
    lemma          varchar(255) not null default '',                            -- 当前词语（或标点）的原型或词干，在中文中，此列与FORM相同
    cpostag        varchar(50)  not null default '',                            -- 当前词语的词性（粗粒度）
    postag         varchar(50)  not null default '',                            -- 当前词语的词性（细粒度）
    head_no        int          not null default 0,                             -- 当前词语的中心词序号
    deprel         varchar(50)  not null default '',                            -- 当前词语与中心词的依存关系
    conllword_name varchar(50)  not null default '',                            -- 等效字符串
    create_at      datetime     not null default (datetime('now', 'localtime')) -- 创建时间
);

create index if not exists idx_sent_word_id on parsed_word (sent_id, word_no);
create index if not exists idx_word on parsed_word (word);
create index if not exists idx_cpostag on parsed_word (cpostag);
create index if not exists idx_postag on parsed_word (postag);
create index if not exists idx_deprel on parsed_word (deprel);
create index if not exists idx_sent_head_no on parsed_word (sent_id, head_no);
create index if not exists idx_create_at on parsed_word (create_at);

insert into parsed_word(sent_id, word, word_no, lemma, cpostag, postag, head_no, deprel, conllword_name, create_at)
values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

-- cfg 表
create table if not exists parsed_cfg_pair
(
    id            integer      not null primary key autoincrement,             -- 主键
    sent_id       integer      not null default 0,                             -- 句子 id
    deprel        varchar(255) not null default '',                            -- 当前词语与中心词的依存关系
    left_cpostag  varchar(50)  not null default '',                            -- 左边词语的词性（粗粒度）
    right_cpostag varchar(50)  not null default '',                            -- 右边词语的词性（粗粒度）
    left_word     varchar(255) not null default '',                            -- 左边词语
    right_word    varchar(255) not null default '',                            -- 右边词语
    left_postag   varchar(50)  not null default '',                            -- 左边词语的词性（细粒度）
    right_postag  varchar(50)  not null default '',                            -- 右边词语的词性（细粒度）
    left_word_no  int          not null default 0,                             -- 左边词语序号
    right_word_no int          not null default 0,                             -- 右边词语序号
    create_at     datetime     not null default (datetime('now', 'localtime')) -- 创建时间
);

create index if not exists idx_sent_id_word_no_lr on parsed_cfg_pair (sent_id, left_word_no, right_word_no);
create index if not exists idx_sent_id_word_no_rl on parsed_cfg_pair (sent_id, right_word_no, left_word_no);
create index if not exists idx_deprel_cpostag on parsed_cfg_pair (deprel, left_cpostag, right_cpostag);
create index if not exists idx_deprel_postag on parsed_cfg_pair (deprel, left_postag, right_postag);
create index if not exists idx_create_at on parsed_cfg_pair (create_at);

insert into parsed_cfg_pair(sent_id, deprel, left_cpostag, right_cpostag, left_word, right_word, left_postag, right_postag, left_word_no, right_word_no, create_at)
values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);