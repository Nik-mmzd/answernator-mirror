CREATE TABLE LOGSCONFIGS (
    ID ${datatype:long} NOT NULL PRIMARY KEY,
    MEMBER_JOIN_LOG    ${datatype:long},
    MEMBER_LEAVE_LOG   ${datatype:long},
    MEMBER_BAN_LOG     ${datatype:long},
    MEMBER_UNBAN_LOG   ${datatype:long},
    MEMBER_MUTE_LOG    ${datatype:long},
    MEMBER_UPDATE_LOG  ${datatype:long},
    LOCALE             VARCHAR(16) NOT NULL
);
