package org.epistasis.snpgen.document;

//import jargs.gnu.CmdLineParser;
import  org.epistasis.snpgen.document.CmdLineParserSrc;
import org.epistasis.snpgen.exception.InputException;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;

import javax.swing.*;

import org.epistasis.snpgen.simulator.*;

public class SnpGenDocument
{
	public static final Double		kDefaultFrequencyMin = 0.01;
	public static final Double		kDefaultFrequencyMax = 0.5;
	public static final Double		kDefaultMissingValueRate = 0.0;
	public static final Integer		kDefaultAtrributeCount = 100;
	public static final Integer		kDefaultCaseCount = 400;
	public static final Integer		kDefaultControlCount = 400;
	public static final Integer		kDefaultSeedCount = 100;
	public static final Integer		kDefaultRasQuantileCount = 3;
	public static final Integer		kDefaultRasPopulationCount = 1000;
	public static final Integer		kDefaultRasTryCount = 50000;
	public static final Double		kDefaultModelFraction = 1.0;
	
	public static final String		kDefaultFrequencyMinString = kDefaultFrequencyMin.toString();
	public static final String		kDefaultFrequencyMaxString = kDefaultFrequencyMax.toString();
	public static final String		kDefaultMissingValueRateString = kDefaultMissingValueRate.toString();
	public static final String		kDefaultAttributeCountString = kDefaultAtrributeCount.toString();
	public static final String		kDefaultCaseCountString = kDefaultCaseCount.toString();
	public static final String		kDefaultControlCountString = kDefaultControlCount.toString();
	public static final String		kDefaultSeedCountString = kDefaultSeedCount.toString();
	public static final String		kDefaultRasQuantileCountString = kDefaultRasQuantileCount.toString();
	public static final String		kDefaultPopulationCountString = kDefaultRasPopulationCount.toString();
	public static final String		kDefaultRasTryCountString = kDefaultRasTryCount.toString();
	public static final String		kDefaultModelFractionString = kDefaultModelFraction.toString();
	
	private static final String		kDefaultAttributeNameBase = "P";
	
   /**
     * Thrown when the parsed command-line is missing a non-optional option.
     * <code>getMessage()</code> returns an error string suitable for reporting the error to the user (in English).
     */
    public static class MissingOptionException extends Exception
    {
        private String optionName = null;
   
        MissingOptionException(String optionName)
    	{
            this(optionName, "Missing option '" + optionName + "'");
        }

    	MissingOptionException(String optionName, String msg)
    	{
            super(msg);
            this.optionName = optionName;
        }

        /**
         * @return the name of the option that was unknown (e.g. "-u")
         */
        public String getOptionName()
        {
        	return this.optionName;
        }
    }

	public interface DocListener
	{
		public void modelAdded(SnpGenDocument inDoc, SnpGenDocument.DocModel inModel, int whichModel);
		public void modelUpdated(SnpGenDocument inDoc, SnpGenDocument.DocModel inModel, int whichModel);
		public void modelRemoved(SnpGenDocument inDoc, SnpGenDocument.DocModel inModel, int whichModel);
		public void datasetAdded(SnpGenDocument inDoc, SnpGenDocument.DocDataset inModel, int whichModel);
		public void attributeCountChanged(SnpGenDocument inDoc, SnpGenDocument.DocModel inModel, int whichModel, int inNewAttributeCount);
	}
	
	public abstract static class DocMember
	{
		public abstract void setValue(Object inValue);
		public abstract Object getValue();
	}
	
	public static class DocBoolean extends DocMember
	{
		private Boolean		value;
		
		public DocBoolean()
		{
			value = null;
		}
		
		public DocBoolean(Boolean inBoolean)
		{
			value = inBoolean;
		}
		
		public DocBoolean(DocBoolean inBoolean)
		{
			this(inBoolean.getBoolean());
		}
		
		public void setValue(Object inValue)
		{
			if(inValue == null)
				value = null;
			else if(String.class.isInstance(inValue))
			{
				String s = (String) inValue;
				if(s.length() == 0)
					value = null;
				else
					value = new Boolean((String)inValue);
			}
			else
				value = (Boolean) inValue;
		}
		
		public void setBoolean(Boolean inBoolean)
		{
			this.value = inBoolean;
		}
		
		public void setBoolean(DocBoolean inBoolean)
		{
			setBoolean(inBoolean.value);
		}
		
		public Object getValue()
		{
			return value;
		}
		
		public Boolean getBoolean()
		{
			return value;
		}
	}
	
	public static class DocInteger extends DocMember
	{
		private Integer		value;
		
		public DocInteger()
		{
			value = null;
		}
		
		public DocInteger(Integer inInteger)
		{
			value = inInteger;
		}
		
		public DocInteger(DocInteger inInteger)
		{
			this(inInteger.getInteger());
		}
		
		public void setValue(Object inValue)
		{
			if(inValue == null)
				value = null;
			else if(String.class.isInstance(inValue))
			{
				String s = (String) inValue;
				if(s.length() == 0)
					value = null;
				else
					value = new Integer((String)inValue);
			}
			else
				value = (Integer) inValue;
		}
		
		public void setInteger(Integer value)
		{
			this.value = value;
		}
		
		public void setInteger(DocInteger value)
		{
			setInteger(value.getInteger());
		}
		
		public Object getValue()
		{
			return value;
		}
		
		public Integer getInteger()
		{
			return value;
		}
	}
	
	public static class DocFloat extends DocMember
	{
		private Float		value;
		
		public DocFloat()
		{
			value = null;
		}
		
		public DocFloat(Float inFloat)
		{
			value = inFloat;
		}
		
