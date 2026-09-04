package fr.openmc.riftengine.core.converter.writers.glyph.font;

import com.google.gson.JsonArray;

import java.util.ArrayList;
import java.util.List;

public class FontChar {
    public record InternalFontChar(int row, int col, int codePoint, String character) {}

    /**
     * Extrait les char qu'on a dans un JsonArray
     * @param charsRows JsonArray contenant les chars
     * @return liste des characteres (row, col, codePoint)
     */
    public static List<InternalFontChar> extractCharPositions(JsonArray charsRows) {
        List<InternalFontChar> result = new ArrayList<>();

        for (int row = 0; row < charsRows.size(); row++) {
            String rowStr = charsRows.get(row).getAsString();
            int col = 0;
            int i = 0;
            while (i < rowStr.length()) {
                int codePoint = rowStr.codePointAt(i); // ex U+0061 retournera 97, car 61 hex -> 97 en dec
                i++;

                if (codePoint != 0) {
                    String character = String.valueOf(Character.toChars(codePoint));
                    result.add(new InternalFontChar(row, col, codePoint, character));
                }
                col++;
            }
        }
        return result;
    }
}
