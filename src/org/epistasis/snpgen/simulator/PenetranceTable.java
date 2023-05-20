package org.epistasis.snpgen.simulator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Random;

public class PenetranceTable implements Cloneable
{
	public enum ErrorState
	{
		None, Ambiguous, Conflict
	};

	private static final double kMaxPenetranceValue = 1.0D;
	private static final double kValueMax = 0.95;
	private static final double kPenetranceSum = 0.0D;		// kPenetranceSum must be 0 for the kValue calculations to work.
	private static final double kErrorLimit = 0.01D;
	private static final int kWhichPenetranceCellNone = -1;

	public static class CellId implements Cloneable
	{
		public int[] indices;

		// Construct a CellId with unspecified indices
		public CellId(int inLength)
		{
			indices = new int[inLength];
		}

		// Construct a CellId with the specified index repeated
		public CellId(int inLength, int inIndex)
		{
			indices = new int[inLength];
			for (int i = 0; i < inLength; ++i)
				indices[i] = inIndex;
		}

		// Construct a CellId with the specified indices
		public CellId(int[] inIndices)
		{
			indices = new int[inIndices.length];
			System.arraycopy(inIndices, 0, indices, 0, inIndices.length);
		}

		// Construct a copy of the given CellId
		public CellId(CellId inCellId)
		{
			this(inCellId.indices);
		}

		public Object clone() throws CloneNotSupportedException
		{
			CellId outClone = new CellId(this);
			return outClone;
		}
		
		public void clear()
		{
			Arrays.fill(indices, 0);
		}

		public int getLength()
		{
			return indices.length;
		}

		public void setIndex(int inDimension, int inIndex)
		{
			indices[inDimension] = inIndex;
		}

		public int getIndex(int inDimension)
		{
			return indices[inDimension];
		}
		
		public boolean matchesOnAnyDimension(CellId inCellId)
		{
			boolean outMatches = false;
			for(int i = 0; i < inCellId.indices.length; ++i)
			{
				if(indices[i] == inCellId.indices[i])
				{
					outMatches = true;
					break;
				}
			}
			return outMatches;
		}

		public void copyFrom(CellId inCellId)
		{
			assert (indices.length == inCellId.indices.length);
			for(int i = 0; i < inCellId.indices.length; ++i)
				setIndex(i, inCellId.getIndex(i));
		}
		
		// Incrementing the master-index amounts to incrementing the cellId in the following order (for, say, a 3-dimensional cellId):
		// 000, 100, 200, 010, 110, 210, 020, 120, 220, 001, 101, 201, 011, ...
		// This would seem to be backwards, but it means that the least-significant entry of the incrementing corresponds to minor-allele-frequency[0], the next to MAF[1], etc. 
		// This is needed in order to get both of the following properties:
		// 1. Incrementing the master-index goes first across rows, then down columns, then from one square to the next.
		// 2. MAF[0] goes across rows, MAF[1] goes down columns, and MAF[2] goes across squares.
		public CellId fromMasterIndex(int inSnpStateCount, int inAttributeCount, int inMasterIndex)
		{
			int index;
			assert (0 <= inMasterIndex && inMasterIndex < (int) Math.round(Math.pow(inSnpStateCount, inAttributeCount)));
			index = inMasterIndex;
			for(int i = 0; i < inAttributeCount; ++i)
//			for(int i = inAttributeCount - 1; i >= 0; --i)
			{
				setIndex(i, index % inSnpStateCount);
				index /= inSnpStateCount;
			}
			return this;
		}
		
		public int toMasterIndex(int inSnpStateCount, int inAttributeCount)
		{
			int foo;
			if(getLength() != inAttributeCount)
				foo = 0;
			assert (getLength() == inAttributeCount);
			return toMasterIndex(inSnpStateCount);
		}
		
		public int toMasterIndex(int inSnpStateCount)
		{
			int index;
			int attributeCount = getLength();
			index = 0;
			for(int i = attributeCount - 1; i >= 0; --i)
//			for(int i = 0; i < attributeCount; ++i)
				index = index * inSnpStateCount + getIndex(i);
			return index;
		}
		
		public void print()
		{
			for (int i = 0; i < indices.length; ++i)
				System.out.print(indices[i] + ", ");
		}
	}

	public static class PenetranceCell implements Cloneable
	{
		public boolean isSet;
		private double value;
		public boolean isBasisElement;
		private int whichBasisElement;
		
		public PenetranceCell()
		{
			clear();
		}

		public PenetranceCell(PenetranceCell inCell)
		{
			setValue(inCell.getValue());
			isSet = inCell.isSet;
			setWhichBasisElement(inCell.getWhichBasisElement());
			isBasisElement = inCell.isBasisElement;
		}

		public PenetranceCell(double inValue)
		{
			setValue(inValue);
			isSet = true;
			isBasisElement = false;
		}

		public PenetranceCell(double inValue, int inWhichBasisElement)
		{
			setValue(inValue);
			isSet = true;
			setWhichBasisElement(inWhichBasisElement);
			isBasisElement = true;
		}
		
		public Object clone() throws CloneNotSupportedException
		{
			return super.clone();
		}
		
		public void clear()
		{
			isSet = false;
			isBasisElement = false;
		}

		public void setValue(double value)
		{
			this.value = value;
			this.isSet = true;
		}

		public double getValue()
		{
			return value;
		}

		public void setWhichBasisElement(int whichBasisElement)
		{
			this.whichBasisElement = whichBasisElement;
			this.isBasisElement = true;
		}

		public int getWhichBasisElement()
		{
			return whichBasisElement;
		}
	}

	public static class PenetranceCellWithId extends PenetranceCell
	{
		public CellId cellId;

		public PenetranceCellWithId(CellId inCellId)
		{
			super();
			cellId = new CellId(inCellId);
		}

		public PenetranceCellWithId(CellId inCellId, double inValue)
		{
			super(inValue);
			cellId = new CellId(inCellId);
		}

		public PenetranceCellWithId(CellId inCellId, double inValue, Integer inWhichBasisElement)
		{
			super(inValue, inWhichBasisElement);
			cellId = new CellId(inCellId);
		}
	}

	// The basis of a penetrance table is the set of independent parameters that
	// are used to generate the table.
	public static class BasisCell implements Cloneable
	{
		public boolean isSet;
		public double value;
		public int whichPenetranceCell; // master-index

		public BasisCell()
		{
			isSet = false;
		}

		public BasisCell(BasisCell inCell)
		{
			isSet = inCell.isSet;
			value = inCell.value;
			whichPenetranceCell = inCell.whichPenetranceCell;
		}

		public BasisCell(double inValue)
		{
			isSet = true;
			value = inValue;
			whichPenetranceCell = kWhichPenetranceCellNone;
		}

		public BasisCell(double inValue, int inWhichPenetranceCell)
		{
			isSet = true;
			value = inValue;
			whichPenetranceCell = inWhichPenetranceCell;
		}
		
		public Object clone() throws CloneNotSupportedException
		{
			return super.clone();
		}
	}
	