		public DocFloat(DocFloat inFloat)
		{
			this(inFloat.getFloat());
		}
		
		public void setValue(Object inValue)
		{
			if(inValue == null)
				value = null;
			else if(String.class.isInstance(inValue))
			{
				String s = (String) inValue;
				if(s.length() == 0)
					value = null;
				else
					value = new Float((String) inValue);
			}
			else if(Float.class.isInstance(inValue))
				value = (Float) inValue;
			else
			{
				double doubleValue = (Double) inValue;
				value = (float) doubleValue;
			}
		}
		
		public Object getValue()
		{
			return value;
		}
		
		public void setFloat(Float value)
		{
			this.value = value;
		}
		
		public void setFloat(DocFloat value)
		{
			setValue(value.getFloat());
		}
		
		public Float getFloat()
		{
			return value;
		}
	}
	
	public static class DocString extends DocMember
	{
		private String		value;
		
		public DocString()
		{
			value = null;
		}
		
		public DocString(String inString)
		{
			value = inString;
		}
		
		public DocString(DocString inString)
		{
			this(inString.getString());
		}
		
		public void setValue(Object inValue)
		{
			value = (String) inValue;
		}
		
		public void setString(String value)
		{
			this.value = value;
		}
		
		public void setString(DocString value)
		{
			setValue(value.getString());
		}
		
		public Object getValue()
		{
			return value;
		}
		
		public String getString()
		{
			return value;
		}
	}
	
	public static class DocDataset
	{
		private SnpGenDocument		parentDoc;
		public DocFloat 			alleleFrequencyMin;
		public DocFloat 			alleleFrequencyMax;
		public DocInteger			totalAttributeCount;
		public DocFloat				missingValueRate;
		public DocInteger			caseCount;
		public DocInteger			controlCount;
		public DocInteger			replicateCount;
		public File					outputFile;
		
		public DocDataset(SnpGenDocument inDoc)
		{
			parentDoc = inDoc;
			alleleFrequencyMin = new DocFloat();
			alleleFrequencyMax = new DocFloat();
			totalAttributeCount = new DocInteger();
			missingValueRate = new DocFloat();
			caseCount = new DocInteger();
			controlCount = new DocInteger();
			replicateCount = new DocInteger();
		}
		
		public Exception verifyAllNeededParameters()
		{
			Exception		outEx = null;
			
	TESTS:
			{
				if(caseCount.getInteger() == null)
				{
					outEx = new InputException("No case-count specified");
					break TESTS;
				}
				if(controlCount.getInteger() == null)
				{
					outEx = new InputException("No control-count specified");
					break TESTS;
				}
			}
			return outEx;
		}
	}
	
	public static class DocModel
	{
		public DocString			modelId;
		public DocInteger			attributeCount;
		public DocFloat				heritability;
		public DocFloat				prevalence;
		public DocFloat				fraction;
		public DocBoolean			useOddsRatio;
		public DocString[]			attributeNameArray;
		public DocFloat[]			attributeAlleleFrequencyArray;
		private SnpGenDocument		parentDoc;
		private PenetranceTable[]	penetranceTables;
		
		public DocModel(int inAttributeCount)
		{
			modelId = new DocString();
			attributeCount = new DocInteger(inAttributeCount);
			heritability = new DocFloat();
			prevalence = new DocFloat();
			fraction = new DocFloat();
			useOddsRatio = new DocBoolean();
			attributeNameArray = new DocString[inAttributeCount];
			attributeAlleleFrequencyArray = new DocFloat[inAttributeCount];
			for(int i = 0; i < inAttributeCount; ++i)
			{
				attributeNameArray[i] = new DocString();
				attributeAlleleFrequencyArray[i] = new DocFloat();
			}
			penetranceTables = new PenetranceTable[0];
		}
		
		public DocModel(SnpGenDocument inDoc, int inAttributeCount)
		{
			this(inAttributeCount);
			setParentDoc(inDoc);
		}
		
		public DocModel(DocModel inDocModel)
		{
			this(inDocModel.attributeCount.getInteger());
			inDocModel.copyTo(this);
		}
		
