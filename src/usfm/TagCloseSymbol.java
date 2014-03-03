package usfm;


public class TagCloseSymbol extends USFMSymbol {
	public boolean spaced = false;
		
	public TagCloseSymbol(String data, boolean spaced, int line, int col) {
		super(SymbolType.TAG_CLOSE, data, line, col);
		this.spaced = spaced;
	}
	
}
