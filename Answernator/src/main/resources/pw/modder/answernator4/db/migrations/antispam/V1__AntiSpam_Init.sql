CREATE TABLE ANTISPAMCONFIGS (
    ID                ${datatype:long} NOT NULL PRIMARY KEY,
    IS_ENABLED        ${datatype:integer} NOT NULL,
    FILTER_MRBEAST    ${datatype:integer} NOT NULL,

    WARNING_TEXT      VARCHAR(255) NOT NULL,
    MUTE_TEXT         VARCHAR(255) NOT NULL,

    WARNING_THRESHOLD ${datatype:integer} NOT NULL,
    MUTE_THRESHOLD    ${datatype:integer} NOT NULL,
    MUTES_BEFORE_BAN  ${datatype:integer} NOT NULL,

    MUTE_DURATION     ${datatype:integer} NOT NULL,
    MUTE_VALIDITY     ${datatype:integer} NOT NULL,

    LOG_CHANNEL       ${datatype:long}
);

CREATE INDEX ANTISPAMCONFIGS_IS_ENABLED     ON ANTISPAMCONFIGS (IS_ENABLED);
CREATE INDEX ANTISPAMCONFIGS_FILTER_MRBEAST ON ANTISPAMCONFIGS (FILTER_MRBEAST);

CREATE TABLE MRBEASTLOVERS (
    USER_ID            ${datatype:long} NOT NULL,
    GUILD_ID           ${datatype:long} NOT NULL,
    ATTACHMENTS_COUNT  ${datatype:integer} NOT NULL,
    VIOLATION_DATE     ${datatype:dateTime} DEFAULT ${datatype:current_timestamp} NOT NULL,
    MESSAGE_CONTENT    ${datatype:text} NOT NULL
);

CREATE INDEX MRBEASTLOVERS_USER_ID ON MRBEASTLOVERS (USER_ID);
CREATE INDEX MRBEASTLOVERS_USER_ID_GUILD_ID ON MRBEASTLOVERS (USER_ID, GUILD_ID);

CREATE TABLE SPAMMUTES (
    USER_ID            ${datatype:long} NOT NULL,
    GUILD_ID           ${datatype:long} NOT NULL,
    VIOLATION_DATE     ${datatype:dateTime} DEFAULT ${datatype:current_timestamp} NOT NULL,
    MESSAGE_CONTENT    ${datatype:text} NOT NULL
);

CREATE INDEX SPAMMUTES_USER_ID ON SPAMMUTES (USER_ID);
CREATE INDEX SPAMMUTES_USER_ID_GUILD_ID ON SPAMMUTES (USER_ID, GUILD_ID);
