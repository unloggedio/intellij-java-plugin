package actions;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import okhttp3.MediaType;
import pojo.VarsValues;

import java.util.ArrayList;
import java.util.List;

public class Constants {
    public static final String CLASSINFO = "classinfo";
    public static final String DATAINFO = "datainfo";
    public static final String DATAEVENTS = "dataevents";
    public static final String STRINGINFO = "stringinfo";
    public static final String BASE_URL = "BASE_URL";
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    public static final String SIGN_IN = "/api/auth/signin";
    public static final String SIGN_UP = "/api/auth/signup";
    public static final String TOKEN = "token";
    public static final String PROJECT_NAME = "project_name";
    public static final String PROJECT_URL = "/api/data/project";
    public static final String PROJECT_ID = "projectId";
    public static final String GENERATE_PROJ_AUTH = "/api/auth/generateAgentToken";

    public static final String NPE = "java.lang.NullPointerException";
    public static final String CURRENT_LINE = "current_line";


    public static List<VarsValues> convert(JSONArray jArr)
    {
        List<VarsValues> list = new ArrayList<>();
        for (int i=0; i < jArr.size(); i++){
            JSONObject jsonObject = (JSONObject)jArr.get(i);
            VarsValues varsValues = new VarsValues(jsonObject.getAsNumber("lineNum").intValue(),
                                                    jsonObject.getAsString("filename"),
                                                    jsonObject.getAsString("variableName"),
                                                    jsonObject.getAsString("variableValue"));
            list.add(varsValues);
        }

        return list;
    }
}
