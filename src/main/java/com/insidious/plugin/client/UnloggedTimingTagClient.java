package com.insidious.plugin.client;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class UnloggedTimingTagClient {
	private int lineNumber;
	private long nanoSecondTimestamp;

	public UnloggedTimingTagClient() {
	}

	public UnloggedTimingTagClient (int lineNumber, long nanoSecondTimestamp) {
		this.lineNumber = lineNumber;
		this.nanoSecondTimestamp = nanoSecondTimestamp;
	}
	
	public static class UnloggedTimingTagClientDeserializer extends StdDeserializer<UnloggedTimingTagClient> {

        public UnloggedTimingTagClientDeserializer() {
            this(null);
        }

        public UnloggedTimingTagClientDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public UnloggedTimingTagClient deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException {
            JsonNode node = jp.getCodec().readTree(jp);

            int lineNumber = node.get("lineNumber").intValue();
            long nanoSecondTimestamp = node.get("nanoSecondTimestamp").longValue();
            return new UnloggedTimingTagClient (lineNumber, nanoSecondTimestamp);
        }
    }

	public UnloggedTimingTag getUnloggedTimingTag() {
		return new UnloggedTimingTag(
			this.lineNumber,
			this.nanoSecondTimestamp
		);
	}
}
