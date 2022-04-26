/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class QualRef extends Reference implements NamedRef {
	
	public QualRef(Reference ref, Identifier id, SourcePosition posn){
		super(posn);
		this.ref = ref;
		this.id  = id;
	}

	@Override
	public <A, R> R visit(Visitor<A, R> v, A o) {
		return v.visitQualRef(this, o);
	}

	public Reference ref;
	public Identifier id;

	@Override
	public Identifier getId() {
		return id;
	}

	@Override
	public boolean isQualified() {
		return true;
	}
}
