package org.epistasis.snpgen.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import org.epistasis.snpgen.document.*;
import org.epistasis.snpgen.document.SnpGenDocument.*;

public class ModelTable extends JTable implements SnpGenDocument.DocListener
{
	ModelTableModel		tableModel;
	ColumnWidget[]		widgets;
	
	public ModelTable(ModelTableModel inTableModel)
	{
		super(inTableModel);
		
		tableModel = inTableModel;
		
		getColumnModel().getColumn(0).setCellRenderer(new ColumnTextRenderer(inTableModel, 0));
		getColumnModel().getColumn(1).setCellRenderer(new ColumnTextRenderer(inTableModel, 1));
		getColumnModel().getColumn(2).setCellRenderer(new ColumnTextRenderer(inTableModel, 2));
		getColumnModel().getColumn(5).setCellRenderer(new ColumnTextRenderer(inTableModel, 5));
		
		widgets = new ColumnWidget[6];
		for(int i = 0; i < widgets.length; ++i)
			widgets[i] = null;
		setWidgetForColumn(tableModel, 3);
		setWidgetForColumn(tableModel, 4);
		tableModel.addTableModelListener(tableModel);
		
		adjustRowHeights();
		setShowGrid(true);
		setGridColor(Color.black);
		setTableHeader(createDefaultTableHeader());
		
		tableModel.document.addDocumentListener(this);
//		setEnabled(false);
	}
	
	public static ModelTable createModelTable(SnpGenDocument inDoc)
	{
		ModelTableModel model = new ModelTableModel(inDoc);
		ModelTable table = new ModelTable(model);
		return table;
	}
	
	public ModelTableModel getTableModel()
	{
		return (ModelTableModel) getModel();
	}
	
	public boolean isCellEditable(int row, int column)
	{
		if(column == 5)
			return true;
		else
			return false;
	}
	
	public void datasetAdded(SnpGenDocument inDoc, SnpGenDocument.DocDataset inDataset, int whichModel)
	{
	}
	
	public void modelAdded(SnpGenDocument inDoc, SnpGenDocument.DocModel inModel, int whichModel)
	{
		setWidgetForColumn(tableModel, 3);		// Quick and dirty -- could adjust each widget instead,
		setWidgetForColumn(tableModel, 4);		// by adding a panel to the panelArray.
		tableModel.fireTableDataChanged();
		adjustRowHeights();
	}
	
	public void modelUpdated(SnpGenDocument inDoc, SnpGenDocument.DocModel inModel, int whichModel)
	{
		setWidgetForColumn(tableModel, 3);		// Quick and dirty -- could adjust each widget instead,
		setWidgetForColumn(tableModel, 4);		// by adding a panel to the panelArray.
		tableModel.fireTableDataChanged();
		adjustRowHeights();
	}
	
	public void modelRemoved(SnpGenDocument inDoc, SnpGenDocument.DocModel inModel, int whichModel)
	{
		setWidgetForColumn(tableModel, 3);		// Quick and dirty -- could adjust each widget instead,
		setWidgetForColumn(tableModel, 4);		// by adding a panel to the panelArray.
//		assert false;
		tableModel.fireTableDataChanged();
		adjustRowHeights();
	}
	
	public void attributeCountChanged(SnpGenDocument inDoc, SnpGenDocument.DocModel inModel, int whichModel, int inNewAttributeCount)
	{
		widgets[3].setNewPanel(whichModel);
		widgets[4].setNewPanel(whichModel);
		ModelTable.this.adjustRowHeight(whichModel);
	}
	
	private ColumnWidget setWidgetForColumn(ModelTableModel inTableModel, int inColumn)
	{
		ColumnWidget widget = new ColumnWidget(inTableModel, inColumn);
		getColumnModel().getColumn(inColumn).setCellRenderer(widget);
		getColumnModel().getColumn(inColumn).setCellEditor(widget);
		widgets[inColumn] = widget;
		return widget;
	}
	
	private void adjustRowHeights()
	{
		int height;
		
		for(int row = 0; row < getRowCount(); ++row)
		{
			height = getPreferredRowHeight(row);
			setRowHeight(row, height);
		}
	}
	
	private void adjustRowHeight(int inRow)
	{
		int height;
		
		height = getPreferredRowHeight(inRow);
		setRowHeight(inRow, height);
	}
	
	private int getPreferredRowHeight(int inRow)
	{
		int height, maxHeight;
		
		maxHeight = getRowHeight();
		for(int i = 0; i < 5; ++i)
		{
			if(widgets[i] != null)
			{
				height = widgets[i].getPreferredRowHeight(inRow);
				if(height > maxHeight)
					maxHeight = height;
			}
		}
		return maxHeight;
	}
	
