package ed.inf.adbs.lightdb;

import java.util.ArrayList;

import java.util.List;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.SignedExpression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.parser.SimpleNode;
import net.sf.jsqlparser.schema.Column;
import java.util.Stack;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;

/**
 * The Paser class is used to process the expression and judge
 * if a tuple satisfies the expression. Originally I want to set
 * the class name to 'Parser' but I realized I made a typo later
 * and used the name in many places, then I decided not to change
 * it :)
 * 
 * @author s2041285
 *
 */


public class Paser {
	public Tuple tuple;
	public Expression expression;
	public ExpressionDeParser expressionDeParser;
	public boolean match = true; // the variable is to judge if the tuple matches the expression.

	/**
	 * Paser constructor to initialize ExpressionDeParser for further processing
	 * expressions.
	 */
	
	public Paser() {
		
		this.expressionDeParser = new ExpressionDeParser();
	}
	
	
	/**
	 * the api used to see if a tuple matches the selection expression. If matched,
	 * return true, else return false for not matching. It requires parameters
	 * expression and the tuple need to be judged.
	 * 
	 * @param exp
	 * @param tuple
	 * @return boolean match
	 * @throws JSQLParserException
	 */
	
	public boolean eval(Expression exp, Tuple tuple) throws JSQLParserException {
		match = true;
		this.expression = exp;
		this.tuple = tuple;
		
		evaluate(this.expression);

		return match;
		
	}
	
	/**
	 * To get the value of Expression e which could either represent a value of a
	 * specified column or a value of LongValue type and turn it into int type.
	 * 
	 * @param Expression e -- single expression of column name such as Sailors.A or
	 * expression of LongValue representing a number such as 37.
	 * @return value of int type.
	 */
	
	public int getValue(Expression e) {
		String column = null;
		String table = null;
		String alias = null;
		int value = 0;
		
		/**
		 * If the expression is a column name with table name, get value based on the 
		 * table and column names and return the value. Else, the expression is of LongValue 
		 * or SignedExpression type, then turn it into integer and return it.
		 */
		if (e instanceof Column) {
//			System.out.println(e);
			column = ((Column)e).getColumnName();
			table = ((Column)e).getTable().getName();
			
			int index = -1;
			
			for (Column c:this.tuple.colNames) {
				/**
				 * if table has alias, to match alias with the table name in
				 * expression.
				 */
				
				if (c.getTable().getAlias()!=null)
				{
					alias = c.getTable().getAlias().getName();
					if (column.equals(c.getColumnName()) & 
							alias.equals(table)) {
						index = this.tuple.colNames.indexOf(c);
						
					}
				}else {
					if (column.equals(c.getColumnName()) & 
							c.getTable().getName().equals(table)){
						index = this.tuple.colNames.indexOf(c);
					}
				}
				
			}
			
			value = this.tuple.values.get(index);
		}else {
			
			value = Integer.parseInt(e.toString());
		}

		return value;
	}
	
	
	
	/**
	 * the evaluate method is to evaluate whether the tuple matches the expression. 
	 * By using ExpressionDeParser(), it could break the expression into small expressions. 
	 * The method evaluates =, !=, >, >=, <,<=, and AndExpression. It first 
	 * recognizes the expression type, if it is predicate of the six types above, 
	 * then it will get the left and right int values, and do comparison. If 
	 * the values match the predicates then it is true or false otherwise. The final 
	 * decision of match is the conjunction of all the boolean values of the expressions.
	 * 
	 * @param expr
	 * @throws JSQLParserException
	 */
	
	public void evaluate(Expression expr) throws JSQLParserException {
		
		expressionDeParser = new ExpressionDeParser() {
			@Override
			public void visit(AndExpression andExpression) {
				super.visit(andExpression);
				
			}
			
			@Override
			public void visit(EqualsTo equalsTo) {
				super.visit(equalsTo);
				Expression lefte = ((EqualsTo) equalsTo).getLeftExpression();
				Expression righte = ((EqualsTo) equalsTo).getRightExpression();
				int left = getValue(lefte);
				int right = getValue(righte);
				
				if (left==right) {
					match = true & match;
				}else {
					match = false & match;
				}

			}

			@Override
			public void visit(NotEqualsTo notEqualsTo) {
				super.visit(notEqualsTo);
				
				Expression lefte = ((NotEqualsTo) notEqualsTo).getLeftExpression();
				Expression righte = ((NotEqualsTo) notEqualsTo).getRightExpression();
				int left = getValue(lefte);
				int right = getValue(righte);
				
				if (left!=right) {
					match = true & match;
				}else {
					match = false & match;
				}
				
			}
	    
			@Override
			public void visit(GreaterThan greaterThan) {
				super.visit(greaterThan);
				
				Expression lefte = ((GreaterThan) greaterThan).getLeftExpression();
				Expression righte = ((GreaterThan) greaterThan).getRightExpression();
				int left = getValue(lefte);
				int right = getValue(righte);
				
				if (left>right) {
					match = true & match;
				}else {
					match = false & match;
				}

			}
			
			@Override
			public void visit(GreaterThanEquals greaterThanEquals) {
				super.visit(greaterThanEquals);
				
				Expression lefte = ((GreaterThanEquals) greaterThanEquals).getLeftExpression();
				Expression righte = ((GreaterThanEquals) greaterThanEquals).getRightExpression();
				int left = getValue(lefte);
				int right = getValue(righte);
				
				if (left>=right) {
					match = true & match;
				}else {
					match = false & match;
				}

			}
			
			@Override
			public void visit(MinorThan minorThan) {
				super.visit(minorThan);
				
				Expression lefte = ((MinorThan) minorThan).getLeftExpression();
				Expression righte = ((MinorThan) minorThan).getRightExpression();
				int left = getValue(lefte);
				int right = getValue(righte);
				
				if (left<right) {
					match = true & match;
				}else {
					match = false & match;
				}
				
			}
			
			@Override
			public void visit(MinorThanEquals minorThanEquals) {
				super.visit(minorThanEquals);
				
				Expression lefte = ((MinorThanEquals) minorThanEquals).getLeftExpression();
				Expression righte = ((MinorThanEquals) minorThanEquals).getRightExpression();
				int left = getValue(lefte);
				int right = getValue(righte);
				
				if (left<=right) {
					match = true & match;
				}else {
					match = false & match;
				}

			}
			
			
		};

		expr.accept(expressionDeParser);
	    
	}
	
	

}
	
