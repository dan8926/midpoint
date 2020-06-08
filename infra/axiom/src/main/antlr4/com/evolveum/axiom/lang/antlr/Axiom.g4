grammar Axiom;

SEMICOLON : ';';
LEFT_BRACE : '{';
RIGHT_BRACE : '}';
COLON : ':';
PLUS : '+';
LINE_COMMENT :  [ \n\r\t]* ('//' (~[\r\n]*)) [ \n\r\t]* -> skip;
SEP: [ \n\r\t]+;
IDENTIFIER : [a-zA-Z_][a-zA-Z0-9_\-]*;

fragment SQOUTE : '\'';
fragment DQOUTE : '"';

fragment ESC : '\\';

STRING_SINGLEQUOTE: SQOUTE ((ESC SQOUTE) | ~[\n'])* SQOUTE;
STRING_DOUBLEQUOTE: DQOUTE ((ESC DQOUTE) | ~[\n"])* DQOUTE;

//statement : SEP* identifier SEP* (argument)? SEP* (SEMICOLON | LEFT_BRACE SEP* (statement)* SEP* RIGHT_BRACE SEP*) SEP*;


item: identifier SEP* value;
dataItem : SEP* item; 
infraItem : SEP* '@' item;
value: (argument)? SEP* (SEMICOLON | LEFT_BRACE SEP* (dataItem | infraItem)* SEP* RIGHT_BRACE SEP*) SEP*;

identifier : (prefix COLON)? localIdentifier;
prefix : IDENTIFIER;
localIdentifier : IDENTIFIER;

argument : identifier | string;
string : singleQuoteString | doubleQuoteString | multilineString;

singleQuoteString : STRING_SINGLEQUOTE;
doubleQuoteString : STRING_DOUBLEQUOTE;
multilineString: '"""\n' (~('"""'))*'"""';

