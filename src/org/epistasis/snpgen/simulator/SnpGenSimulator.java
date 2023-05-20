package org.epistasis.snpgen.simulator;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;

import javax.swing.JProgressBar;

import org.epistasis.snpgen.document.*;
import org.epistasis.snpgen.document.SnpGenDocument.*;
import org.epistasis.snpgen.exception.InputException;
import org.epistasis.snpgen.exception.ProcessingException;

public class SnpGenSimulator
{
	private static final double kErrorLimit = 0.01D;
	private static final int kMajorMajor = 0;
	private static final int kMajorMinor = 1;
	private static final int kMinorMinor = 2;
	private static final int[] kAlleleSymbols = {kMajorMajor, kMajorMinor, kMinorMinor};
	
	private static class PenetranceTablePopulation
	{
		public int currentTableCount;
		public PenetranceTable[] tables;
		
		public PenetranceTablePopulation(int inTableCount)
		{
			tables = new PenetranceTable[inTableCount];
		}
	}
	
	public static class PenetranceTableQuantile
	{
		public PenetranceTable[] tables;
		
		public PenetranceTableQuantile(int inTableCount)
		{
			tables = new PenetranceTable[inTableCount];
		}
	}
	
	public interface ProgressHandler
	{
		public void setMaximum(int inMax);
		public void setValue(int inMax);
	}

	private Random random = new Random();
	private PenetranceTableQuantile[] penetranceTableQuantiles;
	private SnpGenDocument document;
	
	private int tablePopulationCountFound;
	
	public SnpGenSimulator()
	{
//		random.setSeed(1);
	}
	
	public void setDocument(SnpGenDocument inDoc)
	{
		document = inDoc;
	}
	
//	public void setProgressHandler(ProgressHandler progressHandler)
//	{
//		this.progressHandler = progressHandler;
//	}
	
	public PenetranceTableQuantile[] getPenetranceTableQuantiles()
	{
		return penetranceTableQuantiles;
	}
	
	private static Random createRandom(Integer inSeed)
	{
		Random outRandom = new Random();
		setRandomSeed(outRandom, inSeed);
		return outRandom;
	}
	
	private static void setRandomSeed(Random inRandom, Integer inSeed)
	{
		if(inSeed != null)
			inRandom.setSeed(inSeed);
	}
	
	public void setRandomSeed(Integer inSeed)
	{
		setRandomSeed(random, inSeed);
	}
	
	public int getTablePopulationCountFound()
	{
		return tablePopulationCountFound;
	}

	public void setTablePopulationCountFound(int tablePopulationCountFound)
	{
		this.tablePopulationCountFound = tablePopulationCountFound;
	}

	public double[][] generateTablesForModels(ArrayList<DocModel> modelList, int desiredQuantileCount, int inDesiredPopulationCount, int inTryCount, ProgressHandler inProgressHandler) throws Exception
	{
		System.out.println("Generating models...");
		int modelCount = modelList.size();
		
		double[][] allTableScores = new double[modelCount][];
		for(int whichModel = 0; whichModel < modelCount; ++whichModel)
		{
			int progressValueBase = whichModel * inDesiredPopulationCount;
			DocModel model = modelList.get(whichModel);
			allTableScores[whichModel] = generateTablesForOneModel(model, desiredQuantileCount, inDesiredPopulationCount, inTryCount, inProgressHandler, progressValueBase);
		}
		System.out.println("Done generating models.");
		return allTableScores;
	}
	
	// Returns the number of tables found, from which the desired quantiles were chosen.
	private double[] generateTablesForOneModel(DocModel model, int desiredQuantileCount, int inDesiredPopulationCount, int inTryCount, ProgressHandler inProgressHandler, int inProgressValueBase) throws Exception
	{
		double[] outAllTableScores;
		
		PenetranceTable[] tables = generatePenetranceTables(model, inDesiredPopulationCount, inTryCount, inProgressHandler, inProgressValueBase);
		int tableCount = tables.length;
		if(tableCount < desiredQuantileCount)
			throw new ProcessingException("Unable to generate desired number of table quantiles");
		
		outAllTableScores = new double[tableCount];
		for(int i = 0; i < tableCount; ++i)
		{
			outAllTableScores[i] = tables[i].getQuantileScore(model.getUseOddsRatio());
		}
		
		tablePopulationCountFound = tableCount;
		
		selectPenetranceTablesRepresentativesUniformly(desiredQuantileCount, tables, model);
		return outAllTableScores;
	}
	
	// inAllTableScores[eachModel][eachScore]
	public void writeTablesAndScoresToFile(ArrayList<DocModel> modelList, double[][] inAllTableScores, int quantileCount, File destFile) throws IOException
	{
		if(destFile != null)
		{
			int modelCount = modelList.size();
			
			File directory = destFile.getParentFile();
			String baseFilename = destFile.getName();
			if(baseFilename.toLowerCase().endsWith(".txt"))
				baseFilename = baseFilename.substring(0, baseFilename.length() - 4);
			
			boolean useEdmForAnyModel = false;
			boolean useOddsRatioForAnyModel = false;
			for(DocModel m: document.modelList)
			{
				useOddsRatioForAnyModel |= m.getUseOddsRatio();
				useEdmForAnyModel |= !m.getUseOddsRatio();
			}
			
			String scoreName = null;
			if(useEdmForAnyModel && useOddsRatioForAnyModel)
				scoreName = "Mixed";
			else if(useEdmForAnyModel)
				scoreName = "EDM";
			else if(useOddsRatioForAnyModel)
				scoreName = "OddsRatio";
			assert scoreName != null;
			
			// Write scores to the output file, and calculate tablePopulationCountFoundMinimum:
			Integer tablePopulationCountFoundMinimum = null;
			File scoreFile = new File(directory, baseFilename + "_" + scoreName + "_Scores.txt");
			PrintWriter scoreStream = new PrintWriter(new FileWriter(scoreFile));
			for(int whichModel = 0; whichModel < modelCount; ++whichModel)
			{
				double[] populationScores = inAllTableScores[whichModel];
				int tablePopulationCount = populationScores.length;
				if(tablePopulationCountFoundMinimum == null || tablePopulationCount < tablePopulationCountFoundMinimum)
					tablePopulationCountFoundMinimum = tablePopulationCount;
				scoreStream.println(scoreName + " scores for model: " + whichModel);
				for(int i = 0; i < tablePopulationCount; ++i)
				{
					scoreStream.println(populationScores[i]);
				}
				scoreStream.println();
			}
			scoreStream.close();
			
			// Write penetrance tables to the output file:
			File tablesFile = new File(directory, baseFilename + "_models.txt");
			PrintWriter tableStream = null;
			try
			{
				tableStream = new PrintWriter(new FileWriter(tablesFile, false));
				tableStream.println("Selected " + quantileCount + " " + scoreName + " quantiles from a population of " + tablePopulationCountFoundMinimum + " tables.");
			}
			finally
			{
				if (tableStream != null)
					tableStream.close();
			}
			
			for(int q = 0; q < quantileCount; ++q)
			{
				for(int m = 0; m < modelCount; ++m)
				{
					DocModel model = modelList.get(m);
					PenetranceTable[] tables = model.getPenetranceTables();
					if(tables.length > q)
					{
						tables[q].saveToFile(tablesFile, true, false);
					}
				}
			}
		}
	}
	
