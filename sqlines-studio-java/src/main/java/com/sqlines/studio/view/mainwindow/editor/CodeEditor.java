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

package com.sqlines.studio.view.mainwindow.editor;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.NavigationActions;
import org.jetbrains.annotations.NotNull;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Collection;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A text input field with the line number area, highlighter, fixed-width font,
 * scroll bars, context menu and undo manager.
 * <p>
 * Allows listeners to track changes when they occur.
 *
 * @implNote {@link CodeEditor} uses FXMisc library.
 * See details: <a href=https://github.com/FXMisc/RichTextFX">GitHub-RichTextFX</a>.
 */
public class CodeEditor extends VBox {
    private static final Highlighter highlighter = new Highlighter();

    /**
     * An enumeration denoting the policy to be used by a {@link CodeEditor}
     * in deciding whether to wrap lines.
     */
    public enum WrappingPolicy { WRAP_LINES, NO_WRAP }

    /**
     * An enumeration denoting the policy to be used by a {@link CodeEditor}
     * in deciding whether to highlight text.
     */
    public enum HighlighterPolicy { HIGHLIGHT, DO_NOT_HIGHLIGHT }

    /**
     * An enumeration denoting the policy to be used by a {@link CodeEditor}
     * in deciding whether to show line number area.
     */
    public enum LineNumbersPolicy { SHOW, DO_NOT_SHOW }

    private final CodeArea codeArea = new CodeArea();
    private final VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
    private final VisibleParagraphStyler<Collection<String>, String, Collection<String>> styler =
            new VisibleParagraphStyler<>(codeArea, highlighter::computeHighlighting);

    private LineNumbersPolicy lineNumbersPolicy = LineNumbersPolicy.SHOW;
    private HighlighterPolicy highlighterPolicy = HighlighterPolicy.HIGHLIGHT;
    private int fontSize = 13;

    public CodeEditor() {
        setUpHighlighter();
        setUpContextMenu();
        setUpLineNumberArea();

        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // Standard preferred size not working
        scrollPane.setPrefWidth(4000);
        scrollPane.setPrefHeight(4000);

        getChildren().add(scrollPane);
    }

    /**
     * @return text content of this text-editing area
     */
    public @NotNull String getText() {
        return codeArea.getText();
    }

    /**
     * Replaces the entire content with the given text.
     * Moves the caret to the beginning of the text.
     *
     * @param text text to set
     */
    public void setText(@NotNull String text) {
        codeArea.replaceText(text);
        codeArea.moveTo(0, 0, NavigationActions.SelectionPolicy.CLEAR);
    }

    /**
     * @return the index of the current line
     */
    public int getLineIndex() {
        return codeArea.getCaretSelectionBind().paragraphIndexProperty().getValue();
    }

    /**
     * @return the index of the current column
     */
    public int getColumnIndex() {
        return codeArea.caretColumnProperty().getValue();
    }

