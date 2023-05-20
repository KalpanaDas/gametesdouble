package org.epistasis.snpgen.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import org.epistasis.snpgen.document.*;
import org.epistasis.snpgen.document.SnpGenDocument.*;

public class ModelCreationTable extends JTable
{
	private static final int kColumnWidgetCount = 2;
	
	DefaultTableModel		tableModel;
	ColumnWidget[]			widgets = null;
	
	public ModelCreationTable(DefaultTableModel inTableModel)
	{
		super(inTableModel);
		
		tableModel = inTableModel;
		widgets = new ColumnWidget[kColumnWidgetCount];
		Arrays.fill(widgets, null);
		for(int i = 0; i < kColumnWidgetCount; ++i)
			setWidgetForColumn(tableModel, i);
		
		adjustRows();
		setShowGrid(true);
		setGridColor(Color.black);
		setTableHeader(createDefaultTableHeader());
	}
	
//	public static ModelCreationTable createModelCreationTable()
//	{
//		DefaultTableModel model = new DefaultTableModel();
//		
//		model.addColumn("SNP");
//		model.addColumn("Minor Allele Frequency");
//		
//		ModelCreationTable table = new ModelCreationTable(model);
//		return table;
//	}
	
	public TableModel getTableModel()
	{
		return (TableModel) getModel();
	}
	
	private ColumnWidget setWidgetForColumn(TableModel inTableModel, int inColumn)
	{
		ColumnWidget widget = new ColumnWidget(inTableModel, inColumn);
		getColumnModel().getColumn(inColumn).setCellRenderer(widget);
		getColumnModel().getColumn(inColumn).setCellEditor(widget);
		widgets[inColumn] = widget;
		return widget;
	}
	
	public void tableChanged(TableModelEvent inEvent)
	{
		super.tableChanged(inEvent);
		adjustRows();
	}
	
	private void adjustRows()
	{
		int height;
		
		if(widgets != null)
		{
			for(int i = 0; i < kColumnWidgetCount; ++i)
			{
				if(widgets[i] != null)
					widgets[i].adjustRowCount(getRowCount());
			}
			for(int row = 0; row < getRowCount(); ++row)
			{
				height = getPreferredRowHeight(row);
				setRowHeight(row, height);
			}
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
		if(widgets != null)
		{
			for(int i = 0; i < kColumnWidgetCount; ++i)
			{
				if(widgets[i] != null)
				{
					height = widgets[i].getPreferredRowHeight(inRow);
					if(height > maxHeight)
						maxHeight = height;
				}
			}
		}
		return maxHeight;
	}

	public class ColumnWidget extends AbstractCellEditor implements TableCellRenderer, TableCellEditor
	{
		int					column;
		TableModel			tableModel;
		ArrayList<Panel>	panelArray;
		
		public class Panel extends JPanel implements FocusListener
		{
			private JTextField			textField;
			private int					row;
			private int					column;
			private TableModel			tableModel;
			
			public Panel(int inRow, int inColumn, TableModel inTableModel)
			{
				row = inRow;
				column = inColumn;
				tableModel = inTableModel;
				setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
				setOpaque(true); // Must do this for background to show up.
				setBackground(Color.white);
				add(textField = new JTextField(3));
				textField.addFocusListener(this);
			}
			
			public Panel(int inRow, int inColumn, TableModel inTableModel, Object inValue)
			{
				this(inRow, inColumn, inTableModel);
				textField.setText(inValue.toString());
			}
			
			public int getPreferredHeight()
			{
				return getPreferredSize().height;
			}
			
			public void focusGained(FocusEvent e)
			{
			}
			
			public void focusLost(FocusEvent e)
			{
				tableModel.setValueAt(textField.getText(), row, column);
			}
		}
		
		public ColumnWidget(TableModel inModel, int inColumn)
		{
			column = inColumn;
			tableModel = inModel;
//			ArrayList<SnpGenDocument.DocModel> modelList = tableModel.getModelList();
//			int modelCount = modelList.size();
			int modelCount = inModel.getRowCount();
			panelArray = new ArrayList<Panel>(modelCount);
			for (int row = 0; row < modelCount; ++row)
				panelArray.add(new Panel(row, inColumn, tableModel, tableModel.getValueAt(row, inColumn)));
		}
		
		public Component getTableCellRendererComponent(JTable table,
				Object color, boolean isSelected, boolean hasFocus, int row,
				int column)
		{
			return panelArray.get(row);
		}
		
		public void adjustRowCount(int inRowCount)
		{
			for(int row = panelArray.size() - 1; row >= inRowCount; --row)
				panelArray.remove(row);
			for(int row = panelArray.size(); row < inRowCount; ++row)
				panelArray.add(new Panel(row, column, tableModel, tableModel.getValueAt(row, column)));
		}
		
		public int getPreferredRowHeight(int inRow)
		{
			return panelArray.get(inRow).getPreferredHeight();
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
			return panelArray.get(row);
		}
	}
}
