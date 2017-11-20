import java.io.*;
import usfm.*;
import java.util.HashMap;
import java.util.ArrayList;

/**

Utility class to parse USFM files.

The objective of using JFlex was to allow GoBibleCreator to be extended
without recompiling the source. Currently, that is still not possible,
although it should be obvious how that can be achieved with a
configuration file.

For example, under isSingularGreedyTag, we have:

String sMarker[] = {"cl","cp","cd", "qa", "sr", "mr ", "ms", "mte", "mt", "s", "sr ", "r ", "d ", "sp"};

This can be replaced with (pseudo-code):

String sMarker = ReadConfigurationFile().getConfig("SingularGreedyTags").trim().split(" ");

This is better than the previous solution because it does not require the
correct ordering of statements to avoid parsing errors (e.g. \w mangling \wj).
It also handles post-tag spaces well, since it uses the regex:

\\[a-zA-Z0-9]+(*| )?

Any space that follow an opening tag are assumed to be part of the tag. The presence
of a space after a tag is indicated in <code>TagSymbol</code> by the <code>spaced</code> field,
although that is currently unused. A tighter compliance with USFM could 
ensure that for tags where any subsequent spaces are significant, mangled spaces
can be restored as necessary.

Finally, should USFM ever specify that \\ is an escape sequence for \, we can
add that to usfm.flex easily.

*/

public class USFMParse {
	public char sWJ = 1;
	public String emptyVerseString = null;
	
	private USFMSymbol current, unlexed = null;
	private USFMLex scanner;
        
        private HashMap<String, String> macroReplacementTable= new HashMap<String, String>();
        private ArrayList<TwoString> literalsReplacementTable= new ArrayList<TwoString>();
        private HashMap<String, String[]> configTable= new HashMap<String, String[]>();
	
        public USFMParse() {
            String defaultConfig[] = new String[] {
"SingularTags: pmo pm pmc pmr mi nb cls pc pr qr qc pb b m p z li qm q pi ph",
"SingularTagsWithNumbers: li qm q pi ph",
"SingularGreedyTags: cl cp cd qa sr mr  ms mte mt s sr  r  d  sp is v c",
"SingularGreedyTagsWithNumbers: ms mte mt s is",
"DoubleTextualTags: qs qac add dc ndx nd ord pn pro qt sig sls wg wh tl em bd it bdit no sc k w",
"DoubleTextualTagsWithNumbers: ",
"DoubleAnnotationTags: ca va vp fe bk xdc fdc fm fig f x rq xot xnt iqt",
"DoubleAnnotationTagsWithNumbers: ",
"SignificantWhitespace: false",
"Replace: /--newline--/\\n/",
"Replace: /wj/\\01/",
"Replace: /--nbsp--/\\ua0/"
            };
            
            for (String cfg: defaultConfig){
                this.interpretConfigLine(cfg);
            }

            
        }
	public USFMParse(USFMSymbol current, USFMLex scanner) {
            this.reset(current, scanner);
	}
        public void reset(USFMSymbol current, USFMLex scanner) {
		this.current=current;
		this.scanner=scanner;
        }
        
