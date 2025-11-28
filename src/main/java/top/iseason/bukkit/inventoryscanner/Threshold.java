

package top.iseason.bukkit.inventoryscanner;

import lombok.Getter;

@Getter
public class Threshold {
    private final int max;
    private final int warning;

    public Threshold(int warning, int max) {
        this.warning = warning;
        this.max = max;

    }

    public String toString() {
        return this.warning + "/" + this.max;
    }

    public static Threshold fromString(String str) {
        String[] split = str.split("/");
        if (split.length == 2) {
            try {
                return new Threshold(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        try {
            return new Threshold(-1, Integer.parseInt(str));
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
