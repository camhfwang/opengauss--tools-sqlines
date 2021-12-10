/*
 * Copyright (c) 2021 SQLines
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sqlines.studio.view.mainwindow;

import com.sqlines.studio.view.mainwindow.event.RecentFileEvent;

import de.jangassen.MenuToolkit;
import org.jetbrains.annotations.NotNull;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/**
 * Drop down menus at the top of the application window.
 * <p>
 * Fires {@link RecentFileEvent} when the recent file item
 * in the Open Recent menu is clicked.
 * <p>
 * Contains the following menus:
 * <li>Main
 * <li>File
 * <li>Edit
 * <li>View
 * <li>Tools
 * <li>Help
 *
 * @apiNote Initially, all menu items are active.
 */
class MenuBar extends javafx.scene.control.MenuBar {
    // Main tab menu items
    private final MenuItem aboutMenuItem = new MenuItem();
    private final MenuItem preferencesMenuItem = new MenuItem();

    // File tab menu items
    private final MenuItem newTabMenuItem = new MenuItem();
    private final MenuItem closeTabMenuItem = new MenuItem();
    private final MenuItem nextTabMenuItem = new MenuItem();
    private final MenuItem prevTabMenuItem = new MenuItem();
    private final MenuItem openFileMenuItem = new MenuItem();
    private final MenuItem saveFileMenuItem = new MenuItem();
    private final MenuItem saveFileAsMenuItem = new MenuItem();
    private final MenuItem clearRecentMenuItem = new MenuItem();
    private final Menu openRecentMenu = new Menu();

    // Edit tab menu items
    private final MenuItem undoMenuItem = new MenuItem();
    private final MenuItem redoMenuItem = new MenuItem();
    private final MenuItem selectAllMenuItem = new MenuItem();
    private final MenuItem cutMenuItem = new MenuItem();
    private final MenuItem copyMenuItem = new MenuItem();
    private final MenuItem pasteMenuItem = new MenuItem();

    // View tab menu items
    private final MenuItem zoomInMenuItem = new MenuItem();
    private final MenuItem zoomOutMenuItem = new MenuItem();
    private final CheckMenuItem statusBarMenuItem = new CheckMenuItem();
    private final CheckMenuItem targetFieldMenuItem = new CheckMenuItem();
    private final CheckMenuItem wrappingMenuItem = new CheckMenuItem();
    private final CheckMenuItem highlighterMenuItem = new CheckMenuItem();
    private final CheckMenuItem lineNumbersMenuItem = new CheckMenuItem();

    // Tools tab menu items
    private final MenuItem runMenuItem = new MenuItem();

    // Help tab menu items
    private final MenuItem onlineHelpMenuItem = new MenuItem();
    private final MenuItem openSiteMenuItem = new MenuItem();

    private EventHandler<RecentFileEvent> recentFileEventHandler;

    public MenuBar() {
        if (System.getProperty("os.name").toLowerCase().startsWith("mac")) {
            useSystemMenuBarProperty().set(true);

            MenuToolkit toolkit = MenuToolkit.toolkit();
            Menu defaultMenu = toolkit.createDefaultApplicationMenu("SQLines Studio");
            toolkit.setApplicationMenu(makeMainMenu(defaultMenu));
            toolkit.setMenuBar(this);
        }

        makeFileMenu();
        makeEditMenu();
        makeViewMenu();
        makeToolsMenu();
        makeHelpMenu();
    }

    /**
     * Adds new recent file path to the Open Recent menu.
     *
     * @param filePath file path to add
     *
     * @throws IllegalArgumentException if file path is empty
     */
    public void addRecentFile(@NotNull String filePath) {
        if (filePath.isEmpty()) {
            throw new IllegalArgumentException("File path is empty");
        }

        MenuItem newFile = new MenuItem(filePath);
        newFile.setOnAction(event -> {
            RecentFileEvent recentFileEvent = new RecentFileEvent(filePath);
            fireEvent(recentFileEvent);

            if (recentFileEventHandler != null) {
                recentFileEventHandler.handle(recentFileEvent);
            }
        });

        openRecentMenu.getItems().add(0, newFile);
    }

