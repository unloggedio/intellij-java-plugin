package pojo;

public class VarsValues {

    public VarsValues(long lineNum, String filename, String variableName, String variableValue) {
        this.lineNum = lineNum;
        this.filename = filename;
        this.variableName = variableName;
        this.variableValue = variableValue;
    }

    public String getLineNum() {
        return lineNum;
    }

    public String getFilename() {
        return filename;
    }

    public String getVariableName() {
        return variableName;
    }

    public String getVariableValue() {
        return variableValue;
    }

    String lineNum, filename, variableName, variableValue;

}
