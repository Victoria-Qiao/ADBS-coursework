package ed.inf.adbs.lightdb;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.SignedExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.Distinct;
//import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.OrderByElement;

/**
 * The QueryPlan class is to read in a PlainSelect object and retrieve different
 * elements in the query such as SelectItems from SELECT clause, Expression items
 * in WHERE clause, FromItem items and corresponding alias(if have) from FROM clause,
 * as well as DISTINCT object from DISTINCT clause and OrderByElement from ORDER BY
 * clause. Based on the items retrieved from the query, build a query tree with the
 * Operator classes to retrieve the data the query requires.
 * 
 * 
 * @author s2041285
 *
 */


public class QueryPlan {
	
	PlainSelect select;
	Expression where;
	FromItem from;
	Distinct distinct;
	List<Join> joins;
	List<SelectItem> items;
	Operator root = null;
	Operator parent = null;
	DbCatalog catalog = null;
	List<OrderByElement> orderByList = null;
	Map<FromItem, List<Expression>> exprs;
	List<Expression> joinExprs;
	List<FromItem> fromItems;
	
	String databaseDir;
	String inputFile;
	String outputFile;
		
	/**
	 * the QueryPlan constructor takes a PlainSelect object and DbCatalog object
	 * as parameters. PlainSelect object is the object from the query. The SelectItem,
	 * Expression, Join items, Select items are retrieved from the object. Moreover,
	 * whether to use order by and distinct information is also retrieved from the 
	 * plainselect item. In the constructor, Database catalog instance is also
	 * initialized to read schema information.
	 * 
	 * @param select
	 * @param catalog
	 */
	public QueryPlan(PlainSelect select, DbCatalog catalog) {
		this.select = select;
		this.where = select.getWhere();
		this.from = (FromItem)select.getFromItem();
		this.joins = select.getJoins();
		this.items = select.getSelectItems();
		this.orderByList = select.getOrderByElements();
		this.distinct = select.getDistinct();
	
		this.catalog = catalog;
		this.databaseDir = catalog.rootDir;

	}
	
	
	/**
	 * The createTree() class is to construct a tree of Operator objects based
	 * on the query got from query file. It sets the root and children of the
	 * tree. For Operators which have only one child, it sets the leftchild as
	 * its child and right child sets to null.
	 * 
	 * @throws IOException
	 * @throws JSQLParserException
	 */
	public void createTree() throws IOException, JSQLParserException {

		/**
		 * if the query has ORDER BY clause, then build a Sort Operator
		 */
		if (this.orderByList != null) {
			List<Expression> list = new ArrayList<Expression>();
			for (int i=0; i<this.orderByList.size(); i++) {
				list.add(this.orderByList.get(i).getExpression());
			}
			
			SortOperator sort = new SortOperator(list);

			if (root == null) {
				sort.parent = null;
				sort.leftChild = null;
				this.root = sort;
				this.parent = sort;
			}else {
				sort.leftChild = null;
				sort.rightChild = null;
				this.parent.leftChild = sort;
				this.parent = sort;
			}
			
		}
		
		/**
		 * if the query has DISTINCT clause, then build a 
		 * DuplicateEliminationOperator. Because the DuplicateEliminationOperator
		 * works based on sorted tuples, a sort operator is also built as the
		 * DuplicateEliminationOperator's child. And the sort is based on all items
		 * of the tuples from left to right.
		 */
		if (this.distinct != null) {
			
			DuplicateEliminationOperator dE = new DuplicateEliminationOperator();
			
			if (root == null) {
				dE.parent = null;
				dE.leftChild = null;
				dE.rightChild = null;
				this.root = dE;
				this.parent = dE;
			}else {
				dE.leftChild = null;
				dE.rightChild = null;
				this.parent.leftChild = dE;
				this.parent = dE;
				
			}
			
			/**
			 * duplicate elimination is based on sorted tuples. To achieve
			 * fully duplicate elimination, the sort should based on all columns
			 * whether or not it contains ORDER BY clause.
			 */
			
			SortOperator sort = new SortOperator("*");
			sort.leftChild = null;
			sort.rightChild = null;
			this.parent.leftChild = sort;
			this.parent = sort;
			
		}

		/**
		 * If the SELECT clause contains * which is all columns, then the
		 * project operator is not built. Else, if certain columns in SELECT
		 * clause is specified, then built project operator for the select items.
		 */
		if (items.isEmpty()==false & !(items.get(0) instanceof AllColumns)) {

			ProjectOperator project = new ProjectOperator(items);
			project.items = this.items;

			if (root == null)
			{
				project.parent = null;
				project.leftChild = null;
				project.rightChild = null;
				this.root = project;
				this.parent = project;
				
			}else {
				project.leftChild = null;
				project.rightChild = null;
				this.parent.leftChild = project;
				this.parent = project; 
			}
		}
		
		/**
		 * if the FROM clause has at least two table names, then use the join Operator
		 * to build tree to join the tables and based on other clauses to build children 
		 * (either ScanOperator or SelectOperator for each single table).
		 * If the FROM clause only has one table, then it will not use join.
		 */
		
		if (this.joins != null) {
			/**
			 * the exprs hashmap is to store the selection expressions for each
			 * FromItem in FROM clause. the expidx hash map is to store the position
			 * of each table name in FROM clause. The exprs and the expidx hash map
			 * uses Each table in FromItem class as keys.
			 * 
			 * the joinExp hashmap is to store join conditions for each table in FROM clause. 
			 * The keys for the joinExp are item positions in FROM clause in order.
			 * each integer key, stores a list of join expressions, and the integer key represents the
			 * positions of the tables in the join expressions have the largest position 
			 * value which is the key.
			 */
			this.exprs = new HashMap<FromItem, List<Expression>>();
			Map<FromItem,Integer> expidx = new HashMap<FromItem,Integer>();
			Map<Integer,List<Expression>> joinExp = new HashMap<Integer,List<Expression>>();
			this.joinExprs = new ArrayList<Expression>();
			this.exprs.put(from, new ArrayList<Expression>());
			this.fromItems = new ArrayList<FromItem>();
			
			/**
			 * convert the join items to FromItem type and add all the items in FROM clause
			 * to fromItems list, as well as put into exprs, expidx and joinExp hashmap.
			 */
			this.fromItems.add(from);
			expidx.put(from,0);
			joinExp.put(0, new ArrayList<Expression>());
			Integer k = 1;
			for (Join j : this.joins) {
				FromItem i = j.getRightItem();
				this.fromItems.add(i);
				this.exprs.put(i, new ArrayList<Expression>());
				joinExp.put(k, new ArrayList<Expression>());
				expidx.put(i,k);
				k++;
			}
			
			
			/**
			 * initialize a JoinDeparser instance for further join conditions deparsing.
			 * reargwhere means 'rearrange WHERE clause'.
			 */
			JoinDeparser reargwhere = new JoinDeparser(exprs,joinExprs,where);
			
			/**
			 * if the query has WHERE clause, then call evaluate method of JoinDeparser.
			 */
			if (this.where!=null)
			{
				reargwhere.evaluate(this.where);

			}
			
			/**
			 * create a list of operators to store operators for each table before join
			 * operation. The operators could be either ScanOperator or SelectOperator based
			 * on whether the From items have their own selection expressions.
			 */
			List<Operator> operators = new ArrayList<Operator>();

			for (FromItem i : this.fromItems) {
				// if the fromitem does not have selection expression, 
				// then create ScanOperator. Else, create SelectOperator.
				if (this.exprs.get(i).size()==0) {

					ScanOperator scan = new ScanOperator(this.databaseDir, i, this.catalog);
					operators.add(scan);
					
				}else {
					// to concatenate the expressions with AND and parsing them as 
					// an expression. The processed expression is fed into the SelectOperator
					// to retrieve tuples meet the expression.
					List<Expression> exps = this.exprs.get(i);
					Expression w = null;
					if (exps.size()==1) {
						w = exps.get(0);
					}else {
						String s = "";
						for (int j=0; j<exps.size(); j++) {
							if (j<exps.size()-1)
							{
								s += exps.get(j).toString() + " AND ";
							}else {
								s += exps.get(j);
							}
							
						}
						
						w = CCJSqlParserUtil.parseCondExpression(s);

					}

					SelectOperator select = new SelectOperator(w);
					ScanOperator scan = new ScanOperator(this.databaseDir, i, this.catalog);
					scan.parent = select;
					select.leftChild = scan;
					operators.add(select);
					
				}
			}
			
			/**
			 * for each join expression, retrieve the table names and get the
			 * corresponding positions in FROM clause. Then insert the join
			 * expression into the expression list with larger position key
			 * in the joinExp hashmap.
			 */
			for (Expression e : this.joinExprs) {
				Expression[] ele;
				ele = reargwhere.getElements(e);
				
				if ((ele[0] instanceof LongValue || ele[0] instanceof SignedExpression) & 
						(ele[1] instanceof LongValue || ele[1] instanceof SignedExpression)) {
					joinExp.get(1).add(e);
					
				} else {

					String leftTable = ((Column)ele[0]).getTable().getName();
					String rightTable = ((Column)ele[1]).getTable().getName();
					
					int leftidx = 0;
					int rightidx = 0;

					for (FromItem i: expidx.keySet()) {
						
						if (i.getAlias() != null) {
							if (leftTable.equals(i.getAlias().getName())) {
								leftidx = expidx.get(i);
							}else if (rightTable.equals(i.getAlias().getName())) {
								rightidx = expidx.get(i);

							}
						}else {
							if (leftTable.equals(((Table)i).getName())) {
								leftidx = expidx.get(i);
							}else if (rightTable.equals(((Table)i).getName())) {
								rightidx = expidx.get(i);
							}

						}
						
					}

					int maximum = Math.max(leftidx,rightidx);
					
					joinExp.get(maximum).add(e);
					
				}
				
			}
			
			/**
			 * join the first two tables in FROM clause. In hashmap joinExp,
			 * see if the second operator has join conditions. If so, concatenate
			 * the conditions and parse as a single expression as the join condition.
			 */
			Operator op1 = operators.get(0);
			Operator op2 = operators.get(1);

			JoinOperator joinp = null;
			JoinOperator join = null;
			if (joinExp.get(1).size()==0) {
				joinp = new JoinOperator(null,op1,op2);
				join = joinp;
				
			}else {
				String str = "";
				int index = 0;

				for (int i=0; i<joinExp.get(1).size(); i++) {
					if (index == joinExp.get(1).size()-1) {
						str += joinExp.get(1).get(i).toString();
					}else {
						str += joinExp.get(1).get(i).toString() + " AND ";
					}
					index++;
				}
				
				Expression w = CCJSqlParserUtil.parseCondExpression(str);
				joinp = new JoinOperator(w,op1,op2);
				join = joinp;
			}
			
			/**
			 * if the number of join elements are greater than 1, use the previous set
			 * joinOperator to join with following operators to form a new left-deep join
			 * tree.
			 */
			if (this.joins.size()>1) {
//				System.out.println(this.joins.size());
//				System.out.println(this.exprs.keySet().size());
				for (int i =2; i<this.exprs.keySet().size();i++) {
					Operator op = operators.get(i);
					
					String str = "";
					int index = 0;

					for (int j=0; j<joinExp.get(i).size(); j++) {
						if (index == joinExp.get(i).size()-1) {
							str += joinExp.get(i).get(j).toString();

						}else {
							str += joinExp.get(i).get(j).toString() + " AND ";
						}
						index++;
					}

					if (str == "") {
						joinp = new JoinOperator(null,joinp,op);
						join = joinp;
					}else {
						Expression w = CCJSqlParserUtil.parseCondExpression(str);
						joinp = new JoinOperator(w,joinp,op);
						join = joinp;
					}
					
					
				}
			}
			
			
			/**
			 * after doing all the join operations, if root exists, set the child of
			 * preset parent as the final join operator.
			 */
			if (this.root == null) {
				join.parent = null;
				this.root = join;
				this.parent = join;
				
			}else {
				join.parent = this.parent;
				this.parent.leftChild = join;
				this.parent = join;
			}
			
			
			
		}else {			
			
			if (where != null) {
				SelectOperator select = new SelectOperator(this.where);
				select.expression = this.where;
				select.parent = null;
				select.rightChild = null;
				select.leftChild = null;
				
				if (root == null) {
					
					select.leftChild = null;
					select.rightChild = null;
					select.parent = null;
					
					this.root = select;
					this.parent = select;
					
				}else {
					select.leftChild = null;
					select.rightChild = null;
					select.parent = parent;
					
					this.parent.leftChild = select;
					this.parent = select;
				}
				
			}
			
			if (from != null) {

				ScanOperator scan = new ScanOperator(this.databaseDir, this.from, this.catalog);
				if (root == null) {
					scan.leftChild = null;
					scan.rightChild = null;
					scan.parent = null;
					root = scan;
					parent = scan;
					
				}else {
					scan.leftChild = null;
					scan.rightChild = null;
					scan.parent = parent;
					parent.leftChild = scan;
					parent = scan;
				}
			}
			
		}
	
	}
	
	/**
	 * runTree() method is to call getNextTuple() method of the root to get the
	 * tuple satisfying the query.
	 * @return tuple
	 * @throws IOException
	 * @throws JSQLParserException
	 */
	public Tuple runTree() throws IOException, JSQLParserException {
		Tuple tuple;
		tuple = root.getNextTuple();

		return tuple;
	}
	
	
	/**
	 * dump method takes bufferedwriter instance, called runTree() to get all tuples
	 * satisfying the query and write to the file that bufferedwrite instance specified.
	 * @param out
	 * @throws IOException
	 * @throws JSQLParserException
	 */
	public void dump(BufferedWriter out) throws IOException, JSQLParserException {
		root.open();
		Tuple t = runTree();
		
		while (t!=null) {
			String str = "";
			for (int i=0; i<t.values.size();i++) {
				if (i < t.values.size()-1) {
					str += t.values.get(i).toString()+",";
				}else {
					str += t.values.get(i).toString();
				}
			}
			out.write(str);
			out.newLine();
			System.out.println(t.values.toString());
			t = runTree();
		}
		out.close();
	}
	
	
}