    /**
     * Deletes all recent files paths in the Open Recent menu.
     */
    public void clearRecentFiles() {
        ObservableList<MenuItem> items = openRecentMenu.getItems();
        items.removeIf(item -> items.size() != 2);
    }

    /**
     * Moves the specified recent file path in the Open Recent menu to the specified position.
     *
     * @param filePath file path to move
     * @param moveTo index to move the file path to
     *
     * @throws IndexOutOfBoundsException if moveTo is out of range
     * (moveTo < 0 || moveTo >= the number of recent file paths)
     * @throws IllegalArgumentException if no such recent file path exists
     */
    public void moveRecentFile(@NotNull String filePath, int moveTo) {
        ObservableList<MenuItem> items = openRecentMenu.getItems();
        if (moveTo < 0 || moveTo >= items.size()) {
            int endInd = (items.size() == 0) ? 0 : items.size() - 1;
            String errorMsg = "Index is out of range: (0:" + endInd + ") expected, "
                    + moveTo + " provided";
            throw new IndexOutOfBoundsException(errorMsg);
        }

        MenuItem item = null;
        for (MenuItem menuItem : items) {
            if (menuItem.getText().equals(filePath)) {
                item = menuItem;
                break;
            }
        }

        if (item == null) {
            throw new IllegalArgumentException("No such recent file exists: " + filePath);
        }

        items.remove(item);
        items.add(moveTo, item);
    }

    /**
     * Defines the selection state of the Show Status Bar menu item.
     *
     * @param isSelected makes the menu item selected if true, unselected otherwise
     */
    public void setStatusBarSelected(boolean isSelected) {
        statusBarMenuItem.setSelected(isSelected);
    }

    /**
     * Defines the selection state of the Always Show Target Field menu item.
     *
     * @param isSelected makes the menu item selected if true, unselected otherwise
     */
    public void setTargetFieldSelected(boolean isSelected) {
        targetFieldMenuItem.setSelected(isSelected);
    }

    /**
     * Defines the selection state of the Wrap Lines To Editor Width menu item
     * in the Editor menu.
     *
     * @param isSelected makes the menu item selected if true, unselected otherwise
     */
    public void setWrappingSelected(boolean isSelected) {
        wrappingMenuItem.setSelected(isSelected);
    }

    /**
     * Defines the selection state of the Highlighter menu item in the Editor menu.
     *
     * @param isSelected makes the menu item selected if true, unselected otherwise
     */
    public void setHighlighterSelected(boolean isSelected) {
        highlighterMenuItem.setSelected(isSelected);
    }

    /**
     * Defines the selection state of the Line Numbers menu item in the Editor menu.
     *
     * @param isSelected makes the menu item selected if true, unselected otherwise
     */
    public void setLineNumbersSelected(boolean isSelected) {
        lineNumbersMenuItem.setSelected(isSelected);
    }

    /**
     * Defines the state of the Close Tab menu item.
     *
     * @param isEnabled makes the menu item enabled if true, disabled otherwise
     */
    public void setCloseTabState(boolean isEnabled) {
        closeTabMenuItem.setDisable(!isEnabled);
    }

    /**
     * Defines the state of the Next Tab menu item.
     *
     * @param isEnabled makes the menu item enabled if true, disabled otherwise
     */
    public void setNextTabState(boolean isEnabled) {
        nextTabMenuItem.setDisable(!isEnabled);
    }

    /**
     * Defines the state of the Previous Tab menu item.
     *
     * @param isEnabled makes the menu item enabled if true, disabled otherwise
     */
    public void setPrevTabState(boolean isEnabled) {
        prevTabMenuItem.setDisable(!isEnabled);
    }

