package pojo;

public class VarsValues {

    long nanoTime;
    String filename, variableName, variableValue;
    int lineNum;

    public VarsValues(int lineNum, String filename, String variableName, String variableValue, long nanoTime) {
        this.lineNum = lineNum;
        this.filename = filename;
        this.variableName = variableName;
        this.variableValue = variableValue;
        this.nanoTime = nanoTime;
    }

    public int getLineNum() {
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

    public long getNanoTime() {
        return nanoTime;
    }
}