	public static class PenetranceTableComparatorEdm implements Comparator<PenetranceTable>
	{
		public int compare(PenetranceTable in1, PenetranceTable in2)
		{
			if(!(in1 instanceof PenetranceTable) || !(in2 instanceof PenetranceTable))
				throw new ClassCastException();
			else
			{
				if(in1.edm < in2.edm)
					return -1;
				else if(in1.edm > in2.edm)
					return 1;
				else return 0;
			}
		}
	}
	
	public static class PenetranceTableComparatorOddsRatio implements Comparator<PenetranceTable>
	{
		public int compare(PenetranceTable in1, PenetranceTable in2)
		{
			if(!(in1 instanceof PenetranceTable) || !(in2 instanceof PenetranceTable))
				throw new ClassCastException();
			else
			{
				if(in1.oddsRatio < in2.oddsRatio)
					return -1;
				else if(in1.oddsRatio > in2.oddsRatio)
					return 1;
				else return 0;
			}
		}
	}
	
	// Penetrance-table parameters:
	public int attributeCount;
	private String[] attributeNames;
	public int snpStateCount;
	public int cellsPicked;

	public double desiredHeritability;
	private double actualHeritability;
	public int interactingAttributeCount;
	public double prevalence;
	public Float desiredPrevalence;
	public double edm;
	public double oddsRatio;
	
	public double fractionContribution;		// Fractional contribution that this table/model will make to a dataset, when heterogeneity is used. (A bit of a kluge.)

	public boolean useOriginAsStart;
	public double[] minorAlleleFrequencies;
	public double[] majorAlleleFrequencies;
	public double[][] stateProbability;
	public CellId startPoint;
	public int cellCount;
	public int basisSize;
	public BasisCell[] basis;
	public int basisNext;
	public LinkedList<PenetranceCellWithId> pendingCellsToSet;
	
	public static int fixedConflictSuccessfully = 0;
	public static int fixedConflictUnsuccessfully = 0;
	public boolean normalized;
	public boolean rowSumsValid;
	public PenetranceCell[] cells;
	public int[] cellCaseCount;
	public int[] cellControlCount;
	public double[] caseIntervals;
	public double[] controlIntervals;
	private boolean usePointMethod;
	private CellId blockedOutCellForPointMethod;
	private int nextMasterCellIdForPointMethod;
	
	public PenetranceTable(int inSnpStateCount, int inAttributeCount)
	{
		snpStateCount = inSnpStateCount;
		attributeCount = inAttributeCount;
		usePointMethod = (attributeCount >= 6);
		cellCount = 1;
		basisSize = 1;
		for (int i = 0; i < attributeCount; ++i)
		{
			cellCount *= snpStateCount;
			basisSize *= (snpStateCount - 1);
		}
		normalized = false;
		minorAlleleFrequencies = new double[attributeCount];
		majorAlleleFrequencies = new double[attributeCount];
		stateProbability = new double[attributeCount][snpStateCount];
		startPoint = new CellId(attributeCount);
		cells = new PenetranceCell[cellCount];
		cellCaseCount = new int[cellCount];
		cellControlCount = new int[cellCount];
		// penetranceIsSet = new boolean[penetranceArraySize];
		pendingCellsToSet = new LinkedList<PenetranceCellWithId>();
		basis = new BasisCell[basisSize];
		basisNext = -1;
		
		// TODO: Could reuse the cells from run to run, to save time on garbage-collection
		for (int i = 0; i < cellCount; ++i)
			cells[i] = new PenetranceCell();
		clear();
	}
	
	public Object clone() throws CloneNotSupportedException
	{
		PenetranceTable pt = (PenetranceTable) super.clone();
		pt.attributeNames = Arrays.copyOf(attributeNames, attributeNames.length);
		pt.minorAlleleFrequencies = Arrays.copyOf(minorAlleleFrequencies, minorAlleleFrequencies.length);
		pt.majorAlleleFrequencies = Arrays.copyOf(majorAlleleFrequencies, majorAlleleFrequencies.length);

		pt.stateProbability = new double[stateProbability.length][];
		for(int i = 0; i < stateProbability.length; ++i)
			pt.stateProbability[i] = Arrays.copyOf(stateProbability[i], stateProbability[i].length);

		pt.basis = new BasisCell[basis.length];
		for(int i = 0; i < basis.length; ++i)
		{
			if(basis[i] == null)
				pt.basis[i] = null;
			else
				pt.basis[i] = (BasisCell) basis[i].clone();
		}

		pt.cells = new PenetranceCell[cells.length];
		for(int i = 0; i < cells.length; ++i)
		{
			pt.cells[i] = (PenetranceCell) cells[i].clone();
			pt.cellCaseCount[i] = cellCaseCount[i];
			pt.cellControlCount[i] = cellControlCount[i];
		}
		
		return pt;
	}

	public void clear()
	{
		for (int i = 0; i < cellCount; ++i)
			cells[i].clear();
		
		for (int i = 0; i < cellCount; ++i)
		{
			cellCaseCount[i] = 0;
			cellControlCount[i] = 0;
		}
	}

	public void setMinorAlleleFrequencies(float[] inMinorAlleleFrequencies)
	{
		assert inMinorAlleleFrequencies.length == attributeCount;
		for (int i = 0; i < attributeCount; ++i)
		{
			minorAlleleFrequencies[i] = inMinorAlleleFrequencies[i];
			majorAlleleFrequencies[i] = 1 - minorAlleleFrequencies[i];
		}

		// stateProbability[i][0] == the probability of each state of the ith attribute.
		// If snpStateCount == 3, stateProbability[i][0] == prob of AA, [i][1] == prob of Aa or aA, and [i][2] == prob of aa;
		// ie, the major allele comes first and the minor allele comes last.
		// What do others mean?
		// 4 = AAA, AAa or AaA or aAA, Aaa or aAa or aaA, aaa
		// etc (binomial expansion)

		int comb = 1; // N = snpStateCount - 1, comb(N, 0) = N! / (0! * (N-0)!) = 1
		for (int j = 0; j < snpStateCount; ++j)
		{
			// System.out.println("comb(" + (snpStateCount - 1) + ", " + j + ") = " + comb);
			for (int i = 0; i < attributeCount; ++i)
			{
				stateProbability[i][j] = comb * Math.pow(minorAlleleFrequencies[i], j) * Math.pow(majorAlleleFrequencies[i], snpStateCount - j - 1);
			}
			// Calculate the next comb:
			// N = snpStateCount - 1, comb(N, j) = N! / (j! * (N-j)!)
			// comb(N, j+1) = comb(N, j) * (N - (j-1)) / j
			comb *= snpStateCount - 1 - j; // Divide the denominator by (N-j)
			comb /= j + 1; // Multiply the denominator by the next j
		}
	}
	
	// frequency[0] is the major-major allele and frequency[2] is the minor-minor allele.
	public double[] getAlleleFrequencies(int inWhichAttribute, double[] outAlleleFrequencies)
	{
		double maf = minorAlleleFrequencies[inWhichAttribute];
		return calcAlleleFrequencies(maf, outAlleleFrequencies);
	}
	