	public void combineModelTablesIntoQuantiles(ArrayList<DocModel> modelList, File[] inputFiles, float[] inputFileFractions) throws Exception
	{
		PenetranceTableQuantile[] quantiles1 = null;
		PenetranceTableQuantile[] quantiles2 = null;
		
		int modelCount = modelList.size();
		Integer quantileCount1 = null;
		Integer quantileCount2 = null;

		if(modelCount > 0)
		{
			quantileCount1 = modelList.get(0).getPenetranceTables().length;
			for(int whichModel = 1; whichModel < modelCount; ++whichModel)
			{
				assert quantileCount1 == modelList.get(whichModel).getPenetranceTables().length;
			}
			quantiles1 = new PenetranceTableQuantile[quantileCount1];
			for(int q = 0; q < quantileCount1; ++q)
			{
				quantiles1[q] = new PenetranceTableQuantile(modelCount);
				for(int m = 0; m < modelCount; ++m)
				{
					DocModel model = modelList.get(m);
					PenetranceTable table = model.getPenetranceTables()[q];
					if(table != null)
					{
						DocFloat fraction = model.fraction;
						if(fraction != null && fraction.getFloat() != null)
							table.fractionContribution = fraction.getFloat();
						else
							table.fractionContribution = 1;
					}
					quantiles1[q].tables[m] = table;
				}
			}
		}
		
//		if(document.inputFiles.length > 0)
		if(inputFiles.length > 0)
		{
			quantiles2 = parseModelInputFiles(inputFiles);
			quantileCount2 = quantiles2.length;
			
			for(int q = 0; q < quantileCount2; ++q)
			{
				PenetranceTable[] tables = quantiles2[q].tables;
				if(inputFileFractions != null)
					assert inputFileFractions.length == tables.length;
				for(int t = 0; t < tables.length; ++t)
				{
					PenetranceTable table = tables[t];
					if(table != null)
					{
						if(inputFileFractions != null)
							table.fractionContribution = inputFileFractions[t];
						else
							table.fractionContribution = 1;
					}
				}
			}
		}
		
//		if(quantiles1 == null && quantiles2 == null)
//			throw new InputException("Must either generate models or get them from a file");
		
		int quantileCount;
		if(quantiles1 != null && quantiles2 != null)
		{
			assert quantileCount1 == quantileCount2;
			quantileCount = quantileCount1;
			penetranceTableQuantiles = mergeQuantiles(quantiles1, quantiles2);
		}
		else if(quantiles1 != null)
		{
			quantileCount = quantiles1.length;
			penetranceTableQuantiles = quantiles1;
		}
		else
		{
			quantileCount = quantiles2.length;
			penetranceTableQuantiles = quantiles2;
		}
		
		// If there are no penetrance-table quantiles then create some empty ones:
		if(penetranceTableQuantiles == null)
		{
			penetranceTableQuantiles = new PenetranceTableQuantile[quantileCount];
			for(int i = 0; i < quantileCount; ++i)
				penetranceTableQuantiles[i] = new PenetranceTableQuantile(0);
		}
	}
	
	public void loadModel(File inInputFile) throws Exception
	{
		penetranceTableQuantiles = parseModelInputFile(inInputFile);
	}
	
	private boolean generatePenetranceTable(float[] inAlleleFrequencies, PenetranceTable outPenetranceTable) throws Exception
	{
		PenetranceTable.ErrorState error;
		int tablesToTryCount = 1000;

		boolean outSuccess = false;
		for(int whichTableIteration = 0; whichTableIteration < tablesToTryCount; ++whichTableIteration)
		{
			outPenetranceTable.clear();
			outPenetranceTable.initialize(random, inAlleleFrequencies);
			error = outPenetranceTable.generateUnnormalized(random);
			if(error != PenetranceTable.ErrorState.Ambiguous && error != PenetranceTable.ErrorState.Conflict)
			{
				outPenetranceTable.normalize();
				if(outPenetranceTable.normalized)
				{
					outPenetranceTable.checkRowSums();
					if(!outPenetranceTable.rowSumsValid)
						throw new Exception("Table failed the row-sum test!");
					outSuccess = true;
					break;
				}
			}
		}
		return outSuccess;
	}
	
	public PenetranceTable[] generatePenetranceTables(DocModel model, int inDesiredTableCount, int inTryCount, ProgressHandler inProgressHandler, int inProgressValueBase) throws Exception
	{
		return generatePenetranceTables(
			random, inDesiredTableCount, inTryCount, model.heritability.getFloat(), -1, model.prevalence.getFloat(), model.attributeCount.getInteger(), model.getAttributeNames(), model.getAlleleFrequencies(), 
			model.getUseOddsRatio(), inProgressHandler, inProgressValueBase);
	}
	
	public PenetranceTable[] generatePenetranceTables(
		Random inRandom, int inDesiredTableCount, int inTablesToTryCount, float inDesiredHeritability, float inHeritabilityTolerance, Float inDesiredPrevalence,
		int inAttributeCount, String[] inAttributeNames, float[] inAlleleFrequencies, boolean inUseOddsRatio, ProgressHandler inProgressHandler, int inProgressValueBase) throws Exception
	{
		long startTimeMillis;
		PenetranceTable.ErrorState error;
		int ambiguityCount;
		int conflictCount;
		int ambiguousFixedConflictCount;
		int conflictedFixedConflictCount;
		int successfulFixedConflictCount;
		Exception ex;
		PenetranceTable currentPenetranceTable;
		PenetranceTable.PenetranceTableComparatorEdm edmComparator = new PenetranceTable.PenetranceTableComparatorEdm();
		PenetranceTable.PenetranceTableComparatorOddsRatio oddsComparator = new PenetranceTable.PenetranceTableComparatorOddsRatio();
		PenetranceTable[] tablesToSave;
		
//		double[] heritabilities = new double[inTablesToTryCount];
//		int totalHeritabilityCount = 0;
		
		List<PenetranceTable> penetranceTableList = new ArrayList<PenetranceTable>();
		PenetranceTable.fixedConflictSuccessfully = 0;
		PenetranceTable.fixedConflictUnsuccessfully = 0;
		for(int whichTableIteration = 0; whichTableIteration < inTablesToTryCount; ++whichTableIteration)
		{
			ambiguityCount = 0;
			conflictCount = 0;
			ambiguousFixedConflictCount = 0;
			conflictedFixedConflictCount = 0;
			successfulFixedConflictCount = 0;
			
			currentPenetranceTable = new PenetranceTable(3, inAttributeCount);
			currentPenetranceTable.desiredHeritability = inDesiredHeritability;
			currentPenetranceTable.desiredPrevalence = inDesiredPrevalence;
			currentPenetranceTable.setAttributeNames(inAttributeNames);
			currentPenetranceTable.initialize(inRandom, inAlleleFrequencies);
			error = currentPenetranceTable.generateUnnormalized(inRandom);
			
//			if (error == ErrorState.Ambiguous)
//			{
//				++ambiguityCount;
//				if(currentPenetranceTable.fixedConflict)
//					++ambiguousFixedConflictCount;
//			}
//			else if (error == ErrorState.Conflict)
//			{
//				++conflictCount;
//				if(currentPenetranceTable.fixedConflict)
//					++conflictedFixedConflictCount;
//			}
//			else
//			{
//				if(currentPenetranceTable.fixedConflict)
//					++successfulFixedConflictCount;
//			}
			
			if (error == PenetranceTable.ErrorState.Ambiguous || error == PenetranceTable.ErrorState.Conflict)
			{
//				System.out.println("Failed to construct the penetrance table");
			}
			else
			{
//				PenetranceTable copy = null;
//				try
//				{
//					copy = (PenetranceTable) currentPenetranceTable.clone();
//				}
//				catch(CloneNotSupportedException cnse)
//				{
//					
//				}
//				currentPenetranceTable.verify();
				currentPenetranceTable.scaleToUnitInterval();
				currentPenetranceTable.adjustPrevalence();
//				currentPenetranceTable.verify();
				double herit = currentPenetranceTable.calcHeritability();
//				heritabilities[totalHeritabilityCount++] = herit;
				boolean heritabilityAchieved = false;
				if(inHeritabilityTolerance < 0 || Math.abs((herit - currentPenetranceTable.desiredHeritability) / currentPenetranceTable.desiredHeritability) < inHeritabilityTolerance)
				{
					currentPenetranceTable.adjustHeritability();
					heritabilityAchieved = currentPenetranceTable.normalized;
				}
				if(!heritabilityAchieved)
				{
					int foo;
					foo = 0;
//							System.out.println("Failed to normalize the penetrance table");
//							if(inDestFile != null)
//								copy.saveToFile(new File(inDestFile.getParentFile(), "unnormalizedTable.txt"), false, true);
				}
				else
				{
					currentPenetranceTable.checkRowSums();
//					if(!currentPenetranceTable.rowSumsValid)
//						throw new Exception("Table failed the row-sum test!");
					if(currentPenetranceTable.rowSumsValid)
					{
						penetranceTableList.add(currentPenetranceTable);
					}
//					if(inProgressHandler != null)
//						inProgressHandler.setValue(whichModel * inDesiredTableCount + tableCountSoFar);

					if(penetranceTableList.size() >= inDesiredTableCount)
						break;
				}
			}
			if(inProgressHandler != null)
				inProgressHandler.setValue(inProgressValueBase + whichTableIteration);
		}
		
		PenetranceTable[] penetranceTables = penetranceTableList.toArray(new PenetranceTable[0]);
		if(inUseOddsRatio)
			Arrays.sort(penetranceTables, oddsComparator);
		else
			Arrays.sort(penetranceTables, edmComparator);
		
//		System.out.println("fixedConflictSuccessfully: " + PenetranceTable.fixedConflictSuccessfully);
//		System.out.println("fixedConflictUnsuccessfully: " + PenetranceTable.fixedConflictUnsuccessfully);
//		heritabilities = Arrays.copyOf(heritabilities, totalHeritabilityCount);
//		printStats(heritabilities, 20);
//		System.out.print("\t");
		
		return penetranceTables;
	}
	
