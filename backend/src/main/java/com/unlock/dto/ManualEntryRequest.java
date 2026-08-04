package com.unlock.dto;

/**
 * What the frontend sends when a student adds something manually -
 * things GitHub can't see, like leading a club event or a competition win.
 */
public class ManualEntryRequest {
    private String title;
    private String description;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
