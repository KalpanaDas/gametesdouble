package org.epistasis.snpgen.ui;

import org.epistasis.snpgen.ui.PenetranceTablePane.Square;

public interface ModelUpdater
{
	public void setSquares(Square[] squares);
	public void modelToPenetranceSquares();
	public void penetranceSquaresToModel();
	public void updateModel();
}