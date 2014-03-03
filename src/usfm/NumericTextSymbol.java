package usfm;
public class NumericTextSymbol extends TextSymbol {
	public int number;
	
	public NumericTextSymbol(String data, int line, int col) {
		super(data, line, col);
		
		number=Integer.parseInt(data.trim());
	}

}