        public void readConfig(String fn) {
            BufferedReader rdr = null;
            try {
                rdr = new BufferedReader(new FileReader(fn));

            }catch (FileNotFoundException fnfe) {
                System.err.println("Could not find USFMSettings.txt:");
                System.err.println(fnfe.getMessage());
                return;
			}
            try {
                StringBuffer pair = new StringBuffer();
                String line;

                while ( true ) {
                    line = rdr.readLine();

                    if (line == null) {
                        interpretConfigLine(pair.toString());
                        break;
                    }

                    if (line.startsWith("//")) { // comment
                        continue;
                    }
                    if (line.trim().length() == 0) { // empty
                        continue;
                    }

                    if (Character.isSpaceChar(line.charAt(0))) { // continuation from previous line
                        pair.append(line);
                        continue;
                    }
                    else {
                        interpretConfigLine(pair.toString());
                        pair.setLength(0);
                        pair.append(line);
                    }
                }
            }catch (IOException ioe) {
                System.err.println("Error while reading USFM Parse Config file");
                ioe.printStackTrace();
            }
        }
        private void interpretConfigLine(String pair) {
            if (pair.toString().trim().length() == 0)
                return;
            
            String param, value;
            int colonPosition = pair.indexOf(":");
            
            if (colonPosition == -1)
                System.err.println("Unknown config entry: |" + pair + "|");
            
            param = pair.substring(0, colonPosition).trim();
            value = pair.substring(colonPosition + 1).trim();
            
            if (param.equals("Replace") || param.equals("ReplaceTag")
                    || param.equals("ReplaceLiteral")) {
				// split("a", "a1a2a3") -> ["" 1 2 3]
				// split("a", "a1aa") -> ["" 1]
				// split("a", "a1aa3") -> ["" 1 "" 3]
				// therefore need to add another dummy character for the split
				// to work or we'll have too few
                String[] parts = (value + " ").split(java.util.regex.Pattern.quote(value.substring(0,1)));
                
                if (parts.length < 4) { // the 4 are: prefix, needle, replacement, dummy
                    System.err.println("Syntax error parsing replacement table entry: " + pair.toString());
                    return;
                }
                
                // now I need to parse parts[2] to replace escape sequences with
                // meta-characters
                StringBuilder processedPart2 = new StringBuilder();
                StringBuilder octalSeq = null;
                int state = 0;
                
                LOOP: for (int i=0; true; i++) {
                    
                    int c = (i >= parts[2].length())? -1 : parts[2].charAt(i);
                    
                    if (state == 0) {
                        switch (c) {
                            case '\\':
                                state = 1;
                                break;
                            case -1:
                                break LOOP;
                            default:
                                processedPart2.append((char)c);
                        }
                    }
                    else if (state == 1) {
                        switch (c) {
                            case '\\':
                                processedPart2.append('\\');
                                state = 0;
                                break;
                            case 'r':
                                processedPart2.append('\r');
                                state = 0;
                                break;
                            case 'n':
                                processedPart2.append('\n');
                                state = 0;
                                break;
                            case 'b':
                                processedPart2.append('\b');
                                state = 0;
                                break;
                            case 't':
                                processedPart2.append('\t');
                                state = 0;
                                break;
                            case 'u': // unicode sequence
                                octalSeq = new StringBuilder();
                                state = 5;
                                break;
                            case '0': // octal sequence
                                octalSeq = new StringBuilder();
                                state = 2;
                                break;
                            default:
                                System.err.println("Unknown escape character " + c);
                                state = 0;
                        }
                    }
                    else if (state == 2) { // octal
                        switch (c) {
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                octalSeq.append((char)c);
                                
                                if (octalSeq.length() == 3) {
                                    try {
                                        processedPart2.append( (char) Integer.parseInt(octalSeq.toString(), 8) );
                                    }catch (NumberFormatException nfe) {
                                    }
                                    state = 0;
                                }
                                break;
                            default:
                                try {
                                    processedPart2.append( (char) Integer.parseInt(octalSeq.toString(), 8) );
                                }catch (NumberFormatException nfe) {
                                }
                                state = 0;
                                
                                i--; // put back the character       
                        }
                    }
                    else if (state == 5) { // unicode
                        switch (c) {
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                            case 'a':
                            case 'b':
                            case 'c':
                            case 'd':
                            case 'e':
                            case 'f':
                            case 'A':
                            case 'B':
                            case 'C':
                            case 'D':
                            case 'E':
                            case 'F':
                                octalSeq.append((char)c);
                                
                                if (octalSeq.length() == 4) {
                                    try {
                                        processedPart2.append( (char) Integer.parseInt(octalSeq.toString(), 16) );
                                    }catch (NumberFormatException nfe) {
                                    }
                                    state = 0;
                                }
                                break;
                            default:
                                try {
                                    processedPart2.append( (char) Integer.parseInt(octalSeq.toString(), 16) );
                                }catch (NumberFormatException nfe) {
                                }
                                state = 0;
                                
                                i--; // put back the character       
                        }
                        
                    }
                    else {
                        System.err.println("Unknown state " + state);
                    }
                }
                
                if (param.equals("ReplaceLiteral")) {
                    literalsReplacementTable.add(new TwoString(parts[1], processedPart2.toString()));
                }
                else {
                    macroReplacementTable.put(parts[1], processedPart2.toString());
                }
            }
            else {
                configTable.put(param, value.split("\\s+"));
            }
        }
	
