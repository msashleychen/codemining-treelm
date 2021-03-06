package codemining.ast.java;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;

import com.esotericsoftware.kryo.DefaultSerializer;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.google.common.collect.BiMap;

import ch.uzh.ifi.seal.changedistiller.structuredifferencing.StructureFinalDiffNode;
import codemining.ast.AbstractTreeExtractor;
import codemining.ast.AstNodeSymbol;
import codemining.ast.TreeNode;
import codemining.java.codeutils.JavaASTExtractor;
import codemining.java.tokenizers.JavaTokenizer;
import codemining.languagetools.ITokenizer;
import codemining.languagetools.ParseType;

/**
 * An abstract class representing the conversion of Eclipse's ASTNode to
 * ASTNodeSymbols and trees. It also includes the alphabet. Thread safe.
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
@DefaultSerializer(JavaSerializer.class)
public abstract class AbstractJavaTreeExtractor extends AbstractTreeExtractor {

	//private static final long serialVersionUID = -4515326266227881706L;
	private static final long serialVersionUID = 5024821311795558293L;

	public static final Function<Integer, String> JAVA_NODETYPE_CONVERTER = (Function<Integer, String> & Serializable) nodeType -> ASTNode
			.nodeClassForType(nodeType).getSimpleName();

	/**
	 * A node printer using the symbols.
	 */
	private transient final TreeToString javaNodeToString =
		node -> getSymbol(node.getData()).toString(JAVA_NODETYPE_CONVERTER);

	public AbstractJavaTreeExtractor() {
		super();
	}

	protected AbstractJavaTreeExtractor(final BiMap<Integer, AstNodeSymbol> alphabet) {
		super(alphabet);
	}

	/**
	 * Get the ASTNode given the tree.
	 *
	 * @param tree
	 * @return
	 */
	public abstract ASTNode getASTFromTree(final TreeNode<Integer> tree);

	@Override
	public String getCodeFromTree(final TreeNode<Integer> tree) {
		//Converts int tree to AST string using a NariveASTBuffer
		String nu = getASTFromTree(tree).toString();
		return nu;
	}

	@Override
	public TreeNode<Integer> getKeyForCompilationUnit() {
		for (final Entry<Integer, AstNodeSymbol> entry : nodeAlphabet.entrySet()) {
			if (entry.getValue().nodeType == ASTNode.COMPILATION_UNIT) {
				return TreeNode.create(entry.getKey(), entry.getValue().nChildProperties());
			}
		}
		
		// temp workaround?
		for (final Entry<Integer, AstNodeSymbol> entry : nodeAlphabet.entrySet()) {
			if (entry.getValue().nodeType == ASTNode.METHOD_DECLARATION) {
				return TreeNode.create(entry.getKey(), entry.getValue().nChildProperties());
			}
		}
		throw new IllegalStateException("A compilation unit must have been here...");
	}

	@Override
	public ITokenizer getTokenizer() {
		return new JavaTokenizer();
	}

	/**
	 * Get the tree from a given ASTNode
	 *
	 * @param node
	 * @return
	 */
	public abstract TreeNode<Integer> getTree(final ASTNode node);
	
	public abstract TreeNode<Integer> getChangeTree(final StructureFinalDiffNode node);

	/*
	 * (non-Javadoc)
	 *
	 * @see codemining.lm.grammar.tree.ITreeExtractor#getTree(java.io.File)
	 */
	@Override
	public TreeNode<Integer> getTree(final File f) throws IOException {
		final JavaASTExtractor astExtractor = new JavaASTExtractor(false);
		final ASTNode u = astExtractor.getAST(f);
		return getTree(u);
	}
	
	public TreeNode<Integer> getTree(final StructureFinalDiffNode d, final File f) throws IOException{
		final JavaASTExtractor astExtractor = new JavaASTExtractor(false);
		final ASTNode u = astExtractor.getAST(d,f,new HashSet<String>());
		return getTree(u);
	}
	
	public org.eclipse.jdt.core.dom.CompilationUnit getDistillerTree(final File f) throws IOException {
		final JavaASTExtractor astExtractor = new JavaASTExtractor(false);
		final org.eclipse.jdt.core.dom.CompilationUnit u = astExtractor.getAST(f);
		return u;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see codemining.lm.grammar.tree.ITreeExtractor#getTree(java.lang.String)
	 */
	@Override
	public TreeNode<Integer> getTree(final String code, final ParseType parseType) {
		final JavaASTExtractor astExtractor = new JavaASTExtractor(false);
		final ASTNode u = astExtractor.getAST(code, parseType);
		return getTree(u);
	}

	/**
	 * Return a map between the Eclipse ASTNodes and the TreeNodes. This may be
	 * useful for looking up patterns in a reverse order.
	 *
	 * @param node
	 * @return
	 */
	public abstract Map<ASTNode, TreeNode<Integer>> getTreeMap(final ASTNode node);

	/**
	 * Return the tree printer functor for this extractor.
	 *
	 * @return
	 */
	@Override
	public TreeToString getTreePrinter() {
		return javaNodeToString;
	}

}