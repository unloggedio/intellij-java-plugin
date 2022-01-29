package pojo;

public class VarsValues {

    long nanoTime;
    String filename;
    String variableName;
    String variableValue;
    int lineNum;


    public VarsValues() {
    }

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

    public void setLineNum(int lineNum) {
        this.lineNum = lineNum;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public String getVariableValue() {
        return variableValue;
    }

    public void setVariableValue(String variableValue) {
        this.variableValue = variableValue;
    }

    public long getNanoTime() {
        return nanoTime;
    }

    public void setNanoTime(long nanoTime) {
        this.nanoTime = nanoTime;
    }

    public String hash() {
        return filename + "-" + lineNum + "-" + variableName;
    }
}
