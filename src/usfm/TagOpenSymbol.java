package usfm;


public class TagOpenSymbol extends USFMSymbol {
	public boolean spaced = false;
		
	public TagOpenSymbol(String data, boolean spaced, int line, int col) {
		super(SymbolType.TAG_OPEN, data, line, col);
		this.spaced = spaced;
	}
	
}
