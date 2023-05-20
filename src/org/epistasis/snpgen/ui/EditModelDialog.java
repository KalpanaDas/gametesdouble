package org.epistasis.snpgen.ui;

/*
 * Portions copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

import org.epistasis.snpgen.document.*;
import org.epistasis.snpgen.document.SnpGenDocument.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;

import org.epistasis.snpgen.document.SnpGenDocument.DocModel;
import org.epistasis.snpgen.simulator.*;
import org.epistasis.snpgen.simulator.PenetranceTable.*;
import org.epistasis.snpgen.ui.GenerateModelDialog.CancelAction;
import org.epistasis.snpgen.ui.GenerateModelDialog.ConfirmAction;
import org.epistasis.snpgen.ui.PenetranceTablePane.Square;
import org.epistasis.snpgen.ui.SnpGenMainWindow.Action;

import java.text.*;
import java.util.Arrays;
import java.awt.*;
import java.awt.event.*;

class EditModelDialog extends JDialog implements ModelUpdater, FocusListener, ChangeListener
{
	private static final int maxLocusCount = 3;
	private static final int alleleCount = 3;
	
	private DocModel					model;
	private boolean						saved;
	NumberFormat						integerNumberFormat;
	NumberFormat						floatNumberFormat;
	private JRadioButton				locus2Button;
	private JRadioButton				locus3Button;
	PenetranceTablePane					tablePane;
	DefaultTableModel					tableModel;
	
	String[]							snpTitles;
	String[]							snpMajorAlleles;
	String[]							snpMinorAlleles;
	
	LabelledTextField[]					mafTextFields;
	
	private int locusCount;
	
	LabelledLabel heritability;
	LabelledLabel prevalence;
	LabelledLabel edm;
	LabelledLabel oddsRatio;
	MarginalPenetranceSet marginalPenetranceSet;
	
	// For implementing ModelUpdater:
	Square[] squares;
	
	private static class LabelledTextField extends JPanel
	{
		public JLabel label;
		public JFormattedTextField textField;
		
		public LabelledTextField(String inLabel, int inColumnCount, NumberFormat inFormat, FocusListener inFocusListener)
		{
			super();
//			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
//			setAlignmentY(Component.CENTER_ALIGNMENT);
			label = new JLabel(inLabel);
			label.setOpaque(true);
			add(label, BorderLayout.WEST);
			textField = new JFormattedTextField(inFormat);
			textField.setColumns(inColumnCount);
			textField.setOpaque(true);
			textField.addFocusListener(inFocusListener);
			add(textField, BorderLayout.EAST);
		}
	}
	
	private static class LabelledLabel extends JPanel
	{
		public JLabel label;
		public JLabel value;
		
		public LabelledLabel(String inLabel)
		{
			super();
			label = new JLabel(inLabel);
			label.setOpaque(true);
			add(label, BorderLayout.WEST);
			value = new JLabel("");
			value.setOpaque(true);
			add(value, BorderLayout.EAST);
		}
	}
	
	public class MarginalPenetrance extends JPanel
	{
		JLabel[] penetranceValues;
		
		public MarginalPenetrance(Graphics graphics, String inOverallLabel, String inMajorAllele, String inMinorAllele)
		{
			super();
			setLayout(null);
			
			int topTitleMargin = 0;
			int topAlleleMargin = 5;
			int leftTitleMargin = 5;
			int alleleLabelMargin = 5;
			int penetranceValueMarginX = 5;
			int penetranceValueMarginY = 5;
			
			JLabel leftTitleLabel = new JLabel(inOverallLabel);
			
			FontMetrics fontMetrics;
			Font font;
			
			font = leftTitleLabel.getFont();
			fontMetrics = graphics.getFontMetrics(font);
			
			int leftTitleWidth  = fontMetrics.stringWidth(inOverallLabel);
			int leftTitleHeight = fontMetrics.getHeight();
			
			String[] alleleNames = new String[alleleCount];
			alleleNames[0] = inMajorAllele + inMajorAllele;
			alleleNames[1] = inMajorAllele + inMinorAllele;
			alleleNames[2] = inMinorAllele + inMinorAllele;
			
			JLabel[] alleleLabels = new JLabel[alleleCount];
			for(int i = 0; i < alleleCount; ++i)
				alleleLabels[i] = new JLabel(alleleNames[i]);
			int alleleLabelWidth  = Math.max(Math.max(fontMetrics.stringWidth(alleleNames[0]), fontMetrics.stringWidth(alleleNames[1])), fontMetrics.stringWidth(alleleNames[2]));
			int alleleLabelHeight = fontMetrics.getHeight();
			
			int penetranceValueWidth = 100;
			int penetranceValueHeight = (int) (fontMetrics.getHeight() * 1.1);
			
			penetranceValues = new JLabel[alleleCount];
			for(int i = 0; i < alleleCount; ++i)
			{
				penetranceValues[i] = new JLabel();
//				penetranceValueHeight = penetranceValues[i].getHeight();
				penetranceValues[i].setPreferredSize(new Dimension(penetranceValueWidth, penetranceValueHeight));
			}

//			System.out.println("topTitleWidth=" + topTitleWidth + "\ttopTitleHeight=" + topTitleHeight + "\tleftTitleWidth=" + leftTitleWidth + "\tleftTitleHeight=" + leftTitleHeight);
//			System.out.println("topAlleleWidth=" + topAlleleWidth + "\ttopAlleleHeight=" + topAlleleHeight + "\tleftAlleleWidth=" + leftAlleleWidth + "\tleftAlleleHeight=" + leftAlleleHeight);
			
			int leftHeaderWidth = leftTitleWidth + leftTitleMargin + alleleLabelWidth + alleleLabelMargin;
			int totalWidth = leftHeaderWidth + penetranceValueWidth + 40;
//			System.out.println("totalWidth=" + totalWidth);
			int totalHeight = topTitleMargin + topAlleleMargin + alleleCount * (penetranceValueHeight + penetranceValueMarginY);
			
			add(leftTitleLabel);
			leftTitleLabel.setBounds(0, 0, leftTitleWidth, leftTitleHeight);
			
//			System.out.println("topTitleLeft=" + topTitleLeft + "\tleftTitleTop=" + leftTitleTop);
//			System.out.println();
//			System.out.println();
			
			for(int j = 0; j < alleleCount; ++j)
			{
				int y = j * (penetranceValueHeight + penetranceValueMarginY);
				add(alleleLabels[j]);
				alleleLabels[j].setBounds(leftTitleWidth + leftTitleMargin, y, alleleLabelWidth, alleleLabelHeight);
				add(penetranceValues[j]);
				penetranceValues[j].setBounds(leftTitleWidth + leftTitleMargin + alleleLabelWidth + alleleLabelMargin, y, penetranceValueWidth, penetranceValueHeight);
			}
			Dimension overallSize = new Dimension(totalWidth, totalHeight);
			setPreferredSize(overallSize);
		}
		
		public void updateMarginalPenetrances(double[] inMarginalPenetrances)
		{
			try
			{
				for(int whichAlleleValue = 0; whichAlleleValue < alleleCount; ++whichAlleleValue)
				{
					int width1 = penetranceValues[whichAlleleValue].getWidth();
					int sWidth1 = penetranceValues[whichAlleleValue].getPreferredSize().width;
					penetranceValues[whichAlleleValue].setText(floatNumberFormat.format(inMarginalPenetrances[whichAlleleValue]));
//					int penetranceValueWidth = 100;
//					int penetranceValueHeight = penetranceValues[whichAlleleValue].getHeight();
//					penetranceValues[whichAlleleValue].setPreferredSize(new Dimension(penetranceValueWidth, penetranceValueHeight));
					int width2 = penetranceValues[whichAlleleValue].getWidth();
					int sWidth2 = penetranceValues[whichAlleleValue].getPreferredSize().width;
//					System.out.println("width1=" + width1 + ", width2=" + width2 + "\tsWidth1=" + sWidth1 + ", sWidth2=" + sWidth2);
				}
			}
			catch(NullPointerException npe)
			{
				npe = null;
			}
		}
	}
	
	public class MarginalPenetranceSet extends JPanel
	{
		MarginalPenetrance[] panes;
		public MarginalPenetranceSet(Graphics graphics, String[] inSnpTitles, String[] inSnpMajorAlleles, String[] inSnpMinorAlleles)
		{
			super();
			Border inset = BorderFactory.createEmptyBorder(10,10,10,10);
			Border line = BorderFactory.createLineBorder(Color.gray);
			Border compound = BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(inset, line), inset);
			setBorder(compound);
//			setBorder(BorderFactory.createLineBorder(Color.gray));
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setOpaque(true);
			
			JPanel titlePane = new JPanel();
			titlePane.add(new JLabel("Marginal Penetrances:"));
			add(titlePane);
			
			panes = new MarginalPenetrance[3];
			for(int i = 0; i < 3; ++i)
			{
				panes[i] = new MarginalPenetrance(graphics, inSnpTitles[i], inSnpMajorAlleles[i], inSnpMinorAlleles[i]);
				add(panes[i]);
			}
		}
		
		public void setLocusCount()
		{
			if(locusCount == 3)
				panes[2].setVisible(true);
			else
				panes[2].setVisible(false);
		}
		
		public void updateMarginalPenetrances(double[][] inMarginalPenetrances)
		{
			for(int whichLocus = 0; whichLocus < locusCount; ++whichLocus)
			{
				panes[whichLocus].updateMarginalPenetrances(inMarginalPenetrances[whichLocus]);
			}
		}
	}
	
	public class ConfirmAction extends Action {
		public ConfirmAction() {
			super("Save", null, "Confirm", KeyEvent.VK_G, KeyStroke.getKeyStroke(KeyEvent.VK_G, ActionEvent.ALT_MASK));
		}
		public void actionPerformed(ActionEvent e) {
			confirm();
		}
	}
	
	public class CancelAction extends Action {
		public CancelAction() {
			super("Cancel", null, "Confirm", KeyEvent.VK_G, KeyStroke.getKeyStroke(KeyEvent.VK_G, ActionEvent.ALT_MASK));
		}
		public void actionPerformed(ActionEvent e) {
			cancel();
		}
	}

    public EditModelDialog(DocModel inModel, Frame aFrame, String[] inSnpMajorAlleles, String[] inSnpMinorAlleles)
    {
        super(aFrame, true);
        
        model = inModel;
		locusCount = inModel.attributeCount.getInteger();
		assert locusCount == 2 || locusCount == 3;
        
        snpTitles = new String[maxLocusCount];
        snpTitles[2] = "P3";			// default, in case locusCount < 3
		for(int i = 0; i < locusCount; ++i)
			snpTitles[i] = model.attributeNameArray[i].getString();
        snpMajorAlleles = Arrays.copyOf(inSnpMajorAlleles, alleleCount);
        snpMinorAlleles = Arrays.copyOf(inSnpMinorAlleles, alleleCount);
        
        integerNumberFormat = NumberFormat.getIntegerInstance();
        floatNumberFormat = NumberFormat.getNumberInstance();
        
		PenetranceTable[] tables = model.getPenetranceTables();
		if(tables == null || tables.length == 0)
		{
			tables = new PenetranceTable[1];
			model.setPenetranceTables(tables);
		}
		if(tables[0] == null)
		{
			PenetranceTable table = new PenetranceTable(3, locusCount);
			tables[0] = table;
		}
		PenetranceTable table = tables[0];
		assert table.attributeCount == locusCount;
		table.setAttributeNames(Arrays.copyOf(snpTitles, locusCount));
		
        setContentPane(createContentPane());
        modelToPenetranceSquares();
        mafModelToUiAll();
        updateCalculatedFields();
        
//        //Ensure the text field always gets the first focus.
//        addComponentListener(new ComponentAdapter() {
//            public void componentShown(ComponentEvent ce) {
//                textField.requestFocusInWindow();
//            }
//        });
    }
	
	public void focusGained(FocusEvent e)
	{
		
	}
	
	public void focusLost(FocusEvent e)
	{
		boolean isUpdatingComponent = false;
		for(int i = 0; i < alleleCount; ++i)
		{
			if(e.getComponent().equals(mafTextFields[i].textField))
			{
				isUpdatingComponent = true;
				break;
			}
		}
		if(isUpdatingComponent)
			updateModel();
	}

	public boolean isSaved()
	{
		return saved;
	}
	
	public Container createContentPane()
	{
		JPanel contentPane;						// The content pane of the window
		JPanel headerPane;						// The top pane
		JPanel footerPane;						// The top pane
		JPanel modelPane;						// The bottom pane
//		JPanel tablePane;						// The bottom-left pane
		JPanel parameterPane;					// The bottom-right pane
		JPanel mafPane;							// The MAF-parameter pane
		JPanel calculatedPane;					// The calculated-parameter pane
//		JPanel penetrancePane;					// The marginal-penetrance pane
		
		Graphics graphics = getOwner().getGraphics();
		
		contentPane = new JPanel();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
		contentPane.setOpaque(true);
		
		headerPane = new JPanel();
		headerPane.setLayout(new BoxLayout(headerPane, BoxLayout.X_AXIS));
		headerPane.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		// Create the radio buttons.
		locus2Button = new JRadioButton("2-locus");
		locus3Button = new JRadioButton("3-locus");
		if(locusCount == 3)
			locus3Button.setSelected(true);
		else
			locus2Button.setSelected(true);
		
		// Group the radio buttons.
		ButtonGroup group = new ButtonGroup();
		group.add(locus2Button);
		group.add(locus3Button);
		
		// Register a listener for the radio buttons.
		locus2Button.addChangeListener(this);
		locus3Button.addChangeListener(this);
		
		headerPane.add(new JLabel("Model Order:"));
		headerPane.add(locus2Button);
		headerPane.add(locus3Button);

		contentPane.add(headerPane);
		
		modelPane = new JPanel();
		modelPane.setLayout(new BoxLayout(modelPane, BoxLayout.X_AXIS));
		modelPane.setAlignmentY(Component.CENTER_ALIGNMENT);
		
		contentPane.add(modelPane);
		
//		tablePane = new JPanel();
//		tablePane.setLayout(new BoxLayout(tablePane, BoxLayout.X_AXIS));
//		tablePane.setAlignmentY(Component.LEFT_ALIGNMENT);
//		modelPane.add(tablePane);
		
		tablePane = new PenetranceTablePane(this, graphics, snpTitles, snpMajorAlleles, snpMinorAlleles, locusCount);
		modelPane.add(tablePane);
//		modelPane.add(tablePane, BorderLayout.CENTER);
		
		parameterPane = new JPanel();
		parameterPane.setLayout(new BoxLayout(parameterPane, BoxLayout.Y_AXIS));
		parameterPane.setAlignmentX(Component.RIGHT_ALIGNMENT);
		
		mafPane = new JPanel();
		mafPane.setLayout(new BoxLayout(mafPane, BoxLayout.Y_AXIS));
		mafPane.setAlignmentX(Component.RIGHT_ALIGNMENT);
		
		mafTextFields = new LabelledTextField[maxLocusCount];
		for(int i = 0; i < maxLocusCount; ++i)
		{
			mafTextFields[i] = new LabelledTextField("MAF " + snpTitles[i] + ":", 4, floatNumberFormat, this);
			if(i < locusCount)
				mafTextFields[i].setVisible(true);
			else
				mafTextFields[i].setVisible(false);
			mafPane.add(mafTextFields[i]);
		}
		
		Border inset = BorderFactory.createEmptyBorder(10,10,10,10);
		Border line = BorderFactory.createLineBorder(Color.gray);
		Border compound = BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(inset, line), inset);
		mafPane.setBorder(compound);
		
		parameterPane.add(mafPane);
		
		calculatedPane = new JPanel();
		calculatedPane.setLayout(new BoxLayout(calculatedPane, BoxLayout.Y_AXIS));
		calculatedPane.setAlignmentX(Component.RIGHT_ALIGNMENT);
		
		JPanel calculatedSubPane = new JPanel();
		calculatedSubPane.setLayout(new BoxLayout(calculatedSubPane, BoxLayout.Y_AXIS));
		calculatedSubPane.setAlignmentX(Component.RIGHT_ALIGNMENT);
		
		heritability = new LabelledLabel("Heritability:");
		prevalence = new LabelledLabel("Prevalence:");
		edm = new LabelledLabel("EDM:");
		oddsRatio = new LabelledLabel("COR:");
		heritability.setVisible(true);
		prevalence.setVisible(true);
		edm.setVisible(true);
		oddsRatio.setVisible(true);
		calculatedSubPane.add(heritability);
		calculatedSubPane.add(prevalence);
		calculatedSubPane.add(edm);
		calculatedSubPane.add(oddsRatio);
		inset = BorderFactory.createEmptyBorder(10,10,10,10);
		line = BorderFactory.createLineBorder(Color.gray);
		compound = BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(inset, line), inset);
		calculatedSubPane.setBorder(compound);
		calculatedPane.add(calculatedSubPane);
		
		int width = 192;
		int height = calculatedSubPane.getPreferredSize().height;
		calculatedSubPane.setPreferredSize(new Dimension(width, height));
		
		marginalPenetranceSet = new MarginalPenetranceSet(graphics, snpTitles, snpMajorAlleles, snpMinorAlleles);
		marginalPenetranceSet.setLocusCount();
		marginalPenetranceSet.setVisible(true);
		calculatedPane.add(marginalPenetranceSet);
		
		parameterPane.add(calculatedPane);
		
//		penetrancePane = new JPanel();
//		penetrancePane.setLayout(new BoxLayout(penetrancePane, BoxLayout.Y_AXIS));
//		penetrancePane.setAlignmentX(Component.RIGHT_ALIGNMENT);
//		
//		parameterPane.add(penetrancePane);
		
		modelPane.add(parameterPane);
//		modelPane.add(parameterPane, BorderLayout.LINE_END);
		
		footerPane = new JPanel();
		footerPane.setLayout(new BoxLayout(footerPane, BoxLayout.X_AXIS));
		footerPane.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		// Create the Save and Cancel buttons.
		JButton confirmButton = new JButton(new ConfirmAction());
		footerPane.add(confirmButton, BorderLayout.CENTER);
		JButton cancelButton = new JButton(new CancelAction());
		footerPane.add(cancelButton, BorderLayout.CENTER);

		contentPane.add(footerPane);
		
		return contentPane;
	}
	
	// Implement ModelUpdater
	public void setSquares(Square[] squares)
	{
		this.squares = squares;
	}
	
	public void modelToPenetranceSquares()
	{
		PenetranceTable[] tables = model.getPenetranceTables();
		PenetranceTable table = tables[0];
		assert table.attributeCount == locusCount;
		if(locusCount == 2)
		{
			CellId cellId = new CellId(2);
			modelToOnePenetranceSquare(table, cellId, squares[0], 0);
		}
		else if(locusCount == 3)
		{
			CellId cellId = new CellId(3);
			for(int k = 0; k < 3; ++k)
			{
				cellId.setIndex(2, k);
				modelToOnePenetranceSquare(table, cellId, squares[k], 0);
			}
		}
	}
	
	// "Row i, column j" means that i specifies the y-coordinate and j specifies the x-coordinate.
	private void modelToOnePenetranceSquare(PenetranceTable table, CellId cellId, Square square, int firstCellIdIndex)
	{
		for(int i = 0; i < 3; ++i)
		{
			cellId.setIndex(firstCellIdIndex + 1, i);
			for(int j = 0; j < 3; ++j)
			{
				cellId.setIndex(firstCellIdIndex, j);
				square.cells[i][j].setText(floatNumberFormat.format(table.getPenetranceValue(cellId)));
			}
		}
	}
	
	public void penetranceSquaresToModel()
	{
		PenetranceTable[] tables = model.getPenetranceTables();
		PenetranceTable table = tables[0];
		assert table.attributeCount == locusCount;
		if(locusCount == 2)
		{
			CellId cellId = new CellId(2);
			onePenetranceSquareToModel(squares[0], table, cellId, 0);
		}
		else if(locusCount == 3)
		{
			CellId cellId = new CellId(3);
			for(int k = 0; k < 3; ++k)
			{
				cellId.setIndex(2, k);
				onePenetranceSquareToModel(squares[k], table, cellId, 0);
			}
		}
	}
	
	// "Row i, column j" means that i specifies the y-coordinate and j specifies the x-coordinate.
	private void onePenetranceSquareToModel(Square square, PenetranceTable table, CellId cellId, int firstCellIdIndex)
	{
		for(int i = 0; i < 3; ++i)
		{
			cellId.setIndex(firstCellIdIndex + 1, i);
			for(int j = 0; j < 3; ++j)
			{
				cellId.setIndex(firstCellIdIndex, j);
				double value = 0;
				try
				{
					value = Double.parseDouble(square.cells[i][j].getText());
				}
				catch(NumberFormatException nfe)
				{
				}
				table.setPenetranceValue(cellId, value);
			}
		}
	}
	
	public void mafUiToModelAll()
	{
		float[] mafs = new float[locusCount];
		assert locusCount == 2 || locusCount == 3;
		if(locusCount == 2)
		{
			mafs[0] = (float) mafUiToModelOne(0);
			mafs[1] = (float) mafUiToModelOne(1);
			model.attributeAlleleFrequencyArray[0].setFloat(mafs[0]);
			model.attributeAlleleFrequencyArray[1].setFloat(mafs[1]);
		}
		else if(locusCount == 3)
		{
			mafs[0] = (float) mafUiToModelOne(0);
			mafs[1] = (float) mafUiToModelOne(1);
			mafs[2] = (float) mafUiToModelOne(2);
			model.attributeAlleleFrequencyArray[0].setFloat(mafs[0]);
			model.attributeAlleleFrequencyArray[1].setFloat(mafs[1]);
			model.attributeAlleleFrequencyArray[2].setFloat(mafs[2]);
		}
		
		PenetranceTable[] tables = model.getPenetranceTables();
		PenetranceTable table = tables[0];
		table.setMinorAlleleFrequencies(mafs);
	}
	
	private double mafUiToModelOne(int which)
	{
		double value = 0;
		String text = mafTextFields[which].textField.getText();
		if(text != null && text.length() > 0)
		{
			try
			{
				value = Double.parseDouble(text);
			}
			catch(NumberFormatException nfe)
			{
			}
		}
		return value;
	}
	
	public void mafModelToUiAll()
	{
		assert locusCount == 2 || locusCount == 3;
		if(locusCount == 2)
		{
			mafModelToUiOne(0, model.attributeAlleleFrequencyArray[0].getFloat());
			mafModelToUiOne(1, model.attributeAlleleFrequencyArray[1].getFloat());
		}
		else if(locusCount == 3)
		{
			mafModelToUiOne(0, model.attributeAlleleFrequencyArray[0].getFloat());
			mafModelToUiOne(1, model.attributeAlleleFrequencyArray[1].getFloat());
			mafModelToUiOne(2, model.attributeAlleleFrequencyArray[2].getFloat());
		}
	}
	
	private void mafModelToUiOne(int which, Float inValue)
	{
		String text;
		if(inValue == null)
			text = floatNumberFormat.format(0);
		else
			text = floatNumberFormat.format(inValue);
		mafTextFields[which].textField.setText(text);
	}
	
	public void updateModel()
	{
		penetranceSquaresToModel();
		mafUiToModelAll();
		updateCalculatedFields();
	}
	
	// In the PenetranceTable of the model, we use a CellId.
	// If locusCount == 2, cellId[0] specifies which row and cellId[1] specifies which column.
	// If locusCount == 3, cellId[0] specifies which row, cellId[1] specifies which column, and cellId[2] specifies which square.
	public void updateCalculatedFields()
	{
		PenetranceTable[] tables = model.getPenetranceTables();
		PenetranceTable table = tables[0];
		
//		int width1 = prevalence.getWidth();
//		int sWidth1 = prevalence.getPreferredSize().width;
		
		float heritabilityValue = (float) table.calcAndSetHeritability();
		model.heritability.setFloat(heritabilityValue);
		heritability.value.setText(floatNumberFormat.format(heritabilityValue));
		
		float prevalenceValue = (float) table.calcAndSetPrevalence();
		model.prevalence.setFloat(prevalenceValue);
		prevalence.value.setText(floatNumberFormat.format(prevalenceValue));
		
		edm.value.setText(floatNumberFormat.format(table.calcAndSetEdm()));
		oddsRatio.value.setText(floatNumberFormat.format(table.calcAndSetOddsRatio()));
		
//		int width2 = prevalence.getWidth();
//		int sWidth2 = prevalence.getPreferredSize().width;
//		System.out.println("prevalence\twidth1=" + width1 + ", width2=" + width2 + "\tsWidth1=" + sWidth1 + ", sWidth2=" + sWidth2);

		
		double[][] marginalPenetrances = table.calcMarginalPrevalences();
		double[][] rearrangedMarginalPenetrances = null;
		if(locusCount == 2)
		{
			rearrangedMarginalPenetrances = new double[2][];
			rearrangedMarginalPenetrances[0] = marginalPenetrances[0];
			rearrangedMarginalPenetrances[1] = marginalPenetrances[1];
		}
		else if(locusCount == 3)
		{
			rearrangedMarginalPenetrances = new double[3][];
			rearrangedMarginalPenetrances[0] = marginalPenetrances[0];
			rearrangedMarginalPenetrances[1] = marginalPenetrances[1];
			rearrangedMarginalPenetrances[2] = marginalPenetrances[2];
		}
		marginalPenetranceSet.updateMarginalPenetrances(rearrangedMarginalPenetrances);
	}
	
	public void updateLocusCountInModel()
	{
		int oldLocusCount = model.attributeCount.getInteger();
		if(oldLocusCount != locusCount)
		{
			model.resetAttributeCount(locusCount);
			for(int i = oldLocusCount; i < locusCount; ++i)
				model.attributeNameArray[i] = new DocString(snpTitles[i]);
			
			PenetranceTable[] tables = model.getPenetranceTables();
			PenetranceTable oldTable = tables[0];
			assert oldLocusCount == oldTable.attributeCount;
			
			PenetranceTable newTable = new PenetranceTable(3, locusCount);
			CellId oldTableCellId = null;
			CellId newTableCellId = null;
			if(oldLocusCount == 2)
			{
				oldTableCellId = new CellId(2);
			}
			else if(oldLocusCount == 3)
			{
				oldTableCellId = new CellId(3);
				oldTableCellId.setIndex(2, 0);
			}
			if(locusCount == 2)
			{
				newTableCellId = new CellId(2);
			}
			else if(locusCount == 3)
			{
				newTableCellId = new CellId(3);
				newTableCellId.setIndex(2, 0);
			}
			if((oldLocusCount == 2 && locusCount == 3) || (oldLocusCount == 3 && locusCount == 2))
			{
				for(int i = 0; i < 3; ++i)
				{
					oldTableCellId.setIndex(1, i);
					newTableCellId.setIndex(1, i);
					for(int j = 0; j < 3; ++j)
					{
						oldTableCellId.setIndex(0, j);
						newTableCellId.setIndex(0, j);
						newTable.setPenetranceValue(newTableCellId, oldTable.getPenetranceValue(oldTableCellId));
					}
				}
			}
			else
				assert false;		// Should be one of the above two possibilities
			if(locusCount == 2)
				mafTextFields[2].textField.setText("");		// to match the behavior of the penetrance-table pane, which clears the newly-hidden fields
			newTable.setAttributeNames(Arrays.copyOf(snpTitles, locusCount));

			tables[0] = newTable;
			modelToPenetranceSquares();
		}
	}
	
	public void stateChanged(ChangeEvent e)
	{
		if(e.getSource().equals(locus2Button) || e.getSource().equals(locus3Button))
		{
			if(locus2Button.isSelected())
				setLocusCount(2);
			else if(locus3Button.isSelected())
				setLocusCount(3);
		}
	}
	
	protected void confirm()
	{
		saved = true;
		setVisible(false);
	}
	
	protected void cancel()
	{
		saved = false;
		setVisible(false);
	}
	
	public void setLocusCount(int inLocusCount)
	{
		if(locusCount != inLocusCount)
		{
			locusCount = inLocusCount;
			if(tablePane != null)
				tablePane.setLocusCount(inLocusCount);
			if(locusCount == 3)
				mafTextFields[2].setVisible(true);
			else
				mafTextFields[2].setVisible(false);
			marginalPenetranceSet.setLocusCount();
			updateLocusCountInModel();
		}
	}

    /** This method clears the dialog and hides it. */
    public void clearAndHide() {
//        textField.setText(null);
        setVisible(false);
    }

	private int getLocusCount()
	{
		return locusCount;
	}

	DocModel getModel()
	{
		return model;
	}

	void setModel(DocModel model)
	{
		this.model = model;
	}
}