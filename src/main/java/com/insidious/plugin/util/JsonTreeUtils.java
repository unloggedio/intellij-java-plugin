package com.insidious.plugin.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

public class JsonTreeUtils {

    private static final ObjectMapper objectMapper = ObjectMapperInstance.getInstance();

    public static JsonNode treeModelToJson(TreeModel treeModel) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        return buildJsonFromTree(root);
    }

    private static JsonNode buildJsonFromTree(DefaultMutableTreeNode treeNode) {
        ObjectMapper mapper = new ObjectMapper();
        if (treeNode.isLeaf()) {
            String nodeContent = treeNode.getUserObject().toString();
            if (nodeContent.contains(":")) {
                // It's a value
                String value = nodeContent.substring(nodeContent.indexOf(":") + 1).trim();
                return mapper.valueToTree(value);
            }
        }

        Enumeration<TreeNode> children = treeNode.children();
        if (isArrayNode(treeNode)) {
            ArrayNode arrayNode = mapper.createArrayNode();
            while (children.hasMoreElements()) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
                arrayNode.add(buildJsonFromTree(child));
            }
            return arrayNode;
        } else if (isJSONnode(treeNode)) {
            ObjectNode objectNode = mapper.createObjectNode();
            TreeNode childNode = children.nextElement();
            try {
                return mapper.readTree(childNode.toString());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            ObjectNode objectNode = mapper.createObjectNode();
            while (children.hasMoreElements()) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
                String key = child.toString();
                if (key.contains(":")) {
                    key = key.split(":")[0].trim();
                }
                objectNode.set(key, buildJsonFromTree(child));
            }
            return objectNode;
        }
    }

    private static boolean isArrayNode(DefaultMutableTreeNode treeNode) {
        Enumeration<TreeNode> children = treeNode.children();
        if (!children.hasMoreElements()) {
            return false; // Not an array if no children
        }
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            if (!child.getUserObject().toString().matches("^\\[\\d+\\].*")) {
                return false;
            }
        }
        return true;
    }

    private static boolean isJSONnode(DefaultMutableTreeNode treeNode) {
        Enumeration<TreeNode> children = treeNode.children();
        if (!children.hasMoreElements()) {
            return false; // Not an json if no children
        }
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            if (!child.getUserObject().toString().matches("^\\s*(\\{.*\\}|\\[.*\\])\\s*$")) {
                return false;
            }
        }
        return true;
    }

//    public TreeModel addChild(TreeModel treeModel, String childData) {
//        treeModel.add("smth");
//
//
//        return new DefaultTreeModel(treeModel);
//    }

    public static TreeModel jsonToTreeModel(JsonNode jsonNode, String simpleName) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(simpleName);
        buildTreeFromJsonNode(root, jsonNode);
        return new DefaultTreeModel(root);
    }

    private static void buildTreeFromJsonNode(DefaultMutableTreeNode treeNode, JsonNode jsonNode) {
        if (jsonNode.isObject()) {
            ObjectNode objectNode = (ObjectNode) jsonNode;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            if (fields.hasNext()) {
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(field.getKey());
                    buildTreeFromJsonNode(childNode, field.getValue());
                    treeNode.add(childNode);
                }
            } else {
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(jsonNode);
                if (jsonNode.asText().length() == 0) {
                    childNode.setUserObject("{}");
                } else {
                    childNode.setUserObject("{" + jsonNode.asText() + "}");
                }
                treeNode.add(childNode);
            }
        } else if (jsonNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) jsonNode;
            for (int i = 0; i < arrayNode.size(); i++) {
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode("[" + i + "]");
                buildTreeFromJsonNode(childNode, arrayNode.get(i));
                treeNode.add(childNode);
            }
        } else {
            treeNode.setUserObject(treeNode.getUserObject() + ": " + jsonNode.asText());
        }
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

    public static JsonNode getValueFromJsonNode(JsonNode objectNode, String selectedKey) {
        if (selectedKey.equals("/")) {
            selectedKey = "";
        }
        return objectNode.at(selectedKey);
    }

    public static ObjectNode flatten(JsonNode map) {
        ObjectNode newObjectNode = objectMapper.createObjectNode();
        flattenRecursively(newObjectNode, "", map);
        return newObjectNode;
    }

    private static void flattenRecursively(ObjectNode newObjectNode, String currentPath, JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                String newPath = currentPath + "/" + key;
                flattenRecursively(newObjectNode, newPath, value);
            }
        } else if (node.isArray()) {
            int index = 0;
            for (JsonNode childNode : node) {
                String newPath = currentPath + "/" + index;
                flattenRecursively(newObjectNode, newPath, childNode);
                index++;
            }
        } else {
            newObjectNode.put(currentPath, node.asText());
        }
    }

}
