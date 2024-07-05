package com.insidious.plugin.factory;

public class SemanticVersion {
    private final int major;
    private final int minor;
    private final int patch;

    public SemanticVersion(String version) {
        String[] parts = version.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Version must be in the format x.y.z");
        }
        this.major = Integer.parseInt(parts[0]);
        this.minor = Integer.parseInt(parts[1]);
        this.patch = Integer.parseInt(parts[2]);
    }

    // Getters
    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    // Method to compare if this version is above another version
    public boolean isAbove(SemanticVersion other) {
        if (this.major != other.major) {
            return this.major > other.major;
        } else if (this.minor != other.minor) {
            return this.minor > other.minor;
        } else {
            return this.patch > other.patch;
        }
    }

    // Method to compare if this version is below another version
    public boolean isBelow(SemanticVersion other) {
        return !this.isAbove(other) && !this.equals(other);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SemanticVersion that = (SemanticVersion) obj;
        return major == that.major && minor == that.minor && patch == that.patch;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + major;
        result = 31 * result + minor;
        result = 31 * result + patch;
        return result;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}