		public DocModel copyTo(DocModel ioDocModel)
		{
			int attributeCount = this.attributeCount.getInteger();
			ioDocModel.modelId.setString(this.modelId);
			ioDocModel.attributeCount.setInteger(this.attributeCount);
			ioDocModel.heritability.setFloat(this.heritability);
			ioDocModel.prevalence.setFloat(this.prevalence);
			ioDocModel.fraction.setFloat(this.fraction);
			ioDocModel.useOddsRatio.setBoolean(this.useOddsRatio);
			ioDocModel.setParentDoc(this.getParentDoc());
			for(int i = 0; i < attributeCount; ++i)
				ioDocModel.attributeNameArray[i] = new DocString(this.attributeNameArray[i].getString());
			for(int i = 0; i < attributeCount; ++i)
				ioDocModel.attributeAlleleFrequencyArray[i] = new DocFloat(this.attributeAlleleFrequencyArray[i].getFloat());
			ioDocModel.penetranceTables = new PenetranceTable[penetranceTables.length];
			for(int i = 0; i < penetranceTables.length; ++i)
			{
				try
				{
					ioDocModel.penetranceTables[i] = (PenetranceTable) this.penetranceTables[i].clone();
				}
				catch(CloneNotSupportedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return ioDocModel;
		}
		
		public void resetAttributeCount(int inAttributeCount)
		{
			attributeCount.setValue(inAttributeCount);
			DocString[] oldNameArray = attributeNameArray;
			DocFloat[] oldFrequencyArray = attributeAlleleFrequencyArray;
			attributeNameArray = new DocString[inAttributeCount];
			attributeAlleleFrequencyArray = new DocFloat[inAttributeCount];
			
			assert oldNameArray.length == oldFrequencyArray.length;
			System.arraycopy(oldNameArray, 0, attributeNameArray, 0, Math.min(oldNameArray.length, inAttributeCount));
			for(int i = oldNameArray.length; i < inAttributeCount; ++i)
				attributeNameArray[i] = new DocString();
			System.arraycopy(oldFrequencyArray, 0, attributeAlleleFrequencyArray, 0, Math.min(oldFrequencyArray.length, inAttributeCount));
			for(int i = oldFrequencyArray.length; i < inAttributeCount; ++i)
				attributeAlleleFrequencyArray[i] = new DocFloat();
			
			if(getParentDoc() != null)
			{
				Integer whichModel = getParentDoc().findModel(this);
				assert whichModel != null;
				for(DocListener l: getParentDoc().listeners)
					l.attributeCountChanged(getParentDoc(), this, whichModel.intValue(), inAttributeCount);
			}
		}
		
		public boolean getUseOddsRatio()
		{
			Boolean or = useOddsRatio.getBoolean();
			return (or != null && or.booleanValue());
		}

		public String[] getAttributeNames()
		{
			String[] outAttributeNames = new String[attributeNameArray.length];
			for(int i = 0; i < attributeNameArray.length; ++i)
				outAttributeNames[i] = attributeNameArray[i].getString();
			return outAttributeNames;
		}
		
		public float[] getAlleleFrequencies()
		{
			float[] outAlleleFrequencies = new float[attributeAlleleFrequencyArray.length];
			for(int i = 0; i < attributeAlleleFrequencyArray.length; ++i)
				outAlleleFrequencies[i] = attributeAlleleFrequencyArray[i].getFloat();
			return outAlleleFrequencies;
		}
		
		public Exception verifyAllNeededParameters()
		{
			Exception		outEx = null;
			
	TESTS:
			{
				if(modelId.getString() == null)
				{
					outEx = new InputException("Missing a model ID");
					break TESTS;
				}
				if(attributeCount.getInteger() == null)
				{
					outEx = new InputException("Missing an attribute-count");
					break TESTS;
				}
				if(heritability.getFloat() == null)
				{
					outEx = new InputException("Missing a heritability");
					break TESTS;
				}
				for (DocString s: attributeNameArray)
				{
					if(s.getString() == null || s.getString().length() == 0)
					{
						outEx = new InputException("Missing an attribute name");
						break TESTS;
					}
				}
				for (DocFloat s: attributeAlleleFrequencyArray)
				{
					if(s.getFloat() == null)
					{
						outEx = new InputException("Missing a minor-allele frequency");
						break TESTS;
					}
				}
			}
			return outEx;
		}

		public PenetranceTable[] getPenetranceTables()
		{
			return penetranceTables;
		}

		public void setPenetranceTables(PenetranceTable[] penetranceTables)
		{
			this.penetranceTables = penetranceTables;
		}
		
		public boolean needsTables(int inDesiredTableCount)
		{
			boolean buildTables = false;
			PenetranceTable[] tables = getPenetranceTables();
			if(tables == null || tables.length < inDesiredTableCount)
			{
				buildTables = true;
			}
			else
			{
				for(int i = 0; i < inDesiredTableCount; ++i)
				{
					if(tables[i] == null)
					{
						buildTables = true;
						break;
					}
				}
			}
			return buildTables;
		}

		public SnpGenDocument getParentDoc()
		{
			return parentDoc;
		}

		public void setParentDoc(SnpGenDocument parentDoc)
		{
			this.parentDoc = parentDoc;
		}
	}
	
	private int					nextPredictiveAttributeNumber;
	private int					nextModelNumber;
	
	public DocFloat 			alleleFrequencyMin;
	public DocFloat 			alleleFrequencyMax;
	public DocInteger			totalAttributeCount;
	public DocFloat				missingValueRate;
	public DocInteger			caseCount;
	public DocInteger			controlCount;
	public DocInteger			replicateCount;

	public DocBoolean			includeMissingValues;
	public DocBoolean			caseControlRatioBalanced;
	public DocBoolean			generateReplicates;
	
	public DocInteger			rasQuantileCount;
	public DocInteger			rasPopulationCount;
	public DocInteger			rasTryCount;
	
	public ArrayList<DocModel>		modelList;
	public ArrayList<DocDataset>	datasetList;
	public DocDataset				firstDataset;
	public File						outputFile;
	public File						inputFile;
	public File[]					inputFiles;
	public float[]					inputFileFractions;
	public File						predictiveInputFile;
	public String					predictiveInputFilename;
	public File						noiseInputFile;
	public boolean					showHelp;
	public boolean					runDocument;
	
	public File						testingOutputFile;
	public int						minInteractionLevel;
	public int						maxInteractionLevel;
	public Integer					randomSeed;
	
	private ArrayList<DocListener>	listeners;
	
	public SnpGenDocument(boolean inCreateFirstDataset)
	{
//		setNextPredictiveAttributeNumber(1);
		setNextModelNumber(1);
		
		randomSeed = null;
		alleleFrequencyMin = new DocFloat();
		alleleFrequencyMax = new DocFloat();
		totalAttributeCount = new DocInteger();
		missingValueRate = new DocFloat();
		caseCount = new DocInteger();
		controlCount = new DocInteger();
		replicateCount = new DocInteger();
		
		inputFiles = new File[0];
		
		includeMissingValues = new DocBoolean();
		caseControlRatioBalanced = new DocBoolean();
		generateReplicates = new DocBoolean();
		
		rasQuantileCount = new DocInteger();
		rasPopulationCount = new DocInteger();
		rasPopulationCount.setValue(kDefaultRasPopulationCount);
		rasTryCount = new DocInteger();
		rasTryCount.setValue(50000);		// Default
		
		minInteractionLevel = 1;			// Default
		maxInteractionLevel = 4;			// Default
		
		listeners = new ArrayList<DocListener>();
		modelList = new ArrayList<DocModel>();
		datasetList = new ArrayList<DocDataset>();
		firstDataset = null;
		if(inCreateFirstDataset)
			createFirstDataset();
		
		createDocument();
	}

	public static String getDefaultAttributeNameBase()
	{
		return kDefaultAttributeNameBase;
	}

	public DocDataset createFirstDataset()
	{
		firstDataset = this.addNewDocDataset();
		return firstDataset;
	}
	
	private void createDocument()
	{
		// For now we just clear the current document; in the future we may create a new document here.
		clear();
	}
	
	private void clear()
	{
//		assert false: "Not implemented";
	}
	
	// Load the specified file into the document
	public void load(File inFile)
	{
		assert false: "SnpGenDocument.load() not implemented";
	}
	
	// Store the document into the specified file
	public void store(File inFile)
	{
		assert false: "SnpGenDocument.store() not implemented";
	}
	
    public static void printOptionHelp_Old()
    {
        System.err.println(
			"Usage: OptionTest " +
			"[-h, --help] " +
			"[{-o, --outputFile} filename]\n" +
			"[{-n, --alleleFrequencyMin} f] " +
			"[{-x, --alleleFrequencyMax} f] " +
			"[{-a, --totalAttributeCount} n] " +
			"[{-m, --missingValueRate} f] \n" +
			"[{-s, --caseCount} n] " +
			"[{-w, --controlCount} n] " +
			"[{-r, --replicateCount} n] \n" +
			"[{-q, --rasQuantileCount} n] " +
			"[{-p, --rasPopulationCount} n] " +
			"[{-t, --rasTryCount} n] \n" +
			
			"[{-M, --model} \"[{-n, --name} 'word'] [{-h, --heritability} f] [{-a, --attribute} f]\"]");
    }
	
    public static void printOptionHelp()
    {
        System.err.println(
			"Usage: java -jar SNPGen.jar " +
			"[-h, --help]\n" +
			
			"[{-M, --model} \"[{-n, --name} 'word'] [{-h, --heritability} float] [{-p, --prevalence} float] [{-f, --fraction} float] [{-a, --attributeAlleleFrequency} float]\"]\n" +
			
			"[{-q, --rasQuantileCount} integer] " +
			"[{-p, --rasPopulationCount} integer] " +
			"[{-t, --rasTryCount} integer]\n" +
			
			"[{-r, --randomSeed} integer]\n" +
			
			"[{-v, --predictiveInputFile} filename]\n" +
			"[{-z, --noiseInputFile} filename]\n" +
			
			"[{-o, --modelOutputFile} filename]\n" +
			"[{-i, --modelInputFile} filename]\n" +
			"[{-f, --modelInputFileFraction} float]\n" +
			
			"[{-D, --dataset} \"" +
				"[{-n, --alleleFrequencyMin} float] " +
				"[{-x, --alleleFrequencyMax} float] " +
				"[{-a, --totalAttributeCount} integer] " +
//				"[{-m, --missingValueRate} float] " +
				"[{-s, --caseCount} integer] " +
				"[{-w, --controlCount} integer] " +
				"[{-r, --replicateCount} integer] " +
        		"[{-o, --datasetOutputFile} filename]" +
			"\"]\n");
        }
    
    /*
     TODO:
     For multiple models:
     The value of --name is appended to the value of --modelOutputFile to produce multiple model output-files.
     When creating a dataset from existing model-files, the user can specify several values of --modelInputFile.
     In order to make this work, I'll have to create a compound --modelInputFile option with a percentage attached, and possibly move it inside the --dataset option. 
     
     For now, heterogeneity can only be accomplished by generating models and datasets in a single run.
     */
	
// Example arguments:
// -r 17 -M "-n 'Model1' -h 0.1 -a 0.25 -a 0.5 -f 0.99" -M "-n 'Model2' -h 0.1 -a 0.3 -a 0.3 -f 0.01" -o new.txt -q 3 -p 1000 -t 50000 -D "-a 10 -s 10000 -w 10000 -r 1 -o combined.txt"
// -r 17 -i new_models.txt -f .3 -f .7 -D "-a 10 -s 10000 -w 10000 -r 1 -o combined_new.txt"
// -M "-n 'Model1' -h 0.01 -a 0.25 -a 0.5" -o new.txt -q 3 -p 1000 -t 50000 -D "-a 4 -s 100 -w 100 -r 1 -o combined.txt" -v pred_data.txt
// -M "-n 'Model1' -h 0.01 -a 0.25 -a 0.5" -M "-n 'Model2' -h 0.02 -a 0.25 -a 0.5" -o /Users/jfisher/_SnpGen/tables.txt -q 3 -p 1000 -t 50000
// -i /Users/jfisher/_SnpGen/tables.txt -D "-a 20 -s 100 -w 100 -r 100 -o /Users/jfisher/_SnpGen/20_200" -D "-a 40 -s 100 -w 100 -r 100 -o /Users/jfisher/_SnpGen/40_200"
// -M "-n 'Model1' -h 0.01 -a 0.25 -a 0.5" -M "-n 'Model2' -h 0.02 -a 0.25 -a 0.5" -o /Users/jfisher/_SnpGen/tables.txt -q 3 -p 1000 -t 50000 -D "-a 20 -s 100 -w 100 -r 100 -o /Users/jfisher/_SnpGen/20_200" -D "-a 40 -s 100 -w 100 -r 100 -o /Users/jfisher/_SnpGen/40_200"
// -M "-n 'Model1' -h 0.05 -a 0.25 -a 0.5 -a 0.33 -a 0.1" -o output/tables_2_05.txt -q 3 -p 100 -t 50000 -D "-a 20 -s 200 -w 200 -r 50 -o output/4_05_20_400" -D "-a 40 -s 200 -w 200 -r 50 -o output/4_05_40_400" -D "-a 60 -s 200 -w 200 -r 50 -o output/4_05_60_400"
// -M "-n 'Model1' -h 0.05 -a 0.25 -a 0.5 -a 0.33" -o output/tables_2_05.txt -q 50 -p 100 -t 50000 -D "-n 0.01 -x 0.5 -a 20 -s 200 -w 200 -r 1 -o output/3_05_20_400" -D "-a 30 -s 200 -w 200 -r 1 -o output/3_05_30_400" -D "-a 40 -s 200 -w 200 -r 1 -o output/3_05_40_400"
// -M "-n 'Model1' -h 0.05 -a 0.25 -a 0.5 -a 0.33" -i old_tables.txt -o output/tables_2_05.txt -q 3 -p 100 -t 50000 -D "-a 20 -s 200 -w 200 -r 50 -o output/3_05_20_400" -D "-a 40 -s 200 -w 200 -r 50 -o output/3_05_40_400" -D "-a 60 -s 200 -w 200 -r 50 -o output/3_05_60_400"

    public boolean parseArguments(String[] args)
		throws Exception
	{
		boolean outShowGui;
		Boolean showHelpObject;
		String fileName;
		String inputFileName;
		
		
//		THE FOLLOWING COMMENTED-OUT CODE IS FOR TESTING ONLY:
	
		
//		String argString = "-M%-n 'Model1' -h 0.3 -a 0.25 -a 0.25%-o%/Users/jfisher/_SnpGen/tables.txt%-q%1%-p%1000%-t%100000%-D%-a 1000 -s 200 -w 200 -n 0.05 -x .211 -r 10 -o /Users/jfisher/_SnpGen/Surf";
//
//		args = argString.split("%");
		
		
//		String argString = "-M%-n 'Model1' -h 0.4 -a 0.25 -a 0.25%-o%/Users/jfisher/_SnpGen/tables.txt%-q%1%-p%1000%-t%50000%-D%-a 1000 -s 200 -w 200 -m 0.05 -x 0.1 -r 100 -o /Users/jfisher/_SnpGen/Surf";
////		String argString = "-M%-n 'Model1' -h 0.4 -p 0.6 -a 0.25 -a 0.25%-o%/Users/jfisher/_SnpGen/tables.txt%-q%1%-p%1000%-t%50000%-D%-a 10 -s 200 -w 200 -m 0.05 -x 0.1 -r 10 -o /Users/jfisher/_SnpGen/Foo%-n%2%-x%2";
////
//		args = argString.split("%");

//		String argString = "-M%-n 'Model1' -h 0.015 -a 0.25 -a 0.25%-o%/Users/jfisher/_SnpGen/tables.txt%-q%40%-p%1000%-t%50000%-D%-a 10 -s 400 -w 400 -r 1 -o /Users/jfisher/_SnpGen/40_200";
//		String argString = ""
//		+ "-s%hyperIndep%-s%mutualInfo%-s%mdr%-v%/Users/jfisher/_SnpGen/best1.txt%-u%/Users/jfisher/_SnpGen/foo%-r%23%"
//		+ "-q%1%-n%1%-x%3%-D%-a 70 -s 100 -w 100 -r 10";
//
//		String argString = ""
//		+ "-s%heritability%-s%hyperIndep%-s%mutualInfo%-s%mdr%-u%/Users/jfisher/_SnpGen/foo%-r%23%"
//		+ "-q%10%-n%1%-x%3%-D%-a 10 -s 100 -w 100 -r 100";
//
//		String argString = "-M%-n 'Model1' -h 0.01 -a 0.2 -a 0.2%"
//			+ "-s%heritability%-s%hyperIndep%-s%mutualInfo%-s%mdr%-u%/Users/jfisher/_SnpGen/foo%-r%233%"
//			+ "-q%10%-n%1%-x%2%-D%-a 20 -s 100 -w 100 -r 100";
//		
//		args = argString.split("%");

//		String argString = 
//		"-M%-n 'Model1' -h 0.1 -a 0.25 -a 0.5%-M%-n 'Model2' -h 0.02 -a 0.25 -a 0.5%-o%/Users/jfisher/_SnpGen/tables.txt%-q%50%-p%1000%-t%50000%" +
//		"-D%-a 20 -s 100 -w 100 -r 1 -o /Users/jfisher/_SnpGen/20_200%-D%-a 40 -s 100 -w 100 -r 100 -o /Users/jfisher/_SnpGen/40_200";
//	
//	args = argString.split("%");
		
//		String argString = 
//		"-M%-n 'Model1' -h 0.0001 -a 0.2 -a 0.2 -a 0.2 -a 0.2 -a 0.2%-o%/Users/jfisher/_SnpGen/tables.txt%-q%3%-p%100%-t%10000000%-r%2";
//	
//	args = argString.split("%");
		
		
//		String argString = ""
//		+ "-s%hyperIndep%-s%heritability%-s%mutualInfo%-s%mdr%-v%/Users/jfisher/Genomics/MDR/Testing/Standard_Models/VelezData_20_attributes_balanced_200_400_800_1600_samples/200/69%-r%37%";
//		
//		args = argString.split("%");
	
//		String argString = ""
//		+ "-s%hyperIndep%-s%heritability%-s%mutualInfo%-s%mdr%-v%/Users/jfisher/Genomics/MDR/Testing/Standard_Models/Foo/200/00%-r%37%";
//		
//		args = argString.split("%");
	
//		String argString = ""
//			+ "-s%hyperIndep%-s%heritability%-s%mutualInfo%-s%mdr%-i%/Users/jfisher/Genomics/MDR/Testing/Standard_Models/README.data.txt%-r%37%";
//		
//		args = argString.split("%");
	
		if(args.length == 0)
			outShowGui = true;
		else
		{
			outShowGui = false;
			CmdLineParserSrc parser = new CmdLineParserSrc();
			CmdLineParserSrc.Option rasQuantileCountOption = parser.addIntegerOption('q', "rasQuantileCount");
			CmdLineParserSrc.Option rasPopulationCountOption = parser.addIntegerOption('p', "rasPopulationCount");
			CmdLineParserSrc.Option rasTryCountOption = parser.addIntegerOption('t', "rasTryCount");
			CmdLineParserSrc.Option datasetOption = parser.addStringOption('D', "dataset");
			CmdLineParserSrc.Option modelOption = parser.addStringOption('M', "model");
			CmdLineParserSrc.Option outputFileOption = parser.addStringOption('o', "modelOutputFile");
			CmdLineParserSrc.Option inputFileOption = parser.addStringOption('i', "modelInputFile");
			CmdLineParserSrc.Option inputFileFractionOption = parser.addDoubleOption('f', "modelInputFileFraction");
			CmdLineParserSrc.Option predictiveInputFileOption = parser.addStringOption('v', "predictiveInputFile");
			CmdLineParserSrc.Option noiseInputFileOption = parser.addStringOption('z', "noiseInputFile");
			CmdLineParserSrc.Option testingOutputFileOption = parser.addStringOption('u', "testingOutputFile");
			CmdLineParserSrc.Option minInteractionLevelOption = parser.addIntegerOption('n', "minInteractionLevel");
			CmdLineParserSrc.Option maxInteractionLevelOption = parser.addIntegerOption('x', "maxInteractionLevel");
			CmdLineParserSrc.Option randomSeedOption = parser.addIntegerOption('r', "randomSeed");
			CmdLineParserSrc.Option helpOption = parser.addBooleanOption('h', "help");
			
			CmdLineParserSrc datasetParser = new CmdLineParserSrc();
			CmdLineParserSrc.Option alleleFrequencyMinOption = datasetParser.addDoubleOption('n', "alleleFrequencyMin");
			CmdLineParserSrc.Option alleleFrequencyMaxOption = datasetParser.addDoubleOption('x', "alleleFrequencyMax");
			CmdLineParserSrc.Option totalAttributeCountOption = datasetParser.addIntegerOption('a', "totalAttributeCount");
			CmdLineParserSrc.Option missingValueRateOption = datasetParser.addDoubleOption('m', "missingValueRate");
			CmdLineParserSrc.Option caseCountOption = datasetParser.addIntegerOption('s', "caseCount");
			CmdLineParserSrc.Option controlCountOption = datasetParser.addIntegerOption('w', "controlCount");
			CmdLineParserSrc.Option replicateCountOption = datasetParser.addIntegerOption('r', "replicateCount");
			CmdLineParserSrc.Option datasetOutputFileOption = datasetParser.addStringOption('o', "datsetOutputFile");
			    
			CmdLineParserSrc modelParser = new CmdLineParserSrc();
			CmdLineParserSrc.Option modelNameOption = modelParser.addStringOption('n', "name");
			CmdLineParserSrc.Option modelHeritabilityOption = modelParser.addDoubleOption('h', "heritability");
			CmdLineParserSrc.Option modelPrevalenceOption = modelParser.addDoubleOption('p', "prevalence");
			CmdLineParserSrc.Option modelOddsRatioOption = modelParser.addBooleanOption('o', "useOddsRatio");
			CmdLineParserSrc.Option modelAttributeOption = modelParser.addDoubleOption('a', "attribute");
			CmdLineParserSrc.Option modelFractionOption = modelParser.addDoubleOption('f', "fraction");
			    
			parser.parse(args);
			
			showHelpObject = (Boolean)parser.getOptionValue(helpOption);
			showHelp = (showHelpObject != null && showHelpObject);
//			// If the only option was "help", then show GUI
//			if(showHelp && args.length == 1)
//				outShowGui = true;
			
//			fileName = (String) parser.getOptionValue(inputFileOption, null);
//			if(fileName == null)
//				inputFile = null;
//			else
//				inputFile = new File(fileName);
			
			Vector<String> inputFileOptionList = parser.getOptionValues(inputFileOption);
			int inputFileCount = inputFileOptionList.size();
			inputFiles = new File[inputFileCount];
			for(int i = 0; i < inputFileCount; ++i)
			{
				inputFiles[i] = new File(inputFileOptionList.get(i));
			}
			
			Vector<Double> inputFileFractionOptionList = parser.getOptionValues(inputFileFractionOption);
			int inputFileFractionCount = inputFileFractionOptionList.size();
			inputFileFractions = new float[inputFileFractionCount];
			for(int i = 0; i < inputFileFractionCount; ++i)
			{
				inputFileFractions[i] = inputFileFractionOptionList.get(i).floatValue();
			}
			
			predictiveInputFile = null;
			predictiveInputFilename = null;
			fileName = (String) parser.getOptionValue(predictiveInputFileOption, null);
			if(fileName != null)
			{
				predictiveInputFilename = fileName;
				predictiveInputFile = new File(fileName);
			}
			
			fileName = (String) parser.getOptionValue(noiseInputFileOption, null);
			if(fileName == null)
				noiseInputFile = null;
			else
				noiseInputFile = new File(fileName);
			
			fileName = (String) parser.getOptionValue(outputFileOption, null);
			if(fileName == null)
				outputFile = null;
			else
				outputFile = new File(fileName);
			
			fileName = (String) parser.getOptionValue(testingOutputFileOption, null);
			if(fileName == null)
				testingOutputFile = null;
			else
				testingOutputFile = new File(fileName);
			
			rasQuantileCount.setValue(parser.getOptionValue(rasQuantileCountOption, kDefaultRasQuantileCount));
			rasPopulationCount.setValue(parser.getOptionValue(rasPopulationCountOption, kDefaultRasPopulationCount));
			rasTryCount.setValue(parser.getOptionValue(rasTryCountOption, kDefaultRasTryCount));
			minInteractionLevel = (Integer) parser.getOptionValue(minInteractionLevelOption, 1);
			maxInteractionLevel = (Integer) parser.getOptionValue(maxInteractionLevelOption, 4);
			randomSeed = (Integer) parser.getOptionValue(randomSeedOption, null);
			
			Vector<String> datasetOptionList = parser.getOptionValues(datasetOption);
			for(String s: datasetOptionList)
			{
				String[] datasetArgs = s.split(" ");
				datasetParser.parse(datasetArgs);
				DocDataset dataset = addNewDocDataset();
				dataset.alleleFrequencyMin.setValue(((Double)datasetParser.getOptionValue(alleleFrequencyMinOption, kDefaultFrequencyMin)).floatValue());
				dataset.alleleFrequencyMax.setValue(((Double)datasetParser.getOptionValue(alleleFrequencyMaxOption, kDefaultFrequencyMax)).floatValue());
				dataset.totalAttributeCount.setValue(datasetParser.getOptionValue(totalAttributeCountOption, kDefaultAtrributeCount));
				dataset.missingValueRate.setValue(((Double)datasetParser.getOptionValue(missingValueRateOption, kDefaultMissingValueRate)).floatValue());
				dataset.caseCount.setValue(datasetParser.getOptionValue(caseCountOption, kDefaultCaseCount));
				dataset.controlCount.setValue(datasetParser.getOptionValue(controlCountOption, kDefaultControlCount));
				dataset.replicateCount.setValue(datasetParser.getOptionValue(replicateCountOption, kDefaultSeedCount));
				
				fileName = (String) datasetParser.getOptionValue(datasetOutputFileOption, null);
				if(fileName == null)
					dataset.outputFile = null;
				else
					dataset.outputFile = new File(fileName);
			}
			if(datasetOptionList.size() >= 1)
				firstDataset = datasetList.get(0);
			
			Vector<String> modelOptionList = parser.getOptionValues(modelOption);
			for(String s: modelOptionList)
			{
				String[] modelArgs = s.split(" ");
				modelParser.parse(modelArgs);
				Vector<Double> attributeAlleleFrequencyList = modelParser.getOptionValues(modelAttributeOption);
				DocModel model = addNewDocModel(attributeAlleleFrequencyList.size(), (String)modelParser.getOptionValue(modelNameOption, ""), null, null);
				for(int i = 0; i < attributeAlleleFrequencyList.size(); ++i)
					model.attributeAlleleFrequencyArray[i].setValue(((Double)attributeAlleleFrequencyList.elementAt(i)).floatValue());
				Double heritability = (Double)modelParser.getOptionValue(modelHeritabilityOption);
				if(heritability == null)
					throw new MissingOptionException(modelHeritabilityOption.toString());
				else
					model.heritability.setValue(heritability.floatValue());
				Double prevalence = (Double)modelParser.getOptionValue(modelPrevalenceOption);
				if(prevalence == null)
					model.prevalence.setValue(null);
				else
					model.prevalence.setValue(prevalence.floatValue());
				Boolean useOddsRatio = (Boolean)modelParser.getOptionValue(modelOddsRatioOption);
				if(useOddsRatio == null)
					model.useOddsRatio.setValue(null);
				else
					model.useOddsRatio.setValue(useOddsRatio.booleanValue());
				Double fraction = (Double)modelParser.getOptionValue(modelFractionOption, kDefaultModelFraction);
				if(fraction == null)
					model.fraction.setValue(null);
				else
					model.fraction.setValue(fraction.floatValue());
			}
		}
		runDocument = (!showHelp && !outShowGui);
 		return outShowGui;
	}
	
//	public DocModel addDocModel(int inAttributeCount)
//	{
//		String[] attributeNames = new String[inAttributeCount];
//		for(int i = 0; i < inAttributeCount; ++i)
//			attributeNames[i] = "";
//		return addDocModel(inAttributeCount, "", attributeNames);
//	}
	
//	public DocModel addNewDocModel(int inAttributeCount, String inModelId, String[] inAttributeNames, float[] inAttributeAlleleFrequencies)
//	{
//		double[] attributeAlleleFrequencies = new double[inAttributeAlleleFrequencies.length];
//		for(int i = 0; i < inAttributeAlleleFrequencies.length; ++i)
//			attributeAlleleFrequencies[i] = inAttributeAlleleFrequencies[i];
//		return addNewDocModel(inAttributeCount, inModelId, inAttributeNames, attributeAlleleFrequencies);
//	}
	
	public DocModel addNewDocModel(int inAttributeCount, String inModelId, String[] inAttributeNames, double[] inAttributeAlleleFrequencies)
	{
		DocModel outModel = new DocModel(this, inAttributeCount);
		outModel.modelId.setValue(inModelId);
		for(int i = 0; i < inAttributeCount; ++i)
		{
			if(inAttributeNames == null)
				outModel.attributeNameArray[i].setValue(getDefaultAttributeNameBase() + nextPredictiveAttributeNumber++);
			else
				outModel.attributeNameArray[i].setValue(inAttributeNames[i]);
			if(inAttributeAlleleFrequencies != null)
				outModel.attributeAlleleFrequencyArray[i].setValue((float)inAttributeAlleleFrequencies[i]);
		}
		addDocModel(outModel);
		return outModel;
	}
	
	public void addDocModel(DocModel inModel)
	{
		modelList.add(inModel);
		Integer whichModel = findModel(inModel);
		assert whichModel != null;
		for(DocListener l: listeners)
			l.modelAdded(this, inModel, whichModel);
	}
	
	public void updateDocModel(int inWhichModel, DocModel inModel)
	{
		modelList.set(inWhichModel, inModel);
		for(DocListener l: listeners)
			l.modelUpdated(this, inModel, inWhichModel);
	}
	
	public int getModelCount()
	{
		return modelList.size();
	}
	
	public void removeDocModel(int inWhichModel)
	{
		DocModel model = modelList.get(inWhichModel);
		modelList.remove(inWhichModel);
		for(DocListener l: listeners)
			l.modelRemoved(this, model, inWhichModel);
	}
	
	public Integer findModel(DocModel inModel)
	{
		Integer whichModel = null;
		for(int i = 0; i < modelList.size(); ++i)
		{
			if(modelList.get(i) == inModel)
			{
				whichModel = i;
				break;
			}
		}
		return whichModel;
	}
	
	public DocDataset addNewDocDataset()
	{
		DocDataset outDataset = new DocDataset(this);
		addDocDataset(outDataset);
		return outDataset;
	}
	
	public void addDocDataset(DocDataset inDataset)
	{
		datasetList.add(inDataset);
		Integer whichDataset = findDataset(inDataset);
		assert whichDataset != null;
		for(DocListener l: listeners)
			l.datasetAdded(this, inDataset, whichDataset);
	}
	
	public int findDataset(DocDataset inDataset)
	{
		Integer whichDataset = null;
		for(int i = 0; i < datasetList.size(); ++i)
		{
			if(datasetList.get(i) == inDataset)
			{
				whichDataset = i;
				break;
			}
		}
		return whichDataset;
	}
	
	public void addDocumentListener(DocListener inListener)
	{
		listeners.add(inListener);
	}
	
	public Exception verifyAllNeededParameters()
	{
		Exception		outEx = null;
		
		outEx = verifyModelParameters();
		if(outEx == null)
			outEx = verifyDatasetParameters();
		return outEx;
	}
	
	public Exception verifyAllNeededParameters_OLD()
	{
		Exception		outEx = null;
		
TESTS:
		{
			for (int i = 0; i < datasetList.size(); ++i)
			{
				if((outEx = datasetList.get(i).verifyAllNeededParameters()) != null)
					break TESTS;
			}
			for (int i = 0; i < modelList.size(); ++i)
			{
				if((outEx = modelList.get(i).verifyAllNeededParameters()) != null)
					break TESTS;
			}
		}
		return outEx;
	}
	
	public Exception verifyModelParameters()
	{
		Exception		outEx = null;
		
		for (int i = 0; i < modelList.size(); ++i)
		{
			if((outEx = modelList.get(i).verifyAllNeededParameters()) != null)
				break;
		}
		return outEx;
	}
	
	public Exception verifyDatasetParameters()
	{
		Exception		outEx = null;
		
		for (int i = 0; i < datasetList.size(); ++i)
		{
			if((outEx = datasetList.get(i).verifyAllNeededParameters()) != null)
				break;
		}
		return outEx;
	}
	
	public int getNextPredictiveAttributeNumber()
	{
		int outAttributeNumber = 1;		// first guess
		for(DocModel model: modelList)
		{
			for(DocString attr: model.attributeNameArray)
			{
				if(attr != null && attr.value != null)
				{
					String attrName = attr.value.toLowerCase();
					if(attrName.charAt(0) == 'p')
					{
						int attrNumber = 0;
						boolean isNumber = true;		// First guess
						try
						{
							attrNumber = Integer.parseInt(attrName.substring(1));
						}
						catch(NumberFormatException nfe)
						{
							isNumber = false;
						}
						if(isNumber)
						{
							if(outAttributeNumber < attrNumber + 1)
								outAttributeNumber = attrNumber + 1;
						}
					}
				}
			}
		}
		return outAttributeNumber;
	}

//	public void setNextPredictiveAttributeNumber(int nextPredictiveAttributeNumber)
//	{
//		this.nextPredictiveAttributeNumber = nextPredictiveAttributeNumber;
//	}

//	public int getNextPredictiveAttributeNumber()
//	{
//		return nextPredictiveAttributeNumber;
//	}

	public void setNextModelNumber(int nextModelNumber)
	{
		this.nextModelNumber = nextModelNumber;
	}

	public int getNextModelNumber()
	{
		return nextModelNumber;
	}
}
