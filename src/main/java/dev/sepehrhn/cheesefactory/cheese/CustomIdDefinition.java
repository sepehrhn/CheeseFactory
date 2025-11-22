package dev.sepehrhn.cheesefactory.cheese;

public class CustomIdDefinition {
    private final String type;
    private final String item;

    public CustomIdDefinition(String type, String item) {
        this.type = type;
        this.item = item;
    }

    public String type() {
        return type;
    }

    public String item() {
        return item;
    }
}