	private PenetranceTableQuantile[] mergeQuantiles(PenetranceTableQuantile[] inQuantiles1, PenetranceTableQuantile[] inQuantiles2)
		throws InputException
	{
		if(inQuantiles1.length != inQuantiles2.length)
			throw new InputException("The generated models and the models from the file must have the same number of quantiles");
		
		int quantileCount = inQuantiles1.length;
		PenetranceTableQuantile[] outPenetranceTableQuantiles = new PenetranceTableQuantile[quantileCount];
		for(int i = 0; i < quantileCount; ++i)
		{
			outPenetranceTableQuantiles[i] = new PenetranceTableQuantile(inQuantiles1[i].tables.length + inQuantiles2[i].tables.length);
			int dest = 0;
			for(PenetranceTable t: inQuantiles1[i].tables)
				outPenetranceTableQuantiles[i].tables[dest++] = t;
			for(PenetranceTable t: inQuantiles2[i].tables)
				outPenetranceTableQuantiles[i].tables[dest++] = t;
		}
		return outPenetranceTableQuantiles;
	}
	
	private void selectPenetranceTablesRepresentativesUniformly(int inQuantileCount, PenetranceTable[] tablePopulation, DocModel model)
	{
		double[] targetRASs = new double[inQuantileCount];
		
		boolean useOddsRatio = model.getUseOddsRatio();
		int tablePopulationSize = tablePopulation.length;
		
		// Set the target quantile scores:
		double minRAS = tablePopulation[0].getQuantileScore(useOddsRatio);
		double maxRAS = tablePopulation[tablePopulationSize - 1].getQuantileScore(useOddsRatio);
		if(inQuantileCount == 1)
		{
			targetRASs[0] = (minRAS + maxRAS) / 2F;
		}
		else
		{
			double delta = (maxRAS - minRAS) / (inQuantileCount - 1);
			for(int whichQuantile = 0; whichQuantile < inQuantileCount; ++whichQuantile)
				targetRASs[whichQuantile] = minRAS + whichQuantile * delta;
		}
		
		int tableIter = 0;
		int quantileIter;
		int matchingTable;
		int priorMatchingTable = -1;
		
		// Copy the tables from the appropriate spots in outPenetranceTableQuantiles to the model:
		PenetranceTable[] modelTables = new PenetranceTable[inQuantileCount];
		model.setPenetranceTables(modelTables);
		
		// Initialize the outputs to null, for "not set yet":
		for(quantileIter = 0; quantileIter < inQuantileCount; ++quantileIter)
			modelTables[quantileIter] = null;

		quantileIter = 0;
		if(inQuantileCount > 1)
		{
			// If there's more than one quantile, then we know that the first quantile is the very first table.
			modelTables[quantileIter++] = tablePopulation[tableIter++];
			priorMatchingTable = 0;
		}
		for(; tableIter < tablePopulationSize; ++tableIter)
		{
			// Look for the first reliefAccuracyScore greater than the current target:
			if(tablePopulation[tableIter].getQuantileScore(useOddsRatio) > targetRASs[quantileIter])
			{
				// If the table before the current table is closer to the target than the current table, and is available, 
				// then use it; else use the current table:
				if(tableIter > 0 && Math.abs(tablePopulation[tableIter - 1].getQuantileScore(useOddsRatio) - targetRASs[quantileIter]) < Math.abs(tablePopulation[tableIter].getQuantileScore(useOddsRatio) - targetRASs[quantileIter]))
					matchingTable = tableIter - 1;
				else
					matchingTable = tableIter;
				
				// Make sure that the table we want hasn't been used yet:
				if(matchingTable == priorMatchingTable)
				{
					// but don't go off the end:
					if(matchingTable == tablePopulationSize - 1)
						break;
					++matchingTable;
				}
				modelTables[quantileIter] = tablePopulation[matchingTable];
				priorMatchingTable = matchingTable;
				++quantileIter;
				if(quantileIter >= inQuantileCount)
					break;
			}
		}
		// Take care of any quantiles on the end that have not been set yet.
		// (If inQuantileCount == 1, then the quantile will have been set in the loop above, unless all of the reliefAccuracyScores are identical --
		// but in that case, it doesn't matter which table we use, so we might as well use the last one.)
		quantileIter = inQuantileCount - 1;
		// If the last quantile hasn't been set yet,
		if(modelTables[quantileIter] == null)
		{
			tableIter = tablePopulationSize - 1;
			// then set the last quantile to the last table,
			modelTables[quantileIter--] = tablePopulation[tableIter--];
			// and check the quantile before it:
			while(quantileIter >= 0 && (modelTables[quantileIter] == null || modelTables[quantileIter] == modelTables[quantileIter + 1]))
				modelTables[quantileIter--] = tablePopulation[tableIter--];
		}
	}
	
