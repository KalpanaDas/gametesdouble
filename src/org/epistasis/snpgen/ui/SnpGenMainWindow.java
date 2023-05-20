package org.epistasis.snpgen.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;

import org.epistasis.snpgen.document.SnpGenDocument;
import org.epistasis.snpgen.document.SnpGenDocument.*;
import org.epistasis.snpgen.exception.InputException;
import org.epistasis.snpgen.exception.ProcessingException;
import org.epistasis.snpgen.simulator.*;

/* MenuDemo.java requires images/middle.gif. */

public class SnpGenMainWindow // implements ChangeListener
{
	private int				nextModelNumber = 1;
	private int				nextSnpNumber = 1;
	
	JTextArea				output;
	ModelTable				modelTable;
	JFrame					frame;
	String					newline = "\n";
	final JFileChooser		fileChooser = new JFileChooser();
	File					file;
	GuiDocumentLink			documentLink;
	SnpGenSimulator			simulator;
	JLabel					quantileCountFieldMainWindow;
	int						desiredQuantileCount;
	DatasetPanel			datasetControlPanel;
	JButton					editButton;
	JButton					deleteButton;
	
	public static abstract class Action extends AbstractAction
	{
		public Action(String text, ImageIcon icon)
		{
			super(text, icon);
		}
		
		public Action(String text, ImageIcon icon, String desc, Integer mnemonic, KeyStroke accelerator)
		{
			this(text, icon);
			putValue(SHORT_DESCRIPTION, desc);
			putValue(MNEMONIC_KEY, mnemonic);
			putValue(ACCELERATOR_KEY, accelerator);
		}
	}
	
	public static abstract class SnpGenAction extends Action
	{
		protected SnpGenMainWindow mSnpGen;
		
		public SnpGenAction(SnpGenMainWindow SnpGen, String text, ImageIcon icon, String desc, Integer mnemonic, KeyStroke accelerator)
		{
			super(text, icon, desc, mnemonic, accelerator);
			mSnpGen = SnpGen;
		}
	}