	// frequency[0] is the major-major allele and frequency[2] is the minor-minor allele.
	public static double[] calcAlleleFrequencies(double maf, double[] outAlleleFrequencies)
	{
		outAlleleFrequencies[0] = (1.0 - maf) * (1.0 - maf);		// major-major
		outAlleleFrequencies[1] = 2.0 * maf * (1.0 - maf);			// major-minor
		outAlleleFrequencies[2] = maf * maf;						// minor-minor
		return outAlleleFrequencies;
	}
	
	public void setAttributeNames(String[] attributeNames)
	{
		this.attributeNames = attributeNames;
	}

	public String[] getAttributeNames()
	{
		return attributeNames;
	}

	public double[] getMinorAlleleFrequencies()
	{
		return minorAlleleFrequencies;
	}
	
	public double getActualHeritability()
	{
		return actualHeritability;
	}
	
	public void initialize(Random inRandom, float[] inMinorAlleleFrequencies)
	{
		rowSumsValid = false;
		normalized = false;
		
		useOriginAsStart = false;
		
		setMinorAlleleFrequencies(inMinorAlleleFrequencies);

		double value;
		double basisSquaredSum = 0;
		// Generate basis parameters and normalize them:
		for (int i = 0; i < basisSize; ++i)
		{
			value = inRandom.nextDouble();
			basis[i] = new BasisCell(value, kWhichPenetranceCellNone);
			basisSquaredSum += value * value;
		}
		double normalizingFactor = 1 / Math.sqrt(basisSquaredSum);
		// Make the norm of the basis = 1:
		for (int i = 0; i < basisSize; ++i)
		{
			basis[i].value *= normalizingFactor;
		}
		basisNext = 0;

		// start with random SNP states if desired, else use (0,0,...,0)
		for (int i = 0; i < attributeCount; ++i)
		{
			if (useOriginAsStart)
				startPoint.setIndex(i, 0);
			else
				startPoint.setIndex(i, inRandom.nextInt(snpStateCount));
		}
	}

	private ErrorState generateFakeUnnormalizedPenetranceTable() throws Exception
	{
		for(int i = 0; i < cellCount; ++i)
			cells[i] = new PenetranceCell(i%2);
		return ErrorState.None;
	}

	public ErrorState generateUnnormalized(Random inRandom) throws Exception
	{
		ErrorState error;
		ErrorState outError = ErrorState.None;
		CellId cellId = new CellId(attributeCount);
		double penetranceValue;
		boolean emptyCellsRemaining;
		
		if(usePointMethod)
		{
			blockedOutCellForPointMethod = new CellId(attributeCount);
			masterIndexToCellId(inRandom.nextInt(cellCount), blockedOutCellForPointMethod);
			nextMasterCellIdForPointMethod = 0;
		}
		
		long foo = inRandom.nextLong();
//		foo = 8442823125929271046L;
		inRandom.setSeed(foo);
		cellsPicked = 0;
		while (true)
		{
			emptyCellsRemaining = emptyCellRemaining();
			if (!emptyCellsRemaining)
				break;
			pickNextEmptyCell(inRandom, cellId);
			++cellsPicked;
			// if(cellsPicked > basisSize)
			// {
			// outError = ErrorState.Ambiguous;
			// break;
			// }
			error = setRandomPenetranceValueAndPropagateIt(cellId);
			if (error != ErrorState.None)
			{
				outError = error;
				break;
			}
		}
		return outError;
	}

	// Return true if there are any cells not set yet, false otherwise.
	private boolean emptyCellRemaining()
	{
		boolean outFoundEmpty;

		outFoundEmpty = false;
		for (int i = 0; i < cellCount; ++i)
		{
			if (!cells[i].isSet)
			{
				outFoundEmpty = true;
				break;
			}
		}
		return outFoundEmpty;
	}

	// Return the count of any cells not set yet
	public int countRemainingEmptyCells()
	{
		int outEmpty;

		outEmpty = 0;
		for (int i = 0; i < cellCount; ++i)
		{
			if (!cells[i].isSet)
				++outEmpty;
		}
		return outEmpty;
	}

	// // Return true if there are any cells not set yet, false otherwise.
	// private boolean copyIsSetArray(boolean[] inSource, boolean[] inDest)
	// {
	// boolean outFoundEmpty;
	//		
	// assert(inSource.length == inDest.length);
	// outFoundEmpty = false;
	// for(int i = 0; i < inSource.length; ++i)
	// {
	// outFoundEmpty |= !inSource[i];
	// inDest[i] = inSource[i];
	// }
	// return outFoundEmpty;
	// }

	// inPreviousAttempts can be used for randomization if we want determinism
	private CellId pickNextEmptyCell(Random inRandom, CellId outCellId) throws Exception
	{
		int attempts;
		int masterIndex;

		// // For testing only -- use the block from 0 to snpStateCount - 2 (ie,
		// 1, in most cases) in each dimension:
		// outCellId.copyFrom(currentRandomCell);
		// int currentIndex;
		// int i = snpCount - 1;
		// boolean success = false;
		// while(i >= 0)
		// {
		// currentIndex = currentRandomCell.getIndex(i);
		// if(currentIndex >= snpStateCount - 2)
		// {
		// // If the current index is maxed out then set it to zero and move
		// back to the index before it:
		// currentRandomCell.setIndex(i, 0);
		// --i;
		// }
		// else
		// {
		// currentRandomCell.setIndex(i, currentIndex + 1);
		// success = true;
		// break;
		// }
		// }

		// THE REAL VERSION:
		if(usePointMethod)
		{
			boolean found = false;
			while(nextMasterCellIdForPointMethod < cellCount)
			{
				masterIndexToCellId(nextMasterCellIdForPointMethod++, outCellId);
				if(!blockedOutCellForPointMethod.matchesOnAnyDimension(outCellId))
				{
					// The point we're returning is not on one of the blocked-out staves, so it should not be set yet:
					assert !cells[nextMasterCellIdForPointMethod - 1].isSet;
					found = true;
					break;
				}
			}
			if(!found)
				throw new Exception("Unable to find an empty cell that works");
		}
		else
		{
			attempts = 0;
			while (true)
			{
				masterIndex = inRandom.nextInt(cellCount);
				if(!cells[masterIndex].isSet)
					break;
				if(attempts > 100)
					throw new Exception("Unable to find an empty cell that works");
			}
			masterIndexToCellId(masterIndex, outCellId);
		}
		return outCellId;
	}

	// private double pickRandomPenetranceValue(CellId inCellId)
	// {
	// // return 1.0;
	// // return random.nextGaussian();
	// assert(penetranceBasisNext < penetranceBasisSize);
	// return penetranceBasisValue[penetranceBasisNext++];
	// }