	public class ColumnTextRenderer extends DefaultTableCellRenderer implements TableCellRenderer
	{
		int						column;
		ModelTableModel			tableModel;
		protected JTextField	textField;
		
		public ColumnTextRenderer(ModelTableModel inModel, int inColumn)
		{
			textField = new JTextField();
			column = inColumn;
			tableModel = inModel;
		}
		
		public Component getTableCellRendererComponent(JTable table, Object color, boolean isSelected, boolean hasFocus, int row, int column)
		{
			Object obj = tableModel.getVariableAt(row, column);
//			Object obj = tableModel.getVariableAt(0, column);
			if (obj != null && obj.getClass().equals(SnpGenDocument.DocString.class))
			{
				SnpGenDocument.DocString docString = (SnpGenDocument.DocString) obj;
				if(docString.getValue() != null)
					textField.setText(docString.getValue().toString());
			}
			else if (obj.getClass().equals(SnpGenDocument.DocFloat.class))
			{
				SnpGenDocument.DocFloat docFloat = (SnpGenDocument.DocFloat) obj;
				if(docFloat.getValue() != null)
					textField.setText(docFloat.getValue().toString());
			}
			else if (obj.getClass().equals(SnpGenDocument.DocInteger.class))
			{
				SnpGenDocument.DocInteger docInteger = (SnpGenDocument.DocInteger) obj;
				if(docInteger.getValue() != null)
					textField.setText(docInteger.getValue().toString());
			}
			return textField;
		}
	}

	public class ColumnWidget extends AbstractCellEditor implements TableCellRenderer, TableCellEditor
	{
		int					column;
		ModelTableModel		tableModel;
		Panel[]				panelArray;
		
		public abstract class Panel extends JPanel implements FocusListener
		{
			protected JTextField[]				textFieldArray;
			
			public Panel()
			{
				setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
				setOpaque(true); // Must do this for background to show up.
				setBackground(Color.white);
			}
			
			public Panel(int inCount)
			{
				this();
				textFieldArray = new JTextField[inCount];
				for(int j = 0; j < inCount; ++j)
				{	
					add(textFieldArray[j] = new JTextField(3));
					textFieldArray[j].addFocusListener(this);
				}
			}
			
			protected abstract DocMember[] getValueArray();
			
			public int getPreferredHeight()
			{
				return getPreferredSize().height;
			}
			
			public synchronized Panel initFromDocument()
			{
				for(int j = 0; j < getValueArray().length; ++j)
				{
					Object value = getValueArray()[j].getValue();
					if(value == null)
						textFieldArray[j].setText("");
					else
						textFieldArray[j].setText(value.toString());
				}
				return this;
			}
			
			public void focusGained(FocusEvent e)
			{
			}
			
			public void focusLost(FocusEvent e)
			{
				Integer which = null;
				for(int i = 0; i < textFieldArray.length; ++i)
				{
					if(e.getComponent().equals(textFieldArray[i]))
					{
						which = i;
						break;
					}
				}
				if(which != null)
				{
					try
					{
						getValueArray()[which].setValue(textFieldArray[which].getText());
					}
					catch(NumberFormatException nfe)
					{
						textFieldArray[which].setText("");
						getValueArray()[which].setValue(textFieldArray[which].getText());
					}
				}
			}
		}
		
		public class StringPanel extends Panel
		{
			SnpGenDocument.DocString[]	valueArray;
			
			public StringPanel()
			{
				super();
			}
			
			public StringPanel(SnpGenDocument.DocString[] inArray)
			{
				super(inArray.length);
				valueArray = inArray;
				initFromDocument();
			}
			
			protected DocMember[] getValueArray()
			{
				return valueArray;
			}
		}
		
		public class FloatPanel extends Panel
		{
			SnpGenDocument.DocFloat[]	valueArray;
			
			public FloatPanel()
			{
				super();
			}
			
			public FloatPanel(SnpGenDocument.DocFloat[] inArray)
			{
				super(inArray.length);
				valueArray = inArray;
				initFromDocument();
			}
			
			protected DocMember[] getValueArray()
			{
				return valueArray;
			}
		}
		
		public ColumnWidget(ModelTableModel inModel, int inColumn)
		{
			column = inColumn;
			tableModel = inModel;
			Panel panel;
			ArrayList<SnpGenDocument.DocModel> modelList = tableModel.getModelList();
			int modelCount = modelList.size();
			panelArray = new Panel[modelCount];
			for (int row = 0; row < modelCount; ++row)
				setNewPanel(row);
		}
		
