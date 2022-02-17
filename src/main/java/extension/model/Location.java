package extension.model;

import java.util.Objects;

public class Location {
    private final String locationName;
    private final String className;
    private final String methodName;
    private final int lineNumber;

    public Location(String locationName, String className, String methodName, int lineNumber) {
        this.locationName = locationName;
        this.className = className;
        this.methodName = methodName;
        this.lineNumber = lineNumber;
    }

    public String getClassName() {
        return this.className;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }


    public String toString() {
        return this.locationName + ':' + this.className + ':' + this.methodName + ':' + this.lineNumber;
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return (this.lineNumber == location.lineNumber &&
                Objects.equals(this.className, location.className) &&
                Objects.equals(this.methodName, location.methodName));
    }


    public int hashCode() {
        return Objects.hash(this.className, this.methodName, Integer.valueOf(this.lineNumber));
    }
}