	// Returns true if the value at inCellId causes a conflict, false otherwise.
	private ErrorState setRandomPenetranceValueAndPropagateIt(CellId inCellId) throws Exception
	{
		double penetranceValue;
		ErrorState outError = ErrorState.None;
		int filledCells;
		PenetranceCellWithId penetranceCell;
		PenetranceCellWithId currPenetranceCell;
		CellId tempCellId = new CellId(attributeCount);
		CellId emptyCellId = new CellId(attributeCount);
		double sum;

		penetranceCell = new PenetranceCellWithId(inCellId);
		penetranceCell.isBasisElement = true;
		pendingCellsToSet.add(penetranceCell);
		QUEUE: while ((currPenetranceCell = pendingCellsToSet.poll()) != null)
		{
			// TODO: If we add a penetranceIsPending global array, we can avoid
			// putting redundant entries in the queue in the first place.
			if (!getPenetranceIsSet(currPenetranceCell.cellId))
			{
				assert (currPenetranceCell.isSet || currPenetranceCell.isBasisElement);
				if (currPenetranceCell.isBasisElement)
				{
					if (basisNext >= basisSize)
					{
						outError = ErrorState.Ambiguous;
						break QUEUE;
					}
					currPenetranceCell.setValue(basis[basisNext].value);
					currPenetranceCell.setWhichBasisElement(basisNext);
					++basisNext;
				}
				savePenetranceCell(currPenetranceCell);
				boolean foundError = false;
				// Check each "row" in the snpCount-dimensional hypercube that goes through currPenetranceCell.cellId:
				for (int whichDimension = 0; whichDimension < attributeCount; ++whichDimension)
				{
					// For each cell in the current "row" of the hypercube, count how many cells are set:
					filledCells = countFilledCells(currPenetranceCell.cellId, whichDimension, emptyCellId);
					// If the current row is all filled in, then check for a conflict:
					if (filledCells == snpStateCount)
					{
						sum = calculateWeightedSumOfSetPenetranceValues(currPenetranceCell.cellId, whichDimension);
						if (Math.abs(sum - kPenetranceSum) > kErrorLimit)
						{
							foundError = true;
							// Don't bother trying to fix conflicts, it only works a small fraction of the time.
//							if (!fixConflict(currPenetranceCell, whichDimension))
							{
								outError = ErrorState.Conflict;
								break QUEUE;
							}
						}
					}

					// If there are snpStateCount - 1 cells set in the current row,
					// then we can propagate to the remaining empty cell:
					if (filledCells == snpStateCount - 1)
					{
						assert (!getPenetranceIsSet(emptyCellId));
						pendingCellsToSet.add(new PenetranceCellWithId(emptyCellId, calculateForcedPenetranceValue(emptyCellId, whichDimension)));
					}
				}
				if(foundError)
				{
					if(!checkRowSums(0))
					{
						// fixConflict said that it succeeded, but it actually failed:
//						System.out.println("Failed to fix a conflict, but thought we succeeded!");
						++fixedConflictUnsuccessfully;
						outError = ErrorState.Conflict;
						break QUEUE;
					}
					else
					{
						++fixedConflictSuccessfully;
//						System.out.println("Successfully fixed a conflict");
					}
				}

			}
//			checkRowSums(0);
		}
		return outError;
	}

	private boolean fixConflict(PenetranceCellWithId inCell, int inWhichDimension) throws Exception
	{
		boolean outSuccess;
		int conflictingBasisElement;
		CellId tempCellId = new CellId(inCell.cellId);
		CellId cellContainingBasisElement = new CellId(inCell.cellId);
		BasisCell basisCell;
		double newValue;
		CellId otherBasisElement;

		// Find the element in the conflicted row which was most recently chosen
		// from the basis
		conflictingBasisElement = -1; // sentinel
		for (int i = 0; i < snpStateCount; ++i)
		{
			tempCellId.setIndex(inWhichDimension, i);
			if (getIsBasisElement(tempCellId))
			{
				if (conflictingBasisElement < getWhichBasisElement(tempCellId))
				{
					conflictingBasisElement = getWhichBasisElement(tempCellId);
					cellContainingBasisElement.copyFrom(tempCellId);
				}
			}
		}
		outSuccess = false;
		int basisLast = basisNext - 1;
		// If conflictingBasisElement == basisLast then we can't make it into a
		// dependent parameter,
		// because it is not uniquely determined by the prior basis elements
		// (or else the cell containing it would have been filled in already,
		// and not chosen as a random empty cell).
		// Plus the swapping-code below won't work, because RB will be used
		// after it is cleared.
		// So we announce failure.
		// TODO: We could just discard the CB/RB cell completely, and force
		// ourselves to choose another empty cell to try.
		// TODO: We're assuming that if CB is an old basis element and it is in
		// a conflicted row,
		// then it is determined by the basis elements before it and the ones
		// after it -- but is that true?
		// Maybe it could be the result of a feedback loop that happens to also
		// require one of the later basis elements to complete.
		if (conflictingBasisElement > -1 && conflictingBasisElement != basisLast)
		{
			// The most recent basis element in the conflicted row, CB, is
			// currently an independent parameter
			// (that's what it means to be a basis element).
			// Make CB a dependent parameter, and put its value back into the
			// basis-pool.
			// To make CB a dependent parameter, mark it as not being a basis
			// element
			// and recalculate its value based on the other values in the row.
			// To put it back into the basis-pool, take the most recently-used
			// value in the basis-pool, RB --
			// put the value of RB in CB's slot in the basis-pool, put the value
			// of CB in RB's slot in the basis-pool
			// (the last currently-used slot), and decrement penetranceBasisNext
			// to point to that slot:
			// then CB will be the next value to be used.
			// When swapping CB and RB in the basis-pool, find the cell in the
			// penetrance-table which points to RB
			// and change it to point to RB's new location.
			// (Note: CB and RB might happen to be the same -- but the swap will
			// still work in that case,
			// and it doesn't happen often enough to be worth checking for.
			// TODO: What if the conflicted row doesn't have any basis elements
			// in it? Can that happen?

			// First, make CB a dependent parameter:
			clearPenetranceValue(cellContainingBasisElement);
			newValue = calculateForcedPenetranceValue(cellContainingBasisElement, inWhichDimension);
			setPenetranceValue(cellContainingBasisElement, newValue);

			// Then, put CB back into the basis-pool:
			assert (basisNext > 0);
			// Swap penetranceBasisValue[maxBasisWhich] into the next available
			// slot in the basis and make it available for re-use:
			--basisNext; // point to the last used slot in the basis

			// Find the cell in the penetrance-table which points to RB
			// and change it to point to RB's new location, which is CB's old
			// location:
			otherBasisElement = new CellId(attributeCount); // The CellId for RB
			masterIndexToCellId(basis[basisNext].whichPenetranceCell, otherBasisElement);
			setWhichBasisElement(otherBasisElement, conflictingBasisElement);

			// Swap the basis elements:
			basisCell = basis[basisNext];
			basis[basisNext] = basis[conflictingBasisElement];
			basis[conflictingBasisElement] = basisCell;
			// Now, basisNext points to conflictingBasisElement, after the end
			// of the "used" basis elements,
			// and CB/conflictingBasisElement is ready to be used again.
			outSuccess = true;
		}
		return outSuccess;
	}
	
	public void normalize()
	{
		scaleToUnitInterval();
		// adjustPenetrance() must be done before adjustHeritability() because adjustHeritability() preserves penetrance, but not vice-versa.
		adjustPrevalence();
		adjustHeritability();
	}
	
