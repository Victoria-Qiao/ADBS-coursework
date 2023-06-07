package ed.inf.adbs.lightdb;

import java.io.IOException;
import java.util.List;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;


/**
 * The class is to join two set of tuples from two Operators (either ScanOperator
 * or SelectOperator). 
 * 
 * 
 * @author s2041285
 *
 */

public class JoinOperator extends Operator{
	Expression joinCondition;
	Paser conditionParser;
	
	Tuple leftTuple;
	Tuple rightTuple;
	
	int start = 0;
	
	
	/**
	 * The JoinOperator takes joinCondition, left child and right child as arguments,
	 * and initialize joinCondition, left child and right child.
	 * left child and right child are in the order from left to right specified in
	 * FROM clauses. join condition is the expression in WHERE clause which contains
	 * the table names of the two children. 
	 * Moreover, it initialize a new parser for selection.
	 * 
	 * @param joinCondition
	 * @param leftC
	 * @param rightC
	 * @throws IOException
	 * @throws JSQLParserException
	 */
	public JoinOperator(Expression joinCondition, Operator leftC, Operator rightC) throws IOException, JSQLParserException {
		this.joinCondition = joinCondition;
		this.conditionParser = new Paser();
		this.leftChild = leftC;
		this.rightChild = rightC;
//		System.out.println(joinCondition);
		
	}
	
	@Override
	/**
	 * open() method is to call its left child's open method hierarchically
	 */
	public void open() throws IOException, JSQLParserException {
		this.leftChild.open();
		this.rightChild.open();
	}
	
	@Override
	/**
	 * getNextTuple() gets the join table from its left child and right child.
	 * It uses left child as outer loop and inner child as outer loop, then
	 * create a new tuple which combines the values and column names. 
	 * If the join condition of the new tuples are met, then return it. Otherwise
	 * find the tuple until meet the condition. If run out of the two tables, return
	 * null.
	 */
	public Tuple getNextTuple() throws IOException, JSQLParserException {
		Tuple newt = null;

		int match = 0;
		
		/**
		 * if just start to join for the two tuples, get one tuple at each child.
		 * and set start to 1.
		 */
		
		if (this.start == 0) {
			this.leftTuple = this.leftChild.getNextTuple();
			this.start = 1;
			this.rightTuple = this.rightChild.getNextTuple();
		}
		else {
			this.rightTuple = this.rightChild.getNextTuple();
		}

		while (match==0) {
			if (this.leftTuple == null) {
				return null;
			}
			else {
				if (this.rightTuple == null) {
					this.leftTuple = this.leftChild.getNextTuple();
					this.rightChild.reset();
					this.rightTuple = this.rightChild.getNextTuple();
				}
				else {
					newt = new Tuple();
					for (int i=0; i<this.leftTuple.values.size(); i++) {
						Column c = this.leftTuple.colNames.get(i);
						int v = this.leftTuple.values.get(i);
						newt.colNames.add(c);
						newt.values.add(v);
					}
					
					for (int i=0; i<this.rightTuple.values.size(); i++) {
						Column c = this.rightTuple.colNames.get(i);
						int v = this.rightTuple.values.get(i);
						newt.colNames.add(c);
						newt.values.add(v);
					}
					
					if (joinCondition != null) {

						boolean ifmatch = this.conditionParser.eval(joinCondition, newt);
						if (ifmatch == true) {
							match = 1;
						}else {
							newt = null;
							this.rightTuple = this.rightChild.getNextTuple();
							continue;
						}	
					}
					else {
						return newt;
					}
					
				}
				
			}
		}

		return newt;
		
	}
	
	@Override
	/**
	 * reset the JoinOperator to initial state, to let it back to
	 * the first tuple again.
	 */
	public void reset() throws IOException {
		this.leftChild.reset();
		this.rightChild.reset();
		start = 0;
	}
	
	@Override
	/**
	 * close() method is to call its left child's close method.
	 */
	public void close() throws IOException {
		this.leftChild.close();
		this.rightChild.close();
	}
	
	@Override
	/**
	 * used to test the implementation of SelectOperator class
	 */
	public void dump() throws IOException, JSQLParserException {
		Tuple t = this.getNextTuple();
		while(t != null) {
			t = this.getNextTuple();
		}
	}
}