	private static PenetranceTableQuantile[] selectPenetranceTablesRepresentativesByQuantile(int inQuantileCount, PenetranceTablePopulation[] inPenetranceTablePopulations)
	{
		int modelCount = inPenetranceTablePopulations.length;		// There's one population for each model
		PenetranceTableQuantile[] outPenetranceTableQuantiles = new PenetranceTableQuantile[inQuantileCount];
		
		for(int whichQuantile = 0; whichQuantile < inQuantileCount; ++whichQuantile)
			outPenetranceTableQuantiles[whichQuantile] = new PenetranceTableQuantile(modelCount);	// Each quantile covers all of the models
		
		for(int whichModel = 0; whichModel < modelCount; ++whichModel)
		{
			int tablePopulationSize = inPenetranceTablePopulations[whichModel].currentTableCount;
			if(inQuantileCount == 1)
			{
				outPenetranceTableQuantiles[0].tables[whichModel] = inPenetranceTablePopulations[whichModel].tables[tablePopulationSize / 2];
			}
			else
			{
				int priorTable = -1;
				for(int whichQuantile = 0; whichQuantile <= inQuantileCount - 1; ++whichQuantile)
				{
					int whichTable = Math.round((float) ((tablePopulationSize - 1) * whichQuantile) / (float) (inQuantileCount - 1));
					if(whichTable <= priorTable)		// insurance, to make sure we don't reuse a model
						whichTable = priorTable + 1;
					whichTable = Math.min(whichTable, tablePopulationSize - 1);
					outPenetranceTableQuantiles[whichQuantile].tables[whichModel] = inPenetranceTablePopulations[whichModel].tables[whichTable];
					priorTable = whichTable;
				}
			}
		}
		return outPenetranceTableQuantiles;
	}
	
	public PenetranceTableQuantile[] parseModelInputFiles(File[] inInputFiles)
			throws FileNotFoundException, IOException, InputException
	{
		// For now, just parse the first file:
		if(inInputFiles.length > 0)
			return parseModelInputFile(inInputFiles[0]);
		else
			return new PenetranceTableQuantile[0];
	}
	
	private static final String kAttributeToken = "Attribute names:";
	private static final String kFrequencyToken = "Minor allele frequencies:";
	private static final String kTableToken = "Table:";
	public PenetranceTableQuantile[] parseModelInputFile(File inInputFile)
		throws FileNotFoundException, IOException, InputException
	{
		BufferedReader							tableReader = null;
		ArrayList<ArrayList<PenetranceTable>>	tables;
		ArrayList<PenetranceTable>				currentSubList;
		PenetranceTable							currentTable;
		
		tableReader = new BufferedReader(new FileReader(inInputFile));
		tables = new ArrayList<ArrayList<PenetranceTable>>();
		try
		{
			currentSubList = null;
			while(true)
			{
				currentTable = findTable(tableReader);
				if(currentTable == null)
					break;
				if(currentSubList == null || Arrays.equals(currentTable.getAttributeNames(), currentSubList.get(0).getAttributeNames()))
				{
					currentSubList = new ArrayList<PenetranceTable>();
					tables.add(currentSubList);
				}
				currentSubList.add(currentTable);
				parseTable(tableReader, currentTable);
			}
		}
		finally
		{
			if(tableReader != null)
				tableReader.close();
		}
		
		int quantileCount = tables.size();
		int quantileSize = tables.get(0).size();
		for(ArrayList<PenetranceTable> ptl: tables)
		{
			if(ptl.size() != quantileSize)
				throw new InputException("Each quantile must have the same number of tables");
		}
		
		PenetranceTableQuantile[] outPenetranceTableQuantiles = new PenetranceTableQuantile[quantileCount];
		for(int i = 0; i < quantileCount; ++i)
		{
			outPenetranceTableQuantiles[i] = new PenetranceTableQuantile(quantileSize);
			int whichFraction = 0;
			for(int j = 0; j < quantileSize; ++j)
			{
				outPenetranceTableQuantiles[i].tables[j] = tables.get(i).get(j);
				if(document != null && document.inputFileFractions != null && document.inputFileFractions.length > j)
					outPenetranceTableQuantiles[i].tables[j].fractionContribution = document.inputFileFractions[whichFraction++];
				else
					outPenetranceTableQuantiles[i].tables[j].fractionContribution = 0;
			}
		}
		return outPenetranceTableQuantiles;
	}
	
	private PenetranceTable findTable(BufferedReader modelReader)
		throws IOException
	{
		String									line;
		String[]								attributeNames;
		PenetranceTable							outTable;
		
		outTable = null;
		while(true)
		{
			line = modelReader.readLine();
			if(line == null)
				break;
			if(line.toLowerCase().startsWith(kAttributeToken.toLowerCase()))
			{
				attributeNames = line.substring(kAttributeToken.length() + 1).trim().split("\t");
				outTable = new PenetranceTable(3, attributeNames.length);
				outTable.setAttributeNames(attributeNames);
				outTable.normalized = true;
				break;
			}
		}
		return outTable;
	}
	
	private void parseTable(BufferedReader modelReader, PenetranceTable inTable)
		throws IOException, InputException
	{
		String				line;
		String[]			numbers;
		int					whichCell;
		double				cellValue;
		
		// Find the table-token:
		while(true)
		{
			line = modelReader.readLine();
			if(line == null)
				throw new InputException("Got a table-header without a table");
			if(line.toLowerCase().startsWith(kFrequencyToken.toLowerCase()))
			{
				numbers = line.substring(kFrequencyToken.length() + 1).trim().split("\t");
				float[] freqs = new float[numbers.length];
				int whichFreq = 0;
				for(String s: numbers)
				{
					try
					{
						freqs[whichFreq++] = Float.parseFloat(s.trim());
					}
					catch(NumberFormatException nfe)
					{
						throw new InputException("Got a table with a non-numeric minor allele frequency");
					}
				}
				inTable.setMinorAlleleFrequencies(freqs);
			}
			if(line.toLowerCase().startsWith(kTableToken.toLowerCase()))
				break;
		}
		
		whichCell = 0;
		while(true)
		{
			line = modelReader.readLine();
			if(line == null)
				throw new InputException("Got a table with too few cells");
			if(line.trim().length() > 0)
			{
				numbers = line.split(",");
				for(String s: numbers)
				{
					try
					{
						cellValue = Double.parseDouble(s.trim());
					}
					catch(NumberFormatException nfe)
					{
						throw new InputException("Got a table with a non-numeric cell");
					}
					if(whichCell >= inTable.cellCount)
						throw new InputException("Got a table with too many cells");
					inTable.cells[whichCell++] = new PenetranceTable.PenetranceCell(cellValue);
				}
			}
			if(whichCell >= inTable.cellCount)
				break;
		}
		inTable.calcAndSetHeritability();
	}
	
	private static final String kStandardFrequencyToken = "Minor Allele Frequency:";
	private static final String kStandardHeritabilityToken = "Heritability:";