	public void scaleToUnitInterval()
	{
		double		min, max;
		double		slope;
		
		max = cells[0].getValue();
		min = cells[0].getValue();
		for (PenetranceCell c : cells)
		{
			if (max < c.getValue())
				max = c.getValue();
			if (min > c.getValue())
				min = c.getValue();
		}
		// At this point, min must be < 0 and max must be > 0, from the way the penetrance table was constructed.
		// We want slope * min + kValue = 0
		// and     slope * max + kValue = 1.
		// So kValue = min / (min - max)
		// and slope = - kValue / min.
		prevalence = min / (min - max);
		if(prevalence > kValueMax)
			prevalence = kValueMax;
		slope = - prevalence / min;
		for (PenetranceCell c : cells)
			c.setValue(slope * c.getValue() + prevalence);
		// The unnormalized penetrance table was constructed to have a weighted average == 0,
		// so the new penetrance table has a weighted average == prevalence.
	}
	
	public void adjustPrevalence()
	{
		double scale = 1, offset = 0;
		
		if(desiredPrevalence != null && desiredPrevalence != prevalence)
		{
			if(desiredPrevalence < prevalence)
			{
				scale = desiredPrevalence / prevalence;
			}
			else if(desiredPrevalence > prevalence)
			{
				scale = (1 - desiredPrevalence) / (1 - prevalence);
				offset = (desiredPrevalence - prevalence) / (1 - prevalence);
			}
			for (PenetranceCell c : cells)
			{
				c.setValue(scale * c.getValue() + offset);
//				System.out.println(c.getValue());
				assert(-kErrorLimit < c.getValue() && c.getValue() < 1F + kErrorLimit);
			}
			calcAndSetPrevalence();
			assert Math.abs(prevalence - desiredPrevalence) < kErrorLimit;
		}
	}
	
	public void adjustHeritability()
	{
		boolean		success;
		double		herit;
		double		factor;
		
		herit = calcHeritability();
//		System.out.println("Penetrance-table size: " + penetranceTableSize + ", kValue: " + kValue + ", Raw heritability: " + herit);
		factor = Math.sqrt(desiredHeritability / herit);
		if(factor > 1.0D)
			success = false;
		else
		{
			success = true;
			for (PenetranceCell c : cells)
				c.setValue(factor * c.getValue() + prevalence * (1 - factor));		// The intercept allows us to preserve the value of K and makes the heritability-scaling work.
			assert Math.abs(calcPrevalence() - prevalence) < kErrorLimit;
			calcAndSetHeritability();
//			System.out.println("factor, old herit, new herit, desired herit:\t" + factor + "\t " + herit + "\t " + actualHeritability + "\t " + desiredHeritability);
			assert Math.abs(actualHeritability - desiredHeritability) < kErrorLimit;
		}
		if(success)
		{
			edm = calcEdm();
			oddsRatio = calcOddsRatio();
		}
		normalized = success;
	}
	
	public double calcAndSetPrevalence()
	{
		prevalence = calcPrevalence();
		return prevalence;
	}
	
	private double calcPrevalence()
	{
		double outPrevalence;
		double prob;
		CellId cellId = new CellId(attributeCount);
		
		outPrevalence = 0;
		for(int i = 0; i < cellCount; ++i)
		{
			masterIndexToCellId(i, cellId);
			prob = getProbabilityProduct(cellId);
			outPrevalence += prob * cells[i].getValue();
		}
		return outPrevalence;
	}
	
	// outMarginalPenetrances[whichLocus][whichAlleleValue]
	public double[][] calcMarginalPrevalences()
	{
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(3);
		double[][] outMarginalPenetrances = new double[attributeCount][snpStateCount];
		CellId cellId = new CellId(attributeCount);
		// For each locus,
		for(int whichLocus = 0; whichLocus < attributeCount; ++whichLocus)
		{
//			System.out.println();
//			System.out.println();
//			System.out.println("-------------------");
//			System.out.println("Locus " + whichLocus);
//			System.out.println("-------------------");
//			System.out.println();
			// for each allele-value for that locus,
			for(int whichAlleleValue = 0; whichAlleleValue < snpStateCount; ++whichAlleleValue)
			{
//				System.out.println();
//				System.out.println();
//				System.out.println("Allele-value " + whichAlleleValue);
//				System.out.println("-------------------");
//				System.out.println();
				double prevalence;
				double prob;
				
				prevalence = 0;
				// iterate over all of the cells in the table,
				for(int i = 0; i < cellCount; ++i)
				{
					masterIndexToCellId(i, cellId);
//					if(i % 3 == 0)
//						System.out.println();
//					if(i % 9 == 0)
//						System.out.println();
//					if(cellId.getIndex(whichLocus) == whichAlleleValue)
//						System.out.print(nf.format(cells[i].getValue()) + "\t");
//					else
//						System.out.print("-" + "\t");
					// and if the current cell matches whichLocus and whichAlleleValue,
					if(cellId.getIndex(whichLocus) == whichAlleleValue)
					{
						// then add it the cumulative prevalence value:
						prob = getProbabilityProduct(cellId);
						prevalence += prob * cells[i].getValue();
					}
				}
				outMarginalPenetrances[whichLocus][whichAlleleValue] = prevalence;
			}
		}
//		System.out.println();
//		System.out.println();
//		System.out.println("-------------------");
//		System.out.println("Full table");
//		System.out.println("-------------------");
//		System.out.println();
//		try
//		{
//			PrintWriter writer = new PrintWriter(System.out);
//			writer.println();
//			writer.println();
//			writer.println("-------------------");
//			writer.println();
//			write(writer);
//			writer.println();
//			writer.println();
//			writer.println("-------------------");
//			writer.println();
//			writer.flush();
//		}
//		catch(IOException e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		return outMarginalPenetrances;
	}
	
	public void print()
	{
		for(int i = 0; i < cellCount; ++i)
		{
			if(i % 3 == 0)
				System.out.println();
			if(i % 9 == 0)
				System.out.println();
			System.out.print(cells[i].getValue() + "\t");
		}
	}
	
	public double calcAndSetHeritability()
	{
		actualHeritability = calcHeritability();
		return actualHeritability;
	}
	
	public double calcHeritability()
	{
		double sum;
		double outHeritability;
		double prob;
		double diff;
		CellId cellId = new CellId(attributeCount);
		
		calcAndSetPrevalence();
		sum = 0;
		for(int i = 0; i < cellCount; ++i)
		{
			masterIndexToCellId(i, cellId);
			prob = getProbabilityProduct(cellId);
			diff = cells[i].getValue() - prevalence;
			sum += prob * diff * diff;
		}
		outHeritability = sum / (prevalence * (1 - prevalence));
		return outHeritability;
	}
	
	public double getQuantileScore(boolean inUseOddsRatio)
	{
		if(inUseOddsRatio)
			return oddsRatio;
		else
			return edm;
	}
	
	public double calcAndSetEdm()
	{
		edm = calcEdm();
		return edm;
	}
	
