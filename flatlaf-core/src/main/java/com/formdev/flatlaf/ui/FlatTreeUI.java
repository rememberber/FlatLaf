/*
 * Copyright 2019 FormDev Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.formdev.flatlaf.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import com.formdev.flatlaf.util.UIScale;

/**
 * Provides the Flat LaF UI delegate for {@link javax.swing.JTree}.
 *
 * <!-- BasicTreeUI -->
 *
 * @uiDefault Tree.font								Font
 * @uiDefault Tree.background						Color
 * @uiDefault Tree.hash								Color
 * @uiDefault Tree.dropLineColor					Color
 * @uiDefault Tree.expandedIcon						Icon
 * @uiDefault Tree.collapsedIcon					Icon
 * @uiDefault Tree.leftChildIndent					int
 * @uiDefault Tree.rightChildIndent					int
 * @uiDefault Tree.rowHeight						int
 * @uiDefault Tree.scrollsOnExpand					boolean
 * @uiDefault Tree.scrollsHorizontallyAndVertically	boolean
 * @uiDefault Tree.paintLines						boolean
 * @uiDefault Tree.lineTypeDashed					boolean
 * @uiDefault Tree.showsRootHandles					boolean
 * @uiDefault Tree.repaintWholeRow					boolean
 *
 * <!-- DefaultTreeCellRenderer -->
 *
 * @uiDefault Tree.leafIcon							Icon
 * @uiDefault Tree.closedIcon						Icon
 * @uiDefault Tree.openIcon							Icon
 * @uiDefault Tree.textBackground					Color
 * @uiDefault Tree.textForeground					Color
 * @uiDefault Tree.selectionBackground				Color
 * @uiDefault Tree.selectionForeground				Color
 * @uiDefault Tree.selectionBorderColor				Color	focus indicator border color
 * @uiDefault Tree.drawsFocusBorderAroundIcon		boolean
 * @uiDefault Tree.drawDashedFocusIndicator			boolean
 * @uiDefault Tree.rendererFillBackground			boolean	default is true
 * @uiDefault Tree.rendererMargins					Insets
 * @uiDefault Tree.dropCellBackground				Color
 * @uiDefault Tree.dropCellForeground				Color
 *
 * <!-- FlatTreeUI -->
 *
 * @uiDefault Tree.border							Border
 * @uiDefault Tree.selectionBackground				Color
 * @uiDefault Tree.selectionForeground				Color
 * @uiDefault Tree.selectionInactiveBackground		Color
 * @uiDefault Tree.selectionInactiveForeground		Color
 * @uiDefault Tree.wideSelection					boolean
 *
 * @author Karl Tauber
 */