	private static PenetranceTable[] parseStandardTables(File inInputFile)
		throws IOException, InputException
	{
		BufferedReader							tableReader = null;
		ArrayList<PenetranceTable>	outTables = new ArrayList<PenetranceTable>();
		PenetranceTable				table;
		String						line;
		String						number;
		String[]					numbers;
		int							whichCell;
		double						cellValue;
		
		tableReader = new BufferedReader(new FileReader(inInputFile));
		try
		{
			while(true)
			{
				line = tableReader.readLine();
				if(line == null)
					break;
				if(line.toLowerCase().startsWith(kStandardFrequencyToken.toLowerCase()))
				{
					table = new PenetranceTable(3, 2);
					float freq = 0;
					number = line.substring(kStandardFrequencyToken.length() + 1).trim();
					try
					{
						freq = Float.parseFloat(number.trim());
					}
					catch(NumberFormatException nfe)
					{
						throw new InputException("Got a table with a non-numeric minor allele frequency");
					}
					float[] freqs = new float[2];
					freqs[0] = freq;
					freqs[1] = freq;
					table.setMinorAlleleFrequencies(freqs);
					line = tableReader.readLine();
					if(line == null)
						break;
					if(!line.toLowerCase().startsWith(kStandardHeritabilityToken.toLowerCase()))
						throw new InputException("Got a table without a heritability");
					number = line.substring(kStandardHeritabilityToken.length() + 1).trim();
					try
					{
						table.desiredHeritability = Float.parseFloat(number.trim());
					}
					catch(NumberFormatException nfe)
					{
						throw new InputException("Got a table with a non-numeric heritability");
					}
					whichCell = 0;
					for(int whichLine = 0; whichLine < 3; ++whichLine)
					{
						line = tableReader.readLine();
						if(line == null)
							throw new InputException("Got a table with too few cells");
						if(line.trim().length() > 0)
						{
							numbers = line.split(" ");
							for(String s: numbers)
							{
								try
								{
									cellValue = Double.parseDouble(s.trim());
								}
								catch(NumberFormatException nfe)
								{
									throw new InputException("Got a table with a non-numeric cell");
								}
								if(whichCell >= table.cellCount)
									throw new InputException("Got a table with too many cells");
								table.cells[whichCell++] = new PenetranceTable.PenetranceCell(cellValue);
							}
						}
					}
					table.calcAndSetPrevalence();
					outTables.add(table);
				}
			}
		}
		finally
		{
			if(tableReader != null)
				tableReader.close();
		}
		return outTables.toArray(new PenetranceTable[0]);
	}
	
	public static int[][] parseDataInputFile(File inInputFile, StringBuilder outHeader)
		throws FileNotFoundException, IOException, InputException
	{
		List<String>							lines;
		int[][]									outDataset;
		BufferedReader							reader = null;
		
		reader = new BufferedReader(new FileReader(inInputFile));
		lines = new ArrayList<String>();
		try
		{
			String line;
			if((line = reader.readLine()) != null)
			{
				boolean isNumeric = true;
				for(int i = 0; i < line.length(); ++i)
				{
					if("0123456789 \t".indexOf(line.charAt(i)) == -1)
					{
						isNumeric = false;
						break;
					}
				}
				// Don't use the first line if it is non-numeric:
				if(isNumeric)
					lines.add(line);
				else
				{
					// A first, non-numeric, line is a header, in case such is requested by the caller:
					if(outHeader != null)
						outHeader.append(line);
				}
				while((line = reader.readLine()) != null)
				{
					lines.add(line);
				}
			}
		}
		finally
		{
			if(reader != null)
				reader.close();
		}
		String[] items = lines.get(0).split("\t");
		outDataset = new int[lines.size()][items.length];
		int whichLine = 0;
		for(String line: lines)
		{
			int whichItem = 0;
			items = line.split("\t");
			for(String item: items)
			{
				outDataset[whichLine][whichItem] = Integer.valueOf(item);
				++whichItem;
			}
			++whichLine;
		}
		return outDataset;
	}
	
//	public void generateDatasets() throws Exception
//	{
//		generateDatasets(progressHandler);
//	}
	
	// Output: Nested subdirectories named by #attributes, population size, model #; containing files named by model-name, population size, and replicate # 
	// Example: parent-directory/100/400/Model10/Model10.400.007
	public void generateDatasets(ProgressHandler inProgressHandler) throws Exception
	{
		String destFilename;
		File destFile;
		File directory;
		File subdirectory = null;
		File datasetFile;
		int caseCount;
		int controlCount;
		int totalAttributeCount;
		int datasetIterationCount;
		float inMinorAlleleFreqMin;
		float inMinorAlleleFreqMax;
		PrintWriter rawStream = null;
		PrintWriter resultsStream = null;
		PrintWriter successStream = null;
		File successSummaryFile = null;
		PrintWriter meansStream = null;
		File testingOutputFile = null;
		Exception ex;
		int[][] predictiveDataset;
		int[][] noiseDataset;
		int fileCount;
		
		if((ex = document.verifyDatasetParameters()) != null)
			throw ex;
		System.out.println("Generating datasets...");
		
		predictiveDataset = null;
		if(document.predictiveInputFile != null)
			predictiveDataset = parseDataInputFile(document.predictiveInputFile, null);
		noiseDataset = null;
		if(document.noiseInputFile != null)
			noiseDataset = parseDataInputFile(document.noiseInputFile, null);
		
		if(inProgressHandler != null)
		{
			int totalReplicateCount = 0;
			for(DocDataset dd: document.datasetList)
				totalReplicateCount += dd.replicateCount.getInteger();
			int maxProgress = penetranceTableQuantiles.length * totalReplicateCount;
			inProgressHandler.setMaximum(maxProgress);
		}
		fileCount = 0;
		boolean createDirectories = (document.datasetList.size() > 1);
		for(DocDataset dd: document.datasetList)
		{
			destFilename = null;
			directory = null;
			destFile = dd.outputFile;
			if(destFile != null)
			{
				if(createDirectories)
				{
					directory = destFile;
					directory.mkdirs();
				}
				else
				{
					directory = destFile.getParentFile();
				}
				destFilename = destFile.getName();
			}
			
			caseCount = dd.caseCount.getInteger();
			controlCount = dd.controlCount.getInteger();
			totalAttributeCount = dd.totalAttributeCount.getInteger();
			datasetIterationCount = dd.replicateCount.getInteger();
			inMinorAlleleFreqMin = dd.alleleFrequencyMin.getFloat();
			inMinorAlleleFreqMax = dd.alleleFrequencyMax.getFloat();
			
			int maxQuantileNumberLength = (new Integer(penetranceTableQuantiles.length)).toString().length();
			int maxDatasetNumberLength = (new Integer(datasetIterationCount)).toString().length();
			for(int whichQuantile = 0; whichQuantile < penetranceTableQuantiles.length; ++whichQuantile)
			{
				PenetranceTableQuantile q = penetranceTableQuantiles[whichQuantile];
				String quantileName = (new Integer(whichQuantile + 1)).toString();
				quantileName = "0000000000".substring(0, maxQuantileNumberLength - quantileName.length()) + quantileName;
				if(destFilename != null)
				{
					subdirectory = new File(directory, destFilename + "_EDM-" + quantileName);
					subdirectory.mkdirs();
				}
				for(int whichDataset = 0; whichDataset < datasetIterationCount; ++whichDataset)
				{
					String datasetName = (new Integer(whichDataset + 1)).toString();
					datasetName = "0000000000".substring(0, maxDatasetNumberLength - datasetName.length()) + datasetName;
					if(destFilename != null)
						datasetFile = new File(subdirectory, destFilename + "_EDM-" + quantileName + "_" + datasetName + ".txt");
					else
						datasetFile = null;
					StringBuilder header = new StringBuilder();
					
					int[][] dataset = null;
					generateAndSaveDataset(random, predictiveDataset, noiseDataset, q.tables, totalAttributeCount, caseCount, controlCount, inMinorAlleleFreqMin, inMinorAlleleFreqMax, datasetFile, header);
					if(inProgressHandler != null)
						inProgressHandler.setValue(++fileCount);
				}
			}
		}
		
//		File caseControlFile = null;
//		caseControlFile = new File(directory, dd.outputFile + "_caseControlValues.txt");
//		if(penetranceTableQuantiles != null && caseControlFile != null)
//		{
//			boolean append = false;
//			for(PenetranceTableQuantile q: penetranceTableQuantiles)
//			{
//				for(PenetranceTable table: q.tables)
//				{
//					table.saveCaseControlValuesToFile(caseControlFile, append);
//					append = true;
//				}
//			}
//		}
		System.out.println("Done generating datasets.");
	}
	
