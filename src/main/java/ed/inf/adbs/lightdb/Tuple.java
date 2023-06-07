package ed.inf.adbs.lightdb;

import java.util.ArrayList;
import java.util.List;

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.FromItem;

/**
 * Tuple class is used to store the column names and corresponding values
 * of a tuple in a table.
 * 
 * @author s2041285
 *
 */

public class Tuple {
	public List<Integer> values;
	public List<Column> colNames;

	/**
	 * Tuple constructor is used to initialize values in List of Integer type
	 * and column name in List of Column type. 
	 */
	public Tuple() {
		this.values = new ArrayList<Integer>();
		this.colNames = new ArrayList<Column>();

	}

}
