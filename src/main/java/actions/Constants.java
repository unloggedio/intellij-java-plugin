package actions;

import okhttp3.MediaType;

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
    public static final String PROJECT_URL = "/api/data/projects";
    public static final String CREATE_PROJECT_URL = "/api/data/project";
    public static final String PROJECT_ID = "projectId";
    public static final String GENERATE_PROJ_AUTH = "/api/auth/generateAgentToken";

    public static final String CURRENT_LINE = "current_line";
    public static final String TRACK_LINE = "track_lines";
    public static final String PROJECT_TOKEN = "project_token";
    public static final String ERROR_NAMES = "error_names";

}