	public void calcStatsForStdModels(SnpGenDocument inDoc) throws Exception
	{
		if(inDoc.inputFile != null)
		{
			Random random = createRandom(inDoc.randomSeed);
			int popCount = 1000;
			int bucketCount = 100;
			
			printStats(random, 0.02, 0.05F, 0.01, 0.2, popCount, bucketCount, -1);
			printStats(random, 0.02, 0.05F, 0.01, 0.2, popCount, bucketCount, -1);
			printStats(random, 0.02, 0.05F, 0.01, 0.2, popCount, bucketCount, -1);
			printStats(random, 0.02, -1, 0.01, 0.2, popCount, bucketCount, -1);
			printStats(random, 0.02, -1, 0.01, 0.2, popCount, bucketCount, -1);
			printStats(random, 0.02, -1, 0.01, 0.2, popCount, bucketCount, -1);
			
			printStats(random, 0.1, 0.05F, 0.01, 0.4, popCount, bucketCount, -1);
			printStats(random, 0.1, 0.05F, 0.01, 0.4, popCount, bucketCount, -1);
			printStats(random, 0.1, 0.05F, 0.01, 0.4, popCount, bucketCount, -1);
			printStats(random, 0.1, -1, 0.01, 0.4, popCount, bucketCount, -1);
			printStats(random, 0.1, -1, 0.01, 0.4, popCount, bucketCount, -1);
			printStats(random, 0.1, -1, 0.01, 0.4, popCount, bucketCount, -1);
			
			PenetranceTable[] tables = parseStandardTables(inDoc.inputFile);
			int whichModel = 0;
			System.out.println("Model #\tMAF\tHeritability\tRAS\tRAS percentile\tMinimum RAS\tMaximum RAS\tMean RAS\tStd Dev RAS\tBucket-counts");
			for(PenetranceTable t: tables)
			{
				// Generate the distribution twice, to see whether there's any significant variation:
				printStats(random, t, popCount, bucketCount, whichModel);
				printStats(random, t, popCount, bucketCount, whichModel);
				++whichModel;
			}
		}
	}
	
	private void printStats(Random inRandom, PenetranceTable inTable, int inPopCount, int inBucketCount, int inWhichModel) throws Exception
	{
		printStats(inRandom, inTable.calcHeritability(), -1, inTable.calcEdm(), (float)inTable.getMinorAlleleFrequencies()[0], inPopCount, inBucketCount, inWhichModel);
	}
	
	private void printStats(Random inRandom, double herit, float inHeritabilityTolerance, double ras, double maf, int inPopCount, int inBucketCount, int inWhichModel) throws Exception
	{
		int popCount = inPopCount;
		double[] scores = new double[popCount];
		PenetranceTable[] pop;
		pop = generatePenetranceTables(inRandom, popCount, 100 * popCount, (float)herit, inHeritabilityTolerance, null, 2, new String[]{"foo", "bar"}, new float[]{(float)maf, (float)maf}, false, null, 0);
		int scoreCount = 0;
		for(PenetranceTable t: pop)
		{
			if(t == null)
			{
				popCount = scoreCount;
				break;
			}
			scores[scoreCount++] = t.edm;
		}
		if(popCount != inPopCount)
			scores = Arrays.copyOf(scores, popCount);
		Arrays.sort(scores);
		int whichScore = 0;
		while(whichScore < popCount && scores[whichScore] < ras)
			++whichScore;
		float percentile = 100 * (float) whichScore / (float) popCount;
		System.out.print(inWhichModel + "\t" + maf + "\t" + herit + "\t" + ras + "\t" + percentile + "\t");
		printStats(scores, inBucketCount);
		System.out.println();
	}
	
	private static void printStats(double[] scores, int inBucketCount)
	{
		NumberFormat f = NumberFormat.getInstance();
		f.setMaximumFractionDigits(2);
		Arrays.sort(scores);
		int scoreCount = scores.length;
		double sum = 0;
		double sumOfSquares = 0;
		for(double d: scores)
		{
			sum += d;
			sumOfSquares += d * d;
		}
		double mean = (double) sum / (double) scoreCount;
		System.out.print("\t" + scoreCount + "\t" + scores[0] + "\t" + scores[scoreCount - 1] + "\t" + mean + "\t" + Math.sqrt(sumOfSquares / scoreCount - mean * mean));
		int whichScore = 0;
		double bucketWidth = (scores[scoreCount - 1] - scores[0]) / inBucketCount;
		int bucketCount;
		int bucketCountSoFar = 0;
		double bucketRight;
//		System.out.println("\t" + scores[0] + "\t" + scores[inPopCount - 1]);
		for(int i = 1; i < inBucketCount; ++i)		// Don't count the last bucket, assume it's whatever's left at the end
		{
			bucketRight = scores[0] + i * bucketWidth;
			bucketCount = 0;
			while(scores[whichScore] <= bucketRight)
			{
				++bucketCount;
				++whichScore;
			}
			// Now, whichScore is the first score past the current bucket
			bucketCountSoFar += bucketCount;
			System.out.print("\t" + f.format(100 * (float) bucketCount / (float) scoreCount));
		}
		bucketCount = scoreCount - bucketCountSoFar;
		System.out.print("\t" + f.format(100 * (float) bucketCount / (float) scoreCount));
	}
	