	private USFMSymbol lex() throws IOException {
		if (unlexed == null) {
			current = scanner.yylex();
			return current;
		}
		else {
			current = unlexed;
			unlexed = null;
			return current;
		}
	}
	private void unlex(USFMSymbol symb) { // can push back at most 1
		unlexed = symb;
	}
	
	
	public Chapter parseChapter() {
		Chapter c = new Chapter();
		
		try {
			// remove chapter headers part: delete everything before the first \v
            // we now try to keep the headers and add it to the first verse
            String heading = "";
			LOOP: while (true) {
				lex();
		
				switch (current.type) {
                    case TAG_OPEN:
					if (current.data.equals("v")) {
						break LOOP;
					}
					else if (current.data.equals("c")) { // empty chapter
						System.out.println("Warning: Empty chapter");
                                                System.out.println(current.toString());
						return c;
					}
                    else if (isHeading(current.data)) {
                        heading = parseVerse2();
                        c.headingInfo.add(0);
                        c.headingInfo.add(heading.length());
                        c.allHeadings.append(heading);
                        break LOOP;
                    }
					break;
				case EOF:
					return null;
				}
			}
			
			String verseBody;
			while ( (verseBody = parseVerse()) != null ) {
				if (verseBody.length() == 0 && this.emptyVerseString != null) {
					verseBody = new String(this.emptyVerseString);
				}

				c.verses.add(verseBody);
				c.allVerses.append(verseBody);
//			    System.err.printf("ch vs %d %s\n",
//			    	c.verses.size(),
//				((String)c.verses.get( c.verses.size() - 1 )).trim());

				// time to return because it's the next chapter
				if (current.type == SymbolType.TAG_OPEN ) {
				    if (current.data.equals("c")) {
                        break;
                    } else if (isHeading(current.data)) {
				        heading = parseVerse2();
                        c.headingInfo.add(c.verses.size());
                        c.headingInfo.add(heading.length());
                        c.allHeadings.append(heading);
                    }
				}
			}
		
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return c;
	}
	
	// optionally test for the verse number, then move on.
	public String parseVerse() {	
		try {
			lex();
			if (current.type == SymbolType.TEXT &&
				current instanceof NumericTextSymbol) { // verse number
				// ignore the verse number
			}
			else { // if it's text, then...
				unlex(current);
			}	
			return parseVerse2();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return null;
		
	}
	
	// parsing of verse body.
	private String parseVerse2() throws IOException {
		StringBuffer body = new StringBuffer();
		
		LOOP: while (true) {
			lex();
			
			switch (current.type) {
			case TEXT:
                                // to handle ~, //
                                if (current instanceof MacroTextSymbol &&
                                        macroReplacementTable.containsKey( ((MacroTextSymbol)current).macro )) {
                                    body.append(macroReplacementTable.get(((MacroTextSymbol)current).macro));
                                }
                                else {
                                    // just some randomly-generated strings that shouldn't collide with anything real...
                                    String magic = "bc5a23090b3598aed4351b7fc102a16b GoBibleCreator Intermediate Escape Sequence! 99ffbf13e3df30879eda61002c0bac07";
                                    String temp = current.data;
                                    int counter = 0;

                                    // to prevent replacements from clobbering one another...
                                    // e.g. if the results from one will be acted on by another
                                    for (TwoString ts : literalsReplacementTable) {
                                        temp = temp.replace( ts.first, magic + counter + magic );
                                        counter ++;
                                    }

                                    counter = 0;
                                    for (TwoString ts : literalsReplacementTable) {
                                        temp = temp.replace( magic + counter + magic, ts.second );
                                        counter ++;
                                    }
                                    
                                    body.append(temp);
                                }
				break;
			case TAG_CLOSE:
				if (macroReplacementTable.containsKey(current.data + "*")) {
					body.append(macroReplacementTable.get(current.data + "*"));
				}
				else if (isDoubleTextualTag(current.data)) {
					continue LOOP; // ignore
				}
				break;
			case TAG_OPEN:
				if (current.data.equals("v")) { // new verse
					return handleWhitespace(body.toString());
				}
				else if (current.data.equals("c")) { // new chapter
					return handleWhitespace(body.toString());
				}
                else if (isHeading(current.data)) { // new chapter
                    return handleWhitespace(body.toString());
                }
				if (macroReplacementTable.containsKey(current.data)) {
					body.append(macroReplacementTable.get(current.data));
				}
				else if (isSingularTag(current.data) || isDoubleTextualTag(current.data) ) {
					continue LOOP; // ignore
				}
				else if (isSingularGreedyTag(current.data)) {
					// consume until the next greedy tag. NB: this allows the text after tags like \s,
					// \is to break across multiple lines
					do {
						lex();
					} while ( current.type != SymbolType.EOF &&
						!(current.type == SymbolType.TAG_OPEN && isSingularGreedyTag(current.data)) );
                                        
                                        unlex(current);
                                        
					continue LOOP;
				}
				
				else if (isDoubleAnnotationTag(current.data)) {
					String tag = current.data;
                                        USFMSymbol openingTag = current;
					
					// consume until matching closing tag is found
					do {
						lex();
                                                //System.out.println(":" + current.data + ": " + current.type);
					} while ( current.type != SymbolType.EOF &&
								!(current.type == SymbolType.TAG_CLOSE && current.data.equals(tag)) );
                                        
                                        if (current.type == SymbolType.EOF) {
                                            System.out.println("Unclosed tag: " + openingTag.toString());
                                        }
					
					continue LOOP;
				}
				else {
					System.out.println("Unsupported tag: " + current.toString());
				}
				break;
			case EOF:
				break LOOP;
			}
		
		}
		if (body.length() == 0)
			return null;
		else
			return handleWhitespace(body.toString());
	}

    public String handleWhitespace(String s) {
    	String parts[] = configTable.get("SignificantWhitespace");

    	if (parts.length == 1 && (parts[0].equalsIgnoreCase("true") || parts[0].equalsIgnoreCase("yes") || parts[0].equalsIgnoreCase("1")) ) {
		return s;
    	}
    	return s.replaceAll(" [ ]+", " "); // collapse all multiple spaces into a single space.
    }
	private boolean isSingularTag(String comp) {
                String sMarker[] = configTable.get("SingularTags");//.split("\\s+");
	
		for (String s : sMarker) {
			if (comp.equals(s)) return true;
		}
		
		// those followed immediately by a number, e.g. \q1
                String sMarkerN[] = configTable.get("SingularTagsWithNumbers");//.split("\\s+");
		for (String s : sMarkerN) {
			if (comp.startsWith(s) && comp.substring(s.length()).matches("^[0-9]+$")) return true;
		}
		
		return false;
	}

	private boolean isHeading(String comp) {
            return comp.equals("s") || comp.equals("s1") || comp.equals("s2");
    }
	
	private boolean isSingularGreedyTag(String comp) {
	
                String sMarker[] = configTable.get("SingularGreedyTags");//.split("\\s+");
	
		for (String s : sMarker) {
			if (comp.equals(s)) return true;
		}
		
		// those followed immediately by a number, e.g. \q1
                String sMarkerN[] = configTable.get("SingularGreedyTagsWithNumbers");//.split("\\s+");
		for (String s : sMarkerN) {
			if (comp.startsWith(s) && comp.substring(s.length()).matches("^[0-9]+$")) return true;
		}
		
		return false;
	}

	private boolean isDoubleTextualTag(String comp) {
                String sMarker[] = configTable.get("DoubleTextualTags");//.split("\\s+");
	
		for (String s : sMarker) {
			if (comp.equals(s)) return true;
		}
		
		// those followed immediately by a number, e.g. \q1
                String sMarkerN[] = configTable.get("DoubleTextualTagsWithNumbers");//.split("\\s+");
		for (String s : sMarkerN) {
			if (comp.startsWith(s) && comp.substring(s.length()).matches("^[0-9]+$")) return true;
		}
		
		return false;
	}
	private boolean isDoubleAnnotationTag(String comp) {
            String sMarker[] = configTable.get("DoubleAnnotationTags");//.split("\\s+");
        
            for (String s : sMarker) {
                    if (comp.equals(s)) return true;
            }

            String sMarkerN[] = configTable.get("DoubleAnnotationTagsWithNumbers");//.split("\\s+");
            for (String s : sMarkerN) {
                    if (comp.startsWith(s) && comp.substring(s.length()).matches("^[0-9]+$")) return true;
            }
            return false;
	}
}

class TwoString {
    public String first, second;
    
    public TwoString(String a, String b) {
        this.first = a;
        this.second = b;
    }
}