public class FlatTreeUI
	extends BasicTreeUI
{
	protected Color selectionBackground;
	protected Color selectionForeground;
	protected Color selectionInactiveBackground;
	protected Color selectionInactiveForeground;
	protected Color selectionBorderColor;
	protected boolean wideSelection;

	public static ComponentUI createUI( JComponent c ) {
		return new FlatTreeUI();
	}

	@Override
	protected void installDefaults() {
		super.installDefaults();

		LookAndFeel.installBorder( tree, "Tree.border" );

		selectionBackground = UIManager.getColor( "Tree.selectionBackground" );
		selectionForeground = UIManager.getColor( "Tree.selectionForeground" );
		selectionInactiveBackground = UIManager.getColor( "Tree.selectionInactiveBackground" );
		selectionInactiveForeground = UIManager.getColor( "Tree.selectionInactiveForeground" );
		selectionBorderColor = UIManager.getColor( "Tree.selectionBorderColor" );
		wideSelection = UIManager.getBoolean( "Tree.wideSelection" );

		// scale
		int rowHeight = FlatUIUtils.getUIInt( "Tree.rowHeight", 16 );
		if( rowHeight > 0 )
			LookAndFeel.installProperty( tree, "rowHeight", UIScale.scale( rowHeight ) );
		setLeftChildIndent( UIScale.scale( getLeftChildIndent() ) );
		setRightChildIndent( UIScale.scale( getRightChildIndent() ) );
	}

	@Override
	protected void uninstallDefaults() {
		super.uninstallDefaults();

		LookAndFeel.uninstallBorder( tree );

		selectionBackground = null;
		selectionForeground = null;
		selectionInactiveBackground = null;
		selectionInactiveForeground = null;
		selectionBorderColor = null;
	}

	@Override
	protected MouseListener createMouseListener() {
		if( !wideSelection )
			return super.createMouseListener();

		return new BasicTreeUI.MouseHandler() {
			@Override
			public void mousePressed( MouseEvent e ) {
				super.mousePressed( handleWideMouseEvent( e ) );
			}

			@Override
			public void mouseReleased( MouseEvent e ) {
				super.mouseReleased( handleWideMouseEvent( e ) );
			}

			@Override
			public void mouseDragged( MouseEvent e ) {
				super.mouseDragged( handleWideMouseEvent( e ) );
			}

			private MouseEvent handleWideMouseEvent( MouseEvent e ) {
				if( !tree.isEnabled() || !SwingUtilities.isLeftMouseButton( e ) || e.isConsumed() )
					return e;

				int x = e.getX();
				int y = e.getY();
				TreePath path = getClosestPathForLocation( tree, x, y );
				if( path == null || isLocationInExpandControl( path, x, y ) )
					return e;

				Rectangle bounds = getPathBounds( tree, path );
				if( bounds == null || y < bounds.y || y >= (bounds.y + bounds.height) )
					return e;

				int newX = Math.max( bounds.x, Math.min( x, bounds.x + bounds.width - 1 ) );
				if( newX == x )
					return e;

				// clone mouse event, but with new X coordinate
				return new MouseEvent( e.getComponent(), e.getID(), e.getWhen(),
					e.getModifiers() | e.getModifiersEx(), newX, e.getY(),
					e.getClickCount(), e.isPopupTrigger(), e.getButton() );
			}
		};
	}

	/**
	 * Same as super.paintRow(), but uses inactive selection background/foreground if tree is not focused.
	 */
	@Override
	protected void paintRow( Graphics g, Rectangle clipBounds, Insets insets, Rectangle bounds, TreePath path, int row,
		boolean isExpanded, boolean hasBeenExpanded, boolean isLeaf )
	{
		if( editingComponent != null && editingRow == row )
			return;

		boolean hasFocus = tree.hasFocus();
		boolean cellHasFocus = hasFocus && (row == getLeadSelectionRow());
		boolean isSelected = tree.isRowSelected( row );

		// wide selection background
		if( wideSelection && isSelected ) {
			// fill background
			g.setColor( hasFocus ? selectionBackground : selectionInactiveBackground );
			g.fillRect( 0, bounds.y, clipBounds.width, bounds.height );

			// paint expand/collapse icon
			if( shouldPaintExpandControl( path, row, isExpanded, hasBeenExpanded, isLeaf ) ) {
				paintExpandControl( g, clipBounds, insets, bounds,
					path, row, isExpanded, hasBeenExpanded, isLeaf );
			}
		}

		// get renderer component
		Component rendererComponent = currentCellRenderer.getTreeCellRendererComponent( tree,
			path.getLastPathComponent(), isSelected, isExpanded, isLeaf, row, cellHasFocus );

		// apply inactive selection background/foreground if tree is not focused
		Color oldBackgroundSelectionColor = null;
		if( isSelected && !hasFocus ) {
			if( rendererComponent instanceof DefaultTreeCellRenderer ) {
				DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) rendererComponent;
				if( renderer.getBackgroundSelectionColor() == selectionBackground ) {
					oldBackgroundSelectionColor = renderer.getBackgroundSelectionColor();
					renderer.setBackgroundSelectionColor( selectionInactiveBackground );
				}
			} else {
				if( rendererComponent.getBackground() == selectionBackground )
					rendererComponent.setBackground( selectionInactiveBackground );
			}

			if( rendererComponent.getForeground() == selectionForeground )
				rendererComponent.setForeground( selectionInactiveForeground );
		}

		// remove selection border if exactly one item is selected
		Color oldBorderSelectionColor = null;
		if( isSelected && hasFocus &&
			tree.getMinSelectionRow() == tree.getMaxSelectionRow() &&
			rendererComponent instanceof DefaultTreeCellRenderer )
		{
			DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) rendererComponent;
			if( renderer.getBorderSelectionColor() == selectionBorderColor ) {
				oldBorderSelectionColor = renderer.getBorderSelectionColor();
				renderer.setBorderSelectionColor( null );
			}
		}

		// paint renderer
		rendererPane.paintComponent( g, rendererComponent, tree, bounds.x, bounds.y, bounds.width, bounds.height, true );

		// restore background selection color and border selection color
		if( oldBackgroundSelectionColor != null )
			((DefaultTreeCellRenderer)rendererComponent).setBackgroundSelectionColor( oldBackgroundSelectionColor );
		if( oldBorderSelectionColor != null )
			((DefaultTreeCellRenderer)rendererComponent).setBorderSelectionColor( oldBorderSelectionColor );
	}
}
