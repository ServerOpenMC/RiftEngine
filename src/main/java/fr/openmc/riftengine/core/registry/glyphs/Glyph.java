package fr.openmc.riftengine.core.registry.glyphs;

import lombok.Getter;

@Getter
public abstract class Glyph {
    private final String namespacedId;
    private final String page;
    private final int row;
    private final int col;
    private final char bedrockChar;

    public Glyph(String namespacedId, String page, int row, int col) {
        this.namespacedId = namespacedId;
        this.page = page;
        this.row = row;
        this.col = col;

        this.bedrockChar = buildBedrockChar();
    }

    public String getBedrockCharString() {
        return String.format("\\u%04X",
                (int) this.getBedrockChar());
    }

    private char buildBedrockChar() {
        int page = Integer.parseInt(this.page, 16);
        return (char) ((page << 8) | (row * 16 + col));
    }

    @Override
    public String toString() {
        return "Glyph{" +
                "namespacedId='" + namespacedId +
                ", page='" + page +
                ", row=" + row +
                ", col=" + col +
                ", bedrockChar=" + String.format("\\u%04x", (int) bedrockChar) +
                '}';
    }
}