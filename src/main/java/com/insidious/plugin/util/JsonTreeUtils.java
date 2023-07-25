package com.insidious.plugin.util;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class JsonTreeUtils {

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
            String valueTemp = json.get(key)
                    .toString();
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
                DefaultMutableTreeNode thisKVpair = new DefaultMutableTreeNode(key + " : " + valueTemp);
                root.add(thisKVpair);
            }
        }
        return root;
    }

    private static DefaultMutableTreeNode handleArray(JSONArray json, DefaultMutableTreeNode root) {
        for (int i = 0; i < json.length(); i++) {
            String valueTemp = json.get(i)
                    .toString();
            if (valueTemp.startsWith("{")) {
                //obj in obj
                DefaultMutableTreeNode thisKey = new DefaultMutableTreeNode(i + " : ");
                JSONObject subObj = new JSONObject(valueTemp);
                handleObject(subObj, thisKey);
                root.add(thisKey);
            } else {
                DefaultMutableTreeNode thisKVpair = new DefaultMutableTreeNode(i + " : " + valueTemp);
                root.add(thisKVpair);
            }
        }
        return root;
    }

    public static String getFlatMap(Object[] pathnodes)
    {
        StringBuilder flatmap = new StringBuilder("");
        for(Object node : pathnodes)
        {
            flatmap.append(node.toString()+".");
        }
        flatmap.deleteCharAt(flatmap.length()-1);
        return flatmap.toString();
    }

    public static Map.Entry<String,String> getKeyValuePair(String flatmap)
    {
        Map<String,String> map = new TreeMap<>();
        if(flatmap.contains(":"))
        {
            String value = flatmap.substring(flatmap.lastIndexOf(":")+1).trim();
            String key = flatmap.substring(0,flatmap.lastIndexOf(":")).trim();
            map.put(key,value);
        }
        else
        {
            map.put(flatmap,null);
        }
        Map.Entry<String,String>[] entries = new Map.Entry[1];
        return map.entrySet().toArray(entries)[0];
    }
}