    /**
     * Defines the state of the Open Recent menu.
     *
     * @param isEnabled makes the menu enabled if true, disabled otherwise
     */
    public void setOpenRecentState(boolean isEnabled) {
        openRecentMenu.setDisable(!isEnabled);
    }

    /**
     * Defines the state of the Undo menu item.
     *
     * @param isEnabled makes the menu item enabled if true, disabled otherwise
     */
    public void setUndoState(boolean isEnabled) {
        undoMenuItem.setDisable(!isEnabled);
    }

    /**
     * Defines the state of the Redo menu item.
     *
     * @param isEnabled makes the menu item enabled if true, disabled otherwise
     */
    public void setRedoState(boolean isEnabled) {
        redoMenuItem.setDisable(!isEnabled);
    }

    /**
     * Sets the action which is invoked when the About menu item is clicked.
     *
     * @param action the action to register
     */
    public void setOnAboutAction(@NotNull EventHandler<ActionEvent> action) {
        aboutMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the Preferences menu item is clicked.
     *
     * @param action the action to register
     */
    public void setOnPreferencesAction(@NotNull EventHandler<ActionEvent> action) {
        preferencesMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the New Tab menu item is clicked.
     *
     * @param action the action to register
     */
    public void setOnNewTabAction(@NotNull EventHandler<ActionEvent> action) {
        newTabMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the Close Tab menu item is clicked.
     *
     * @param action the action to register
     */
    public void setOnCloseTabAction(@NotNull EventHandler<ActionEvent> action) {
        closeTabMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the Next Tab menu item is clicked.
     *
     * @param action the action to register
     */
    public void setOnNextTabAction(@NotNull EventHandler<ActionEvent> action) {
        nextTabMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the Previous Tab menu item is clicked.
     *
     * @param action the action to register
     */
    public void setOnPrevTabAction(@NotNull EventHandler<ActionEvent> action) {
        prevTabMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the Open File menu item is clicked.
     *
     * @param action the action to register
     */
    public void setOnOpenFileAction(@NotNull EventHandler<ActionEvent> action) {
        openFileMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when
     * the Recent File menu item in the Open Recent menu is clicked.
     *
     * @param action the action to register
     */
    public void setOnOpenRecentAction(@NotNull EventHandler<RecentFileEvent> action) {
        recentFileEventHandler = action;
    }

    /**
     * Sets the action which is invoked when the Clear menu item
     * in the Open Recent menu is clicked.
     *
     * @param action the action to register
     */
    public void setOnClearRecentAction(@NotNull EventHandler<ActionEvent> action) {
        clearRecentMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the Save File menu item is clicked.
     *
     * @param action the action to register
     */
    public void setOnSaveFileAction(@NotNull EventHandler<ActionEvent> action) {
        saveFileMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the Save File as menu item is clicked.
     *
     * @param action the action to register
     */
    public void setOnSaveAsAction(@NotNull EventHandler<ActionEvent> action) {
        saveFileAsMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the Undo menu item is clicked.
     *
     * @param action the action to register
     */
    public void setOnUndoAction(@NotNull EventHandler<ActionEvent> action) {
        undoMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the Redo menu item is clicked.
     *
     * @param action the action to register
     */
    public void setOnRedoAction(@NotNull EventHandler<ActionEvent> action) {
        redoMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the Select All menu item is clicked.
     *
     * @param action the action to register
     */
    public void setOnSelectAllAction(@NotNull EventHandler<ActionEvent> action) {
        selectAllMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the Cut menu item is clicked.
     *
     * @param action the action to register
     */
    public void setOnCutAction(@NotNull EventHandler<ActionEvent> action) {
        cutMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the Copy menu item is clicked.
     *
     * @param action the action to register
     */
    public void setOnCopyAction(@NotNull EventHandler<ActionEvent> action) {
        copyMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the Paste menu item is clicked.
     *
     * @param action the action to register
     */
    public void setOnPasteAction(@NotNull EventHandler<ActionEvent> action) {
        pasteMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the Zoom In menu item is clicked.
     *
     * @param action the action to register
     */
    public void setOnZoomInAction(@NotNull EventHandler<ActionEvent> action) {
        zoomInMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the Zoom Out menu item is clicked.
     *
     * @param action the action to register
     */
    public void setOnZoomOutAction(@NotNull EventHandler<ActionEvent> action) {
        zoomOutMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the Show Status Bar
     * check menu item in the Editor menu is clicked.
     *
     * @param action the action to register
     */
    public void setOnStatusBarAction(@NotNull EventHandler<ActionEvent> action) {
        statusBarMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the Always Show Target Field
     * check menu item in the Editor menu check menu item is clicked.
     *
     * @param action the action to register
     */
    public void setOnTargetFieldAction(@NotNull EventHandler<ActionEvent> action) {
        targetFieldMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the Wrap Lines To Editor Width
     * check menu item in the Editor menu is clicked.
     *
     * @param action the action to register
     */
    public void setOnWrappingAction(@NotNull EventHandler<ActionEvent> action) {
        wrappingMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when
     * the Highlighter check menu item in the Editor menu is clicked.
     *
     * @param action the action to register
     */
    public void setOnHighlighterAction(@NotNull EventHandler<ActionEvent> action) {
        highlighterMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when
     * the Line Numbers check menu item in the Editor menu is clicked.
     *
     * @param action the action to register
     */
    public void setOnLineNumbersAction(@NotNull EventHandler<ActionEvent> action) {
        lineNumbersMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the Run Conversion menu item is clicked.
     *
     * @param action the action to register
     */
    public void setOnRunAction(@NotNull EventHandler<ActionEvent> action) {
        runMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the Open Online Help menu item is clicked.
     *
     * @param action the action to register
     */
    public void setOnOnlineHelpAction(@NotNull EventHandler<ActionEvent> action) {
        onlineHelpMenuItem.setOnAction(action);
    }

    /**
     * Sets the action which is invoked when the Open Official Site menu item is clicked.
     *
     * @param action the action to register
     */
    public void setOnOpenSiteAction(@NotNull EventHandler<ActionEvent> action) {
        openSiteMenuItem.setOnAction(action);
    }

    private @NotNull Menu makeMainMenu(@NotNull Menu defaultMenu) {
        aboutMenuItem.setText("About SQLines Studio...");
        preferencesMenuItem.setText("Preferences...");

        defaultMenu.getItems().set(0, aboutMenuItem);
        defaultMenu.getItems().set(1, preferencesMenuItem);
        defaultMenu.getItems().add(2, new SeparatorMenuItem());

        return defaultMenu;
    }

    private void makeFileMenu() {
        newTabMenuItem.setText("New Tab");
        KeyCombination newTab = new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN);
        newTabMenuItem.setAccelerator(newTab);

        closeTabMenuItem.setText("Close Tab");
        KeyCombination closeTab = new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN);
        closeTabMenuItem.setAccelerator(closeTab);

        nextTabMenuItem.setText("Next Tab");
        KeyCombination nextTab = new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.SHORTCUT_DOWN);
        nextTabMenuItem.setAccelerator(nextTab);

        prevTabMenuItem.setText("Previous Tab");
        KeyCombination prevTab = new KeyCodeCombination(KeyCode.LEFT, KeyCombination.SHORTCUT_DOWN);
        prevTabMenuItem.setAccelerator(prevTab);

        openFileMenuItem.setText("Open File...");
        KeyCombination openFile = new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN);
        openFileMenuItem.setAccelerator(openFile);

        clearRecentMenuItem.setText("Clear");

        openRecentMenu.setText("Open Recent");
        openRecentMenu.getItems().add(new SeparatorMenuItem());
        openRecentMenu.getItems().add(clearRecentMenuItem);

        saveFileMenuItem.setText("Save File");
        KeyCombination saveFile = new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN);
        saveFileMenuItem.setAccelerator(saveFile);

        saveFileAsMenuItem.setText("Save File As...");
        KeyCombination saveFileAs = new KeyCodeCombination(KeyCode.S,
                KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN);
        saveFileAsMenuItem.setAccelerator(saveFileAs);

        Menu fileMenu = new Menu("File");
        fileMenu.getItems().addAll(newTabMenuItem, closeTabMenuItem,
                nextTabMenuItem, prevTabMenuItem);
        fileMenu.getItems().add(new SeparatorMenuItem());
        fileMenu.getItems().addAll(openFileMenuItem, openRecentMenu,
                saveFileMenuItem, saveFileAsMenuItem);

        getMenus().add(fileMenu);
    }

    private void makeEditMenu() {
        undoMenuItem.setText("Undo");
        redoMenuItem.setText("Redo");
        selectAllMenuItem.setText("Select All");
        cutMenuItem.setText("Cut");
        copyMenuItem.setText("Copy");
        pasteMenuItem.setText("Paste");

        Menu editMenu = new Menu("Edit");
        editMenu.getItems().addAll(undoMenuItem, redoMenuItem);
        editMenu.getItems().add(new SeparatorMenuItem());
        editMenu.getItems().addAll(selectAllMenuItem, cutMenuItem, copyMenuItem, pasteMenuItem);

        getMenus().add(editMenu);
    }

    private void makeViewMenu() {
        zoomInMenuItem.setText("Zoom In");
        KeyCombination zoomIn = new KeyCodeCombination(KeyCode.A,
                KeyCombination.SHIFT_DOWN, KeyCombination.SHORTCUT_DOWN);
        zoomInMenuItem.setAccelerator(zoomIn);

        zoomOutMenuItem.setText("Zoom Out");
        KeyCombination zoomOut = new KeyCodeCombination(KeyCode.E,
                KeyCombination.SHIFT_DOWN, KeyCombination.SHORTCUT_DOWN);
        zoomOutMenuItem.setAccelerator(zoomOut);

        statusBarMenuItem.setText("Show Status Bar");
        targetFieldMenuItem.setText("Always Show Target Field");
        wrappingMenuItem.setText("Wrap Lines To Editor Width");
        highlighterMenuItem.setText("Highlighter");
        lineNumbersMenuItem.setText("Line Numbers");

        Menu editorMenu = new Menu("Editor");
        editorMenu.getItems().addAll(wrappingMenuItem, highlighterMenuItem, lineNumbersMenuItem);

        Menu viewMenu = new Menu("View");
        viewMenu.getItems().addAll(zoomInMenuItem, zoomOutMenuItem);
        viewMenu.getItems().add(new SeparatorMenuItem());
        viewMenu.getItems().addAll(statusBarMenuItem, targetFieldMenuItem);
        viewMenu.getItems().add(new SeparatorMenuItem());
        viewMenu.getItems().add(editorMenu);

        getMenus().add(viewMenu);
    }

    private void makeToolsMenu() {
        runMenuItem.setText("Run Conversion");
        KeyCombination run = new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN);
        runMenuItem.setAccelerator(run);

        Menu toolsMenu = new Menu("Tools");
        if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
            preferencesMenuItem.setText("Settings");
            toolsMenu.getItems().add(preferencesMenuItem);
            toolsMenu.getItems().add(new SeparatorMenuItem());
        }

        toolsMenu.getItems().add(runMenuItem);
        getMenus().add(toolsMenu);
    }

    private void makeHelpMenu() {
        onlineHelpMenuItem.setText("Online Help...");
        openSiteMenuItem.setText("Official Site...");

        Menu helpMenu = new Menu("Help");
        helpMenu.getItems().addAll(onlineHelpMenuItem, openSiteMenuItem);

        getMenus().add(helpMenu);
    }
}
