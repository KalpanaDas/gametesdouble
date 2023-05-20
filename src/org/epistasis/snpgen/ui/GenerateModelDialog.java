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

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import org.epistasis.snpgen.document.SnpGenDocument.DocModel;
import org.epistasis.snpgen.ui.SnpGenMainWindow.Action;
import org.epistasis.snpgen.ui.SnpGenMainWindow.NoiseGenerator;
import org.epistasis.snpgen.ui.SnpGenMainWindow.NoiseReader;
//import org.epistasis.snpgen.ui.SnpGenMainWindow.GenerateAction;

import java.beans.*; //property change stuff
import java.text.*;
import java.awt.*;
import java.awt.event.*;

class GenerateModelDialog extends JDialog implements ActionListener, PropertyChangeListener, FocusListener, ChangeListener
{
	private int							firstModelNumber;
	private int							firstAttributeNumber;
	private boolean						saved;
	NumberFormat						integerNumberFormat;
	NumberFormat						floatNumberFormat;
	private JRadioButton				edmButton;
	private JRadioButton				oddsButton;
	private JFormattedTextField			quantileCountTextField;
	private JFormattedTextField			quantilePopulationTextField;
	private JFormattedTextField			attributeCountTextField;
	private JFormattedTextField			heritabilityTextField;
	private JCheckBox					prevalenceCheckBox;
	private JFormattedTextField			prevalenceTextField;
	DefaultTableModel					tableModel;
	
	private static class LabelledTextField extends JPanel
	{
		public JLabel label;
		public JFormattedTextField textField;
		
		public LabelledTextField(String inLabel, int inColumnCount, NumberFormat inFormat)
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
			add(textField, BorderLayout.WEST);
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

    public GenerateModelDialog(Frame aFrame, int inNextModelNumber, int inNextSnpNumber)
    {
        super(aFrame, true);
        firstModelNumber = inNextModelNumber;
        firstAttributeNumber = inNextSnpNumber;
        integerNumberFormat = NumberFormat.getIntegerInstance();
        floatNumberFormat = NumberFormat.getNumberInstance();
        
        setContentPane(createContentPane());

        //Ensure the text field always gets the first focus.
        addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent ce) {
//                textField.requestFocusInWindow();
            }
        });

        //Register an event handler that puts the text into the option pane.