	private static int[][] generateAndSaveDataset(
		Random inRandom, int[][] inPredictiveDataset, int[][] inNoiseDataset, PenetranceTable[] inTables, int inTotalAttributeCount, int inCaseCount, int inControlCount,
		double inMinorAlleleFreqMin, double inMinorAllelFreqMax, File inDestFile, StringBuilder outHeader)
		throws Exception
	{
		double prob;
		double penetrance;
		PenetranceTable.CellId cellId;
		double sumCaseFractions, sumControlFractions;
		// caseIntervals[i] == the right edge of the ith probability-interval for cases; similarly for controls
		double[][] caseIntervals, controlIntervals;
		PrintWriter outputStream = null;
		int[][] outputArray = new int[inCaseCount + inControlCount][inTotalAttributeCount + 1]; 

		try
		{
			if(inDestFile != null)
				outputStream = new PrintWriter(new FileWriter(inDestFile));
			
			// The order of attributes: non-predictive attributes, followed by predictive attributes from the file, followed by predictive attributes from the SNPGen models.
			
			int snpGenAttributeCount = 0;
			for(PenetranceTable t: inTables)
				snpGenAttributeCount += t.attributeCount;
			int predictiveAttributeCount = snpGenAttributeCount;
			if(inPredictiveDataset != null)
				predictiveAttributeCount += inPredictiveDataset[0].length - 1;		// Account for the columns in inPredictiveDataset, but don't count the class column
			
			int totalNoiseAttributeCount = inTotalAttributeCount - predictiveAttributeCount;
			int neededNoiseAttributeCount = totalNoiseAttributeCount;
			if(inNoiseDataset != null)
				neededNoiseAttributeCount -= inNoiseDataset[0].length;		// We don't need to create noise for the columns of inNoiseData
			
			// Header for non-predictive attributes:
			for(int i = 0; i < totalNoiseAttributeCount; ++i)
			{
				if(outputStream != null)
					outputStream.print("N" + i + "\t");
				outHeader.append("N" + i + "\t");
			}
			
			// Header for predictive attributes from file:
			if(inPredictiveDataset != null)
			{
				for(int i = 0; i < inPredictiveDataset[0].length - 1; ++i)
				{
					String attributeName = "P" + (snpGenAttributeCount + 1 + i);
					if(outputStream != null)
						outputStream.print(attributeName + "\t");
					outHeader.append(attributeName + "\t");
				}
			}
			
			// Header for predictive attributes from SNPGen models:
			for(PenetranceTable t: inTables)
			{
				for(String n: t.getAttributeNames())
				{
					if(outputStream != null)
						outputStream.print(n + "\t");
					outHeader.append(n + "\t");
				}
			}
			
			if(outputStream != null)
			{
				outputStream.print("Class");
				outputStream.println();
			}
			outHeader.append("Class");
			
			// Calculate the values of caseIntervals and controlIntervals, such that the length of each interval is the desired probability
			// of a given case or control (respectively) landing in a given cell.
			// This is used for sampling the cells in the dataset.
			int tableCount = inTables.length;
			caseIntervals = new double[tableCount][];
			controlIntervals = new double[tableCount][];
			for(int j = 0; j < tableCount; ++j)
			{
				cellId = new PenetranceTable.CellId(inTables[j].attributeCount);
				sumCaseFractions = 0;
				sumControlFractions = 0;
				caseIntervals[j] = new double[inTables[j].cellCount];
				controlIntervals[j] = new double[inTables[j].cellCount];
				// Sum up all the case-fractions, storing the partial case-fractions to the caseIntervals array; do the same with controls:
				for(int i = 0; i < inTables[j].cellCount; ++i)
				{
					inTables[j].masterIndexToCellId(i, cellId);
					prob = inTables[j].getProbabilityProduct(cellId);
					penetrance = inTables[j].getPenetranceValue(cellId);
					
					sumCaseFractions += prob * penetrance;
					sumControlFractions += prob * (1 - penetrance);
					caseIntervals[j][i] = sumCaseFractions;
					controlIntervals[j][i] = sumControlFractions;
				}
				assert Math.abs(sumCaseFractions + sumControlFractions - 1.0) < kErrorLimit;
				// Divide each element of caseIntervals and controlIntervals by the appropriate total sum.
				for(int i = 0; i < inTables[j].cellCount; ++i)
				{
					caseIntervals[j][i] /= sumCaseFractions;
					controlIntervals[j][i] /= sumControlFractions;
				}
			}
			// Now, the length of the interval from caseIntervals[i-1] to caseIntervals[i]
			// == the probability that a random case is in the ith cell of the penetrance table; similarly for controls.
			
			// Generate allele frequencies
			// For each attribute, frequency[0] is the major-major allele and frequency[2] is the minor-minor allele.
			double[][] alleleFrequencies = new double[neededNoiseAttributeCount][3];
			for(int i = 0; i < neededNoiseAttributeCount; ++i)
			{
				double maf = inRandom.nextDouble() * (inMinorAllelFreqMax - inMinorAlleleFreqMin) + inMinorAlleleFreqMin;
				PenetranceTable.calcAlleleFrequencies(maf, alleleFrequencies[i]);
			}
			
			for(int j = 0; j < tableCount; ++j)
				inTables[j].clear();
			printInstances(inRandom, inPredictiveDataset, inNoiseDataset, 0, inTables, neededNoiseAttributeCount, alleleFrequencies, 1, inCaseCount, caseIntervals, outputStream, outputArray, 0);
			printInstances(inRandom, inPredictiveDataset, inNoiseDataset, inCaseCount, inTables, neededNoiseAttributeCount, alleleFrequencies, 0, inControlCount, controlIntervals, outputStream, outputArray, inCaseCount);
		}
		finally
		{
			if (outputStream != null)
			{
				outputStream.close();
			}
		}
		return outputArray;
	}
	
