package ed.inf.adbs.lightdb;

import java.io.IOException;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.select.FromItem;

/**
 * The class is to select tuples based on conditions in WHERE clause.
 * If matched,return the tuple. Otherwise, search for the next matching 
 * tuple and return null if the scan file reaches to the end.
 * 
 * @author s2041285
 *
 */


public class SelectOperator extends Operator {
	
	public Expression expression;
	public Paser parser;
	public Expression condition;
	
	/**
	 * SelectOperator constructor: to initialize the predicate expression
	 * for selecting tuples that satisfying it.
	 * @param where
	 */
	
	public SelectOperator(Expression where) {
		this.condition = where;
	}
	
	/**
	 * open() method is to call its left child's open method,
	 * as well as set new expression parser instance (called class Paser) for
	 * further processing of expression.
	 */
	public void open() throws IOException, JSQLParserException {
		this.leftChild.open();
		parser = new Paser(); 
	}
	
	/**
	 * close() method is to call its left child's close method.
	 */
	public void close() throws IOException {
		this.leftChild.close();
	}
	
	
	/**
	 * first get the tuple from its left child, then send it to
	 * expression parser together with the expression to see if 
	 * it matches the expression. If so, return the tuple. Else,
	 * read the next tuple and see if it matches. If there is no
	 * tuple to be judged, return null.
	 * 
	 * @return Tuple tuple
	 */
	public Tuple getNextTuple() throws IOException, JSQLParserException{
		
		
		boolean ifmatch = true;
		Tuple tuple = this.leftChild.getNextTuple();
		
		while (tuple!=null) {
			ifmatch = parser.eval(this.condition, tuple);

			if (ifmatch == true) {
				return tuple;
			}else {
				tuple = this.leftChild.getNextTuple();
			}
		}

		return tuple;
		
	}
	
	@Override
	/**
	 * reset the SelectOperator to initial state, to let it select 
	 * from the first matching tuple again.
	 */
	public void reset() throws IOException {
		this.leftChild.reset();
	}
	
	@Override
	/**
	 * used to test the implementation of SelectOperator class
	 */
	public void dump() throws JSQLParserException, IOException {
		Tuple tuple = this.getNextTuple();	
		while (tuple != null) {
			
			tuple = this.getNextTuple();
			
		}
	}
	
}
