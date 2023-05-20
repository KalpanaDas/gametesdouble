package org.epistasis.snpgen.ui;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.ParseException;
import java.util.Arrays;

import javax.swing.*;

import org.epistasis.snpgen.document.SnpGenDocument.DocModel;
import org.epistasis.snpgen.simulator.*;

public class PenetranceTablePane extends JPanel implements FocusListener
{
	private static final int alleleCount = 3;
	
	private int locusCount;
	Graphics graphics;
	String[] snpTitles;
	String[] snpMajorAlleles;
	String[] snpMinorAlleles;
	ModelUpdater updater;
	
	public class Row extends JPanel
	{
		JTextField[] items;
		
		public Row()
		{
			super();
			
			items = new JTextField[alleleCount];
			
//			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
//			setAlignmentY(Component.CENTER_ALIGNMENT);
			
//			setLayout(new GridBagLayout());
//			GridBagConstraints c = new GridBagConstraints();
//
//			for(int i = 0; i < itemCount; ++i)
//			{
//				items[i] = new JTextField();
////				c.fill = GridBagConstraints.HORIZONTAL;
////				c.weightx = 0.5;
//				c.gridx = i + 1;
//				c.gridy = 0;
//				add(items[i], c);
//			}

			setLayout(null);
			Dimension overallSize = getPreferredSize();
			overallSize.width = 0;
			for(int i = 0; i < alleleCount; ++i)
			{
				items[i] = new JTextField(4);
				add(items[i]);
				Insets insets = getInsets();
				Dimension size = items[i].getPreferredSize();
				items[i].setBounds(insets.left + (size.width + 5) * i, insets.top, size.width, size.height);
				overallSize.width += items[i].getPreferredSize().width + 5;
			}
			overallSize.height= items[0].getPreferredSize().height;
			setPreferredSize(overallSize);
		}
	}
	
	public class Square extends JPanel
	{
		JTextField[][] cells;
		
