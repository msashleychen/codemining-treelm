/**
 * 
 */
package codemining.lm.grammar.tsg.pattern.tui;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NodeFinder;

import codemining.java.codeutils.JavaASTExtractor;
import codemining.lm.grammar.tree.AbstractJavaTreeExtractor;
import codemining.lm.grammar.tree.TreeNode;
import codemining.lm.grammar.tsg.JavaFormattedTSGrammar;
import codemining.lm.grammar.tsg.TSGNode;
import codemining.lm.grammar.tsg.pattern.PatternExtractor;
import codemining.lm.grammar.tsg.pattern.PatternStatsCalculator;
import codemining.util.serialization.ISerializationStrategy.SerializationException;
import codemining.util.serialization.Serializer;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * Get a file containing the Deckard clones, parse it and find the overlap with
 * the TSG.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class DeckardClonesInTsg {

	public static Pattern decardClone = Pattern
			.compile("[0-9]{9}\\tdist:0\\.0\\tFILE\\s(\\S+\\.java)\\sLINE:([0-9]+)\\:([0-9]+)");

	/**
	 * Convert the ASTNodes in the collection to our format.
	 */
	private static Set<TreeNode<Integer>> convertNodes(
			final Collection<ASTNode> nodes,
			final AbstractJavaTreeExtractor javaTreeExtractor) {
		final Set<TreeNode<Integer>> treeNodes = Sets.newIdentityHashSet();

		for (final ASTNode node : nodes) {
			treeNodes.add(javaTreeExtractor.getTree(node));
		}

		return treeNodes;
	}

	private static Set<TreeNode<Integer>> getClonePatterns(
			final Multimap<Integer, ASTNode> decardClones,
			final AbstractJavaTreeExtractor javaTreeExtractor) {
		final Set<TreeNode<Integer>> decardPatterns = Sets.newHashSet();

		for (final int key : decardClones.keySet()) {
			final Collection<ASTNode> nodes = decardClones.get(key);
			final Set<TreeNode<Integer>> convertedNodes = convertNodes(nodes,
					javaTreeExtractor);

			// then get the maximal common subtree
			final Iterator<TreeNode<Integer>> nodeIterator = convertedNodes
					.iterator();
			TreeNode<Integer> maximumOverlappingNode = nodeIterator.next();
			while (nodeIterator.hasNext()) {
				final Optional<TreeNode<Integer>> maximalOverlappingTree = maximumOverlappingNode
						.getMaximalOverlappingTree(nodeIterator.next());
				if (maximalOverlappingTree.isPresent()) {
					maximumOverlappingNode = maximalOverlappingTree.get();
				}
			}

			// and push it into decardPatterns
			decardPatterns.add(maximumOverlappingNode);
		}

		return decardPatterns;
	}

	private static Multimap<Integer, ASTNode> getClonesFromDecard(
			final String clusterFile, final File baseDirectory)
			throws IOException {
		final List<String> lines = FileUtils.readLines(new File(clusterFile));
		int id = 0;
		final Multimap<Integer, ASTNode> nodesInCluster = ArrayListMultimap
				.create();

		final JavaASTExtractor astExtractor = new JavaASTExtractor(false);

		for (final String line : lines) {
			if (line.length() == 0) {
				id++;
				continue;
			}
			final Matcher matcher = decardClone.matcher(line);
			checkArgument(matcher.find());
			final String filename = matcher.group(1);
			final int startingLine = Integer.parseInt(matcher.group(2));
			final int offestLine = Integer.parseInt(matcher.group(3));

			final File targetFile = new File(baseDirectory.getAbsolutePath()
					+ "/" + filename);
			final CompilationUnit fileAst = astExtractor.getAST(targetFile);
			final int start = fileAst.getPosition(startingLine, 0);
			final int end = fileAst.getPosition(startingLine + offestLine - 1,
					0);

			final ASTNode cloneNode = NodeFinder.perform(fileAst, start, end
					- start);
			nodesInCluster.put(id, cloneNode);
		}

		return nodesInCluster;
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws SerializationException
	 */
	public static void main(final String[] args) throws IOException,
			SerializationException {
		if (args.length != 4) {
			System.err
					.println("Usage <decardCloneClustersFile> <baseDir> <tsg> <testDir>");
			System.exit(-1);
		}

		final Multimap<Integer, ASTNode> decardClones = getClonesFromDecard(
				args[0], new File(args[1]));

		// Read tsg
		final JavaFormattedTSGrammar grammar = (JavaFormattedTSGrammar) Serializer
				.getSerializer().deserializeFrom(args[2]);
		final Set<TreeNode<Integer>> decardCloneTrees = getClonePatterns(
				decardClones, grammar.getJavaTreeExtractor());

		// Now check how many of them we have in our TSG
		final Set<TreeNode<TSGNode>> patterns = PatternExtractor
				.getTSGPatternsFrom(grammar, 0, 0);
		final Set<TreeNode<Integer>> intPatterns = Sets.newHashSet();
		for (final TreeNode<TSGNode> tree : patterns) {
			intPatterns.add(TSGNode.tsgTreeToInt(tree));
		}

		final Set<TreeNode<Integer>> common = Sets.intersection(
				decardCloneTrees, intPatterns).immutableCopy();
		System.out.println("Common:" + common.size());
		final double pct = ((double) common.size()) / patterns.size();
		System.out.println("PctCommon:" + pct);

		final PatternStatsCalculator psc = new PatternStatsCalculator(
				grammar.getJavaTreeExtractor(), decardCloneTrees, new File(
						args[3]));

		final int[] zeroArray = { 0 };
		psc.printStatisticsFor(zeroArray, zeroArray);

	}

}
