package io.unlogged;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class TestJackoutOutput {

    @Test
    public void testJackson() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        byte[] jsonBytes = new byte[100000];

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PojoA test = new PojoA();
        test.setSize(234);
        test.setName("adsfaf");

        objectMapper.writeValue(outputStream, test);

        jsonBytes = outputStream.toByteArray();
        outputStream.reset();


        System.out.println("jsonBytes: " + new String(jsonBytes));
        PojoA test1 = new PojoA();
        test1.setSize(123);
        test1.setName("adgadsf");

        objectMapper.writeValue(outputStream, test1);
        byte[] jsonBytes2 = outputStream.toByteArray();
        System.out.println("jsonBytes: " + new String(jsonBytes2));


    }

    class PojoA {
        Integer size;
        String name;

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