	public double calcEdm()
	{
		double sum;
		double outRas;
		double prob;
		double diff;
		double kProduct;
		CellId cellId = new CellId(attributeCount);
		
		calcAndSetPrevalence();
		sum = 0;
		for(int i = 0; i < cellCount; ++i)
		{
			masterIndexToCellId(i, cellId);
			prob = getProbabilityProduct(cellId);
			diff = cells[i].getValue() - prevalence;
			sum += prob * prob * diff * diff;
		}
		kProduct = prevalence * (1 - prevalence);
		outRas = sum / (2 * kProduct * kProduct);
		return outRas;
	}
	
	public double calcAndSetOddsRatio()
	{
		oddsRatio = calcOddsRatio();
		return oddsRatio;
	}
	
	public double calcOddsRatio()
	{
		double sumTP, sumTN, sumFP, sumFN;
		double outOddsRatio;
		double prob;
		double prev;
		CellId cellId = new CellId(attributeCount);
		
		calcAndSetPrevalence();
		sumTP = 0;
		sumTN = 0;
		sumFP = 0;
		sumFN = 0;
		for(int i = 0; i < cellCount; ++i)
		{
			masterIndexToCellId(i, cellId);
			prob = getProbabilityProduct(cellId);
			prev = cells[i].getValue();
			if(prev >= prevalence)
			{
				sumTP += prob * prev;
				sumFP += prob * (1 - prev);
			}
			else
			{
				sumTN += prob * (1 - prev);
				sumFN += prob * prev;
			}
		}
		outOddsRatio = sumTP * sumTN / (sumFN * sumFP);
		return outOddsRatio;
	}
	
	public void checkRowSums()
	{
		rowSumsValid = checkRowSums(prevalence);
	}
	
	public boolean checkRowSums(double inDesiredRowSum)
	{
		boolean success = true;
		CellId cellId = new CellId(attributeCount);
		double sum;
		
		for (int whichDimension = 0; whichDimension < attributeCount; ++whichDimension)
		{
			// This is a little redundant, but only by a factor of snpStateCount --
			// and it's easier to do than the non-redundant method.
			for(int i = 0; i < cellCount; ++i)
			{
				masterIndexToCellId(i, cellId);
				if(allValuesSetInRow(cellId, whichDimension))
				{
					sum = calculateWeightedSumOfSetPenetranceValues(cellId, whichDimension);
					if (Math.abs(sum - inDesiredRowSum) > kErrorLimit)
					{
						success = false;
//						System.out.println("* * * ERROR * * -- Incorrect row-sum along dimension " + whichDimension + " from cell " + i  + "; correct value is " + prevalence + ", actual value is " + sum);
//						printIn3D();
//						sum = calculateWeightedSumOfSetPenetranceValues(cellId, whichDimension);		// For debugging only
					}
				}
			}
		}
		return success;
	}

	// Calculate caseIntervals such that the length of the interval from caseIntervals[i-1] to caseIntervals[i]
	// == the probability that a random case is in the ith cell of the penetrance table; similarly for controls.
	public void calcSamplingIntervals()
	{
		double prob;
		double penetrance;
		CellId cellId;
		double sumCaseFractions, sumControlFractions;
		// caseIntervals[i] == the right edge of the ith probability-interval for cases; similarly for controls
		
		cellId = new CellId(attributeCount);
		sumCaseFractions = 0;
		sumControlFractions = 0;
		caseIntervals = new double[cellCount];
		controlIntervals = new double[cellCount];
		// Sum up all the case-fractions, storing the partial case-fractions to the caseIntervals array; do the same with controls:
		for(int i = 0; i < cellCount; ++i)
		{
			masterIndexToCellId(i, cellId);
			prob = getProbabilityProduct(cellId);
			penetrance = getPenetranceValue(cellId);
			
			sumCaseFractions += prob * penetrance;
			sumControlFractions += prob * (1 - penetrance);
			caseIntervals[i] = sumCaseFractions;
			controlIntervals[i] = sumControlFractions;
		}
		assert Math.abs(sumCaseFractions + sumControlFractions - 1.0) < kErrorLimit;
		// Divide each element of caseIntervals and controlIntervals by the appropriate total sum.
		for(int i = 0; i < cellCount; ++i)
		{
			caseIntervals[i] /= sumCaseFractions;
			controlIntervals[i] /= sumControlFractions;
		}
	}

	// Calculate outInstanceIntervals such that the length of the interval from outInstanceIntervals[i-1] to outInstanceIntervals[i]
	// == the probability that a random instance is in the ith cell of the penetrance table.
	// This method calculates outInstanceIntervals from the relative values of inCellInstanceCounts.
	public static void calcSamplingIntervals(int inCellCount, int[] inCellInstanceCounts, double[] outInstanceIntervals)
	{
		double sumCaseFractions;
		// caseIntervals[i] == the right edge of the ith probability-interval for cases; similarly for controls
		
		assert inCellInstanceCounts.length == inCellCount; 
		sumCaseFractions = 0;
		// Sum up all the case-fractions, storing the partial case-fractions to the caseIntervals array:
		for(int i = 0; i < inCellCount; ++i)
		{
			sumCaseFractions += inCellInstanceCounts[i];
			outInstanceIntervals[i] = sumCaseFractions;
		}
		// Divide each element of caseIntervals and controlIntervals by the appropriate total sum.
		for(int i = 0; i < inCellCount; ++i)
		{
			outInstanceIntervals[i] /= sumCaseFractions;
		}
	}
	
	// Fill in outCellInstanceCounts with randomly-allocated instances according to inInstanceIntervals
	public static void generateInstanceCountsBySamplingIntervals(Random inRandom, int inTotalInstanceCount, double[] inInstanceIntervals, int[] inCellInstanceLimits, int[] outCellInstanceCounts)
	{
		int cellCount = inInstanceIntervals.length;
		Arrays.fill(outCellInstanceCounts, 0);
		for(int i = 0; i < inTotalInstanceCount; ++i)
		{
			double rand = inRandom.nextDouble();
			for(int k = 0; k < cellCount; ++k)
			{
				if(rand < inInstanceIntervals[k])
				{
					if(inCellInstanceLimits != null && outCellInstanceCounts[k] >= inCellInstanceLimits[k])
						--i;
					else
						++outCellInstanceCounts[k];
					break;
				}
			}
		}
	}
	
	// Generate allele frequencies
	// The index-order is a little counter-intuitive, but this avoids having lots of extra references:
	//	double[][] alleleFrequencyIntervals = new double[snpStateCount == 3][attributeCount];
	public static void generateMinorAlleleFrequencyIntervals(Random random, int inAttributeCount, double inMinorAlleleFreqMin, double inMinorAlleleFreqMax, double[][] outAlleleFrequencyIntervals)
	{
		for(int i = 0; i < inAttributeCount; ++i)
		{
			double maf = random.nextDouble() * (inMinorAlleleFreqMax - inMinorAlleleFreqMin) + inMinorAlleleFreqMin;
			outAlleleFrequencyIntervals[0][i] = (1.0 - maf) * (1.0 - maf);
			outAlleleFrequencyIntervals[1][i] = outAlleleFrequencyIntervals[0][i] + 2.0 * maf * (1.0 - maf);
			outAlleleFrequencyIntervals[2][i] = outAlleleFrequencyIntervals[1][i] + maf * maf;
		}
	}
	
