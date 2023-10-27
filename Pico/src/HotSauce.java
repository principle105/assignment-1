import java.util.List;
import java.util.Arrays;

public class HotSauce {
    private final String name;
    private final String origin;
    private final String heat;
    private final String size;
    private final String quantity;
    private final String year;
    private final String tasted;
    private final String comments;

    public HotSauce(String name, String origin, String heat, String size, String quantity, String year, String tasted,
            String comments) {
        this.name = name;
        this.origin = origin;
        this.heat = heat;
        this.size = size;
        this.quantity = quantity;
        this.year = year;
        this.tasted = tasted;
        this.comments = comments;
    }

    public String getName() {
        return name;
    }

    public String getOrigin() {
        return origin;
    }

    public String getHeat() {
        return heat;
    }

    public static List<String> getFieldNames() {
        return Arrays.asList("Name", "Origin", "Heat Level", "Bottle Size", "Quantity", "Year", "Tasted", "Comments");
    }

    public static List<String> getRequiredFieldNames() {
        return Arrays.asList("Name", "Origin", "Heat Level", "Bottle Size");
    }

    public List<String> getRowValues() {
        return Arrays.asList(name, origin, heat, size, quantity, year, tasted, comments);
    }

    public static boolean isValidInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static HotSauce fromRow(List<String> row) throws Exception {
        String name = row.get(0);
        String origin = row.get(1);
        String heat = row.get(2);
        String size = row.get(3);
        String quantity = row.get(4);
        String year = row.get(5);
        String tasted = row.get(6);
        String comments = row.get(7);

        if (!heat.isEmpty()) {
            switch (heat.toLowerCase()) {
                case "hot":
                case "medium":
                case "mild":
                    break;
                default:
                    if (!isValidInt(heat)) {
                        throw new Exception("Heat must be an integer, hot, medium or mild");
                    }
            }
        }

        if (!size.isEmpty()) {
        	if (!isValidInt(size)) {
                throw new Exception("Size must be an integer");
            }
        	
            int sizeInt = Integer.parseInt(size);
            if (sizeInt <= 0) {
                throw new Exception("Size must be greater than 0");
            }
        }

        if (!quantity.isEmpty()) {
        	if (!isValidInt(quantity)) {
                throw new Exception("Quantity must be an integer");
            }
        	
            int quantityInt = Integer.parseInt(quantity);
            if (quantityInt <= 0) {
                throw new Exception("Quantity must be greater than 0");
            }
        }

        if (!year.isEmpty()) {
        	if (!isValidInt(year)) {
                throw new Exception("Year must be an integer.");
            }
            int yearInt = Integer.parseInt(year);
        }

        if (!tasted.isEmpty()) {
            switch (tasted.toLowerCase()) {
                case "yes":
                case "no":
                    break;
                default:
                    throw new Exception("Tasted must be yes or no");
            }
        }

        return new HotSauce(name, origin, heat, size, quantity, year, tasted, comments);
    }
}