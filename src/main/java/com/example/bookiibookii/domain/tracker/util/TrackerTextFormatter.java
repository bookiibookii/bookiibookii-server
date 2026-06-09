package com.example.bookiibookii.domain.tracker.util;

public final class TrackerTextFormatter {

    public static final int STEP_BOOK_TITLE_DISPLAY_MAX_LENGTH = 16;

    private TrackerTextFormatter() {
    }

    public static String abbreviateStepBookTitle(String bookTitle) {
        if (bookTitle == null) {
            return "";
        }
        if (bookTitle.codePointCount(0, bookTitle.length()) <= STEP_BOOK_TITLE_DISPLAY_MAX_LENGTH) {
            return bookTitle;
        }
        int endIndex = bookTitle.offsetByCodePoints(0, STEP_BOOK_TITLE_DISPLAY_MAX_LENGTH);
        return bookTitle.substring(0, endIndex) + "...";
    }
}