//        textField.addActionListener(this);

        //Register an event handler that reacts to option pane state changes.
 //       optionPane.addPropertyChangeListener(this);
    }
	
	public void focusGained(FocusEvent e)
	{
		
	}
	
	public void focusLost(FocusEvent e)
	{
		Component where = e.getComponent();
		if(where == attributeCountTextField)
		{
			int attrCount = 0;
			try
			{
				attributeCountTextField.commitEdit();
			}
			catch(ParseException pe)
			{
				// Ignore the exception and just use attributeCountTextField's best guess, below.
			}
			try
			{
//				attrCount = Integer.parseInt(attributeCountTextField.getValue().toString());
				attrCount = integerNumberFormat.parse(attributeCountTextField.getValue().toString()).intValue();
			}
			catch(ParseException pe)
			{
				// TODO: Should never get here, because the field is a JFormattedTextField with a NumberFormat.getIntegerInstance().
			}
			resetAttributeCount(attrCount);
		}
	}

	public boolean isSaved()
	{
		return saved;
	}
	
	public int getQuantileCountFieldValue()
	{
		int quantileCount = 0;
		try
		{
//			quantileCount = Integer.parseInt(quantileCountTextField.getText());
			quantileCount = integerNumberFormat.parse(quantileCountTextField.getText()).intValue();
		}
		catch(ParseException pe)
		{
			// TODO: Should never get here, because the field is a JFormattedTextField with a NumberFormat.getIntegerInstance().
		}
		return quantileCount;
	}
	
	public int getQuantilePopulationFieldValue()
	{
		int quantilePopulation = 0;
		try
		{
//			quantilePopulation = Integer.parseInt(quantilePopulationTextField.getText());
			quantilePopulation = integerNumberFormat.parse(quantilePopulationTextField.getText()).intValue();
		}
		catch(ParseException pe)
		{
			// TODO: Should never get here, because the field is a JFormattedTextField with a NumberFormat.getIntegerInstance().
		}
		return quantilePopulation;
	}
	
	public int getAttributeCountFieldValue()
	{
		int attrCount = 0;
		try
		{
//			attrCount = Integer.parseInt(attributeCountTextField.getText());
			attrCount = integerNumberFormat.parse(attributeCountTextField.getText()).intValue();
		}
		catch(ParseException pe)
		{
			// TODO: Should never get here, because the field is a JFormattedTextField with a NumberFormat.getIntegerInstance().
		}
		return attrCount;
	}
	
	public float getHeritability()
	{
		float herit = 0;
		try
		{
//			herit = Float.parseFloat(heritabilityTextField.getText());
			herit = floatNumberFormat.parse(heritabilityTextField.getText()).floatValue();
		}
		catch(ParseException pe)
		{
			// TODO: Should never get here, because the field is a JFormattedTextField with a NumberFormat.getIntegerInstance().
		}
		return herit;
	}
	
	public Float getPrevalence()
	{
		Float prevalence = null;
		if(prevalenceCheckBox.isSelected())
		{
			try
			{
				prevalence = floatNumberFormat.parse(prevalenceTextField.getText()).floatValue();
			}
			catch(ParseException pe)
			{
				// TODO: Should never get here, because the field is a JFormattedTextField with a NumberFormat.getIntegerInstance().
			}
		}
		return prevalence;
	}
	
	public boolean getUseOddsRatio()
	{
		boolean useOddsRatio;
		useOddsRatio = oddsButton.isSelected();
		return useOddsRatio;
	}
	
	public int getAttributeCount()
	{
		return tableModel.getRowCount();
	}
	
	public String[] getAttributeNames()
	{
		int attributeCount = tableModel.getRowCount();
		String[] outNames = new String[attributeCount];
		for(int i = 0; i < attributeCount; ++i)
		{
			outNames[i] = tableModel.getValueAt(i, 0).toString();
		}
		return outNames;
	}
	
	public double[] getAttributeMinorAlleleFrequencies()
	{
		int attributeCount = tableModel.getRowCount();
		double[] outMafs = new double[attributeCount];
		for(int i = 0; i < attributeCount; ++i)
		{
			try
			{
				outMafs[i] = Float.parseFloat(tableModel.getValueAt(i, 1).toString());
			}
			catch(NumberFormatException nfe)
			{
				outMafs[i] = 0F;
			}
		}
		return outMafs;
	}
	
	public Container createContentPane()
	{
		JPanel contentPane;						// The content pane of the window
		JPanel parameterPane;					// The top pane
		JScrollPane tableScrollPane;			// The bottom pane
		
		contentPane = new JPanel();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
		contentPane.setOpaque(true);
		
		parameterPane = new JPanel();
		parameterPane.setLayout(new BoxLayout(parameterPane, BoxLayout.Y_AXIS));
		parameterPane.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		JPanel attributePane = new JPanel();
		attributePane.setLayout(new BoxLayout(attributePane, BoxLayout.X_AXIS));
		
		LabelledTextField attributeCountPane = new LabelledTextField("Number of attributes", 4, integerNumberFormat);
		attributeCountTextField = attributeCountPane.textField;
		attributeCountTextField.addFocusListener(this);
		attributePane.add(attributeCountPane);
		
		LabelledTextField heritabilityPane = new LabelledTextField("Heritability", 4, floatNumberFormat);
		heritabilityTextField = heritabilityPane.textField;
//		heritabilityPane.textField.setValue("0.1");
		attributePane.add(heritabilityPane);
		
		JPanel prevalencePane = new JPanel();
		prevalenceCheckBox = new JCheckBox();
		prevalenceCheckBox.addChangeListener(this);
		LabelledTextField prevalenceLabelledField = new LabelledTextField("Prevalence", 4, floatNumberFormat);
		prevalenceTextField = prevalenceLabelledField.textField;
		prevalenceTextField.setEnabled(prevalenceCheckBox.isSelected());
//		prevalencePane.textField.setValue("0.1");
		prevalencePane.add(prevalenceCheckBox);
		prevalencePane.add(prevalenceLabelledField);
		attributePane.add(prevalencePane);
		
		parameterPane.add(attributePane);
		
		JPanel variantPane = new JPanel();
		variantPane.setLayout(new BoxLayout(variantPane, BoxLayout.X_AXIS));
		variantPane.setAlignmentY(Component.CENTER_ALIGNMENT);
		
		// Create the radio buttons.
		edmButton = new JRadioButton("EDM");
		edmButton.setSelected(true);
		oddsButton = new JRadioButton("Odds ratio");
		
		// Group the radio buttons.
		ButtonGroup group = new ButtonGroup();
		group.add(edmButton);
		group.add(oddsButton);
		
		JPanel radioControlPanel;
		radioControlPanel = new JPanel();
		radioControlPanel.add(new JLabel("Quantiles:"));
		radioControlPanel.add(edmButton);
		radioControlPanel.add(oddsButton);

		variantPane.add(radioControlPanel);
		
		LabelledTextField countPane = new LabelledTextField("Quantile count", 8, integerNumberFormat);
		quantileCountTextField = countPane.textField;
		quantileCountTextField.setText(SnpGenDocument.kDefaultRasQuantileCountString);
//		countPane.textField.setValue(3);
		variantPane.add(countPane);
		
		LabelledTextField populationPane = new LabelledTextField("Quantile population size", 8, integerNumberFormat);
		quantilePopulationTextField = populationPane.textField;
		quantilePopulationTextField.setText(SnpGenDocument.kDefaultPopulationCountString);
//		populationPane.textField.setValue("100");
		variantPane.add(populationPane);
		
		parameterPane.add(variantPane);
		
		
		
		
		
		tableModel = new DefaultTableModel();
//		JTable table = new JTable(tableModel);
		
		tableModel.addColumn("SNP");
		tableModel.addColumn("Minor allele frequency");
		ModelCreationTable table = new ModelCreationTable(tableModel);
		
		int attributeCount = 2;
		// Create two rows in the table:
		pushDefaultTableRow();
		pushDefaultTableRow();
		
		tableScrollPane = new JScrollPane(table);
		table.setFillsViewportHeight(true);
		
		attributeCountPane.textField.setText(((Integer)attributeCount).toString());
		
		contentPane.add(parameterPane, BorderLayout.CENTER);
		contentPane.add(tableScrollPane, BorderLayout.CENTER);

		// Create the command section at the bottom of the frame:
		JPanel commandPanel;
		commandPanel = new JPanel();
		commandPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		JButton cancelButton = new JButton(new CancelAction());
		commandPanel.add(cancelButton, BorderLayout.CENTER);
		JButton confirmButton = new JButton(new ConfirmAction());
		commandPanel.add(confirmButton, BorderLayout.CENTER);
		contentPane.add(commandPanel);

		return contentPane;
	}
	
	public void stateChanged(ChangeEvent e)
	{
		if(e.getSource().equals(prevalenceCheckBox))
		{
			prevalenceTextField.setEnabled(prevalenceCheckBox.isSelected());
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
	
	private void resetAttributeCount(int inAttributeCount)
	{
		while(tableModel.getRowCount() > inAttributeCount)
			popTableRow();
		while(tableModel.getRowCount() < inAttributeCount)
			pushDefaultTableRow();
	}
	
	private void pushDefaultTableRow()
	{
		tableModel.addRow(new Object[] {getNextDefaultAttributeName(), getNextDefaultMaf()});
	}
	
	private void popTableRow()
	{
		tableModel.removeRow(tableModel.getRowCount() - 1);
	}
	
	private String getNextDefaultAttributeName()
	{
		int outAttributeNumber = firstAttributeNumber;		// first guess
		for(int i = 0; i < tableModel.getRowCount(); ++i)
		{
			String attrName = (String) tableModel.getValueAt(i, 0);
			attrName = attrName.toLowerCase();
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
		return "P" + (outAttributeNumber);
	}
	
	private float getNextDefaultMaf()
	{
		return 0.2F;
	}
	
    /** This method handles events for the text field. */
    public void actionPerformed(ActionEvent e)
    {
//        optionPane.setValue(btnString1);
    }

    /** This method reacts to state changes in the option pane. */
    public void propertyChange(PropertyChangeEvent e)
    {
//        String prop = e.getPropertyName();
//
//        if (isVisible()
//         && (e.getSource() == optionPane)
//         && (JOptionPane.VALUE_PROPERTY.equals(prop) ||
//             JOptionPane.INPUT_VALUE_PROPERTY.equals(prop))) {
//            Object value = optionPane.getValue();
//
//            if (value == JOptionPane.UNINITIALIZED_VALUE) {
//                //ignore reset
//                return;
//            }
//
//            //Reset the JOptionPane's value.
//            //If you don't do this, then if the user
//            //presses the same button next time, no
//            //property change event will be fired.
//            optionPane.setValue(
//                    JOptionPane.UNINITIALIZED_VALUE);
//
//            if (btnString1.equals(value)) {
//                    typedText = textField.getText();
//                String ucText = typedText.toUpperCase();
//                if (magicWord.equals(ucText)) {
//                    //we're done; clear and dismiss the dialog
//                    clearAndHide();
//                } else {
//                    //text was invalid
//                    textField.selectAll();
//                    JOptionPane.showMessageDialog(
//                                    GenerateModelDialog.this,
//                                    "Sorry, \"" + typedText + "\" "
//                                    + "isn't a valid response.\n"
//                                    + "Please enter "
//                                    + magicWord + ".",
//                                    "Try again",
//                                    JOptionPane.ERROR_MESSAGE);
//                    typedText = null;
//                    textField.requestFocusInWindow();
//                }
//            } else { //user closed dialog or clicked cancel
////                dd.setLabel("It's OK.  We won't force you to type " + magicWord + ".");
//                typedText = null;
//                clearAndHide();
//            }
//        }
    }

    /** This method clears the dialog and hides it. */
    public void clearAndHide() {
//        textField.setText(null);
        setVisible(false);
    }
}