    /**
     * Sets the {@link WrappingPolicy}.
     * <p>
     * The default value is {@link WrappingPolicy#NO_WRAP}.
     *
     * @param policy wrapping policy to set
     */
    public void setWrappingPolicy(@NotNull WrappingPolicy policy) {
        if (policy == WrappingPolicy.NO_WRAP) {
            codeArea.setWrapText(false);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        } else if (policy == WrappingPolicy.WRAP_LINES) {
            codeArea.setWrapText(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        }
    }

    /**
     * Sets the {@link HighlighterPolicy}.
     * <p>
     * The default value is {@link HighlighterPolicy#HIGHLIGHT}.
     *
     * @param policy highlighter policy to set
     */
    public void setHighlighterPolicy(@NotNull HighlighterPolicy policy) {
        if (this.highlighterPolicy == policy) {
            return;
        }

        this.highlighterPolicy = policy;
        if (policy == HighlighterPolicy.HIGHLIGHT) {
            setUpHighlighter();
        } else if (policy == HighlighterPolicy.DO_NOT_HIGHLIGHT) {
            removeHighlighter();
        }
    }

    /**
     * Sets the {@link LineNumbersPolicy}.
     * <p>
     * The default value is {@link LineNumbersPolicy#SHOW}.
     *
     * @param policy line numbers policy to set
     */
    public void setLineNumbersPolicy(@NotNull LineNumbersPolicy policy) {
        if (this.lineNumbersPolicy == policy) {
            return;
        }

        this.lineNumbersPolicy = policy;
        if (policy == LineNumbersPolicy.SHOW) {
            setUpLineNumberArea();
        } else if (policy == LineNumbersPolicy.DO_NOT_SHOW) {
            codeArea.setParagraphGraphicFactory(null);
        }
    }

    /**
     * Erases the last change done.
     */
    public void undo() {
        codeArea.undo();
    }

    /**
     * Restores any actions that were previously undone using an undo.
     */
    public void redo() {
        codeArea.redo();
    }

    /**
     * Selects everything in the text-editing area.
     */
    public void selectAll() {
        codeArea.selectAll();
    }

    /**
     * Transfers the currently selected text to the clipboard, removing the current selection.
     */
    public void cut() {
        codeArea.cut();
    }

    /**
     * Transfers the currently selected text to the clipboard, leaving the current selection.
     */
    public void copy() {
        codeArea.copy();
    }

    /**
     * Inserts the content from the clipboard into this text-editing area,
     * replacing the current selection.
     * If there is no selection, the content from the clipboard is inserted
     * at the current caret position.
     */
    public void paste() {
        codeArea.paste();
    }

    /**
     * @return true if undo is available for use, false otherwise
     */
    public boolean isUndoAvailable() {
        return codeArea.isUndoAvailable();
    }

    /**
     * @return true if redo is available for use, false otherwise
     */
    public boolean isRedoAvailable() {
        return codeArea.isRedoAvailable();
    }

    /**
     * Increases the font size of the text-editing area by 1 px.
     */
    public void zoomIn() {
        fontSize++;
        codeArea.setStyle("-fx-font-size: " + fontSize +"px");
    }

    /**
     * Decreases the font size of the text-editing area by 1 px.
     */
    public void zoomOut() {
        fontSize--;
        codeArea.setStyle("-fx-font-size: " + fontSize +"px");
    }

    /**
     * Adds a listener which will be notified when the text changes.
     * If the same listener is added more than once, then it will be notified more than once.
     *
     * @param listener the listener to register
     */
    public void addTextListener(@NotNull ChangeListener<String> listener) {
        codeArea.textProperty().addListener(listener);
    }

    /**
     * Adds a listener which will be notified when the current line index changes.
     * If the same listener is added more than once, then it will be notified more than once.
     *
     * @param listener the listener to register
     */
    public void addLineListener(@NotNull ChangeListener<Integer> listener) {
        codeArea.getCaretSelectionBind().paragraphIndexProperty().addListener(listener);
    }

    /**
     * Adds a listener which will be notified when the current column index changes.
     * If the same listener is added more than once, then it will be notified more than once.
     *
     * @param listener the listener to register
     */
    public void addColumnListener(@NotNull ChangeListener<Integer> listener) {
        codeArea.caretColumnProperty().addListener(listener);
    }

    /**
     * Adds a listener which will be notified when the focus changes.
     * If the same listener is added more than once, then it will be notified more than once.
     *
     * @param listener the listener to register
     */
    public void addFocusListener(@NotNull ChangeListener<Boolean> listener) {
        codeArea.focusedProperty().addListener(listener);
    }

    @Override
    public void requestFocus() {
        Platform.runLater(codeArea::requestFocus);
    }

    /**
     * @return true if the text-editing area is in focus, false otherwise
     */
    public boolean hasFocus() {
        return codeArea.isFocused();
    }

    private void setUpHighlighter() {
        codeArea.getVisibleParagraphs().addModificationObserver(styler);

        // Auto-indent: insert previous line's indents on enter
        Pattern whiteSpace = Pattern.compile( "^\\s+" );
        addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                int caretPos = codeArea.getCaretPosition();
                int currParagraph = codeArea.getCurrentParagraph();
                CharSequence sequence = codeArea.getParagraph(currParagraph - 1).getSegments().get(0);
                Matcher matcher = whiteSpace.matcher(sequence);
                if (matcher.find()) {
                    Platform.runLater(() -> codeArea.insertText(caretPos, matcher.group()));
                }
            }
        });
    }

    private void removeHighlighter() {
        codeArea.getVisibleParagraphs().removeModificationObserver(styler);

        // Reset the current text to remove the currently applied highlighting
        String currText = codeArea.getText();
        codeArea.replaceText(currText);
        codeArea.moveTo(0, 0, NavigationActions.SelectionPolicy.CLEAR);
    }

    private void setUpLineNumberArea() {
        IntFunction<Node> lineNumFactory = LineNumberFactory.get(codeArea);
        codeArea.setParagraphGraphicFactory(currLine -> {
            HBox layout = new HBox(lineNumFactory.apply(currLine));
            layout.setPadding(new Insets(0, 35, 0, 0));
            return layout;
        });
    }

    private void setUpContextMenu() {
        ContextMenu menu = new ContextMenu();
        menu.setOnUndoAction(event -> codeArea.undo());
        menu.setOnRedoAction(event -> codeArea.redo());
        menu.setOnSelectAllAction(event -> codeArea.selectAll());
        menu.setOnCutAction(event -> codeArea.cut());
        menu.setOnCopyAction(event -> codeArea.copy());
        menu.setOnPasteAction(event -> codeArea.paste());

        codeArea.setContextMenu(menu);
        codeArea.setOnContextMenuRequested(event -> {
            menu.setUndoState(codeArea.isUndoAvailable());
            menu.setRedoState(codeArea.isRedoAvailable());
        });
    }
}
