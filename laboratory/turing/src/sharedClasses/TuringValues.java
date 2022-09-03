package sharedClasses;

//operazioni e messaggi di esito delle op del server
public enum TuringValues {
    LOGIN,
    REGISTER,
    ALREADY_REG,
    ALREADY_LOGGED,
    ALREADY_EDITING,
    CREATE, //op creazione documento
    SHARE,  //op condivisione documento
    NOT_LOGGED,
    LOGOUT,
    SHOW,
    DOC_LOCKED,
    NOT_EDITING,
    SHOW_SECTION,
    SECTION_UNK,
    LIST,
    EDIT,
    WRONG_SECTION, //end edit chiede una sezione esistente ma che non e' in editing
    SEND, //invio messaggio in chat
    INVITE_HELLO, //messaggio per risveglio della socket di inviti nel server
    RECEIVE, //ricezione messaggio in chat
    END_EDIT,
    OP_OK,
    UNREG_USER,
    OP_FAIL,
    UNAUTHORIZATED, //utente non autorizzato ad editare il documento
    WRONG_PASSWORD,
    UNKNOWN_DOCUMENT,
    DOCUMENT_ALREADY_EXISTS
}
