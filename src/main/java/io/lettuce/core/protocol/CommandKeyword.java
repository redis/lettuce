/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.protocol;

import java.nio.charset.StandardCharsets;

/**
 * Keyword modifiers for redis commands.
 *
 * @author Will Glozer
 * @author Mark Paluch
 * @author Zhang Jessey
 * @author dengliming
 * @author Tihomir Mateev
 * @author Ali Takavci
 */
public enum CommandKeyword implements ProtocolKeyword {

    ABSTTL, ADD, ADDR, ADDSLOTS, ADDSLOTSRANGE, AFTER, AGGREGATE, ALLCHANNELS, ALLCOMMANDS, ALLKEYS, ALPHA, AND, ANDOR, ASK, ASC, ASYNC, BEFORE, BLOCK, BUMPEPOCH,

    BY, BYLEX, BYSCORE, CACHING, CAT, CH, CHANNELS, CLAIM, COPY, COUNT, COUNTKEYSINSLOT, CONSUMERS, CREATE, DB, DELSLOTS, DELSLOTSRANGE, DELUSER, DESC, DIFF, DIFF1, DRYRUN, SOFT, HARD, ENCODING,

    FAILOVER, FORGET, FIELDS, FLAGS, FLUSH, FORCE, FREQ, FLUSHSLOTS, GENPASS, GETNAME, GETUSER, GETKEYSINSLOT, GETREDIR, GROUP, GROUPS, HTSTATS, ID, IDS, IDLE, IDX, INFO,

    IDLETIME, JUSTID, KILL, KEYSLOT, LEFT, LEN, LIMIT, LINKS, LIST, LOAD, LOG, MATCH,

    MAX, MAXLEN, MEET, MIN, MINID, MINMATCHLEN, MOVED, NO, NOACK, NOCOMMANDS, NODE, NODES, NOMKSTREAM, NOPASS, NOSAVE, NOT, NOVALUES, NUMSUB, SHARDCHANNELS, SHARDNUMSUB, NUMPAT, NX, FNX, OFF, ON, ONE, OR, PAUSE, PREFIXES,

    REFCOUNT, REMOVE, RELOAD, REPLACE, REDIRECT, REPLICATE, REPLICAS, REV, RESET, RESETCHANNELS, RESETKEYS, RESETPASS,

    RESETSTAT, RESTART, RETRYCOUNT, REWRITE, RIGHT, SAVECONFIG, SDSLEN, SETINFO, SETNAME, SETSLOT, SHARDS, SLOTS, STABLE,

    MIGRATING, IMPORTING, SAVE, SKIPME, SLAVES, STREAM, STORE, SUM, SEGFAULT, SETUSER, TAKEOVER, TRACKING, MAINT_NOTIFICATIONS, TRACKINGINFO, TYPE, UNBLOCK, USERS, USAGE, WEIGHTS, WHOAMI,

    WITHMATCHLEN, WITHSCORE, WITHSCORES, WITHVALUES, XOR, XX, FXX, YES, INDENT, NEWLINE, SPACE, GT, LT,

    CAS, EF, ELE, SETATTR, M, NOQUANT, BIN, Q8, FILTER, FILTER_EF(
            "FILTER-EF"), TRUTH, NOTHREAD, REDUCE, VALUES, RAW, EPSILON, WITHATTRIBS,

    MAXTEXTFIELDS, PREFIX, LANGUAGE, LANGUAGE_FIELD, SCORE, SCORE_FIELD, PAYLOAD_FIELD, TEMPORARY, NOOFFSETS, NOHL, NOFIELDS, NOFREQS, SKIPINITIALSCAN, STOPWORDS, AS, SORTABLE, SCHEMA, UNF, NOINDEX,

    NOSTEM, PHONETIC, WEIGHT, SEPARATOR, CASESENSITIVE, WITHSUFFIXTRIE, INDEXEMPTY, INDEXMISSING, DD, SORTBY, WITHCOUNT, SUMMARIZE, FRAGS, HIGHLIGHT, TAGS, DIALECT, PARAMS, TIMEOUT, SLOP, EXPLAINSCORE, PAYLOAD,

    SCORER, EXPANDER, INORDER, RETURN, INFIELDS, INKEYS, WITHSORTKEYS, WITHPAYLOADS, NOSTOPWORDS, VERBATIM, NOCONTENT, FLAT, SPHERICAL, HNSW, DIM, DISTANCE_METRIC, FLOAT32, FLOAT64, L2, COSINE, IP, WITHCURSOR, MAXIDLE, ADDSCORES, GROUPBY, APPLY, READ, DEL, TERMS, DISTANCE, EX, PX, EXAT, PXAT, KEEPTTL, IFEQ, IFNE, IFDEQ, IFDNE;

    public final byte[] bytes;

    private final String keyword;

    CommandKeyword() {
        keyword = name();
        bytes = keyword.getBytes(StandardCharsets.US_ASCII);
    }

    CommandKeyword(String keyword) {
        this.keyword = keyword;
        bytes = keyword.getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public String toString() {
        return keyword;
    }

}
