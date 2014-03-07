/**
 * 
 */
package codemining.lm.grammar.tsg.pattern.tui;

import java.io.File;
import java.util.logging.Logger;

import codemining.lm.grammar.tsg.JavaFormattedTSGrammar;
import codemining.lm.grammar.tsg.pattern.PatternStatsCalculator;
import codemining.util.serialization.ISerializationStrategy.SerializationException;
import codemining.util.serialization.Serializer;

/**
 * TUI for computing the coverage of some TSG patterns.
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
public class StatsComputer {

	private static final Logger LOGGER = Logger.getLogger(StatsComputer.class
			.getName());

	public static void main(final String[] args) throws SerializationException {
		if (args.length != 4) {
			System.err
					.println("Usage <tsg> <directoryToComputeCoverage> <minPatternCountList> <minPatternSizeList>");
			System.exit(-1);
		}

		final JavaFormattedTSGrammar grammar = (JavaFormattedTSGrammar) Serializer
				.getSerializer().deserializeFrom(args[0]);
		final int[] minPatternCount = parseInt(args[2].split(","));
		final int[] minPatternSize = parseInt(args[3].split(","));

		final File directory = new File(args[1]);

		LOGGER.info("Finished loading, creating core structures");
		final PatternStatsCalculator pcc = new PatternStatsCalculator(
				grammar.getJavaTreeExtractor(), grammar, directory);

		LOGGER.info("Initiating stats computation...");
		pcc.printStatisticsFor(minPatternSize, minPatternCount);

	}

	static int[] parseInt(final String[] strVals) {
		final int[] vals = new int[strVals.length];
		for (int i = 0; i < strVals.length; i++) {
			vals[i] = Integer.parseInt(strVals[i]);
		}
		return vals;
	}
}