	private static void printInstances(
		Random inRandom, int[][] inPredictiveDataset, int[][] inNoiseDataset, int inWhichFirstNoise, PenetranceTable[] inTables, int inNoiseAttributeCount, double[][] inAlleleFrequencies,
		int inInstanceClass, int inInstanceCount, double[][] inInstanceIntervals, PrintWriter inOutputStream, int[][] inOutputArray, int inFirstOutputLine)
		throws Exception
	{
		double rand;
		int whichCell;
		PenetranceTable.CellId cellId;
		int whichOutputLine = inFirstOutputLine;
		
		int predictiveDatasetAttributeCount = 0;
		if(inPredictiveDataset != null)
			predictiveDatasetAttributeCount = inPredictiveDataset[0].length - 1;		// Don't count the class column
		int whichPredictive = 0;
		
		int noiseDatasetAttributeCount = 0;
		if(inNoiseDataset != null)
			noiseDatasetAttributeCount = inNoiseDataset[0].length;
		int whichNoise = inWhichFirstNoise;
		
		double[] alleleFrequencies = new double[3];
		
		// The order of attributes: non-predictive attributes, followed by predictive attributes from the file, followed by predictive attributes from the SNPGen models.
		
		// How heterogeneity works:
		// If there are two tables, and Table 1 has a contribution-fraction of 0.3 and Table 2 has a contribution-fraction of 0.7,
		// then for the first 0.3 of the instances we generate the columns corresponding to Table 1 according to Table 1's signal (ie, according to Table 1's inInstanceIntervals)
		// and we generate the columns corresponding to Table 2 as noise;
		// for the next 0.7 of the instances we generate noise for Table 1 and signal for Table 2.
		
		// For each instance desired:
		for(int row = 0; row < inInstanceCount; ++row)
		{
			// Figure out which table has the signal for the current row:
			double sumTableFractions = 0;
			for(int k = 0; k < inTables.length; ++k)
			{
				sumTableFractions += inTables[k].fractionContribution;
			}
			double rowFraction = (float) row / inInstanceCount;
			double tableFractionBefore = 0;
			int whichSignalTable = inTables.length - 1;		// Sentinel, in case of slight floating-point discrepancies.
			for(int k = 0; k < inTables.length; ++k)
			{
				if(rowFraction * sumTableFractions < tableFractionBefore + inTables[k].fractionContribution)
				{
					whichSignalTable = k;
					break;
				}
				tableFractionBefore += inTables[k].fractionContribution;
			}
			
			int destWhich = 0;
			
			if(inNoiseDataset != null)
			{
				// Copy the noise attributes from inNoiseDataset
				if(whichNoise >= inNoiseDataset.length)
					throw new Exception("Not enough noise input data");
				for(int j = 0; j < noiseDatasetAttributeCount; ++j)
					valueToOutput(inNoiseDataset[whichNoise][j], inOutputStream, true, inOutputArray, whichOutputLine, destWhich++);
				++whichNoise;
			}
			
			// Generate noise attributes
			for(int j = 0; j < inNoiseAttributeCount; ++j)
			{
				noiseToOutput(inRandom, inAlleleFrequencies[j], inOutputStream, inOutputArray, whichOutputLine, destWhich++);
//				rand = inRandom.nextDouble();
//				if(rand < inAlleleFrequencyIntervals[0][j])
//					valueToOutput(kMajorMajor, inOutputStream, true, inOutputArray, whichOutputLine, destWhich++);
//				else if(rand < inAlleleFrequencyIntervals[1][j])
//					valueToOutput(kMajorMinor, inOutputStream, true, inOutputArray, whichOutputLine, destWhich++);
//				else
//					valueToOutput(kMinorMinor, inOutputStream, true, inOutputArray, whichOutputLine, destWhich++);
			}
			
			if(inPredictiveDataset != null)
			{
				// Copy the predictive attributes from inPredictiveDataset:
				// First, find a match in inPredictiveDataset for inInstanceClass:
				while(whichPredictive < inPredictiveDataset.length && inPredictiveDataset[whichPredictive][predictiveDatasetAttributeCount] != inInstanceClass)
					++whichPredictive;
				if(whichPredictive >= inPredictiveDataset.length)
					throw new Exception("Not enough predictive input data");
				for(int j = 0; j < predictiveDatasetAttributeCount; ++j)
					valueToOutput(inPredictiveDataset[whichPredictive][j], inOutputStream, true, inOutputArray, whichOutputLine, destWhich++);
				++whichPredictive;
			}
			
			// We're going to put the current instance (either a case or a control, as determined by this method's caller; let's say it's a case)
			// into some cell of each specified table.
			// Let's say there are three tables.
			// Then we're going to put the current case into some cell of the first table,
			// and, simultaneously, into some cell of the second table, and some cell of the third table.
			// Equivalently, we're going to put the case into some cell of the cross-product of the three tables:
			// if the first table is 2-D, the second table is 4-D, and the third table is 3-D,
			// then we are choosing a cell in the 9-D table which is the cross-product of the three given tables.
			// (Added later: I'm not sure what the above comment is in aid of.)
			
			// As we iterate through the outer loop, we need to fill in each table's cellCaseCount or cellControlCount, as determined by inInstanceClass (a kluge).
			
//			System.out.println("signal\t" + whichSignalTable);
			
			for(int whichTable = 0; whichTable < inTables.length; ++whichTable)
			{
				PenetranceTable table = inTables[whichTable];
				cellId = new PenetranceTable.CellId(table.attributeCount);
				
				if(whichTable == whichSignalTable)
				{
//					System.out.println("signal");
					// Pick a random number from 0 to 1 and see which instance-interval it's in:
					rand = inRandom.nextDouble();
					whichCell = -1;
					for(int k = 0; k < table.cellCount; ++k)
					{
						if(rand < inInstanceIntervals[whichTable][k])
						{
							whichCell = k;
							break;
						}
					}
					assert 0 <= whichCell && whichCell < table.cellCount;
					if(inInstanceClass == 1)
						++table.cellCaseCount[whichCell];
					else
						++table.cellControlCount[whichCell];
					table.masterIndexToCellId(whichCell, cellId);
					for(int k = 0; k < table.attributeCount; ++k)
					{
						valueToOutput(kAlleleSymbols[cellId.getIndex(k)], inOutputStream, true, inOutputArray, whichOutputLine, destWhich++);
					}
				}
				else
				{
//					System.out.println("noise");
					// The current table is not the signal table, so generate noise:
					for(int j = 0; j < table.attributeCount; ++j)
					{
						table.getAlleleFrequencies(j, alleleFrequencies);
						int whichValue = noiseToOutput(inRandom, alleleFrequencies, inOutputStream, inOutputArray, whichOutputLine, destWhich++);
						cellId.setIndex(j, whichValue);
					}
					whichCell = cellId.toMasterIndex(3);
					if(inInstanceClass == 1)
						++table.cellCaseCount[whichCell];
					else
						++table.cellControlCount[whichCell];
				}
			}
			
			valueToOutput(inInstanceClass, inOutputStream, false, inOutputArray, whichOutputLine, destWhich++);
			if(inOutputStream != null)
				inOutputStream.println();
			++whichOutputLine;
		}
		
//		// Output for testing:
//		if(inInstanceClass == 0)
//		{
//			NumberFormat nf = NumberFormat.getInstance();
//			nf.setMaximumFractionDigits(4);
//			for(int whichTable = 0; whichTable < inTables.length; ++whichTable)
//			{
//				PenetranceTable table = inTables[whichTable];
//				System.out.println("Table " + whichTable + ", penetrances");
//				for(int j = 0; j < 3; ++j)
//				{
//					for(int k = 0; k < 3; ++k)
//					{
//						System.out.print(nf.format(table.cells[3 * j + k].getValue()) + "\t");
//					}
//					System.out.println();
//				}
//				System.out.println();
//				System.out.println("Table " + whichTable + ", allele frequencies");
//				double[] alleleFrequencies0 = new double[3];
//				double[] alleleFrequencies1 = new double[3];
//				table.getAlleleFrequencies(0, alleleFrequencies0);
//				table.getAlleleFrequencies(1, alleleFrequencies1);
//				for(int j = 0; j < 3; ++j)
//				{
//					for(int k = 0; k < 3; ++k)
//					{
//						System.out.print(nf.format(alleleFrequencies0[j]  * alleleFrequencies1[k]) + "\t");
//					}
//					System.out.println();
//				}
//				System.out.println();
//				System.out.println("Table " + whichTable + ", case-counts");
//				for(int j = 0; j < 3; ++j)
//				{
//					for(int k = 0; k < 3; ++k)
//					{
//						System.out.print(table.cellCaseCount[3 * j + k] + "\t");
//					}
//					System.out.println();
//				}
//				System.out.println();
//				System.out.println("Table " + whichTable + ", control-counts");
//				for(int j = 0; j < 3; ++j)
//				{
//					for(int k = 0; k < 3; ++k)
//					{
//						System.out.print(table.cellControlCount[3 * j + k] + "\t");
//					}
//					System.out.println();
//				}
//				System.out.println();
//				System.out.println();
//			}
//		}
	}
	
	private static int noiseToOutput(Random inRandom, double[] inAlleleFrequencies, PrintWriter inOutputStream, int[][] inOutputArray, int inWhichOutputLine, int inWhichOutputColumn)
	{
		int outWhich;
		double rand = inRandom.nextDouble();
		if(rand < inAlleleFrequencies[0])
		{
			outWhich = 0;
			valueToOutput(kMajorMajor, inOutputStream, true, inOutputArray, inWhichOutputLine, inWhichOutputColumn);
		}
		else if(rand < inAlleleFrequencies[0] + inAlleleFrequencies[1])
		{
			outWhich = 1;
			valueToOutput(kMajorMinor, inOutputStream, true, inOutputArray, inWhichOutputLine, inWhichOutputColumn);
		}
		else
		{
			outWhich = 2;
			valueToOutput(kMinorMinor, inOutputStream, true, inOutputArray, inWhichOutputLine, inWhichOutputColumn);
		}
		return outWhich;
	}
	
	private static void valueToOutput(int inValue, PrintWriter inOutputStream, boolean inTabAfter, int[][] inOutputArray, int inWhichOutputLine, int inWhichOutputColumn)
	{
		if(inOutputStream != null)
		{
			inOutputStream.print(inValue);
			if(inTabAfter)
				inOutputStream.print("\t");
		}
		if(inOutputArray != null)
			inOutputArray[inWhichOutputLine][inWhichOutputColumn] = inValue;
	}
}
