/*
This file generates USFMLex.java with JFlex


*/

import usfm.*;

%%

%public
%class USFMLex
%standalone

%unicode
%line
%column

%state TAGID SPECIAL POST_TAG_NUMERAL SPECIAL_CHAR
%type USFMSymbol

%throws RuntimeException

/* class initializer */
%{
	StringBuffer buf = new StringBuffer();
	StringBuffer tagBuf = new StringBuffer();
	
	
	private USFMSymbol symbol(SymbolType sym) {
		return symbol(sym, null);
	}
	private USFMSymbol symbol(SymbolType sym, String data) {
		return new USFMSymbol(sym, data, yyline, yycolumn);
	}
%}


LineTerminator = \r|\n|\r\n
TagIdCharacter = [a-zA-Z0-9+]
TextCharacter = [^\\]
SpecialSequences = "//" | "~"

%%
<YYINITIAL> {

"\\"						{
							String s = buf.toString();
							buf.setLength(0);
							yybegin(TAGID);
							tagBuf.setLength(0);
							return new TextSymbol(s, yyline, yycolumn);
							}

{LineTerminator}	{
                            buf.append(" ");
                            String s = buf.toString();
                            buf.setLength(0);
                            TextSymbol ts = new TextSymbol(s, yyline, yycolumn);
                            ts.EOLAfter = true;
                            return ts;
                            }

/* special characters, e.g. newline, quotation marks */
{SpecialSequences}      {
                            // return the string up to this point.
                            TextSymbol ts = new TextSymbol(buf.toString(), yyline, yycolumn);

                            buf.setLength(0);

                            // jump to a state where we can encode the special character
                            yypushback(yylength());
                            yybegin( SPECIAL_CHAR );

                            return ts;
                        }

<<EOF>>                 {
                            if (buf.length() == 0)
                                    return symbol(SymbolType.EOF);
                            else {
                                    String s = buf.toString();
                                    buf.setLength(0);
                                    return new TextSymbol(buf.toString(), yyline, yycolumn);
                            }
                        }

.			{ buf.append(yytext()); }

}

<SPECIAL_CHAR> {

/* USFM line break */
"//"    { yybegin(YYINITIAL); return new MacroTextSymbol("--newline--", " ",  yyline, yycolumn); }

/* USFM non breaking space */
"~"	{ yybegin(YYINITIAL); return new MacroTextSymbol("--nbsp--", "\160", yyline, yycolumn); }
}

<POST_TAG_NUMERAL> {
/*
we take the number, as well as all subsequent spaces, s.t.: "\v 52 And then" ==> "\v " + "52 " + "And then"
*/
[0-9]+[ ]*					{ yybegin(YYINITIAL); return new NumericTextSymbol(yytext(), yyline, yycolumn); }

.|{LineTerminator}			{ yypushback(yylength()); yybegin(YYINITIAL); }

}

<TAGID> {

{TagIdCharacter}+ 			{ tagBuf.append(yytext()); }
"*"						{ yybegin(YYINITIAL); return new TagCloseSymbol(tagBuf.toString(), /* with space? */ false, yyline, yycolumn); }
[ ]+							{ yybegin(POST_TAG_NUMERAL); return new TagOpenSymbol(tagBuf.toString(), /* with space? */ true, yyline, yycolumn); }
.|{LineTerminator}			{ yypushback(yylength()); yybegin(YYINITIAL); return new TagOpenSymbol(tagBuf.toString(), false, yyline, yycolumn); }

}


.							{ throw new RuntimeException("Problem at line " + yyline + " col " + yycolumn); }
{LineTerminator}			{ throw new RuntimeException("Unexpected temrinator at " + yyline + " col " + yycolumn); }
<<EOF>>						{ return symbol(SymbolType.EOF); }