	public static void generateInstanceCountsByMinorAlleleFrequencies(Random random, int inTotalInstanceCount, double[][] inAlleleFrequencyIntervals, int[] outCellInstanceCounts)
	{
		double rand;
		int attributeCount = inAlleleFrequencyIntervals[0].length;
		CellId cellId = new CellId(attributeCount);
		
		Arrays.fill(outCellInstanceCounts, 0);
		for(int i = 0; i < inTotalInstanceCount; ++i)
		{
			for(int j = 0; j < attributeCount; ++j)
			{
				rand = random.nextDouble();
				if(rand < inAlleleFrequencyIntervals[0][j])
					cellId.indices[j] = 0;
				else if(rand < inAlleleFrequencyIntervals[1][j])
					cellId.indices[j] = 1;
				else
					cellId.indices[j] = 2;
			}
			++outCellInstanceCounts[cellId.toMasterIndex(3, attributeCount)];
		}
	}
	
	public void saveToFile(File inDestFile, boolean inAppend, boolean inSaveUnnormalized) throws IOException
	{
		PrintWriter outputStream = null;

		try
		{
			outputStream = new PrintWriter(new FileWriter(inDestFile, inAppend));
			if(inAppend)
			{
				outputStream.println();
				outputStream.println();
				outputStream.println();
				outputStream.println();
			}
			writeWithStats(outputStream, inSaveUnnormalized);
		}
		finally
		{
			if (outputStream != null)
			{
				outputStream.close();
			}
		}
	}
	
	public void writeWithStats(PrintWriter outputStream, boolean inSaveUnnormalized) throws IOException
	{
//		outputStream.println("Attribute count: " + attributeCount);
		outputStream.print("Attribute names:");
		for(String n: attributeNames)
			outputStream.print("\t" + n);
		outputStream.println();
		outputStream.print("Minor allele frequencies:");
		for(Double freq: this.minorAlleleFrequencies)
			outputStream.print("\t" + freq);
		outputStream.println();
		if(!normalized && !inSaveUnnormalized)
		{
			outputStream.println("Failed to normalize penetrance table!");
		}
		else
		{
			outputStream.println("K: " + prevalence);
			outputStream.println("Heritability: " + actualHeritability);
			outputStream.println("Ease-of-detection metric: " + edm);
			outputStream.println("Odds ratio: " + oddsRatio);
			if(normalized)
			{
				if(rowSumsValid)
					outputStream.println("Table has passed the row-sum test.");
				else
					outputStream.println("Table has FAILED the row-sum test.");
			}
			else
				outputStream.println("Unable to normalize table.");
			outputStream.println();
			outputStream.println("Table:");
			outputStream.println();
			write(outputStream, ",  ");
			if(!normalized && inSaveUnnormalized)
			{
				outputStream.println();
				outputStream.println();
				outputStream.println("Basis:");
				outputStream.println();
				for (int i = 0; i < basis.length; ++i)
				{
					outputStream.println(basis[i].whichPenetranceCell + ", " + basis[i].value);
				}
			}
		}
	}
	
	public void write(PrintWriter outputStream) throws IOException
	{
		write(outputStream, "\t");
	}
	
	public void write(PrintWriter outputStream, String delimiter) throws IOException
	{
		int foo;
		foo = 0;
		for (int i = 0; i < cellCount; ++i)
		{
			if(i > 0)														// Don't print blank lines at the beginning
			{
				if(i % snpStateCount == 0)
					outputStream.println();									// Skip to the next line of output
				if(i % (snpStateCount * snpStateCount) == 0)
					outputStream.println();									// Print a blank line between squares
			}
			outputStream.print(cells[i].getValue());
			if((i + 1) % snpStateCount != 0)
				outputStream.print(delimiter);
		}
	}

	public float saveCaseControlValuesToFile(File inDestFile, boolean inAppend) throws IOException
	{
		PrintWriter outputStream = null;
		float outBalancedAccuracy;
		
		try
		{
			outputStream = new PrintWriter(new FileWriter(inDestFile, inAppend));
			if(inAppend)
			{
				outputStream.println();
				outputStream.println();
				outputStream.println();
				outputStream.println();
			}
			outputStream.print("Attribute names:");
			for(String n: attributeNames)
				outputStream.print("\t" + n);
			outputStream.println();
			
			int totalCaseCount = 0;
			int totalControlCount = 0;
			int correctCaseCount = 0;
			int correctControlCount = 0;
			for(int i = 0; i < cellCount; ++i)
			{
				totalCaseCount += cellCaseCount[i];
				totalControlCount += cellControlCount[i];
			}
			for(int i = 0; i < cellCount; ++i)
			{
				if(cellCaseCount[i] * totalControlCount >= cellControlCount[i] * totalCaseCount)
					correctCaseCount += cellCaseCount[i];
				else
					correctControlCount += cellControlCount[i];
			}
			outBalancedAccuracy = ((float)correctCaseCount / (float)totalCaseCount + (float)correctControlCount / (float)totalControlCount) / 2;
			outputStream.println("Balanced accuracy of best model: " + outBalancedAccuracy);
			
			outputStream.println("Case values:");
			for(int i = 0; i < cellCount; ++i)
			{
				if(i > 0)														// Don't print blank lines at the beginning
				{
					if(i % snpStateCount == 0)
						outputStream.println();									// Skip to the next line of output
					if(i % (snpStateCount * snpStateCount) == 0)
						outputStream.println();									// Print a blank line between squares
				}
				outputStream.print(cellCaseCount[i]);
				if((i + 1) % snpStateCount != 0)
					outputStream.print(",  ");
			}
			outputStream.println();
			outputStream.println("Control values:");
			for(int i = 0; i < cellCount; ++i)
			{
				if(i > 0)														// Don't print blank lines at the beginning
				{
					if(i % snpStateCount == 0)
						outputStream.println();									// Skip to the next line of output
					if(i % (snpStateCount * snpStateCount) == 0)
						outputStream.println();									// Print a blank line between squares
				}
				outputStream.print(cellControlCount[i]);
				if((i + 1) % snpStateCount != 0)
					outputStream.print(",  ");
			}
		}
		finally
		{
			if (outputStream != null)
			{
				outputStream.close();
			}
		}
		return outBalancedAccuracy;
	}

	public void saveBasisToFile(File inDestFile, boolean inAppend) throws IOException
	{
		PrintWriter outputStream = null;

		try
		{
			outputStream = new PrintWriter(new FileWriter(inDestFile, inAppend));
			if(inAppend)
			{
				outputStream.println();
				outputStream.println();
				outputStream.println();
				outputStream.println();
			}
			outputStream.println("Basis:");
			outputStream.println();
			for (int i = 0; i < basis.length; ++i)
			{
				outputStream.print(basis[i].value);
				if(i < basis.length - 1)
					outputStream.print(",  ");
			}
		}
		finally
		{
			if (outputStream != null)
			{
				outputStream.close();
			}
		}
	}
	
