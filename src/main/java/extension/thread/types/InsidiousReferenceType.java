package extension.thread.types;

import com.sun.jdi.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public abstract class InsidiousReferenceType implements ReferenceType {
    @Override
    public String signature() {
        return null;
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public String genericSignature() {
        return null;
    }

    @Override
    public ClassLoaderReference classLoader() {
        return null;
    }

    @Override
    public String sourceName() throws AbsentInformationException {
        return null;
    }

    @Override
    public List<String> sourceNames(String s) throws AbsentInformationException {
        return null;
    }

    @Override
    public List<String> sourcePaths(String s) throws AbsentInformationException {
        return null;
    }

    @Override
    public String sourceDebugExtension() throws AbsentInformationException {
        return null;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isFinal() {
        return false;
    }

    @Override
    public boolean isPrepared() {
        return false;
    }

    @Override
    public boolean isVerified() {
        return false;
    }

    @Override
    public boolean isInitialized() {
        return false;
    }

    @Override
    public boolean failedToInitialize() {
        return false;
    }

    @Override
    public List<Field> fields() {
        return null;
    }

    @Override
    public List<Field> visibleFields() {
        return null;
    }

    @Override
    public List<Field> allFields() {
        return null;
    }

    @Override
    public Field fieldByName(String s) {
        return null;
    }

    @Override
    public List<Method> methods() {
        return null;
    }

    @Override
    public List<Method> visibleMethods() {
        return null;
    }

    @Override
    public List<Method> allMethods() {
        return null;
    }

    @Override
    public List<Method> methodsByName(String s) {
        return null;
    }

    @Override
    public List<Method> methodsByName(String s, String s1) {
        return null;
    }

    @Override
    public List<ReferenceType> nestedTypes() {
        return null;
    }

    @Override
    public Value getValue(Field field) {
        return null;
    }

    @Override
    public Map<Field, Value> getValues(List<? extends Field> list) {
        return null;
    }

    @Override
    public ClassObjectReference classObject() {
        return null;
    }

    @Override
    public List<Location> allLineLocations() throws AbsentInformationException {
        return null;
    }

    @Override
    public List<Location> allLineLocations(String s, String s1) throws AbsentInformationException {
        return null;
    }

    @Override
    public List<Location> locationsOfLine(int i) throws AbsentInformationException {
        return null;
    }

    @Override
    public List<Location> locationsOfLine(String s, String s1, int i) throws AbsentInformationException {
        return null;
    }

    @Override
    public List<String> availableStrata() {
        return null;
    }

    @Override
    public String defaultStratum() {
        return null;
    }

    @Override
    public List<ObjectReference> instances(long l) {
        return null;
    }

    @Override
    public int majorVersion() {
        return 0;
    }

    @Override
    public int minorVersion() {
        return 0;
    }

    @Override
    public int constantPoolCount() {
        return 0;
    }

    @Override
    public byte[] constantPool() {
        return new byte[0];
    }

    @Override
    public int modifiers() {
        return 0;
    }

    @Override
    public boolean isPrivate() {
        return false;
    }

    @Override
    public boolean isPackagePrivate() {
        return false;
    }

    @Override
    public boolean isProtected() {
        return false;
    }

    @Override
    public boolean isPublic() {
        return false;
    }

    @Override
    public int compareTo(@NotNull ReferenceType referenceType) {
        return 0;
    }
}
