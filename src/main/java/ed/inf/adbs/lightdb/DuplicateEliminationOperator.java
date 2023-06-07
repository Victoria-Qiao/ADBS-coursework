package ed.inf.adbs.lightdb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.jsqlparser.JSQLParserException;


/**
 * The DuplicateEliminationOperator class is to eliminate duplicate tuples.
 * The tuples given the class is sorted using SortOperator.
 * 
 * @author s2041285
 *
 */

public class DuplicateEliminationOperator extends Operator{
	
	List<Tuple> tuples;
	int counter;
	
	/**
	 * DuplicateEliminationOperator() initializes a new Tuple List instance to store
	 * the tuples after duplicate elimination process, as well as set the counter to 0.
	 * counter is used in getNextTuple() method to return the tuple in the position specified
	 * by counter.
	 *  
	 * @throws IOException
	 * @throws JSQLParserException
	 */
	public DuplicateEliminationOperator() throws IOException, JSQLParserException {
		this.tuples = new ArrayList<Tuple>();
		counter = 0;
		
	}
	
	@Override
	/**
	 * open method calls its left child's open method, as well as get tuples from
	 * its left child (already sorted) and do duplicate elimination. 
	 */
	public void open() throws IOException, JSQLParserException {
		this.leftChild.open();
		Tuple t = this.leftChild.getNextTuple();
		
		while(t!=null) {
			/**
			 * First insert a tuple into empty list.
			 */
			if (this.tuples.size()==0) {
				
				this.tuples.add(t);
			}else {
				
				/**
				 * then for every latter tuple, compare it with the last tuple 
				 * already stored in the list. If they are the same, skip to the 
				 * next tuple, else insert it into the list. 
				 */
				int last = this.tuples.size();
				//size: the number of columns of a tuple.
				int size = this.tuples.get(last-1).values.size();
				int match = 1;
	
				for (int i=0; i<size;i++){
					if (!t.values.get(i).equals(this.tuples.get(last-1).values.get(i))) {
						match = 0;
						break;
					}
					
				}
				if (match == 0) {
					this.tuples.add(t);
				}
				
			}
			t = this.leftChild.getNextTuple();
		}

	}
	
	
	@Override
	/**
	 * close() method is to call its left child's close method.
	 */
	public void close() throws IOException {
		this.leftChild.close();
	}
	
	@Override
	/**
	 * getNextTuple() reads the tuple in the position specified in counter of
	 * the list and return it. If counter reaches the end of the list, return null.
	 */
	public Tuple getNextTuple() throws IOException, JSQLParserException {
		if (this.counter == this.tuples.size()) {
			return null;
		}
		return this.tuples.get(this.counter++);
		
	}
	
	@Override
	/**
	 * set the counter variable to 0, meaning the start of the duplicate elimination
	 * list.
	 */
	public void reset() throws IOException {
		this.counter = 0;
	}
	
	@Override
	public void dump() throws IOException, JSQLParserException {
		
	}
}