		public Square(Graphics graphics, int squareCount, int whichSquare, String topLocus, String topMajorAllele, String topMinorAllele, String leftLocus, String leftMajorAllele, String leftMinorAllele, String rightLocus, String rightMajorAllele, String rightMinorAllele)
		{
			super();
			setLayout(null);
			
			int leftMargin = 5;				// margin to the left of everything
			int rightMargin = 5;			// margin to the right of everything
			int topLocusMargin = 0;
			int topAlleleMargin = 5;
			int leftLocusMargin = 5;		// margin to the right of the left locus-label
			int leftAlleleMargin = 5;		// margin to the right of the left allele-label
			int rightAlleleMargin = 5;		// margin to the left of the right allele-label
			int rightLocusMargin = 15;		// margin to the left of the right locus-label
			int cellMarginX = 5;
			int cellMarginY = 5;
			
			JLabel topLocusLabel = new JLabel(topLocus);
			JLabel leftLocusLabel = new JLabel(leftLocus);
			JLabel rightLocusLabel = new JLabel(rightLocus);
			
			FontMetrics fontMetrics;
			Font font;
			
			font = topLocusLabel.getFont();
			fontMetrics = graphics.getFontMetrics(font);
			
			int topLocusWidth   = fontMetrics.stringWidth(topLocus);
			int topLocusHeight  = fontMetrics.getHeight();
			
			int leftLocusWidth  = fontMetrics.stringWidth(leftLocus);
			int leftLocusHeight = fontMetrics.getHeight();
			int rightLocusWidth  = fontMetrics.stringWidth(rightLocus);
			int rightLocusHeight = fontMetrics.getHeight();
			
			String[] topAlleles = new String[alleleCount];
			topAlleles[0] = topMajorAllele + topMajorAllele;
			topAlleles[1] = topMajorAllele + topMinorAllele;
			topAlleles[2] = topMinorAllele + topMinorAllele;
			
			JLabel[] topAlleleLabels = new JLabel[alleleCount];
			for(int i = 0; i < alleleCount; ++i)
				topAlleleLabels[i] = new JLabel(topAlleles[i]);
			int topAlleleWidth  = Math.max(Math.max(fontMetrics.stringWidth(topAlleles[0]), fontMetrics.stringWidth(topAlleles[1])), fontMetrics.stringWidth(topAlleles[2]));
			int topAlleleHeight = fontMetrics.getHeight();
			
			String[] leftAlleles = new String[alleleCount];
			leftAlleles[0] = leftMajorAllele + leftMajorAllele;
			leftAlleles[1] = leftMajorAllele + leftMinorAllele;
			leftAlleles[2] = leftMinorAllele + leftMinorAllele;
			
			JLabel[] leftAlleleLabels = new JLabel[alleleCount];
			for(int i = 0; i < alleleCount; ++i)
				leftAlleleLabels[i] = new JLabel(leftAlleles[i]);
			int leftAlleleWidth  = Math.max(Math.max(fontMetrics.stringWidth(leftAlleles[0]), fontMetrics.stringWidth(leftAlleles[1])), fontMetrics.stringWidth(leftAlleles[2]));
			int leftAlleleHeight = fontMetrics.getHeight();
			
			String[] rightAlleles = new String[alleleCount];
			rightAlleles[0] = rightMajorAllele + rightMajorAllele;
			rightAlleles[1] = rightMajorAllele + rightMinorAllele;
			rightAlleles[2] = rightMinorAllele + rightMinorAllele;
			
			JLabel[] rightAlleleLabels = new JLabel[alleleCount];
			for(int i = 0; i < alleleCount; ++i)
				rightAlleleLabels[i] = new JLabel(rightAlleles[i]);
			int rightAlleleWidth  = Math.max(Math.max(fontMetrics.stringWidth(rightAlleles[0]), fontMetrics.stringWidth(rightAlleles[1])), fontMetrics.stringWidth(rightAlleles[2]));
			int rightAlleleHeight = fontMetrics.getHeight();
			
			JTextField template = new JTextField(4);
			int itemWidth = Math.max(topAlleleWidth, template.getPreferredSize().width);
			int itemHeight = Math.max(leftAlleleHeight, template.getPreferredSize().height);
			
			int[] cellX = new int[alleleCount + 1];
			int[] cellY = new int[alleleCount + 1];
			
//			System.out.println("topTitleWidth=" + topTitleWidth + "\ttopTitleHeight=" + topTitleHeight + "\tleftTitleWidth=" + leftTitleWidth + "\tleftTitleHeight=" + leftTitleHeight);
//			System.out.println("topAlleleWidth=" + topAlleleWidth + "\ttopAlleleHeight=" + topAlleleHeight + "\tleftAlleleWidth=" + leftAlleleWidth + "\tleftAlleleHeight=" + leftAlleleHeight);
			
			for(int i = 0; i < alleleCount + 1; ++i)
			{
				cellX[i] = leftMargin + leftLocusWidth + leftLocusMargin + leftAlleleWidth + leftAlleleMargin + i * (itemWidth + cellMarginX);
//				System.out.print(cellX[i] + "\t");
			}
//			System.out.println();
			for(int j = 0; j < alleleCount + 1; ++j)
			{
				cellY[j] = topLocusHeight + topLocusMargin + topAlleleHeight + topAlleleMargin + j * (itemHeight + cellMarginY);
//				System.out.print(cellY[j] + "\t");
			}
//			System.out.println();
			
			int rightAlleleSeparatorWidth = 3;
			int rightAlleleSeparatorHeight = cellY[alleleCount] - cellY[0];
			
			
			int rightAlleleX = cellX[alleleCount] + rightAlleleMargin;
			int rightLocusX = rightAlleleX + rightAlleleWidth + rightLocusMargin;
			
			int leftHeaderWidth = leftMargin + leftLocusWidth + leftLocusMargin + leftAlleleWidth + leftAlleleMargin;
			int topHeaderHeight = topLocusHeight + topLocusMargin + topAlleleHeight + topAlleleMargin;
			int tableWidth = alleleCount * itemWidth + (alleleCount - 1) * cellMarginX;
			int tableHeight = alleleCount * itemHeight + (alleleCount - 1) * cellMarginY;
			int totalWidth = rightLocusX + rightLocusWidth + rightMargin;
			int totalHeight = topHeaderHeight + tableHeight;
			
			int topTitleLeft = leftHeaderWidth + (tableWidth - topLocusWidth) / 2;
			add(topLocusLabel);
			topLocusLabel.setBounds(topTitleLeft, 0, topLocusWidth, topLocusHeight);
			
			int leftTitleTop = topHeaderHeight + (tableHeight - leftLocusHeight) / 2;
			add(leftLocusLabel);
			leftLocusLabel.setBounds(leftMargin, leftTitleTop, leftLocusWidth, leftLocusHeight);
			
//			System.out.println("topTitleLeft=" + topTitleLeft + "\tleftTitleTop=" + leftTitleTop);
//			System.out.println();
//			System.out.println();
			
			for(int i = 0; i < alleleCount; ++i)
			{
				add(topAlleleLabels[i]);
				topAlleleLabels[i].setBounds(cellX[i] + 6, topLocusHeight + topLocusMargin, itemWidth, itemHeight);
			}
			for(int j = 0; j < alleleCount; ++j)
			{
				add(leftAlleleLabels[j]);
				leftAlleleLabels[j].setBounds(leftMargin + leftLocusWidth + leftLocusMargin, cellY[j], itemWidth, itemHeight);
			}
			
			if(squareCount >= 3)
			{
//				JSeparator rightAlleleSeparator = new JSeparator(SwingConstants.VERTICAL);
//				add(rightAlleleSeparator);
//				rightAlleleSeparator.setVisible(true);
//				rightAlleleSeparator.setBounds(rightAlleleX, cellY[1], rightAlleleSeparatorWidth, rightAlleleSeparatorHeight);
//				rightAlleleSeparator.setPreferredSize(new Dimension(rightAlleleSeparatorWidth, rightAlleleSeparatorHeight));
//				rightAlleleSeparator.setMinimumSize(new Dimension(rightAlleleSeparatorWidth, rightAlleleSeparatorHeight));
				
				add(rightAlleleLabels[whichSquare]);
				rightAlleleLabels[whichSquare].setBounds(rightAlleleX, cellY[1], rightAlleleWidth, rightAlleleHeight);
				if(whichSquare == 1)
				{
					add(rightLocusLabel);
					rightLocusLabel.setBounds(rightLocusX, cellY[1], rightLocusWidth, rightLocusHeight);
				}
			}
			
			cells = new JTextField[alleleCount][alleleCount];
			// "Row i, column j" means that i specifies the y-coordinate and j specifies the x-coordinate.
			for(int i = 0; i < alleleCount; ++i)
			{
				for(int j = 0; j < alleleCount; ++j)
				{
					cells[i][j] = new JTextField(4);
					cells[i][j].addFocusListener(PenetranceTablePane.this);
					add(cells[i][j]);
//					Insets insets = getInsets();
					cells[i][j].setBounds(cellX[j], cellY[i], itemWidth, itemHeight);
				}
			}
			Dimension overallSize = new Dimension(totalWidth, totalHeight);
			setPreferredSize(overallSize);
		}
	}
	
