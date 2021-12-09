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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.collection.ListModification;
import org.jetbrains.annotations.NotNull;

import javafx.application.Platform;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Highlights the specified text according to the highlighting patterns.
 */
class Highlighter {
    private static final Logger logger = LogManager.getLogger(Highlighter.class);
    private static Pattern pattern;

    static {
        try {
            String keywordRegex = "\\b(" + String.join("|", loadKeywords()) + ")\\b";
            String digitRegex = "\\b[0-9]+\\b";
            String stringRegex = "\"([^\"\\\\]|\\\\.)*\"";
            String charRegex = "'.*'";
            String commentRegex = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/"
                    + "|" + "/\\*[^\\v]*" + "|" + "^\\h*\\*([^\\v]*|/)" + "|" + "--[^\n]*";

            pattern = Pattern.compile(
                    "(?<KEYWORD>" + keywordRegex + ")"
                            + "|(?<DIGIT>" + digitRegex + ")"
                            + "|(?<STRING>" + stringRegex + ")"
                            + "|(?<CHAR>" + charRegex + ")"
                            + "|(?<COMMENT>" + commentRegex + ")", Pattern.CASE_INSENSITIVE
            );
        } catch (Exception e) {
            logger.error("static() - " + e.getMessage());
        }
    }

    /**
     * Highlights the specified text according to the highlighting patterns.
     *
     * @param text text to highlight
     *
     * @return a list of {@link org.fxmisc.richtext.model.StyleSpan} objects.
     */
    public @NotNull StyleSpans<Collection<String>> computeHighlighting(@NotNull String text) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        if (pattern == null) {
            spansBuilder.create();
        }

        int lastKwEnd = 0;
        Matcher matcher = pattern.matcher(text);
        while(matcher.find()) {
            String styleClass = matcher.group("KEYWORD") != null ? "keyword" :
                    matcher.group("DIGIT") != null ? "digit" :
                        matcher.group("STRING") != null ? "string" :
                            matcher.group("CHAR") != null ? "char" : "comment";

            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }

        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    private static @NotNull String[] loadKeywords() throws IOException {
        try (InputStream stream = Highlighter.class.getResourceAsStream("/keywords.txt")) {
            if (stream == null) {
                String errorMsg = "File not found in application resources: keywords.txt";
                throw new IllegalStateException(errorMsg);
            }

            String data = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            StringTokenizer tokenizer = new StringTokenizer(data, ", ");
            List<String> words = new LinkedList<>();
            while (tokenizer.hasMoreTokens()) {
                String word = tokenizer.nextToken().replace('\n', ' ').trim();
                words.add(word);
            }

            words.removeIf(String::isEmpty);
            String[] wordsArray = new String[words.size()];
            words.toArray(wordsArray);
            return wordsArray;
        }
    }
}

/**
 * Highlights the current paragraph in the text-editing field.
 *
 * @param <PS> the type of the paragraph style
 * @param <SEG> the type of the content segments in the paragraph (e.g. String)
 * @param <S> the type of the style of individual segments
 */
class VisibleParagraphStyler<PS, SEG, S>
        implements Consumer<ListModification<? extends Paragraph<PS, SEG, S>>> {
    private final GenericStyledArea<PS, SEG, S> area;
    private final Function<String, StyleSpans<S>> computeStyles;
    private int prevParagraph;
    private int prevTextLength;

    /**
     * Creates a new {@link VisibleParagraphStyler} with the specified
     * text-editing area and highlighting function.
     *
     * @param area text-editing area to set
     * @param computeStyles highlighting function to set
     */
    public VisibleParagraphStyler(@NotNull GenericStyledArea<PS, SEG, S> area,
                                  @NotNull Function<String, StyleSpans<S>> computeStyles) {
        this.computeStyles = computeStyles;
        this.area = area;
    }

    /**
     * Applies the highlighting function to the current paragraph in the text-editing field.
     */
    @Override
    public void accept(@NotNull ListModification<? extends Paragraph<PS, SEG, S>> modification) {
        if (modification.getAddedSize() > 0) {
            int paragraph = Math.min(area.firstVisibleParToAllParIndex() + modification.getFrom(),
                    area.getParagraphs().size() - 1);
            String text = area.getText(paragraph, 0, paragraph, area.getParagraphLength(paragraph));
            if (paragraph != prevParagraph || text.length() != prevTextLength) {
                int startPos = area.getAbsolutePosition(paragraph, 0);
                Platform.runLater( () -> area.setStyleSpans(startPos, computeStyles.apply(text)) );

                prevTextLength = text.length();
                prevParagraph = paragraph;
            }
        }
    }
}