	public void printIn3D()
	{
		int[] indices = new int[3];
		CellId tempCellId = new CellId(3, 0);
		double value;
		long longValue;

		for (int i = 0; i < snpStateCount; ++i)
		{
			for (int j = 0; j < snpStateCount; ++j)
			{
				for (int k = 0; k < snpStateCount; ++k)
				{
					// Inefficient but simple:
					indices[0] = i;
					indices[1] = j;
					indices[2] = k;
					tempCellId = new CellId(indices);
					if (getPenetranceIsSet(tempCellId))
					{
						value = getPenetranceValue(tempCellId);
						longValue = (long) ((float) ((float) value * 10000.0F) + 0.5);
//						System.out.print(longValue / 10000.0 + ",    ");
						System.out.print(longValue / 10000.0 + "\t");
					}
					else
					{
//						System.out.print("----,    ");
						System.out.print("----\t");
					}
				}
				System.out.println();
			}
			System.out.println();
		}
	}

	public void printRow(CellId inWhichCell, int inWhichDimension)
	{
		int filledCells;
		CellId tempCellId = new CellId(inWhichCell);

		System.out.print("Row through ");
		inWhichCell.print();
		System.out.print(" along " + inWhichDimension + ": ");
		for (int j = 0; j < snpStateCount; ++j)
		{
			tempCellId.setIndex(inWhichDimension, j);
			if (getPenetranceIsSet(tempCellId))
				System.out.print(getPenetranceValue(tempCellId) + ", ");
			else
				System.out.print("*, ");
		}
	}

	// For each cell in the "row" specified by inWhichDimension of the hypercube, return how many cells are set.
	// If there is at least one empty cell, outEmptyCellId returns one of them.
	private int countFilledCells(CellId inWhichCell, int inWhichDimension, CellId outEmptyCellId)
	{
		int filledCells;
		CellId tempCellId = new CellId(inWhichCell);

		filledCells = 0;
		for (int j = 0; j < snpStateCount; ++j)
		{
			tempCellId.setIndex(inWhichDimension, j);
			if (getPenetranceIsSet(tempCellId))
				++filledCells;
			else
			{
				if (outEmptyCellId != null)
					outEmptyCellId.copyFrom(tempCellId);
			}
		}
		return filledCells;
	}

	// All of the cells in the row through inCellId along inWhichDimension are filled,
	// so we can use them to calculate the value at inCellId.
	private double calculateForcedPenetranceValue(CellId inCellId, int inWhichDimension)
	{
		double sum;
		double outValue;
		int whereIsCellAlongDimension;

		whereIsCellAlongDimension = inCellId.getIndex(inWhichDimension);
		sum = calculateWeightedSumOfSetPenetranceValues(inCellId, inWhichDimension);
		outValue = (kPenetranceSum - sum) / stateProbability[inWhichDimension][whereIsCellAlongDimension];

		// For debugging only:
		assert (countFilledCells(inCellId, inWhichDimension, null) == snpStateCount - 1);
		setPenetranceValue(inCellId, outValue);
		// printRow(inCellId, inWhichDimension); System.out.println();
		sum = calculateWeightedSumOfSetPenetranceValues(inCellId, inWhichDimension);
		assert ((Math.abs(sum - kPenetranceSum) < kErrorLimit));
		clearPenetranceValue(inCellId);

		return outValue;
	}

	// Return true iff all the penetrance-values are set in the row along
	// inWhichDimension through the cell specified by inCellId.
	// inWhichDimension is the same as which-snp
	private boolean allValuesSetInRow(CellId inCellId, int inWhichDimension)
	{
		return (countFilledCells(inCellId, inWhichDimension, null) == snpStateCount);
	}

	// Return the sum of the set penetrance-values in the row along
	// inWhichDimension through the cell specified by inCellId.
	// inWhichDimension is the same as which-snp
	private double calculateWeightedSumOfSetPenetranceValues(CellId inCellId, int inWhichDimension)
	{
		double sum;
		CellId tempCellId = new CellId(inCellId);

		sum = 0;
		for (int i = 0; i < snpStateCount; ++i)
		{
			tempCellId.setIndex(inWhichDimension, i);
			if (getPenetranceIsSet(tempCellId))
				sum += stateProbability[inWhichDimension][i] * getPenetranceValue(tempCellId);
		}
		return sum;
	}

	public double getProbabilityProduct(CellId inCellId)
	{
		double product;
		
		product = 1;
		for (int dimension = 0; dimension < attributeCount; ++dimension)
			product *= stateProbability[dimension][inCellId.getIndex(dimension)];
		return product;
	}
	
	public int cellIdToMasterIndex(CellId inCellId)
	{
		return inCellId.toMasterIndex(snpStateCount, attributeCount);
	}
	
	public CellId masterIndexToCellId(int inMasterIndex, CellId outCellId)
	{
		assert (0 <= inMasterIndex && inMasterIndex < cellCount);
		return outCellId.fromMasterIndex(snpStateCount, attributeCount, inMasterIndex);
	}

	public double getPenetranceValue(CellId inCellId)
	{
		int index = cellIdToMasterIndex(inCellId);
		return cells[index].getValue();
	}

	public void savePenetranceCell(PenetranceCellWithId inCell)
	{
		CellId cellId = inCell.cellId;
		assert (!getPenetranceIsSet(cellId));
		int index = cellIdToMasterIndex(cellId);
		cells[index] = new PenetranceCell(inCell);
		if (inCell.isBasisElement)
		{
			// if(cellIdToMasterIndex(cellId) == -1)
			// index = -1000;
			// System.out.println("Setting whichPenetranceCell for " +
			// inCell.getWhichBasisElement() + " to " +
			// cellIdToMasterIndex(cellId));
			basis[inCell.getWhichBasisElement()].whichPenetranceCell = cellIdToMasterIndex(cellId);
		}
	}

	public void setPenetranceValue(CellId inCellId, double inValue)
	{
//		assert (!getPenetranceIsSet(inCellId));
		int index = cellIdToMasterIndex(inCellId);
		cells[index].setValue(inValue);
		cells[index].isSet = true;
	}

	public void clearPenetranceValue(CellId inCellId)
	{
		int index = cellIdToMasterIndex(inCellId);
		if (cells[index].isBasisElement)
		{
			// Make the basis not point to the penetranceTable:
			int whichBasisElement = cells[index].getWhichBasisElement();
			basis[whichBasisElement].whichPenetranceCell = kWhichPenetranceCellNone;
		}
		cells[index].isSet = false;
		cells[index].isBasisElement = false; // Make the
														// penetranceTable not
														// point to the basis
	}

	private boolean getPenetranceIsSet(CellId inCellId)
	{
		int index = cellIdToMasterIndex(inCellId);
		return cells[index].isSet;
	}

	private boolean getIsBasisElement(CellId inCellId)
	{
		int index = cellIdToMasterIndex(inCellId);
		return cells[index].isBasisElement;
	}

	private int getWhichBasisElement(CellId inCellId)
	{
		int index = cellIdToMasterIndex(inCellId);
		return cells[index].whichBasisElement;
	}

	private void setWhichBasisElement(CellId inCellId, int inWhichBasisElement)
	{
		int index = cellIdToMasterIndex(inCellId);
		cells[index].whichBasisElement = inWhichBasisElement;
	}
}		// end class PenetranceTable
