package com.insidious.plugin.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Set;

public class JsonTreeUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static DefaultMutableTreeNode buildJsonTree(String source, String name) {
        if (source.startsWith("{")) {
            return handleObject(new JSONObject(source), new DefaultMutableTreeNode(name));
        } else if (source.startsWith("[")) {
            return handleArray(new JSONArray(source), new DefaultMutableTreeNode(name));
        } else {
            return new DefaultMutableTreeNode(name + " = " + source);
        }
    }

    private static DefaultMutableTreeNode handleObject(JSONObject json, DefaultMutableTreeNode root) {
        Set<String> keys = json.keySet();
        for (String key : keys) {
            String valueTemp = json.get(key).toString();
            if (valueTemp.startsWith("{")) {
                //obj in obj
                DefaultMutableTreeNode thisKey = new DefaultMutableTreeNode(key);
                JSONObject subObj = new JSONObject(valueTemp);
                handleObject(subObj, thisKey);
                root.add(thisKey);
            } else if (valueTemp.startsWith("[")) {
                //list
                DefaultMutableTreeNode thisKey = new DefaultMutableTreeNode(key);
                JSONArray subObjArray = new JSONArray(valueTemp);
                handleArray(subObjArray, thisKey);
                root.add(thisKey);
            } else {
                DefaultMutableTreeNode thisKVpair = new DefaultMutableTreeNode(key + ": " + valueTemp);
                root.add(thisKVpair);
            }
        }
        return root;
    }

    private static DefaultMutableTreeNode handleArray(JSONArray json, DefaultMutableTreeNode root) {
        for (int i = 0; i < json.length(); i++) {
            String valueTemp = json.get(i).toString();
            if (valueTemp.startsWith("{")) {
                //obj in obj
                DefaultMutableTreeNode thisKey = new DefaultMutableTreeNode(i + ": ");
                JSONObject subObj = new JSONObject(valueTemp);
                handleObject(subObj, thisKey);
                root.add(thisKey);
            } else {
                DefaultMutableTreeNode thisKVpair = new DefaultMutableTreeNode(i + ": " + valueTemp);
                root.add(thisKVpair);
            }
        }
        return root;
    }

    public static String getFlatMap(TreeNode[] treeNodes) {
        StringBuilder flatmap = new StringBuilder("");
        boolean first = true;
        for (TreeNode treeNode : treeNodes) {
            String keyName = treeNode.toString();
            if (first) {
                first = false;
            } else {
                if (keyName.contains(":")) {
                    keyName = keyName.substring(0, keyName.indexOf(":"));
                }
                flatmap.append("/").append(keyName);
            }
        }
//        flatmap.deleteCharAt(flatmap.length() - 1);
        String jsonPointer = flatmap.toString();
        if (jsonPointer.length() == 0) {
            return "/";
        }
        return jsonPointer;
    }

    public static Object getValueFromJsonNode(String source, String selectedKey) {
        try {
            JsonNode objectNode = objectMapper.readTree(source);
            return getValueFromJsonNode(objectNode, selectedKey);
        } catch (JsonProcessingException e) {
            return source;
        }
    }

    public static Object getValueFromJsonNode(JsonNode objectNode, String selectedKey) {
        if (selectedKey.equals("/")) {
            selectedKey = "";
        }
        return objectNode.at(selectedKey);
    }
}
