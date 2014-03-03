package usfm;
public class USFMSymbol {
	public SymbolType type;
	public String data;
	
	private static USFMSymbol _initial;
	public static USFMSymbol initial() {
		if (_initial == null) 
			return (_initial = new USFMSymbol(SymbolType.INITIAL, null, 0, 0));
		return _initial;
	}
	
	public int line, col;
	
	public USFMSymbol(SymbolType type, String data, int line, int col) {
		this.type=type;
		this.data=data;
		
		this.line = line;
		this.col=col;
	}
        
    @Override
        public String toString() {
            return "" + type + " (" + data.substring(0, Math.min(20,data.length())) + ") on line " + this.line + " column " + this.col;
                
        }
}