		private void setNewPanel(int inRow)
		{
			Panel panel;
			panel = createPanel(inRow);
			if(panel != null)
				panelArray[inRow] = panel;
		}
		
		private Panel createPanel(int inRow)
		{
			Panel outPanel = null;
			Object obj = tableModel.getVariableAt(inRow, column);
			if (obj.getClass().equals(SnpGenDocument.DocString[].class))
			{
				SnpGenDocument.DocString[] docStringArray = (SnpGenDocument.DocString[]) obj;
				outPanel = new StringPanel(docStringArray);
			}
			else if (obj.getClass().equals(
					SnpGenDocument.DocFloat[].class))
			{
				SnpGenDocument.DocFloat[] docFloatArray = (SnpGenDocument.DocFloat[]) obj;
				outPanel = new FloatPanel(docFloatArray);
			}
			return outPanel;
		}
		
		public Component getTableCellRendererComponent(JTable table,
				Object color, boolean isSelected, boolean hasFocus, int row,
				int column)
		{
			return panelArray[row];
		}
		
		public int getPreferredRowHeight(int inRow)
		{
			return panelArray[inRow].getPreferredHeight();
		}

		// Implement the one CellEditor method that AbstractCellEditor doesn't.
		public Object getCellEditorValue()
		{
//			return currentColor;
			return new Integer(1);
		}

		// Implement the one method defined by TableCellEditor.
		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int column)
		{
			return panelArray[row];
		}
	}

	public static class ModelTableModel extends DefaultTableModel implements TableModelListener
	{
		SnpGenDocument			document;
		
		private String[] columnNames =
		{"Model", "# Attributes", "Heritability", "SNPs", "Minor allele freq", "Heterogeneity fraction"};
		
		public ModelTableModel(SnpGenDocument inDoc)
		{
			document = inDoc;
		}
		
		public ArrayList<SnpGenDocument.DocModel> getModelList()
		{
			if(document == null)
				return null;
			else
				return document.modelList;
		}
		
		public int getColumnCount()
		{
			return columnNames.length;
		}

		public int getRowCount()
		{
			if(getModelList() == null)
				return 0;
			else
				return getModelList().size();
		}

		public String getColumnName(int col)
		{
			return columnNames[col];
		}
		
		public Object getVariableAt(int row, int col)
		{
			Object out = null;
			if(row < getModelList().size())
			{
				if(col == 0)
					out = getModelList().get(row).modelId;
				else if(col == 1)
					out = getModelList().get(row).attributeCount;
				else if(col == 2)
					out = getModelList().get(row).heritability;
				else if(col == 3)
					out = getModelList().get(row).attributeNameArray;
				else if(col == 4)
					out = getModelList().get(row).attributeAlleleFrequencyArray;
				else if(col == 5)
					out = getModelList().get(row).fraction;
			}
			return out;
		}
		
		public Object getValueAt(int row, int col)
		{
			Object var = getVariableAt(row, col);
			if(DocMember.class.isInstance(var))
				return ((DocMember)var).getValue();
			else
				return var;
		}

		public void setValueAt(Object value, int row, int col)
		{
			Object var = getVariableAt(row, col);
			if(DocMember.class.isInstance(var))
				((DocMember)var).setValue(value);
			fireTableCellUpdated(row, col);
		}

		/*
		 * JTable uses this method to determine the default renderer/editor for each cell.
		 */
		public Class getColumnClass(int c)
		{
			if(c == 5)
				return Double.class;
			else
				return Object.class;
//			return getValueAt(0, c).getClass();
		}

		public boolean isCellEditable(int row, int col)
		{
			return true;
		}
		
		public void tableChanged(TableModelEvent ev)
		{
			int row = ev.getFirstRow();
			int column = ev.getColumn();
			TableModel model = (TableModel) ev.getSource();
			if(column == 1)		// If we're in the Attribute-count column
			{
				Object data = model.getValueAt(row, column);
				Integer attributeCount;
				try
				{
					attributeCount = new Integer(data.toString());
				}
				catch(NumberFormatException nfe)
				{
					attributeCount = null;
				}
				if(attributeCount != null)
					resetAttributeCount(row, attributeCount);
			}
		}
		
		private void resetAttributeCount(int inRow, int inAttributeCount)
		{
			DocModel snpModel = getModelList().get(inRow);
			snpModel.resetAttributeCount(inAttributeCount);
		}
	}
}
