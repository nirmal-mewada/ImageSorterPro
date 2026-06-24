package com.imagesorter.model;

/**
 * Model representing a rule-based sorting condition and action
 */
public class SortingRule {
    public enum Field {
        FILE_NAME, FILE_EXTENSION, CAMERA
    }

    public enum Operator {
        CONTAINS, EQUALS, STARTS_WITH, ENDS_WITH, GREATER_THAN, LESS_THAN, IS_SET
    }

    public enum Action {
        MOVE, COPY
    }

    private Field field;
    private Operator operator;
    private String value;
    private Action action;
    private String destinationFolder;

    public SortingRule() {}

    public SortingRule(Field field, Operator operator, String value, Action action, String destinationFolder) {
        this.field = field;
        this.operator = operator;
        this.value = value;
        this.action = action;
        this.destinationFolder = destinationFolder;
    }

    // Getters and Setters
    public Field getField() { return field; }
    public void setField(Field field) { this.field = field; }

    public Operator getOperator() { return operator; }
    public void setOperator(Operator operator) { this.operator = operator; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public Action getAction() { return action; }
    public void setAction(Action action) { this.action = action; }

    public String getDestinationFolder() { return destinationFolder; }
    public void setDestinationFolder(String destinationFolder) { this.destinationFolder = destinationFolder; }

    /**
     * Evaluates the rule against file information and metadata.
     */
    public boolean matches(String filePath, String fileName, String camera) {
        String testVal = "";
        switch (field) {
            case FILE_NAME:
                testVal = fileName;
                break;
            case FILE_EXTENSION:
                int dotIdx = fileName.lastIndexOf('.');
                testVal = dotIdx >= 0 ? fileName.substring(dotIdx + 1).toLowerCase() : "";
                break;
            case CAMERA:
                testVal = camera != null ? camera : "";
                break;
        }

        if (operator == Operator.IS_SET) {
            return testVal != null && !testVal.trim().isEmpty();
        }

        String checkVal = value != null ? value.trim() : "";
        if (testVal == null) testVal = "";
        
        switch (operator) {
            case CONTAINS:
                return testVal.toLowerCase().contains(checkVal.toLowerCase());
            case EQUALS:
                return testVal.equalsIgnoreCase(checkVal);
            case STARTS_WITH:
                return testVal.toLowerCase().startsWith(checkVal.toLowerCase());
            case ENDS_WITH:
                return testVal.toLowerCase().endsWith(checkVal.toLowerCase());
            default:
                return false;
        }
    }
    
    @Override
    public String toString() {
        String actionStr = action == Action.MOVE ? "Move" : "Copy";
        String destName = destinationFolder != null ? new java.io.File(destinationFolder).getName() : "None";
        return "IF " + field + " " + operator + " \"" + (value != null ? value : "") + "\" THEN " + actionStr + " to \"" + destName + "\"";
    }
}
