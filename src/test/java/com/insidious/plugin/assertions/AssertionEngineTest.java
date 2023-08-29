package com.insidious.plugin.assertions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class AssertionEngineTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void assertionTestAllPassing() throws JsonProcessingException {


        JsonNode actualNode = objectMapper.readTree("""
                {
                    "a":  1,
                    "b":  2,
                    "c":  {
                        "a":  "3",
                        "b":  "4"
                    },
                    "nameA": "nameA",
                    "nameB": "nameB",
                    "nameC": "nameC",
                    "nameD": "nameD",
                    "d": "false",
                    "e": "true",
                    "f": [
                        {
                            "a":  "5",
                            "b":  "6",
                            "c":  {
                                "a":  "7",
                                "b":  "8"
                            },
                            "d": "true",
                            "e": "false"
                        }
                    ]
                }
                """);
        List<AtomicAssertion> assertionList = new ArrayList<>();

        AtomicAssertion assertion1 = new AtomicAssertion(AssertionType.EQUAL, "/a", "1");
        AtomicAssertion assertion2 = new AtomicAssertion(AssertionType.EQUAL, "/b", "2");
        AtomicAssertion assertion3 = new AtomicAssertion(AssertionType.LESS_THAN, "/a", "2");
        AtomicAssertion assertion4 = new AtomicAssertion(AssertionType.GREATER_THAN, "/b", "1");

        AtomicAssertion assertion5 = new AtomicAssertion(Expression.SIZE, AssertionType.EQUAL, "/f", "1");
        AtomicAssertion assertion6 = new AtomicAssertion(Expression.SIZE, AssertionType.NOT_EQUAL, "/f", "2");
        AtomicAssertion assertion7 = new AtomicAssertion(Expression.SIZE, AssertionType.GREATER_THAN, "/f", "0");
        AtomicAssertion assertion8 = new AtomicAssertion(Expression.SIZE, AssertionType.LESS_THAN, "/f", "2");
        AtomicAssertion assertion9 = new AtomicAssertion(AssertionType.GREATER_THAN, "/c/b", "1");


        assertionList.add(assertion1);
        assertionList.add(assertion2);
        assertionList.add(assertion3);
        assertionList.add(assertion4);
        assertionList.add(assertion5);
        assertionList.add(assertion6);
        assertionList.add(assertion7);
        assertionList.add(assertion8);
        assertionList.add(assertion9);

        AtomicAssertion assertionGroup = new AtomicAssertion(AssertionType.ALLOF, assertionList);
        AssertionResult result = AssertionEngine.executeAssertions(assertionGroup, actualNode);

        Assertions.assertTrue(result.getResults().get(assertion1.getId()), assertion1.toString());
        Assertions.assertTrue(result.getResults().get(assertion2.getId()), assertion2.toString());
        Assertions.assertTrue(result.getResults().get(assertion3.getId()), assertion3.toString());
        Assertions.assertTrue(result.getResults().get(assertion4.getId()), assertion4.toString());
        Assertions.assertTrue(result.getResults().get(assertion5.getId()), assertion5.toString());
        Assertions.assertTrue(result.getResults().get(assertion6.getId()), assertion6.toString());
        Assertions.assertTrue(result.getResults().get(assertion7.getId()), assertion7.toString());
        Assertions.assertTrue(result.getResults().get(assertion8.getId()), assertion8.toString());
        Assertions.assertTrue(result.getResults().get(assertion9.getId()), assertion9.toString());
        Assertions.assertTrue(result.isPassing());


    }

    @Test
    void assertionTestObjectCompare() throws JsonProcessingException {


        JsonNode actualNode = objectMapper.readTree("""
                {
                    "a":  "1",
                    "b":  "2",
                    "c":  {
                        "a":  "3",
                        "b":  "4"
                    },
                    "nameA": "nameA",
                    "nameB": "nameB",
                    "nameC": "nameC",
                    "nameD": "nameD",
                    "d": "false",
                    "e": "true",
                    "f": [
                        {
                            "a":  "5",
                            "b":  "6",
                            "c":  {
                                "a":  "7",
                                "b":  "8"
                            },
                            "d": "true",
                            "e": "false"
                        }
                    ]
                }
                """);
        List<AtomicAssertion> assertionList = new ArrayList<>();

        AtomicAssertion assertion1 = new AtomicAssertion(AssertionType.EQUAL, "/c", """
                    {
                        "a":  "3",
                        "b":  "4"
                    }
                """);

        assertionList.add(assertion1);


        AtomicAssertion assertionGroup = new AtomicAssertion(AssertionType.ALLOF, assertionList);
        AssertionResult result = AssertionEngine.executeAssertions(assertionGroup, actualNode);

        Assertions.assertTrue(result.getResults().get(assertion1.getId()), assertion1.toString());

        Assertions.assertTrue(result.isPassing());


    }

    @Test
    void assertionTestObjectCompare2() throws JsonProcessingException {


        JsonNode actualNode = objectMapper.readTree("""
                {
                    "a":  "1",
                    "b":  "2",
                    "c":  {
                        "a":  "3",
                        "b":  "4"
                    },
                    "nameA": "nameA",
                    "nameB": "nameB",
                    "nameC": "nameC",
                    "nameD": "nameD",
                    "d": "false",
                    "e": "true",
                    "f": [
                        {
                            "a":  "5",
                            "b":  "6",
                            "c":  {
                                "a":  "7",
                                "b":  "8"
                            },
                            "d": "true",
                            "e": "false"
                        }
                    ]
                }
                """);
        List<AtomicAssertion> assertionList = new ArrayList<>();

        AtomicAssertion assertion1 = new AtomicAssertion(AssertionType.EQUAL, "/c", """
                    {
                        "b":  "4",
                        "a":  "3"
                    }
                """);

        assertionList.add(assertion1);


        AtomicAssertion assertionGroup = new AtomicAssertion(AssertionType.ALLOF, assertionList);
        AssertionResult result = AssertionEngine.executeAssertions(assertionGroup, actualNode);

        Assertions.assertTrue(result.getResults().get(assertion1.getId()), assertion1.toString());

        Assertions.assertTrue(result.isPassing());


    }

    @Test
    void assertionTestOneFailingButPassingWithOr() throws JsonProcessingException {


        JsonNode actualNode = objectMapper.readTree("""
                {
                    "a":  1,
                    "b":  2,
                    "c":  {
                        "a":  "3",
                        "b":  "4"
                    },
                    "nameA": "nameA",
                    "nameB": "nameB",
                    "nameC": "nameC",
                    "nameD": "nameD",
                    "d": "false",
                    "e": "true",
                    "f": [
                        {
                            "a":  "5",
                            "b":  "6",
                            "c":  {
                                "a":  "7",
                                "b":  "8"
                            },
                            "d": "true",
                            "e": "false"
                        }
                    ]
                }
                """);
        List<AtomicAssertion> assertionList = new ArrayList<>();

        AtomicAssertion assertion1 = new AtomicAssertion(AssertionType.EQUAL, "/a", "1");

        // this should fail
        AtomicAssertion assertion2 = new AtomicAssertion(AssertionType.NOT_EQUAL, "/b", "2");

        AtomicAssertion assertion3 = new AtomicAssertion(AssertionType.LESS_THAN, "/a", "2");
        AtomicAssertion assertion4 = new AtomicAssertion(AssertionType.GREATER_THAN, "/b", "1");

        AtomicAssertion assertion5 = new AtomicAssertion(Expression.SIZE, AssertionType.EQUAL, "/f", "1");
        AtomicAssertion assertion6 = new AtomicAssertion(Expression.SIZE, AssertionType.NOT_EQUAL, "/f", "2");

        AtomicAssertion assertion7 = new AtomicAssertion(Expression.SIZE, AssertionType.GREATER_THAN, "/f", "0");

        AtomicAssertion assertion8 = new AtomicAssertion(Expression.SIZE, AssertionType.LESS_THAN, "/f", "2");

        AtomicAssertion assertion9 = new AtomicAssertion(AssertionType.GREATER_THAN, "/c/b", "1");


        assertionList.add(assertion1);
        assertionList.add(assertion2);
        assertionList.add(assertion3);
        assertionList.add(assertion4);
        assertionList.add(assertion5);
        assertionList.add(assertion6);
        assertionList.add(assertion7);
        assertionList.add(assertion8);
        assertionList.add(assertion9);

        AtomicAssertion assertionGroup = new AtomicAssertion(AssertionType.ANYOF, assertionList);
        AssertionResult result = AssertionEngine.executeAssertions(assertionGroup, actualNode);

        Assertions.assertTrue(result.getResults().get(assertion1.getId()), assertion1.toString());
        Assertions.assertFalse(result.getResults().get(assertion2.getId()), assertion2.toString());
        Assertions.assertTrue(result.getResults().get(assertion3.getId()), assertion3.toString());
        Assertions.assertTrue(result.getResults().get(assertion4.getId()), assertion4.toString());
        Assertions.assertTrue(result.getResults().get(assertion5.getId()), assertion5.toString());
        Assertions.assertTrue(result.getResults().get(assertion6.getId()), assertion6.toString());
        Assertions.assertTrue(result.getResults().get(assertion7.getId()), assertion7.toString());
        Assertions.assertTrue(result.getResults().get(assertion8.getId()), assertion8.toString());
        Assertions.assertTrue(result.getResults().get(assertion9.getId()), assertion9.toString());
        Assertions.assertTrue(result.isPassing());


    }

    @Test
    void assertionTestBuildFromJson() throws JsonProcessingException {


        JsonNode actualNode = objectMapper.readTree("""
                {
                    "a":  1,
                    "b":  2,
                    "c":  {
                        "a":  "3",
                        "b":  "4"
                    },
                    "nameA": "nameA",
                    "nameB": "nameB",
                    "nameC": "nameC",
                    "nameD": "nameD",
                    "d": "false",
                    "e": "true",
                    "f": [
                        {
                            "a":  "5",
                            "b":  "6",
                            "c":  {
                                "a":  "7",
                                "b":  "8"
                            },
                            "d": "true",
                            "e": "false"
                        }
                    ]
                }
                """);


        AtomicAssertion assertionGroup =
                objectMapper.readValue("""
                        {
                        	"subAssertions": [{
                        		"subAssertions": null,
                        		"expression": "SELF",
                        		"expectedValue": "1",
                        		"id": "daa57efc-5a30-4752-8c9a-a4184786afcc",
                        		"assertionType": "EQUAL",
                        		"key": "/a"
                        	}, {
                        		"subAssertions": null,
                        		"expression": "SELF",
                        		"expectedValue": "2",
                        		"id": "5838b045-01ce-4228-8d14-9b64b6ec108a",
                        		"assertionType": "NOT_EQUAL",
                        		"key": "/b"
                        	}, {
                        		"subAssertions": null,
                        		"expression": "SELF",
                        		"expectedValue": "2",
                        		"id": "9dad329f-d92d-41e5-8204-de5186ced553",
                        		"assertionType": "LESS_THAN",
                        		"key": "/a"
                        	}, {
                        		"subAssertions": null,
                        		"expression": "SELF",
                        		"expectedValue": "1",
                        		"id": "b85668ba-b7d8-417c-86e0-78e3ec00dbe0",
                        		"assertionType": "GREATER_THAN",
                        		"key": "/b"
                        	}, {
                        		"subAssertions": null,
                        		"expression": "SIZE",
                        		"expectedValue": "1",
                        		"id": "e3f88da5-f3f3-4c26-a33c-76f571c538e0",
                        		"assertionType": "EQUAL",
                        		"key": "/f"
                        	}, {
                        		"subAssertions": null,
                        		"expression": "SIZE",
                        		"expectedValue": "2",
                        		"id": "fd715da0-93d3-472a-9b40-0b6bc2548927",
                        		"assertionType": "NOT_EQUAL",
                        		"key": "/f"
                        	}, {
                        		"subAssertions": null,
                        		"expression": "SIZE",
                        		"expectedValue": "0",
                        		"id": "637cb60b-4a77-43fd-9426-4f100698b3e9",
                        		"assertionType": "GREATER_THAN",
                        		"key": "/f"
                        	}, {
                        		"subAssertions": null,
                        		"expression": "SIZE",
                        		"expectedValue": "2",
                        		"id": "1d8cb59a-52cd-4c2a-886d-e2bd2a37a93d",
                        		"assertionType": "LESS_THAN",
                        		"key": "/f"
                        	}, {
                        		"subAssertions": null,
                        		"expression": "SELF",
                        		"expectedValue": "1",
                        		"id": "76318875-44f0-4864-9580-45a5fa9c6885",
                        		"assertionType": "GREATER_THAN",
                        		"key": "/c/b"
                        	}, {
                        		"subAssertions": [
                        		    {
                                        "subAssertions": null,
                                        "expression": "SELF",
                                        "expectedValue": "5",
                                        "id": "76318875-44f0-4864-9580-45a5fa9c6886",
                                        "assertionType": "GREATER_THAN",
                                        "key": "/c/b"
                                    },
                        		    {
                                        "subAssertions": null,
                                        "expression": "SELF",
                                        "expectedValue": "6",
                                        "id": "76318875-44f0-4864-9580-45a5fa9c6887",
                                        "assertionType": "LESS_THAN",
                                        "key": "/c/b"
                                    }
                        		],
                        		"expression": "SELF",
                        		"expectedValue": "1",
                        		"id": "76318875-44f0-4864-9580-45a5fa9c6886",
                        		"assertionType": "ALLOF",
                        		"key": "/c/b"
                        	}],
                        	"expression": "SELF",
                        	"expectedValue": null,
                        	"id": "6a10e916-3c6d-44f5-b6d5-51b30d32c1e8",
                        	"assertionType": "ALLOF",
                        	"key": null
                        }
                        """, AtomicAssertion.class);
        AssertionResult result = AssertionEngine.executeAssertions(assertionGroup, actualNode);

        Assertions.assertTrue(result.getResults().get("daa57efc-5a30-4752-8c9a-a4184786afcc"));
        Assertions.assertFalse(result.getResults().get("5838b045-01ce-4228-8d14-9b64b6ec108a"));
        Assertions.assertTrue(result.getResults().get("9dad329f-d92d-41e5-8204-de5186ced553"));
        Assertions.assertTrue(result.getResults().get("b85668ba-b7d8-417c-86e0-78e3ec00dbe0"));
        Assertions.assertTrue(result.getResults().get("e3f88da5-f3f3-4c26-a33c-76f571c538e0"));
        Assertions.assertTrue(result.getResults().get("fd715da0-93d3-472a-9b40-0b6bc2548927"));
        Assertions.assertTrue(result.getResults().get("637cb60b-4a77-43fd-9426-4f100698b3e9"));
        Assertions.assertTrue(result.getResults().get("1d8cb59a-52cd-4c2a-886d-e2bd2a37a93d"));
        Assertions.assertTrue(result.getResults().get("76318875-44f0-4864-9580-45a5fa9c6885"));
        Assertions.assertFalse(result.getResults().get("6a10e916-3c6d-44f5-b6d5-51b30d32c1e8"));

        Assertions.assertFalse(result.getResults().get("76318875-44f0-4864-9580-45a5fa9c6886"));
        Assertions.assertTrue(result.getResults().get("76318875-44f0-4864-9580-45a5fa9c6887"));

        Assertions.assertFalse(result.isPassing());


    }


    @Test
    void assertionTestBuildFromJsonNot() throws JsonProcessingException {


        JsonNode actualNode = objectMapper.readTree("""
                {
                    "a":  1,
                    "b":  2,
                    "c":  {
                        "a":  "3",
                        "b":  "4"
                    },
                    "nameA": "nameA",
                    "nameB": "nameB",
                    "nameC": "nameC",
                    "nameD": "nameD",
                    "d": "false",
                    "e": "true",
                    "f": [
                        {
                            "a":  "5",
                            "b":  "6",
                            "c":  {
                                "a":  "7",
                                "b":  "8"
                            },
                            "d": "true",
                            "e": "false"
                        }
                    ]
                }
                """);


        AtomicAssertion assertionGroup =
                objectMapper.readValue("""
                        {
                        	"subAssertions": [{
                        		"subAssertions": null,
                        		"expression": "SELF",
                        		"expectedValue": "1",
                        		"id": "daa57efc-5a30-4752-8c9a-a4184786afcc",
                        		"assertionType": "EQUAL",
                        		"key": "/a"
                        	}, {
                        		"subAssertions": null,
                        		"expression": "SELF",
                        		"expectedValue": "2",
                        		"id": "5838b045-01ce-4228-8d14-9b64b6ec108a",
                        		"assertionType": "EQUAL",
                        		"key": "/b"
                        	}, {
                        		"subAssertions": null,
                        		"expression": "SELF",
                        		"expectedValue": "2",
                        		"id": "9dad329f-d92d-41e5-8204-de5186ced553",
                        		"assertionType": "LESS_THAN",
                        		"key": "/a"
                        	}, {
                        		"subAssertions": null,
                        		"expression": "SELF",
                        		"expectedValue": "1",
                        		"id": "b85668ba-b7d8-417c-86e0-78e3ec00dbe0",
                        		"assertionType": "GREATER_THAN",
                        		"key": "/b"
                        	}, {
                        		"subAssertions": null,
                        		"expression": "SIZE",
                        		"expectedValue": "1",
                        		"id": "e3f88da5-f3f3-4c26-a33c-76f571c538e0",
                        		"assertionType": "EQUAL",
                        		"key": "/f"
                        	}, {
                        		"subAssertions": null,
                        		"expression": "SIZE",
                        		"expectedValue": "2",
                        		"id": "fd715da0-93d3-472a-9b40-0b6bc2548927",
                        		"assertionType": "NOT_EQUAL",
                        		"key": "/f"
                        	}, {
                        		"subAssertions": null,
                        		"expression": "SIZE",
                        		"expectedValue": "0",
                        		"id": "637cb60b-4a77-43fd-9426-4f100698b3e9",
                        		"assertionType": "GREATER_THAN",
                        		"key": "/f"
                        	}, {
                        		"subAssertions": null,
                        		"expression": "SIZE",
                        		"expectedValue": "2",
                        		"id": "1d8cb59a-52cd-4c2a-886d-e2bd2a37a93d",
                        		"assertionType": "LESS_THAN",
                        		"key": "/f"
                        	}, {
                        		"subAssertions": null,
                        		"expression": "SELF",
                        		"expectedValue": "1",
                        		"id": "76318875-44f0-4864-9580-45a5fa9c6885",
                        		"assertionType": "GREATER_THAN",
                        		"key": "/c/b"
                        	}, {
                        		"subAssertions": [
                        		    {
                                        "subAssertions": null,
                                        "expression": "SELF",
                                        "expectedValue": "5",
                                        "id": "76318875-44f0-4864-9580-45a5fa9c6886",
                                        "assertionType": "GREATER_THAN",
                                        "key": "/c/b"
                                    }
                        		],
                        		"expression": "SELF",
                        		"expectedValue": "1",
                        		"id": "76318875-44f0-4864-9580-45a5fa9c6887",
                        		"assertionType": "NOTALLOF",
                        		"key": "/c/b"
                        	}],
                        	"expression": "SELF",
                        	"expectedValue": null,
                        	"id": "6a10e916-3c6d-44f5-b6d5-51b30d32c1e8",
                        	"assertionType": "ALLOF",
                        	"key": null
                        }
                        """, AtomicAssertion.class);
        AssertionResult result = AssertionEngine.executeAssertions(assertionGroup, actualNode);

        Assertions.assertTrue(result.getResults().get("daa57efc-5a30-4752-8c9a-a4184786afcc"));
        Assertions.assertTrue(result.getResults().get("5838b045-01ce-4228-8d14-9b64b6ec108a"));
        Assertions.assertTrue(result.getResults().get("9dad329f-d92d-41e5-8204-de5186ced553"));
        Assertions.assertTrue(result.getResults().get("b85668ba-b7d8-417c-86e0-78e3ec00dbe0"));
        Assertions.assertTrue(result.getResults().get("e3f88da5-f3f3-4c26-a33c-76f571c538e0"));
        Assertions.assertTrue(result.getResults().get("fd715da0-93d3-472a-9b40-0b6bc2548927"));
        Assertions.assertTrue(result.getResults().get("637cb60b-4a77-43fd-9426-4f100698b3e9"));
        Assertions.assertTrue(result.getResults().get("1d8cb59a-52cd-4c2a-886d-e2bd2a37a93d"));
        Assertions.assertTrue(result.getResults().get("76318875-44f0-4864-9580-45a5fa9c6885"));
        Assertions.assertTrue(result.getResults().get("6a10e916-3c6d-44f5-b6d5-51b30d32c1e8"));

        Assertions.assertFalse(result.getResults().get("76318875-44f0-4864-9580-45a5fa9c6886"));
        Assertions.assertTrue(result.getResults().get("76318875-44f0-4864-9580-45a5fa9c6887"));


        Assertions.assertTrue(result.isPassing());


    }


}