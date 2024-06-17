package com.insidious.plugin.client;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.insidious.common.cqengine.TypeInfoDocument;

public class TypeInfoDocumentClient {

	private int typeId;
	private String typeName;
	private byte[] typeBytes;

	public TypeInfoDocumentClient() {

	}

	public TypeInfoDocumentClient(int typeId, String typeName, byte[] typeBytes) {
		this.typeId = typeId;
		this.typeName = typeName;
		this.typeBytes = typeBytes;
	}

	// Custom deserializer
    public static class TypeInfoDocumentClientDeserializer extends StdDeserializer<TypeInfoDocumentClient> {

        public TypeInfoDocumentClientDeserializer() {
            this(null);
        }

        public TypeInfoDocumentClientDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public TypeInfoDocumentClient deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException {
            JsonNode node = jp.getCodec().readTree(jp);

            // Extract values from JSON
			byte[] typeBytes = node.get("typeBytes").binaryValue();
			int typeId = node.get("typeId").intValue();
			String typeName = node.get("typeName").textValue();
			
            return new TypeInfoDocumentClient (
				typeId,
				typeName,
				typeBytes
			);
        }
    }

	public TypeInfoDocument getTypeInfoDocument() {
		return new TypeInfoDocument(
			this.typeId,
			this.typeName,
			this.typeBytes
		);
	}
}