	public static class NewAction extends SnpGenAction {
		public NewAction(SnpGenMainWindow SnpGen) {
			super(SnpGen, "New", null, "Create a new file", KeyEvent.VK_N, KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.ALT_MASK));
		}
		public void actionPerformed(ActionEvent e) {
				try
				{
					mSnpGen.createSnpGenDocument();
				}
				catch(IllegalAccessException iea)
				{}
		}
	}

	public static class OpenAction extends SnpGenAction {
		public OpenAction(SnpGenMainWindow SnpGen) {
			super(SnpGen, "Open", null, "Open an existing file", KeyEvent.VK_O, KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.ALT_MASK));
		}
		public void actionPerformed(ActionEvent e) {
			try
			{
				mSnpGen.openDocument();
			}
			catch(IllegalAccessException iea)
			{}
		}
	}

	public static class SaveAction extends SnpGenAction {
		public SaveAction(SnpGenMainWindow SnpGen) {
			super(SnpGen, "Save", null, "Save the current file", KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.ALT_MASK));
		}
		public void actionPerformed(ActionEvent e) {
			try
			{
				mSnpGen.saveDocument();
			}
			catch(IllegalAccessException iea)
			{}
		}
	}

	public static class SaveAsAction extends SnpGenAction {
		public SaveAsAction(SnpGenMainWindow SnpGen) {
			super(SnpGen, "SaveAs", null, "Save the current file to a new location", KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.ALT_MASK | ActionEvent.SHIFT_MASK));
		}
		public void actionPerformed(ActionEvent e) {
			try
			{
				mSnpGen.saveDocumentAs();
			}
			catch(IllegalAccessException iea)
			{}
		}
	}
	
	public static class GenerateAction extends SnpGenAction {
		public GenerateAction(SnpGenMainWindow inWindow) {
			super(inWindow, "Generate Datasets...", null, "Generate simulated values", KeyEvent.VK_G, KeyStroke.getKeyStroke(KeyEvent.VK_G, ActionEvent.ALT_MASK));
		}
		public void actionPerformed(ActionEvent e) {
			mSnpGen.generateDatasets();
		}
	}
	
	public static class GenerateModelAction extends SnpGenAction
	{
		public GenerateModelAction(SnpGenMainWindow inWindow)
		{
//			super(inWindow, "Add Model", null, "Add a model", KeyEvent.VK_M, KeyStroke.getKeyStroke(KeyEvent.VK_M, ActionEvent.ALT_MASK));
			super(inWindow, "Generate Model", null, "Generate a simulatedmodel", KeyEvent.VK_G, KeyStroke.getKeyStroke(KeyEvent.VK_G, ActionEvent.ALT_MASK));
		}
		public void actionPerformed(ActionEvent e) {
			mSnpGen.addSnpModel();
		}
	}
	
	public static class CreateModelAction extends SnpGenAction
	{
		public CreateModelAction(SnpGenMainWindow inWindow)
		{
			super(inWindow, "Create Model", null, "Create a model and edit it", KeyEvent.VK_C, KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));
		}
		public void actionPerformed(ActionEvent e) {
			mSnpGen.createSnpModel();
		}
	}
	
	public static class EditModelAction extends SnpGenAction
	{
		public EditModelAction(SnpGenMainWindow inWindow)
		{
			super(inWindow, "Edit Model", null, "Edit an existing model", KeyEvent.VK_E, KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.ALT_MASK));
		}
		public void actionPerformed(ActionEvent e) {
			mSnpGen.editSnpModel();
		}
	}
	
	public static class RemoveModelAction extends SnpGenAction
	{
		public RemoveModelAction(SnpGenMainWindow inWindow)
		{
			super(inWindow, "Delete Model", null, "Delete a model", KeyEvent.VK_D, KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.ALT_MASK));
		}
		public void actionPerformed(ActionEvent e) {
			mSnpGen.removeSnpModel();
		}
	}
	
	public static class LoadModelAction extends SnpGenAction
	{
		public LoadModelAction(SnpGenMainWindow inWindow)
		{
			super(inWindow, "Load Model", null, "Load a model", KeyEvent.VK_L, KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
		}
		public void actionPerformed(ActionEvent e) {
			mSnpGen.loadModelFile();
		}
	}
	
	private static class GuiDocumentLink
	{
		private SnpGenDocument				document;
		private ArrayList<BackedComponent>	backedComponents;
		
		public GuiDocumentLink()
		{
			backedComponents = new ArrayList<BackedComponent>();
		}
		
		public void addBackedComponent(BackedComponent inBackedComponent)
		{
			backedComponents.add(inBackedComponent);
		}
		
		public void documentToGui() throws IllegalAccessException
		{
			for(BackedComponent comp: backedComponents)
			{
				comp.load(getDocument());
			}
		}
		
		public void guiToDocument() throws IllegalAccessException
		{
			for(BackedComponent comp: backedComponents)
			{
				comp.store(getDocument());
			}
		}

		public void setDocument(SnpGenDocument document) {
			this.document = document;
		}

		public SnpGenDocument getDocument() {
			return document;
		}
	}
	   
	private static interface BackedComponent
	{
		public void load(SnpGenDocument inDest) throws IllegalAccessException;
		public void store(SnpGenDocument inDest) throws IllegalAccessException;
	}
	
	private static class BackedModelUi extends JPanel // implements BackedComponent
	{
		public int							attributeCount;
		public BackedTextField				modelIdUi;
		public BackedTextField				attributeCountUi;
		public BackedTextField				heritabilityUi;
		public JPanel						attributeNamePanel;
		public ArrayList<BackedTextField>	attributeNameUiList;
		public JPanel						attributeAlleleFrequencyPanel;
		public ArrayList<BackedTextField>	attributeAlleleFrequencyUiList;
		
		public BackedModelUi()
		{
		}
		
// Not needed, because all of the individual fields are directly backed to the document		
//		public void load(SnpGenDocument inDest)
//			throws IllegalAccessException
//		{
//		}
//
//		public void store(SnpGenDocument inDest)
//			throws IllegalAccessException
//		{
//			int		attributeCount = (Integer) attributeCountUi.getValue();
//			modelIdUi.store(inDest);
//			attributeCountUi.store(inDest);
//			heritabilityUi.store(inDest);
//			for(int i = 0; i < attributeCount; ++i)
//			{
//				attributeNameUiList.get(i).store(inDest);
//				attributeAlleleFrequencyUiList.get(i).store(inDest);
//			}
//		}
	}
	
	private static class BackedCheckBox extends JCheckBox implements BackedComponent
	{
		
		private Object docField;
		
		public BackedCheckBox(Object inDocField)
		{
			super();
			docField = inDocField;
		}

		public void load(SnpGenDocument inDest)
			throws IllegalAccessException
		{
		}

		public void store(SnpGenDocument inDest)
			throws IllegalAccessException
		{
			if(docField != null)
				docField = getValue();
		}
		
		public void setValue(Object in)
		{
			setSelected((Boolean) in);
		}
		
		public Object getValue()
		{
			return isSelected();
		}
	}
	
	private static class BackedTextField extends JTextField implements BackedComponent
	{
		
		private DocMember docField;
		
		public BackedTextField(int inColumnCount, DocMember inDocField)
		{
			super(inColumnCount);
			docField = inDocField;
		}

		public void load(SnpGenDocument inDest)
			throws IllegalAccessException
		{
		}

		public void store(SnpGenDocument inDest)
			throws IllegalAccessException
		{
			if(docField != null)
				docField.setValue(getValue());
		}
		
		public void setValue(Object in)
		{
			setText(in.toString());
		}
		
		public Object getValue()
		{
			Object outValue;
			
			outValue = null;
			try
			{
				if(docField == null)
					outValue = getText();
				else if(docField.getClass().equals(DocInteger.class))
					outValue = new Integer(getText());
				else if(docField.getClass().equals(DocFloat.class))
					outValue = new Float(getText());
				else if(docField.getClass().equals(DocString.class))
					outValue = getText();
			}
			catch(NumberFormatException nfe)
			{}

			return outValue;
		}
	}
	
//	public static class SnpFileLoader extends JPanel
//	{
//		JLabel filenameLabel;
//		JLabel attributeCountLabel;
//		public JLabel instanceCountLabel;
//		
//		public SnpFileLoader(SnpGenMainWindow inWindow)
//		{
//			create(inWindow);
//		}
//		
//		private void create(SnpGenMainWindow inWindow)
//		{
////			setBorder(BorderFactory.createTitledBorder("Input file"));
//			setAlignmentY(Component.TOP_ALIGNMENT);
//			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
//			
//			JPanel buttonPanel = new JPanel();
//			buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
//			buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
//			add(buttonPanel, BorderLayout.CENTER);
//			
//			JButton loadButton = new JButton(new LoadSnpFileAction(inWindow));
//			buttonPanel.add(loadButton, BorderLayout.CENTER);
//			JButton viewButton = new JButton(new ViewSnpFileAction(inWindow));
//			buttonPanel.add(viewButton, BorderLayout.CENTER);
//			
//			filenameLabel = (JLabel) addComponent(JLabel.class, this, 0, "File:");
//			filenameLabel.setText("(none)");
//			attributeCountLabel = (JLabel) addComponent(JLabel.class, this, 0, "Number of Attributes:");		// The total number of attributes
//			attributeCountLabel.setText("0");
//			instanceCountLabel = (JLabel) addComponent(JLabel.class, this, 0, "Total number of Instances:");	// The total number of instances
//			instanceCountLabel.setText("0");
//		}
//		
//		public class LoadSnpFileAction extends SnpGenAction {
//			public LoadSnpFileAction(SnpGenMainWindow inWindow) {
//				super(inWindow, "Load SNP file", null, "Load a SNP file", KeyEvent.VK_L, KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
//			}
//			public void actionPerformed(ActionEvent e) {
//				loadSnpFile(mSnpGen);
//			}
//		}
//		
//		public static class ViewSnpFileAction extends SnpGenAction {
//			public ViewSnpFileAction(SnpGenMainWindow inWindow) {
//				super(inWindow, "View SNP file", null, "View a SNP file", KeyEvent.VK_V, KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.ALT_MASK));
//			}
//			public void actionPerformed(ActionEvent e) {
//				mSnpGen.viewSnpFile();
//			}
//		}
//		
//		private void loadSnpFile(SnpGenMainWindow inWindow)
//		{
//			File			file = null;
//			
//			final JFileChooser fc = new JFileChooser();
//			int returnVal = fc.showOpenDialog(inWindow.frame);
//			if(returnVal == JFileChooser.APPROVE_OPTION)
//			{
//				file = fc.getSelectedFile();
//				filenameLabel.setText(file.getName());
//				inWindow.loadSnpFile();
//			}
//		}
//	}
	
	public class CaseControlPanel extends JPanel implements FocusListener, ChangeListener
	{
		BackedTextField caseCountField;
		BackedTextField controlCountField;
		BackedCheckBox fairAndBalancedCheckBox;
		boolean isBalanced;
		JLabel sampleSizeLabel;
		
		public CaseControlPanel(SnpGenDocument inDocument)
		{
			create(inDocument);
		}
		
		private void create(SnpGenDocument inDocument)
		{
			setAlignmentY(Component.TOP_ALIGNMENT);
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			fairAndBalancedCheckBox = addBackedBoolean(this, inDocument.caseControlRatioBalanced, 3, "Balanced case/control ratio");			// Whether the ratio of cases to controls is balanced
			fairAndBalancedCheckBox.addChangeListener(this);
			isBalanced = false;
			if(inDocument.caseControlRatioBalanced != null && inDocument.caseControlRatioBalanced.getBoolean() != null)
				isBalanced = inDocument.caseControlRatioBalanced.getBoolean();
			JPanel caseControlParameters = new JPanel();
			caseCountField = addBackedInteger(caseControlParameters, inDocument.firstDataset.caseCount, 3, "Number of cases");					// The number of cases
			caseCountField.setText(SnpGenDocument.kDefaultCaseCountString);
			caseCountField.addFocusListener(this);
			controlCountField = addBackedInteger(caseControlParameters, inDocument.firstDataset.controlCount, 3, "Number of controls");			// The number of controls
			controlCountField.setText(SnpGenDocument.kDefaultControlCountString);
			controlCountField.addFocusListener(this);
			add(caseControlParameters, BorderLayout.CENTER);
			sampleSizeLabel = (JLabel) addComponent(JLabel.class, this, null, 0, "Total sample size:");											// The total number of instances
			updateSampleSize();
		}
		
		public void focusGained(FocusEvent e)
		{
			
		}
		
		public void focusLost(FocusEvent e)
		{
			if(isBalanced)
				setControlCountToCaseCount();
			updateSampleSize();
		}
		
		public void stateChanged(ChangeEvent e)
		{
			if(e.getSource().equals(fairAndBalancedCheckBox))
				updateFairAndBalancedCheckBox();
		}
		
		public void updateSampleSize()
		{
			Integer size = ((Integer) caseCountField.getValue() + (Integer) controlCountField.getValue());
			sampleSizeLabel.setText(size.toString());
		}
		
		private void updateFairAndBalancedCheckBox()
		{
			isBalanced = (Boolean) fairAndBalancedCheckBox.getValue();
			if(isBalanced)
				setControlCountToCaseCount();
			controlCountField.setEnabled(!isBalanced);
		}
		
		public void setControlCountToCaseCount()
		{
			controlCountField.setValue((int) ((Integer) caseCountField.getValue()));
			updateSampleSize();
		}
	}
	
	public class DatasetPanel extends JPanel implements FocusListener
	{
		BackedTextField replicateCountField;
//		BackedTextField quantileCountField;
		JLabel datasetCountField;
		
		public DatasetPanel()
		{
			create();
		}
		
		private void create()
		{
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			
			replicateCountField = addBackedInteger(this, getDocument().firstDataset.replicateCount, 3, "Number of replicates");		// The number of random seeds
			replicateCountField.setText(SnpGenDocument.kDefaultSeedCountString);
			replicateCountField.addFocusListener(this);
			
			datasetCountField = (JLabel) addComponent(JLabel.class, this, null, 0, "Total number of datasets:");								// The total number of datasets
			updateDatasetCountField();
		}
		
		public void focusGained(FocusEvent e)
		{
		}
		
		public void focusLost(FocusEvent e)
		{
			updateDatasetCountField();
		}
		
		public void updateDatasetCountField()
		{
//			Integer size = ((Integer) replicateCountField.getValue() * (Integer) quantileCountField.getValue());
			Integer size = ((Integer) replicateCountField.getValue() * (Integer) desiredQuantileCount);
			datasetCountField.setText(size.toString());
		}
	}
	
	public class NoiseGenerator extends JPanel
	{
		public NoiseGenerator(SnpGenDocument inDocument)
		{
			create(inDocument);
		}
		
		private void create(SnpGenDocument inDocument)
		{
			BackedTextField field;
			
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			
			JPanel topParameters;
			topParameters = new JPanel();
			topParameters.setBorder(BorderFactory.createLineBorder(Color.gray));
			topParameters.setAlignmentX(Component.CENTER_ALIGNMENT);
			topParameters.setLayout(new BoxLayout(topParameters, BoxLayout.X_AXIS));
			
			field = addBackedInteger(topParameters, getDocument().firstDataset.totalAttributeCount, 3, "Total number of attributes");	// The number of attributes
			field.setText(SnpGenDocument.kDefaultAttributeCountString);
			
			JPanel mafPanel = new JPanel();
			JLabel alleleFrequencyLabel = new JLabel("Minor-allele-frequency range");
			alleleFrequencyLabel.setOpaque(true);
			mafPanel.add(alleleFrequencyLabel, BorderLayout.WEST);
			field = addBackedFloat(mafPanel, getDocument().firstDataset.alleleFrequencyMin, 3, null);							// The minimum allele frequency
			field.setText(SnpGenDocument.kDefaultFrequencyMinString);
			field = addBackedFloat(mafPanel, getDocument().firstDataset.alleleFrequencyMax, 3, null);							// The maximum allele frequency
			field.setText(SnpGenDocument.kDefaultFrequencyMaxString);
			topParameters.add(mafPanel, BorderLayout.CENTER);
			
			add(topParameters, BorderLayout.CENTER);
			
			JPanel caseControlPanel = new CaseControlPanel(inDocument);
			caseControlPanel.setBorder(BorderFactory.createLineBorder(Color.gray));
			add(caseControlPanel, BorderLayout.CENTER);
			
			datasetControlPanel = new DatasetPanel();
			datasetControlPanel.setBorder(BorderFactory.createLineBorder(Color.gray));
			add(datasetControlPanel, BorderLayout.CENTER);
		}
	}
	
	public class NoiseReader extends JPanel implements FocusListener
	{
		JLabel filenameLabel;
		JLabel attributeCountLabel;
		JLabel instanceCountLabel;
		Integer instanceCount = null;
		BackedTextField caseCountField = null;
		BackedTextField controlCountField = null;
		
		public NoiseReader()
		{
			create();
		}
		
		private void create()
		{
			BackedTextField field;
			SnpGenDocument document;
			document = SnpGenMainWindow.this.getDocument();
			
//			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
//			add(new SnpFileLoader(SnpGenMainWindow.this));
//			setBorder(BorderFactory.createTitledBorder("Input file"));
			setAlignmentY(Component.TOP_ALIGNMENT);
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			
			JPanel buttonPanel = new JPanel();
			buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
			buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
			add(buttonPanel, BorderLayout.CENTER);
			
			JButton loadButton = new JButton(new LoadDataFileAction(SnpGenMainWindow.this));
			buttonPanel.add(loadButton, BorderLayout.CENTER);
//			JButton viewButton = new JButton(new ViewDataFileAction(SnpGenMainWindow.this));
//			buttonPanel.add(viewButton, BorderLayout.CENTER);
			
			filenameLabel = (JLabel) addComponent(JLabel.class, this, 0, "File:");
			filenameLabel.setText("(none)");
			attributeCountLabel = (JLabel) addComponent(JLabel.class, this, 0, "Number of attributes:");		// The total number of attributes
			attributeCountLabel.setText("0");
			instanceCountLabel = (JLabel) addComponent(JLabel.class, this, 0, "Total number of instances:");	// The total number of instances
			setInstanceCount(0);
			
//			add(new CaseControlPanel(inDocument), BorderLayout.CENTER);
			
//			JPanel caseControlParameters = new JPanel();
//			caseCountField = addBackedInteger(caseControlParameters, document.firstDataset.caseCount, 3, "Number of Cases");					// The number of cases
//			caseCountField.setText("0");
//			caseCountField.addFocusListener(this);
//			controlCountField = addBackedInteger(caseControlParameters, document.firstDataset.controlCount, 3, "Number of Controls");			// The number of controls
//			updateControlCountField();
//			controlCountField.addFocusListener(this);
//			controlCountField.setEnabled(false);
//			add(caseControlParameters);

//			field = addBackedInteger(this, getDocument().rasQuantileCount, 3, "Number of EDM quantiles");				// The number of penetrance-table variants
//			field.setText(SnpGenDocument.kDefaultRasQuantileCountString);
		}
		
		private void setInstanceCount(Integer inInstanceCount)
		{
			instanceCount = inInstanceCount;
			instanceCountLabel.setText(inInstanceCount.toString());
			updateControlCountField();
		}
		
		public void focusGained(FocusEvent e)
		{
		}
		
		public void focusLost(FocusEvent e)
		{
			updateControlCountField();
		}
		
		public void updateControlCountField()
		{
			if(instanceCount != null && caseCountField != null && controlCountField != null)
			{
				Integer count = instanceCount - (Integer) caseCountField.getValue();
				controlCountField.setText(count.toString());
			}
		}
		
		public class LoadDataFileAction extends SnpGenAction {
			public LoadDataFileAction(SnpGenMainWindow inWindow) {
				super(inWindow, "Load SNP file", null, "Load a SNP file", KeyEvent.VK_L, KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
			}
			public void actionPerformed(ActionEvent e) {
				loadDataFile(mSnpGen);
			}
		}
		
		public class ViewDataFileAction extends SnpGenAction {
			public ViewDataFileAction(SnpGenMainWindow inWindow) {
				super(inWindow, "View SNP file", null, "View a SNP file", KeyEvent.VK_V, KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.ALT_MASK));
			}
			public void actionPerformed(ActionEvent e) {
				mSnpGen.viewDataFile();
			}
		}
		
		private void loadDataFile(SnpGenMainWindow inWindow)
		{
			File			file = null;
			SnpGenDocument	document;
			
			document = SnpGenMainWindow.this.getDocument();
			
//			final JFileChooser fileChooser = new JFileChooser();
			int returnVal = fileChooser.showOpenDialog(inWindow.frame);
			if(returnVal == JFileChooser.APPROVE_OPTION)
			{
				file = fileChooser.getSelectedFile();
				filenameLabel.setText(file.getName());
				document.noiseInputFile = file;
				int[][] dataArray = null;
				try
				{
					dataArray = SnpGenSimulator.parseDataInputFile(file, null);
				}
				catch(InputException ie)
				{}
				catch(IOException ioe)
				{}
				if(dataArray != null)
				{
					attributeCountLabel.setText(((Integer)dataArray[0].length).toString());
					setInstanceCount(dataArray.length);
				}
			}
		}
	}
	
	public class NonPredictiveAttributesPanel extends JPanel implements ActionListener
	{
		JRadioButton generateButton;
		JRadioButton readButton;
		NoiseGenerator generator;
		NoiseReader reader;
		
		public NonPredictiveAttributesPanel(SnpGenDocument inDocument)
		{
			create(inDocument);
		}
		
		private void create(SnpGenDocument inDocument)
		{
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setBorder(BorderFactory.createTitledBorder("Non-predictive Attributes"));
			setAlignmentX(Component.CENTER_ALIGNMENT);
			
			JPanel radioControlPanel;
			radioControlPanel = new JPanel();
			
			// Create the radio buttons.
			generateButton = new JRadioButton("Generate");
			generateButton.setSelected(true);
			readButton = new JRadioButton("Read from file");
			
			// Group the radio buttons.
			ButtonGroup group = new ButtonGroup();
			group.add(generateButton);
			group.add(readButton);
			
			// Register a listener for the radio buttons.
			generateButton.addActionListener(this);
			readButton.addActionListener(this);

			radioControlPanel.add(generateButton);
			radioControlPanel.add(readButton);

			add(radioControlPanel);
			
			generator = new NoiseGenerator(getDocument());
			generator.setVisible(true);
			add(generator);
			
			reader = new NoiseReader();
			reader.setVisible(false);
			add(reader);
		}
		
		public void actionPerformed(ActionEvent e)
		{
			if(e.getSource().equals(generateButton))
			{
				generator.setVisible(true);
				reader.setVisible(false);
			}
			else if(e.getSource().equals(readButton))
			{
				generator.setVisible(false);
				reader.setVisible(true);
			}
		}
	}
	
	public class ProgressDialog implements SnpGenSimulator.ProgressHandler
	{
		JDialog dialog;
		JProgressBar progressBar;
		
		public ProgressDialog(String inLabel)
		{
			dialog = new JDialog(frame, "Progress", true);
			progressBar = new JProgressBar(0, 500);
			dialog.add(BorderLayout.CENTER, progressBar);
			progressBar.setVisible(true);
			dialog.add(BorderLayout.NORTH, new JLabel(inLabel));
			dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			dialog.setSize(300, 75);
			dialog.setLocationRelativeTo(frame);
		}
		
		public void setMaximum(int inMax)
		{
			progressBar.setMaximum(inMax);
		}
		
		public void setValue(int inMax)
		{
			progressBar.setValue(inMax);
		}
		
		public void setVisible(boolean inVisibility)
		{
			dialog.setVisible(inVisibility);
		}
	}

	public SnpGenMainWindow(SnpGenDocument inSnpGenDocument)
	{
		file = null;
		simulator = new SnpGenSimulator();
		documentLink = new GuiDocumentLink();
		documentLink.setDocument(inSnpGenDocument);
	}
	
	private SnpGenDocument getDocument()
	{
		return documentLink.getDocument();
	}
	
	private void createSnpGenDocument()
		throws IllegalAccessException
	{
		documentLink.setDocument(new SnpGenDocument(true));
		documentLink.documentToGui();
	}
	
	private void openDocument()
		throws IllegalAccessException
	{
		int returnVal = fileChooser.showOpenDialog(frame);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			file = fileChooser.getSelectedFile();
			loadDocument(file, documentLink);
		}
	}
	
	private void saveDocument()
		throws IllegalAccessException
	{
		if(file == null)
			saveDocumentAs();
		else
			storeDocument(file, documentLink);
	}
	
	private void saveDocumentAs()
		throws IllegalAccessException
	{
		int returnVal = fileChooser.showSaveDialog(frame);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			file = fileChooser.getSelectedFile();
			storeDocument(file, documentLink);
		}
	}
	
	// Load the file specified by the instance variable "file" into the UI
	private void loadDocument(File inFile, GuiDocumentLink inLink)
		throws IllegalAccessException
	{
		inLink.getDocument().load(inFile);
		inLink.documentToGui();
	}
	
	// Store the UI into the file specified by the instance variable "file"
	private void storeDocument(File inFile, GuiDocumentLink inLink)
		throws IllegalAccessException
	{
		inLink.guiToDocument();
		inLink.getDocument().store(inFile);
	}
	
	public JMenuBar createMenuBar() {
		JMenuBar menuBar;
		JMenu menu;
		JMenuItem menuItem;

		//Create the menu bar.
		menuBar = new JMenuBar();

		//Build the file menu.
		menu = new JMenu("File");
		menu.setMnemonic(KeyEvent.VK_F);
		menu.getAccessibleContext().setAccessibleDescription("The file menu");
		menuBar.add(menu);
		menu.add(new JMenuItem(new NewAction(this)));
		menu.add(new JMenuItem(new OpenAction(this)));
		menu.add(new JMenuItem(new SaveAction(this)));
		menu.add(new JMenuItem(new SaveAsAction(this)));
		
		return menuBar;
	}

	public Container createContentPane()
	{
		JPanel contentPane;						// The content pane of the window
		JPanel datasetPane;						// The left tab
		JPanel modelPane;						// The right tab
		
		contentPane = new JPanel();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
		contentPane.setOpaque(true);
		
		modelPane = new JPanel();
		modelPane.setBorder(BorderFactory.createTitledBorder("Model Construction"));
		fillInModelPane(modelPane);
		
		datasetPane = new JPanel();
		datasetPane.setBorder(BorderFactory.createTitledBorder("Dataset Construction"));
		fillInDatasetPane(datasetPane);
		
		contentPane.add(modelPane, BorderLayout.CENTER);
		contentPane.add(datasetPane, BorderLayout.CENTER);

		// Create the command section at the bottom of the frame:
		JPanel commandPanel;
		commandPanel = new JPanel();
		commandPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		JButton generateButton = new JButton(new GenerateAction(this));
		commandPanel.add(generateButton, BorderLayout.CENTER);
		contentPane.add(commandPanel);

		return contentPane;
	}
	
	private void fillInDatasetPane(JPanel inDatasetPane)
	{
		inDatasetPane.setLayout(new BoxLayout(inDatasetPane, BoxLayout.Y_AXIS));
		
//		// Create the non-predictive attributes section, the first section:
//		JPanel nonPredictiveAttributes;
//		nonPredictiveAttributes = new JPanel();
//		nonPredictiveAttributes.setBorder(BorderFactory.createTitledBorder("Non-predictive Attributes"));
//		nonPredictiveAttributes.setAlignmentX(Component.CENTER_ALIGNMENT);
//		
//		NoiseGenerator generator = new NoiseGenerator(getDocument());
//		generator.setVisible(true);
//		nonPredictiveAttributes.add(generator);
//		
//		NoiseReader reader = new NoiseReader(getDocument());
//		reader.setVisible(false);
//		nonPredictiveAttributes.add(reader);
//		
//		inDatasetPane.add(nonPredictiveAttributes);
		
		inDatasetPane.add(new NonPredictiveAttributesPanel(getDocument()));
	}
	
	private void fillInDatasetPane_OLD(JPanel inDatasetPane)
	{
		BackedTextField field;
		inDatasetPane.setLayout(new BoxLayout(inDatasetPane, BoxLayout.Y_AXIS));
		
		// Create the non-predictive attributes section, the first section:
		JPanel nonPredictiveAttributes;
		nonPredictiveAttributes = new JPanel();
		nonPredictiveAttributes.setBorder(BorderFactory.createTitledBorder("Non-predictive Attributes"));
		nonPredictiveAttributes.setAlignmentX(Component.CENTER_ALIGNMENT);
//		nonPredictiveAttributes.setLayout(new BoxLayout(nonPredictiveAttributes, BoxLayout.X_AXIS));
		JLabel alleleFrequencyLabel = new JLabel("Minor-allele-frequency Range");
		alleleFrequencyLabel.setOpaque(true);
		nonPredictiveAttributes.add(alleleFrequencyLabel, BorderLayout.WEST);
		field = addBackedFloat(nonPredictiveAttributes, getDocument().firstDataset.alleleFrequencyMin, 3, null);							// The minimum allele frequency
		field.setText(SnpGenDocument.kDefaultFrequencyMinString);
		field = addBackedFloat(nonPredictiveAttributes, getDocument().firstDataset.alleleFrequencyMax, 3, null);							// The maximum allele frequency
		field.setText(SnpGenDocument.kDefaultFrequencyMaxString);
		inDatasetPane.add(nonPredictiveAttributes);
		
		// Create the input-file section, the second section:
//		SnpFileLoader inputFilePanel = new SnpFileLoader(this);
//		inDatasetPane.add(inputFilePanel);
		
		// Create the major-parameters section, the third section:
		JPanel majorParameters;
		majorParameters = new JPanel();
		majorParameters.setBorder(BorderFactory.createTitledBorder("Major Parameters"));
		majorParameters.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		majorParameters.setLayout(new BoxLayout(majorParameters, BoxLayout.X_AXIS));
		JPanel majorParametersLeft = new JPanel();
		majorParametersLeft.setAlignmentY(Component.TOP_ALIGNMENT);
		majorParametersLeft.setLayout(new BoxLayout(majorParametersLeft, BoxLayout.Y_AXIS));
		field = addBackedInteger(majorParametersLeft, getDocument().firstDataset.totalAttributeCount, 3, "Total number of attributes");						// The number of attributes
		field.setText(SnpGenDocument.kDefaultAttributeCountString);
		
		JPanel caseControlPanel = new CaseControlPanel(getDocument());
		
		majorParameters.add(majorParametersLeft, BorderLayout.CENTER);
		majorParameters.add(caseControlPanel, BorderLayout.CENTER);
		inDatasetPane.add(majorParameters);
		
		// Create the replicate-parameters section, the fourth section:
		JPanel replicateParameters;
		replicateParameters = new JPanel();
		replicateParameters.setBorder(BorderFactory.createTitledBorder("Replicate Parameters"));
		replicateParameters.setAlignmentX(Component.CENTER_ALIGNMENT);
		replicateParameters.setLayout(new BoxLayout(replicateParameters, BoxLayout.Y_AXIS));
		addBackedBoolean(replicateParameters, getDocument().generateReplicates, 3, "Generate Replicates");							// Whether to generate replicates
		JPanel datasetParametersUpper = new JPanel();
		field = addBackedInteger(datasetParametersUpper, getDocument().firstDataset.replicateCount, 3, "Number of Replicates");		// The number of random seeds
		field.setText(SnpGenDocument.kDefaultSeedCountString);
		field = addBackedInteger(datasetParametersUpper, getDocument().rasQuantileCount, 3, "Number of EDM quantiles");				// The number of penetrance-table variants
		field.setText(SnpGenDocument.kDefaultRasQuantileCountString);
		replicateParameters.add(datasetParametersUpper, BorderLayout.CENTER);
		JPanel datasetParametersLower = new JPanel();
		field = addBackedInteger(datasetParametersLower, getDocument().rasPopulationCount, 3, "Size of Variant Distribution");		// Size of the distribution of variants
		field.setText(SnpGenDocument.kDefaultPopulationCountString);
		addComponent(JLabel.class, datasetParametersLower, null, 0, "Total number of Datasets:");									// The total number of datasets
		replicateParameters.add(datasetParametersLower, BorderLayout.CENTER);
		inDatasetPane.add(replicateParameters);
	}
	
	private void fillInModelPane(JPanel inModelPane)
	{
		inModelPane.setLayout(new BoxLayout(inModelPane, BoxLayout.Y_AXIS));
		
		// Create the model-creation pane:
		JPanel modelCreationPane;	
		modelCreationPane = new JPanel();
		modelCreationPane.setAlignmentX(Component.CENTER_ALIGNMENT);
		JButton generateButton = new JButton(new GenerateModelAction(this));
		modelCreationPane.add(generateButton, BorderLayout.CENTER);
		JButton createButton = new JButton(new CreateModelAction(this));
		modelCreationPane.add(createButton, BorderLayout.CENTER);
		JButton loadButton = new JButton(new LoadModelAction(this));
		modelCreationPane.add(loadButton, BorderLayout.CENTER);
		editButton = new JButton(new EditModelAction(this));
		editButton.setEnabled(false);
		modelCreationPane.add(editButton, BorderLayout.CENTER);
		deleteButton = new JButton(new RemoveModelAction(this));
		deleteButton.setEnabled(false);
		modelCreationPane.add(deleteButton, BorderLayout.CENTER);
		inModelPane.add(modelCreationPane);
		
		// Create the model-parameter pane:
		JPanel modelParameters;
		modelParameters = new JPanel();
		modelParameters.setBorder(BorderFactory.createLineBorder(Color.black));
		modelParameters.setAlignmentX(Component.CENTER_ALIGNMENT);
//		addBackedModel(modelParameters, documentLink.getDocument().addDocModel(1));
		modelParameters.setLayout(new BoxLayout(modelParameters, BoxLayout.Y_AXIS));
		
//		initExampleModels();
		modelTable = ModelTable.createModelTable(documentLink.getDocument());
		JScrollPane scrollPane = new JScrollPane(modelTable);
		modelTable.setFillsViewportHeight(true);
		modelParameters.add(scrollPane);
//		modelParameters.add(table);
		
		inModelPane.add(modelParameters);
        
		// Create the model-footer pane:
		JPanel modelFooterPane;	
		modelFooterPane = new JPanel();
		modelFooterPane.setAlignmentX(Component.CENTER_ALIGNMENT);
		quantileCountFieldMainWindow = (JLabel) addComponent(JLabel.class, modelFooterPane, null, 0, "Number of EDM Quantiles:");
		quantileCountFieldMainWindow.setText("0");
		inModelPane.add(modelFooterPane);
	}
	
	public void updateQuantileCountField(int inCount)
	{
		Integer count = inCount;
		quantileCountFieldMainWindow.setText(count.toString());
		datasetControlPanel.updateDatasetCountField();
	}

	
	private void initExampleModels()
	{
		SnpGenDocument doc = documentLink.getDocument();
		SnpGenDocument.DocModel model;
		model = doc.addNewDocModel(2, "Model A", new String[] {"Snp1", "Snp2"}, new double[] {0.4F, 0.6F});
		model.heritability.setValue(0.2F);
		
		model = doc.addNewDocModel(1, "Model T", new String[] {"Snp3"}, new double[] {0.4F});
		model.heritability.setValue(0.3F);
		
		model = doc.addNewDocModel(2, "Model Q", new String[] {"Snp4", "Snp5", "Snp6"}, new double[] {0.4F, 0.6F, 0.5F});
		model.heritability.setValue(0.05F);
	}
	
	public BackedModelUi addBackedModel(Container inContainer, DocModel inDocModel)
	{
		BackedModelUi		outModelUi = new BackedModelUi();
		
//		outModelUi.setLayout(new BoxLayout(outModelUi, BoxLayout.X_AXIS));
		outModelUi.setOpaque(true);
		
		outModelUi.attributeCount = inDocModel.attributeCount.getInteger();
		outModelUi.modelIdUi = addBackedString(outModelUi, inDocModel.modelId, 3, null);
		outModelUi.attributeCountUi = addBackedInteger(outModelUi, inDocModel.attributeCount, 3, null);
		outModelUi.heritabilityUi = addBackedFloat(outModelUi, inDocModel.heritability, 3, null);
		outModelUi.attributeNameUiList = new ArrayList<BackedTextField>();
		outModelUi.attributeAlleleFrequencyUiList = new ArrayList<BackedTextField>();
		outModelUi.add(outModelUi.attributeNamePanel = new JPanel());
		outModelUi.add(outModelUi.attributeAlleleFrequencyPanel = new JPanel());
		outModelUi.attributeNamePanel.setLayout(new BoxLayout(outModelUi.attributeNamePanel, BoxLayout.Y_AXIS));
		outModelUi.attributeAlleleFrequencyPanel.setLayout(new BoxLayout(outModelUi.attributeAlleleFrequencyPanel, BoxLayout.Y_AXIS));
		for(int i = 0; i < outModelUi.attributeCount; ++i)
		{
			outModelUi.attributeNameUiList.add(addBackedString(outModelUi.attributeNamePanel, inDocModel.attributeNameArray[i], 3, null));
			outModelUi.attributeAlleleFrequencyUiList.add(addBackedFloat(outModelUi.attributeAlleleFrequencyPanel, inDocModel.attributeAlleleFrequencyArray[i], 3, null));
		}
		inContainer.add(outModelUi);
		return outModelUi;
	}
	
	private BackedCheckBox addBackedBoolean(Container inContainer, DocBoolean inBackingField, int inColumnCount, String inLabel)
	{
		return (BackedCheckBox) addComponent(JCheckBox.class, inContainer, inBackingField, inColumnCount, inLabel);
	}
	
	private BackedTextField addBackedInteger(Container inContainer, DocInteger inBackingField, int inColumnCount, String inLabel)
	{
		return (BackedTextField) addComponent(JTextField.class, inContainer, inBackingField, inColumnCount, inLabel);
	}
	
	private BackedTextField addBackedFloat(Container inContainer, DocFloat inBackingField, int inColumnCount, String inLabel)
	{
		return (BackedTextField) addComponent(JTextField.class, inContainer, inBackingField, inColumnCount, inLabel);
	}
	
	private BackedTextField addBackedString(Container inContainer, DocString inBackingField, int inColumnCount, String inLabel)
	{
		return (BackedTextField) addComponent(JTextField.class, inContainer, inBackingField, inColumnCount, inLabel);
	}
	
	// WARNING: If you change the relationship of inClass to the class of the output JComponent,
	// be sure to update the createBacked<whatever> methods above.
	protected JComponent addComponentOLD(Class inClass, Container inContainer, DocMember inBackingField, int inColumnCount, String inLabel)
	{
		JLabel				label = null;
		JComponent			outComp = null;
		JComponent			holder;
		JPanel				panel;
		
		if(inLabel != null)
		{
			label = new JLabel(inLabel);
			label.setOpaque(true);
		}
		if(inBackingField != null)
		{
			if(inClass.equals(JCheckBox.class))
				outComp = new BackedCheckBox(inBackingField);
			if(inClass.equals(JTextField.class))
				outComp = new BackedTextField(inColumnCount, inBackingField);
			documentLink.addBackedComponent((BackedComponent)outComp);
		}
		else
		{
			if(inClass.equals(JCheckBox.class))
				outComp = new JCheckBox();
			if(inClass.equals(JTextField.class))
				outComp = new JTextField(inColumnCount);
			if(inClass.equals(JLabel.class))
				outComp = new JLabel();
		}
		outComp.setOpaque(true);
		holder = outComp;
		if(inLabel != null)
		{
			panel = new JPanel();
			panel.add(label, BorderLayout.WEST);
			panel.add(outComp, BorderLayout.WEST);
			holder = panel;
		}
		if(inContainer != null)
			inContainer.add(holder, BorderLayout.WEST);
		return outComp;
	}
	
	// WARNING: If you change the relationship of inClass to the class of the output JComponent,
	// be sure to update the createBacked<whatever> methods above.
	protected JComponent addComponent(Class inClass, Container inContainer, DocMember inBackingField, int inColumnCount, String inLabel)
	{
		JLabel				label = null;
		JComponent			outComp = null;
		JComponent			holder;
		JPanel				panel;
		
		outComp = null;
		if(inBackingField != null)
		{
			if(inClass.equals(JCheckBox.class))
				outComp = new BackedCheckBox(inBackingField);
			if(inClass.equals(JTextField.class))
				outComp = new BackedTextField(inColumnCount, inBackingField);
			documentLink.addBackedComponent((BackedComponent)outComp);
		}
		return addComponent(outComp, inClass, inContainer, inColumnCount, inLabel);
	}
	
	// WARNING: If you change the relationship of inClass to the class of the output JComponent,
	// be sure to update the createBacked<whatever> methods above.
	static protected JComponent addComponent(Class inClass, Container inContainer, int inColumnCount, String inLabel)
	{
		return addComponent(null, inClass, inContainer, inColumnCount, inLabel);
	}
	
	// WARNING: If you change the relationship of inClass to the class of the output JComponent,
	// be sure to update the createBacked<whatever> methods above.
	static protected JComponent addComponent(JComponent inComp, Class inClass, Container inContainer, int inColumnCount, String inLabel)
	{
		JLabel				label = null;
		JComponent			outComp = null;
		JComponent			holder;
		JPanel				panel;
		
		if(inComp == null)
		{
			if(inClass.equals(JCheckBox.class))
				outComp = new JCheckBox();
			if(inClass.equals(JTextField.class))
				outComp = new JTextField(inColumnCount);
			if(inClass.equals(JLabel.class))
				outComp = new JLabel();
		}
		else
			outComp = inComp;
		
		if(inLabel != null)
		{
			label = new JLabel(inLabel);
			label.setOpaque(true);
		}
		
		outComp.setOpaque(true);
		holder = outComp;
		if(inLabel != null)
		{
			panel = new JPanel();
			panel.add(label, BorderLayout.WEST);
			panel.add(outComp, BorderLayout.WEST);
			holder = panel;
		}
		if(inContainer != null)
			inContainer.add(holder, BorderLayout.WEST);
		return outComp;
	}
	
	private Field getDocumentField(String inFieldName)
	{
		Field outField = null;
		
		try {
			outField = SnpGenDocument.class.getDeclaredField(inFieldName);
		}
		catch(NoSuchFieldException nsfe)
		{
//			throw new Exception("Field not found in the Document class: " + inFieldName);
			System.out.println("Field not found in the Document class: " + inFieldName);
//			nsfe.printStackTrace();
		}
		return outField;
	}
	
	private void addSnpModel()
	{
		final SnpGenDocument document = getDocument();
		int modelNumber = nextModelNumber++;
		GenerateModelDialog modelDialog = new GenerateModelDialog(frame, modelNumber, document.getNextPredictiveAttributeNumber());
//		// Since the new model will replace the old one, the new model's predictive attribute names should always start at 1:
//		GenerateModelDialog modelDialog = new GenerateModelDialog(frame, getDocument().getNextModelNumber(), 1);
        modelDialog.pack();
        modelDialog.setLocationRelativeTo(frame);
        modelDialog.setVisible(true);
        if(modelDialog.isSaved())
        {
			int attributeCount = modelDialog.getAttributeCount();
			String[] attributeNames = modelDialog.getAttributeNames();
			double[] attributeMafs = modelDialog.getAttributeMinorAlleleFrequencies();
//			removeSnpModel();
			final DocModel model = document.addNewDocModel(attributeCount, /*"Model 1"*/ "Model " + modelNumber, attributeNames, attributeMafs);
			if(document.getModelCount() > 0)
			{
				editButton.setEnabled(true);
				deleteButton.setEnabled(true);
			}
			model.heritability.setValue(modelDialog.getHeritability());
			model.prevalence.setValue(modelDialog.getPrevalence());
			model.useOddsRatio.setValue(modelDialog.getUseOddsRatio());
			model.fraction.setValue(new Float(0));
			desiredQuantileCount = modelDialog.getQuantileCountFieldValue();
			document.rasQuantileCount.setValue(desiredQuantileCount);
			final int desiredPopulationCount = modelDialog.getQuantilePopulationFieldValue();
			long tryCountLong = Math.max((long) desiredPopulationCount * 100L, 100000L);
			tryCountLong = Math.min(tryCountLong, Integer.MAX_VALUE);
			final int tablesToTryCount = (int) tryCountLong;
			document.rasPopulationCount.setValue(desiredPopulationCount);
			document.rasTryCount.setValue(tablesToTryCount);
			updateQuantileCountField(desiredQuantileCount);
			
			boolean errorReported = false;
			try
			{
 //   			documentLink.guiToDocument();
    			final File outputFile = chooseFile(frame, "Location for model files");
    			document.outputFile = outputFile;
				final ProgressDialog progressor = new ProgressDialog("Generating models...");
				
				simulator.setDocument(document);
				SwingWorker<Exception, Void> worker = new SwingWorker<Exception, Void>()
				{
				    @Override
				    public Exception doInBackground()
				    {
				    	Exception outException = null;
				    	try
				    	{
				    		if(progressor != null)
				    		{
				    			progressor.setMaximum(tablesToTryCount);
				    		}
				    		
				    		ArrayList<DocModel> modelList = new ArrayList<DocModel>();
				    		modelList.add(model);
							double[][] allTableScores = simulator.generateTablesForModels(modelList, desiredQuantileCount, desiredPopulationCount, tablesToTryCount, progressor);
							simulator.writeTablesAndScoresToFile(modelList, allTableScores, desiredQuantileCount, outputFile);
//			    			progressor.setVisible(false);
				    	}
				    	catch(Exception ex)
				    	{
				    		outException = ex;
				    	}
			    		progressor.setVisible(false);
				        return outException;
				    }
				};
				synchronized(worker)
				{
					worker.execute();
		    		progressor.setVisible(true);
				}
				Exception except = worker.get();
				if(except != null)
					throw except;
    		}
    		catch(Exception ex)
    		{
    			handleException(ex);
    			errorReported = true;
    		}
    		if(!errorReported && simulator.getTablePopulationCountFound() < desiredPopulationCount)
    			showErrorMessage("Warning", "You asked for a population of " + desiredPopulationCount + " models, but I only found " + simulator.getTablePopulationCountFound());
        }
	}
	
	private void createSnpModel()
	{
		final SnpGenDocument document = getDocument();
		int firstAttributeNumber = document.getNextPredictiveAttributeNumber();
		String[] snpTitles = new String[]{"P" + firstAttributeNumber, "P" + (firstAttributeNumber + 1), "P" + (firstAttributeNumber + 2), };
		int locusCount = 3;
		DocModel model = new DocModel(locusCount);
//		model.modelId.setString("Model " + document.getNextModelNumber());
		model.modelId.setString("Model " + nextModelNumber++);
		for(int i = 0; i < locusCount; ++i)
			model.attributeNameArray[i].setString(snpTitles[i]);
		editSnpModel(-1, model);
	}
	
	private void editSnpModel()
	{
		final SnpGenDocument document = getDocument();
		int whichModel = document.modelList.size() - 1;
		DocModel model = document.modelList.get(whichModel);
		editSnpModel(whichModel, model);
	}
	
	private void editSnpModel(int whichModel, DocModel inModel)
	{
		final SnpGenDocument document = getDocument();
		DocModel model = new DocModel(inModel);
		model.setParentDoc(null);
		String[] snpMajorAlleles = new String[]{"A", "B", "C"};
		String[] snpMinorAlleles = new String[]{"a", "b", "c"};
		EditModelDialog editModelDialog = new EditModelDialog(model, frame, snpMajorAlleles, snpMinorAlleles);
        editModelDialog.pack();
        editModelDialog.setLocationRelativeTo(frame);
        editModelDialog.setVisible(true);
        if(editModelDialog.isSaved())
        {
        	model = editModelDialog.getModel();
        	if(whichModel < 0)
        		document.addDocModel(model);
        	else
        		document.updateDocModel(whichModel, model);
    		model.setParentDoc(document);
			if(document.getModelCount() > 0)
			{
				editButton.setEnabled(true);
				deleteButton.setEnabled(true);
			}
        }
	}
	
	private void removeSnpModel()
	{
		final SnpGenDocument document = getDocument();
		int whichModel = document.modelList.size() - 1;
		if(document.getModelCount() > 0)
			document.removeDocModel(whichModel);
		if(document.getModelCount() <= 0)
		{
			editButton.setEnabled(false);
			deleteButton.setEnabled(false);
		}
	}
	
	private void loadModelFile()
	{
		try
		{
//			final JFileChooser fileChooser = new JFileChooser();
			int returnVal = fileChooser.showOpenDialog(frame);
			if(returnVal == JFileChooser.APPROVE_OPTION)
			{
				simulator.loadModel(fileChooser.getSelectedFile());
				PenetranceTable firstTable = simulator.getPenetranceTableQuantiles()[0].tables[0];
				String[] attributeNames = firstTable.getAttributeNames();
				double[] attributeMafs = firstTable.getMinorAlleleFrequencies();
				removeSnpModel();
				DocModel model = documentLink.getDocument().addNewDocModel(attributeNames.length, "Model 1", attributeNames, attributeMafs);
				if(documentLink.getDocument().getModelCount() > 0)
				{
					editButton.setEnabled(true);
					deleteButton.setEnabled(true);
				}
				model.heritability.setValue((float)firstTable.getActualHeritability());
			}
		}
		catch(Exception ex)
		{
			handleException(ex);
		}
	}
	
	private void viewDataFile()
	{
	}
	
	/**
	 * Create the GUI and show it.  For thread safety,
	 * this method should be invoked from the
	 * event-dispatching thread.
	 */
	private void createAndShowGUI()
	{
		try
		{
			documentLink.documentToGui();
		}
		catch(IllegalAccessException iea)
		{}
		
		//Create and set up the window.
		frame = new JFrame("GAMETES 1.0 Beta");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		//Create and set up the content pane.
//		frame.setJMenuBar(createMenuBar());
		frame.setContentPane(createContentPane());

		//Display the window.
		frame.setSize(720, 900);
		frame.setVisible(true);
	}
	
	private void generateDatasets()
	{
		Exception paramError;
		try
		{
			documentLink.guiToDocument();
			SnpGenDocument document = documentLink.getDocument();
			if((paramError = document.verifyAllNeededParameters()) != null)
				throw paramError;
			document.outputFile = chooseFile(frame, "Location for generated datasets");
			if(document.outputFile != null)
			{
				for(SnpGenDocument.DocDataset dataset: document.datasetList)
					dataset.outputFile = document.outputFile;
				
				final ProgressDialog progressor = new ProgressDialog("Saving datasets...");
//				simulator.setProgressHandler(progressor);
				simulator.setDocument(document);
				simulator.combineModelTablesIntoQuantiles(document.modelList, document.inputFiles, document.inputFileFractions);
				SwingWorker<Exception, Void> worker = new SwingWorker<Exception, Void>()
				{
				    @Override
				    public Exception doInBackground()
				    {
				    	Exception outException = null;
				    	try
				    	{
				    		simulator.generateDatasets(progressor);
				    		progressor.setVisible(false);
				    	}
				    	catch(Exception ex)
				    	{
				    		ex.printStackTrace();
				    		outException = ex;
				    	}
				        return outException;
				    }
				};
				synchronized(worker)
				{
					worker.execute();
		    		progressor.setVisible(true);
				}
				Exception except = worker.get();
				if(except != null)
					throw except;
				
//				simulator.generateDatasets(doc, null);
				
				
				
//				progressor.setVisible(false);
			}
		}
		catch(Exception ex)
		{
			handleException(ex);
		}
	}
	
	private Exception handleException(Exception inEx)
	{
		if(inEx instanceof InputException)
		{
			showErrorMessage("Input error", inEx.getLocalizedMessage());
			inEx.printStackTrace();
		}
		else if(inEx instanceof ProcessingException)
		{
			showErrorMessage("Error in processing", inEx.getLocalizedMessage());
			inEx.printStackTrace();
		}
		else
		{
			String msg = inEx.getMessage();
			System.out.println(msg);
			inEx.printStackTrace();
		}
		return null;		// could return an exception that still needs to be handled
	}
	
	private void showErrorMessage(String inTitle, String inMessage)
	{
		System.out.println(inMessage);
		JOptionPane.showMessageDialog(frame, inMessage, inTitle, JOptionPane.ERROR_MESSAGE);
	}
	
	private File chooseFile(Component inParent, String inTitle)
	{
		File			outFile = null;
		
//		final JFileChooser fileChooser = new JFileChooser();
		if(inTitle != null)
			fileChooser.setDialogTitle(inTitle);
		int returnVal = fileChooser.showSaveDialog(inParent);
		if(returnVal == JFileChooser.APPROVE_OPTION)
			outFile = fileChooser.getSelectedFile();
		return outFile;
	}
	
	private static void createAndShowGui(SnpGenDocument inSnpGenDocument)
	{
		final SnpGenMainWindow snpGen = new SnpGenMainWindow(inSnpGenDocument);
		
		//Schedule a job for the event-dispatching thread: creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				snpGen.createAndShowGUI();
			}
		});
	}
	
	private static void runDocument(SnpGenDocument inDocument)
	{
//		if(inDoc.modelList.size() > 1)
//		{
//			System.out.println("* * * No more than one model allowed, ignoring others.");
//			DocModel firstModel = inDoc.modelList.get(0);
//			inDoc.modelList.clear();
//			inDoc.modelList.add(firstModel);
//		}
		Exception paramError;
		try
		{
			if((paramError = inDocument.verifyAllNeededParameters()) != null)
				throw paramError;
			
			SnpGenSimulator simulator = new SnpGenSimulator();
			simulator.setDocument(inDocument);
			simulator.setRandomSeed(inDocument.randomSeed);
			int desiredQuantileCount = inDocument.rasQuantileCount.getInteger();
			ArrayList<DocModel> modelList = inDocument.modelList;
			double[][] allTableScores = simulator.generateTablesForModels(modelList, desiredQuantileCount, inDocument.rasPopulationCount.getInteger(), inDocument.rasTryCount.getInteger(), null);
			if(inDocument.outputFile != null)
				simulator.writeTablesAndScoresToFile(modelList, allTableScores, desiredQuantileCount, inDocument.outputFile);
			simulator.combineModelTablesIntoQuantiles(modelList, inDocument.inputFiles, inDocument.inputFileFractions);
			simulator.generateDatasets(null);
		}
		catch(Exception ex)
		{
			System.err.println(ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	public static void main(String[] args)
	{
//		String argString = "-r 17 -i new_models.txt -f .99 -f .01 -D \"-a 10 -s 10000 -w 10000 -r 1 -o combined_new.txt\"";
//		String argString = "-M \"-n test -h 0.1 -p 0.5 -a 0.3 -a 0.1 \" -M \"-n test -h 0.03 -p 0.5 -a 0.5 -a 0.5 -a 0.5 \" -q 3 -p 1000 -t 100000-o combined_new.txt\"";
//		args = argString.split(" ");
		SnpGenDocument doc = new SnpGenDocument(false);
		
		boolean showGui = false;
		try
		{
			showGui = doc.parseArguments(args);
		}
		catch(Exception e )
		{
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		
		boolean runDocument = doc.runDocument;
		
		if(doc.showHelp)
			SnpGenDocument.printOptionHelp();
		
		if(showGui)
		{
			doc.createFirstDataset();
			createAndShowGui(doc);
		}
		
		if(runDocument)
			runDocument(doc);
	}
}