	Square[] squares;
	
	public PenetranceTablePane(ModelUpdater inUpdater, Graphics graphics, String[] inSnpTitles, String[] inSnpMajorAlleles, String[] inSnpMinorAlleles, int inLocusCount)
	{
		super();
		
		updater = inUpdater;
		locusCount = inLocusCount;
		this.graphics = graphics;
        snpTitles = Arrays.copyOf(inSnpTitles, alleleCount);
        snpMajorAlleles = Arrays.copyOf(inSnpMajorAlleles, alleleCount);
        snpMinorAlleles = Arrays.copyOf(inSnpMinorAlleles, alleleCount);
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setAlignmentX(Component.CENTER_ALIGNMENT);
		
		createSquares();
		updater.setSquares(squares);
		updater.modelToPenetranceSquares();
	}
	
	public void setLocusCount(int inLocusCount)
	{
		if(locusCount != inLocusCount)
		{
			locusCount = inLocusCount;
			for(int i = 0; i < alleleCount; ++i)
			{
				if(locusCount < 3 && i >= 1)
					squares[i].setVisible(false);
				else
					squares[i].setVisible(true);
			}
		}
	}
	
	public void createSquares()
	{
		assert locusCount == 2 || locusCount == 3;
//		if(locusCount == 2)
//		{
//			squares = new Square[1];
//			squares[0] = new Square(graphics, topTitle, topMajorAllele, topMinorAllele, leftTitle, leftMajorAllele, leftMinorAllele);
//			add(squares[0]);
//		}
//		else if(locusCount == 3)
//		{
			squares = new Square[alleleCount];
			for(int i = 0; i < alleleCount; ++i)
			{
				squares[i] = new Square(graphics, locusCount, i, snpTitles[0], snpMajorAlleles[0], snpMinorAlleles[0], snpTitles[1], snpMajorAlleles[1], snpMinorAlleles[1], snpTitles[2], snpMajorAlleles[2], snpMinorAlleles[2]);
				if(locusCount < 3 && i >= 1)
					squares[i].setVisible(false);
				add(squares[i]);
			}
//		}
	}
	
	public void focusGained(FocusEvent e)
	{
	}
	
	public void focusLost(FocusEvent e)
	{
		updater.updateModel();
	}
}
