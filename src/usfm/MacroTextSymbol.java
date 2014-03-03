package usfm;
public class MacroTextSymbol extends TextSymbol {
	public String macro;
	
	public MacroTextSymbol(String macro, String data, int line, int col) {
		super(data, line, col);
		this.macro = macro;
	}

}
