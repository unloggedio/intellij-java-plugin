package extension.model;


public class TypeInfo {

    String id;
    String sessionId;
    private long typeId;
    private String typeNameFromClass;
    private String classLocation;
    private String superClass;
    private String componentType;
    private String classLoaderIdentifier;

    public TypeInfo(String sessionId, long typeId, String typeNameFromClass, String classLocation, String superClass, String componentType, String classLoaderIdentifier) {
        this.sessionId = sessionId;
        this.typeId = typeId;
        this.typeNameFromClass = typeNameFromClass;
        this.classLocation = classLocation;
        this.superClass = superClass;
        this.componentType = componentType;
        this.classLoaderIdentifier = classLoaderIdentifier;
    }

    public TypeInfo() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public long getTypeId() {
        return typeId;
    }

    public String getTypeNameFromClass() {
        return typeNameFromClass;
    }

    public String getClassLocation() {
        return classLocation;
    }

    public String getSuperClass() {
        return superClass;
    }

    public String getComponentType() {
        return componentType;
    }

    public String getClassLoaderIdentifier() {
        return classLoaderIdentifier;
    }
}
