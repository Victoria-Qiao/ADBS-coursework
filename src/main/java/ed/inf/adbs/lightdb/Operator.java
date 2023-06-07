package ed.inf.adbs.lightdb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.schema.Column;


/**
 * Operator class is an abstract class, which needs to be implemented
 * for other specific Operator classes. The class contains variables
 * parent, leftChild, rightChild of Operator type, and methods need
 * to override such as open(), close(), getNextTuple(), reset() and
 * dump().
 * 
 * @author s2041285
 *
 */

public abstract class Operator {
	
	public Operator parent;
	public Operator leftChild;
	public Operator rightChild;
	

	public void open() throws IOException, JSQLParserException {
		
	}
	
	public void close() throws IOException {
		
	}
	
	public Tuple getNextTuple() throws IOException, JSQLParserException {
		return null;
		
	}
	
	public void reset() throws IOException {
		
	}
	
	public void dump() throws IOException, JSQLParserException {
		
	}
}
