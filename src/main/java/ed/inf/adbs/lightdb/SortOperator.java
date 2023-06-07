package ed.inf.adbs.lightdb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.OrderByElement;

/**
 * This class is to sort the tuples based on columns in ORDER BY
 * clause in ascending order. The sorting is in the order of the
 * columns specified in ORDER BY clause.
 * 
 * @author s2041285
 *
 */

public class SortOperator extends Operator{
	
	List <Expression> orderByList;
	List<Tuple> tuples;
	List<Integer> orderByPos;
	int counter;
	int sortAll = 0;
	
	/**
	 * SortOperator constructor is to initialize the order by elements
	 * the ORDER BY clause specified, a tuple list instance to contain
	 * tuples from its child and a list of integer which represent the 
	 * position of each column in orderby list, which is convenient for 
	 * further comparison. 
	 * 
	 * Counter is used to record the tuple position that getNextTuple() 
	 * needs to read. Initially it is set to 0 for the first tuple.
	 * 
	 * @param orderByList
	 */
	public SortOperator(List<Expression> orderByList) {
		this.orderByList = orderByList;
		this.tuples = new ArrayList<Tuple>();
		this.orderByPos = new ArrayList<Integer>();
		this.counter = 0;
		
	}
	
	/**
	 * the constructor is for DISTINCT operation to sort all tuples.
	 * in this constructor, sortAll is set to 1.
	 * @param s
	 */
	public SortOperator(String s){
		this.tuples = new ArrayList<Tuple>();
		this.orderByPos = new ArrayList<Integer>();
		this.counter = 0;
		this.sortAll = 1;
		
	}
	
	
	public List<Tuple> getSortedTuples(){
		return tuples;
	}
	
	/**
	 * open() method is to call its left child's open method,
	 * as well as get the tuples from its left child and store them into
	 * 'tuples' list. Then use Collections.sort to sort the tuples based on
	 * the order of sorting columns specified in ORDER BY clause.
	 */
	@Override
	public void open() throws IOException, JSQLParserException {
		this.leftChild.open();
		
		Tuple t = this.leftChild.getNextTuple();

		while (t != null) {
			this.tuples.add(t);
			t = this.leftChild.getNextTuple();
		}
		
		if (tuples.size()!=0) {
			Tuple tuple = tuples.get(0);
			
			/**
			 *  find positions of ORDER BY columns in original tuples, for DISTINCT
			 *  operation, it needs to sort tuples based on all columns. Therefore,
			 *  for such status, the sortAll parameter is set to 1.
			 */
			
			if (this.sortAll == 1){
				for (int i=0; i<tuple.colNames.size(); i++) {
					orderByPos.add(i);
				}
			}else {
				for (int j=0; j<orderByList.size(); j++) {
					String orderByName = orderByList.get(j).toString();
					
					for (int i=0; i<tuple.colNames.size(); i++){
//						System.out.println(tuple.colNames.get(i).toString());
						if (orderByName.equals(tuple.colNames.get(i).toString())) {	
							orderByPos.add(i);
						}
					}
				}
			}
			
			
			
		}
		
		// sort the tuples based on ORDER BY columns
		Collections.sort(this.tuples, new Comparator<Tuple>(){

			@Override
			public int compare(Tuple o1, Tuple o2) {
				int k = 0;
				// sort tuples in the specified column order
				// in ascending order.
				for (int i=0; i<orderByPos.size(); i++) {
					int index = orderByPos.get(i);
					if (o1.values.get(index) < o2.values.get(index)) {
						k = -1;
						break;
					}
					else if (o1.values.get(index) > o2.values.get(index)){
						k = 1;
						break;
					}
					else {
						continue;
					}
				}
				
				return k;
			}
			
		});
		
	}
	
	/**
	 * close() method is to call its left child's close method.
	 */
	@Override
	public void close() throws IOException {
		this.leftChild.close();
	}
	
	/**
	 * return the tuple stored in the tuple list of position of counter.
	 * @return Tuple
	 */
	@Override
	public Tuple getNextTuple() throws IOException, JSQLParserException {

		if (this.counter == this.tuples.size()) {
			return null;
		}
		return this.tuples.get(this.counter++);
		
	}
	
	/**
	 * set the counter value to 0, meaning the first element in tuple list.
	 */
	@Override
	public void reset() throws IOException {
		this.counter = 0;
	}
	
	@Override
	public void dump() throws IOException, JSQLParserException {
		
	}
}
