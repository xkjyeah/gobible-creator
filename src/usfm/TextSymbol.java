package usfm;
public class TextSymbol extends USFMSymbol {
	public boolean EOLAfter = false;
	
	public TextSymbol(String data, int line, int col) {
		super(SymbolType.TEXT, data, line, col);
	}

}
