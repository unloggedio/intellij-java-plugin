package com.insidious.plugin.client;

import java.io.IOException;
import java.io.Serializable;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.insidious.common.weaver.TypeInfo;

public class TypeInfoClient implements Serializable {

	private int[] interfaces;
	private int typeId;
	private String typeNameFromClass;
	private String classLocation;
	private int superClass;
	private int componentType;
	private String classLoaderIdentifier;

	public TypeInfoClient() {
    }

	public TypeInfoClient(	
		int typeId,
		String typeNameFromClass,
		String classLocation,
		int superClass,
		int componentType,
		String classLoaderIdentifier,
		int[] interfaces) {
		
		this.typeId = typeId;
		this.typeNameFromClass = typeNameFromClass;
		this.classLocation = classLocation;
		this.superClass = superClass;
		this.componentType = componentType;
		this.classLoaderIdentifier = classLoaderIdentifier;
		this.interfaces = interfaces;
	}

	// Custom deserializer
    public static class TypeInfoClientDeserializer extends StdDeserializer<TypeInfoClient> {

        public TypeInfoClientDeserializer() {
            this(null);
        }

        public TypeInfoClientDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public TypeInfoClient deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException {
            JsonNode node = jp.getCodec().readTree(jp);

            // Extract values from JSON
			JsonNode interfacesNode = node.get("interfaces");
			
			int[] interfaces = new int[interfacesNode.size()];
			if (interfacesNode != null && interfacesNode.isArray()) {
				for (int i=0;i<=interfacesNode.size()-1;i++) {
					interfaces[i] = interfacesNode.get(i).intValue();
				}
			}
			
            int typeId = node.get("typeId").intValue();
            String typeNameFromClass = node.get("typeNameFromClass").textValue();
			String classLocation = node.get("classLocation").textValue();
			int superClass = node.get("superClass").intValue();
			int componentType = node.get("componentType").intValue();
			String classLoaderIdentifier = node.get("classLoaderIdentifier").textValue();

            return new TypeInfoClient (typeId, typeNameFromClass, classLocation, superClass, componentType, classLoaderIdentifier, interfaces);
        }
    }

	public TypeInfo getTypeInfo() {
		return new TypeInfo (
			this.typeId,
			this.typeNameFromClass,
			this.classLocation,
			this.superClass,
			this.componentType,
			this.classLoaderIdentifier,
			this.interfaces
		);
	}

}
