package ed.inf.adbs.lightdb;

import java.io.IOException;
import java.util.List;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;


/**
 * The class is to select columns that is specified in SELECT clause. If it is 'SELECT *',
 * it means to select all the columns.
 * 
 * @author s2041285
 *
 */

public class ProjectOperator extends Operator {
	
	List<SelectItem> items;
	
	/**
	 * ProjectOperator constructor: to initialize select items specified in SELECT clause
	 * into List of SelectItem.
	 * @param items
	 */
	public ProjectOperator(List<SelectItem> items) {
		this.items = items;

	}
	
	@Override
	/**
	 * open() method is to call its left child's open method.
	 */
	public void open() throws IOException, JSQLParserException {

		this.leftChild.open();
		
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
	 * getNextTuple() reads the next tuple from its leftChild and returns the
	 * tuple with specified columns in SELECT clause. If it has no tuples to read, 
	 * it will return null.
	 */
	public Tuple getNextTuple() throws IOException, JSQLParserException {
		Tuple t = null;
		Tuple newt = null;
		t = this.leftChild.getNextTuple();

		if (t == null) {
			return null;
		}
		else {
			
			/**
			 * first initialize a new tuple instance for containing new selected 
			 * columns and corresponding values, then for the selected items, find
			 * corresponding columns and values in original tuple.  
			 */
			int i = 0;

			newt = new Tuple();
			
			for (SelectItem item : items) {
				String str = item.toString();

				String table = str.split("\\.")[0];
				String column = str.split("\\.")[1];
				
				i = 0;
				for (Column col : t.colNames) {
					
					if (col.getTable().getAlias() != null) {
						if (col.getColumnName().toString().equals(column) & 
								(col.getTable().getAlias().getName().equals(table) )) {
							newt.colNames.add(col);
							newt.values.add(t.values.get(i));
						}
						
					}else {
						if (col.getColumnName().toString().equals(column) & 
								col.getTable().getName().equals(table)) {
							newt.colNames.add(col);
							newt.values.add(t.values.get(i));
						}
						
					}
					i++;
				}
				
			}	
			
			
		}
		
		
		return newt;
		
	}
	
	@Override
	/**
	 * reset the ProjectOperator to initial state, to let it select 
	 * from the first tuple with specified columns when getNextTuple() 
	 * is called again.
	 */
	public void reset() throws IOException {
		this.leftChild.reset();
	}
	
	@Override
	/**
	 * used to test the implementation of ProjectOperator class
	 */
	public void dump() throws IOException, JSQLParserException {
		Tuple tuple = this.getNextTuple();	
		while (tuple != null) {

			tuple = this.getNextTuple();
			
		}
	}
}
