package com.exsoinn.util.epf;

import com.exsoinn.util.EscapeUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TODO: Test Incompatible SP exception for all these cases:
 *   - regular search path
 *   - a filter key which is a search path
 * TODO: Test invalid nested target element value
 * Created by QuijadaJ on 5/5/2017.
 */
public class ContextTest {
    private static final String searchPath1 = "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.ORG_ID.REGN_NBR_ENTR.REGN_NBR_CD";
    private static final String searchPath2 = "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[0]";
    private static final String searchPath3 = "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[0].NME_ENTR_VW";
    private static final String searchPath4 = "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.MTCH_RSLT.CAND_REF";
    private static final String searchPath5 = "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR.NME_ENTR_VW";
    private static final String searchPath6 = "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG";
    private static final String searchPath7 = "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR";
    private static final String searchPath8 = "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[1].NME_ENTR_VW";
    private static final String searchPathStandardizedMailAddrPostCode
            = "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=13135||ADR_ENTR_VW.POST_CODE";

    private static final String verOrgXml = TestData.verOrgXml;
    private static final String jsonStr = TestUtils.convertXmlToJson(verOrgXml);
    private static final Context context = ContextFactory.INSTANCE.obtainContext(jsonStr);


    @Test
    public void obtainJsonContext() {
        Context c = ContextFactory.INSTANCE.obtainContext(jsonStr);
        assertTrue(c instanceof JsonContext);
    }

    @Test
    public void canSearch() {
        String key = "REGN_NBR_CD";
        SearchPath sp = SearchPath.valueOf(searchPath1);
        SearchResult searchRes = context.findElement(sp, null, null, null);
        assertTrue(searchRes.containsKey(key) && "15336".equals(searchRes.get(key).stringRepresentation()));
    }


    /**
     * Test capability to search for a specific entry of an array when the array is found in the last node of the
     * search path. We test selecting array entries individually, and also selecting entire array, and in each instance
     * then check the expected results are the only ones found.
     */
    @Test
    public void canSearchWithArrayElementInSearchPath() {
        String key = "NME_ENTR";

        /*
         * Search for array entry "1", and examine results
         */
        SearchPath sp = SearchPath.valueOf("VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[0]");
        SearchResult sr = context.findElement(sp, null, null, null);
        assertTrue(sr.containsKey(key));
        assertTrue(sr.get(key).isRecursible());
        assertTrue(!sr.get(key).memberValue("NME_ENTR_VW").containsElement("STDN_APPL_CD"));

        /*
         * Search for array entry "2", and examine results
         */
        sp = SearchPath.valueOf("VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[1]");
        sr = context.findElement(sp, null, null, null);
        assertTrue(sr.containsKey(key));
        assertTrue(sr.get(key).isRecursible());
        assertTrue("24099".equals(sr.get(key).memberValue("NME_ENTR_VW").memberValue("STDN_APPL_CD").stringRepresentation()));

        /*
         * Search for array entry "3", and examine results
         */
        sp = SearchPath.valueOf("VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[2]");
        sr = context.findElement(sp, null, null, null);
        assertTrue(sr.containsKey(key));
        assertTrue(sr.get(key).isRecursible());
        assertTrue("13135".equals(sr.get(key).memberValue("NME_ENTR_VW").memberValue("STDN_APPL_CD").stringRepresentation()));

        /*
         * Search for all array entries, and examine results
         */
        sp = SearchPath.valueOf("VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR");
        sr = context.findElement(sp, null, null, null);
        assertTrue(sr.containsKey(key));
        assertTrue(sr.get(key).isArray());
        assertTrue(sr.get(key).asArray().size() == 3);
    }

    @Test
    public void canSearchWithArrayElementInSearchPathThatIsNotLastNode() {
        String key = "NME_ENTR_VW";
        SearchPath sp = SearchPath.valueOf(searchPath3);
        SearchResult searchRes = context.findElement(sp, null, null, null);
        assertTrue(searchRes.containsKey(key));
    }

    /**
     * For nodes that are not the last one in the search path, tests that they contain square brackets if
     * an array is expected in the JSON when that node is encountered. The reason for this check is that
     * even though currently only getting the first array entry is supported ("[0]"), in future might add support to
     * get something other than the first array entry.
     */
    @Test(expected = IllegalArgumentException.class)
    public void nonFinalNodeWithoutBracketsThrowsExceptionWhenArrayEncountered() {
        SearchPath sp = SearchPath.valueOf(searchPath5);
        context.findElement(sp, null, null, null);
    }


    @Test
    public void canSearchUsingFilter() {
        String key = "CAND_REF";
        String filterFld1 = "CAND_RNK";
        String filterVal1 = "1";
        String filterFld2 = "REGN_STAT_CD";
        String filterVal2 = "15201";
        Filter f = Filter.valueOf("CAND_RNK=1;REGN_STAT_CD=15201");

        SearchPath sp = SearchPath.valueOf(searchPath4);
        SearchResult sr = context.findElement(sp, f, null, null);

        assertTrue(sr.containsKey(key));
        Context elem = sr.get(key);
        Context aryEnt = elem.entryFromArray(0);
        assertEquals(aryEnt.memberValue(filterFld1).toString(), filterVal1);
        assertEquals(aryEnt.memberValue(filterFld2).toString(), filterVal2);
    }


    /**
     * Test scenario where the search involves filtering on a field which is one or more level
     * deeper than the located node.
     */
    @Test
    public void canSearchUsingNestedFilter() {
        String data = "{\"node\": [\n" +
                "  {\"key\": \"val1\",\n" +
                "   \"nestedKey\": {\n" +
                "     \"nestedKeyVal\":[0,1,2,3,4]\n" +
                "   }\n" +
                "  },\n" +
                "  {\"key\": \"val2\"},\n" +
                "  {\"key\": \"val3\"}\n" +
                "]}";

        String key = "ADR_ENTR";
        String expectedElemName = "STD_STRG_VW";
        String expectedSubElemName = "STDN_APPL_CD";
        String filterVal1 = "13135";
        Filter f = Filter.valueOf("STD_STRG_VW.STDN_APPL_CD=13135");

        SearchPath sp = SearchPath.valueOf(searchPath7);
        SearchResult sr = context.findElement(sp, f, null, null);

        assertTrue(sr.containsKey(key));
        Context elem = sr.get(key);
        Context aryEnt = elem.entryFromArray(0);
        assertTrue(null != aryEnt && aryEnt.containsElement(expectedElemName));
        Context subCtx = aryEnt.memberValue(expectedElemName);
        assertTrue(null != subCtx && filterVal1.equals(subCtx.memberValue(expectedSubElemName).stringRepresentation()));

        f = Filter.valueOf("STD_STRG_VW.STDN_APPL_CD=12345");
        sr = context.findElement(sp, f, null, null);
        assertTrue(sr.isEmpty());


        /*
         * Test when the value found for a nested filter key is an array
         */
        Context c = ContextFactory.INSTANCE.obtainContext(data);
        sp = SearchPath.valueOf(c.startSearchPath().toString());
        f = Filter.valueOf("nestedKey.nestedKeyVal=4");
        sr = c.findElement(sp, f, null, null);
        assertTrue(sr.size() == 1);
        Context foundCtx = sr.get(c.startSearchPath().toString());
        assertTrue(foundCtx.isArray());
        assertTrue(foundCtx.asArray().size() == 1);
        assertTrue(foundCtx.asArray().get(0).memberValue("key").stringRepresentation().equals("val1"));
    }


    /**
     * Test scenario where the search involves selecting from found node a field
     * which is one or more levels deeper than found node.
     */
    @Test
    public void canSelectFieldDeeperThanFoundNode() {
        SelectionCriteria sc = SelectionCriteria.valueOf(searchPathStandardizedMailAddrPostCode);
        SearchResult sr = context.findElement(sc, null);
        assertTrue(sr.size() == 1);
        assertEquals(sr.get("POST_CODE").stringRepresentation(), "11236");
    }


    /**
     * For a node in the JSON, select only a sub-set of the elements contained therein. This can be useful for instance
     * when a rule is to be applied only to certain elements within an entity. Example: apply rule only to
     * city and state fields of mailing address of a business entity.
     */
    @Test
    public void selectSubsetOfElementsInFoundNode() {
        String key = "VERORG_MSG";

        String elemName1 = "ORG_ID";
        String elemName2 = "BUS_ADR";
        TargetElements t = TargetElements.valueOf("ORG_ID,BUS_ADR");

        SearchPath sp = SearchPath.valueOf(searchPath6);
        SearchResult sr = context.findElement(sp, null, t, null);
        Context elem = sr.get(key);
        assertNotNull(elem.memberValue(elemName1));
        assertNotNull(elem.memberValue(elemName2));
        assertEquals(2, elem.entrySet().size());
    }


    /**
     * Ensure a {@link SelectionCriteria} string is correctly parsed, and search results are correct.
     */
    @Test
    public void searchUsingSelectionCriteriaObject() {
        SelectionCriteria sc = SelectionCriteria.valueOf("VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.MTCH_RSLT.CAND_REF"
                + SelectionCriteria.SEARCH_CRITERIA_DELIM + "CAND_RNK=1;REGN_STAT_CD=15200"
                + SelectionCriteria.SEARCH_CRITERIA_DELIM + "CFDC_LVL_VAL");
        SearchResult sr = context.findElement(sc, null);
        assertTrue("4".equals(sr.get("CFDC_LVL_VAL").stringRepresentation()));
    }


    /**
     * Tests that the correct array entry is selected when the search path contains an array entry anywhere
     * that is *not* the final node. Final node array test will be added later elsewhere.
     */
    @Test
    public void selectCorrectArrayEntryInSearchPath() {
        String elemName1 = "STDN_APPL_CD";
        TargetElements t = TargetElements.valueOf("STDN_APPL_CD");
        SearchPath sp = SearchPath.valueOf(searchPath8);
        SearchResult sr = context.findElement(sp, null, t, null);
        assertTrue(sr.size() == 1);
        assertTrue(sr.containsKey(elemName1));
        assertTrue(sr.get(elemName1).stringRepresentation().equals("24099"));
    }

    @Test
    public void searchReturnsArray() {
        String key = "CAND_REF";

        SearchPath sp = SearchPath.valueOf(searchPath4);
        SearchResult sr = context.findElement(sp, null, null, null);

        assertTrue(sr.containsKey(key));
        assertTrue(sr.get(key).isArray());
        assertTrue(sr.get(key).asArray().size() == 8);
    }


    /**
     * Test scenario when we're supposed to filter on an element which is an array of other complex structures,
     * and the filter contains a name/value pair that corresponds to a member that's an array of primitives
     * in the complex structures inside the found element array of complex structures. In such cases {@link Context}
     * should be smart enough to compare the passed in a value to the entries contained in the actual array, and if
     * there's a match, the complex structure node that contains such an array should be included in the search results.
     */
    @Test
    public void filterNodeThatHasArrayMember() {
        String elemName = "MTCH_GRD_CMPT";
        String data = "{  \n" +
                "   \"CFDC_LVL_VAL\":4,\n" +
                "   \"MTCH_BASS_TEXT\":\"0000000000989800000000989898\",\n" +
                "   \"MTCH_GRD_CMPT\":[  \n" +
                "      {  \n" +
                "         \"MTCH_GRD_CMPT_RAT\":\"B\",\n" +
                "         \"DSPL_SEQ_NBR\":1,\n" +
                "         \"MTCH_GRD_CMPT_SCR\":37,\n" +
                "         \"MTCH_GRD_CMPT_TYP_CD\":12176,\n" +
                "         \"ary\": [1,2,3,4,5]" +
                "      },\n" +
                "      {  \n" +
                "         \"MTCH_GRD_CMPT_RAT\":\"F\",\n" +
                "         \"DSPL_SEQ_NBR\":2,\n" +
                "         \"MTCH_GRD_CMPT_SCR\":0,\n" +
                "         \"MTCH_GRD_CMPT_TYP_CD\":12179,\n" +
                "         \"ary\": [1,2,3,4,5]" +
                "      },\n" +
                "      {  \n" +
                "         \"MTCH_GRD_CMPT_RAT\":\"F\",\n" +
                "         \"DSPL_SEQ_NBR\":3,\n" +
                "         \"MTCH_GRD_CMPT_SCR\":0,\n" +
                "         \"MTCH_GRD_CMPT_TYP_CD\":12177,\n" +
                "         \"ary\": [11,12,13,14,15]" +
                "      },\n" +
                "      {  \n" +
                "         \"MTCH_GRD_CMPT_RAT\":\"A\",\n" +
                "         \"DSPL_SEQ_NBR\":4,\n" +
                "         \"MTCH_GRD_CMPT_SCR\":100,\n" +
                "         \"MTCH_GRD_CMPT_TYP_CD\":12186,\n" +
                "         \"ary\": [16,17,18,19,20]" +
                "      },\n" +
                "      {  \n" +
                "         \"MTCH_GRD_CMPT_RAT\":\"A\",\n" +
                "         \"DSPL_SEQ_NBR\":5,\n" +
                "         \"MTCH_GRD_CMPT_SCR\":100,\n" +
                "         \"MTCH_GRD_CMPT_TYP_CD\":12187,\n" +
                "         \"ary\": [21,22,23,24,25]" +
                "      },\n" +
                "      {  \n" +
                "         \"MTCH_GRD_CMPT_RAT\":\"Z\",\n" +
                "         \"DSPL_SEQ_NBR\":6,\n" +
                "         \"MTCH_GRD_CMPT_SCR\":-1,\n" +
                "         \"MTCH_GRD_CMPT_TYP_CD\":12189,\n" +
                "         \"ary\": [26,27,28,29,30]" +
                "      },\n" +
                "      {  \n" +
                "         \"MTCH_GRD_CMPT_RAT\":\"Z\",\n" +
                "         \"DSPL_SEQ_NBR\":7,\n" +
                "         \"MTCH_GRD_CMPT_SCR\":-1,\n" +
                "         \"MTCH_GRD_CMPT_TYP_CD\":12190,\n" +
                "         \"ary\": [31,32,33,34,35]" +
                "      },\n" +
                "      {  \n" +
                "         \"MTCH_GRD_CMPT_RAT\":\"B\",\n" +
                "         \"DSPL_SEQ_NBR\":8,\n" +
                "         \"MTCH_GRD_CMPT_SCR\":80,\n" +
                "         \"MTCH_GRD_CMPT_TYP_CD\":12227,\n" +
                "         \"ary\": [36,37,38,39,40]" +
                "      },\n" +
                "      {  \n" +
                "         \"MTCH_GRD_CMPT_RAT\":\"F\",\n" +
                "         \"DSPL_SEQ_NBR\":9,\n" +
                "         \"MTCH_GRD_CMPT_SCR\":0,\n" +
                "         \"MTCH_GRD_CMPT_TYP_CD\":12230,\n" +
                "         \"ary\": [41,42,43,44,45]" +
                "      },\n" +
                "      {  \n" +
                "         \"MTCH_GRD_CMPT_RAT\":\"Z\",\n" +
                "         \"DSPL_SEQ_NBR\":10,\n" +
                "         \"MTCH_GRD_CMPT_SCR\":-1,\n" +
                "         \"MTCH_GRD_CMPT_TYP_CD\":12229,\n" +
                "         \"ary\": [46,47,48,49,50]" +
                "      },\n" +
                "      {  \n" +
                "         \"MTCH_GRD_CMPT_RAT\":\"F\",\n" +
                "         \"DSPL_SEQ_NBR\":11,\n" +
                "         \"MTCH_GRD_CMPT_SCR\":0,\n" +
                "         \"MTCH_GRD_CMPT_TYP_CD\":12228,\n" +
                "         \"ary\": [51,52,53,54,55]" +
                "      }\n" +
                "   ],\n" +
                "   \"INDS_CODE\":{  \n" +
                "      \"STD_INDS_CODE\":99992222,\n" +
                "      \"DSPL_SEQ_NBR\":50,\n" +
                "      \"INDS_CODE_TYPE_CD\":15361\n" +
                "   },\n" +
                "   \"DUNS_NBR\":33538330,\n" +
                "   \"CAND_RNK\":1,\n" +
                "   \"REGN_STAT_CD\":15200,\n" +
                "   \"MTCH_GRD_TEXT\":\"BFFAAZZBFZF\",\n" +
                "   \"CONF_LVL\":\"L\"\n" +
                "}";
        SearchPath sp = SearchPath.valueOf(elemName);
        Filter f = Filter.valueOf("ary=55");
        Context c = ContextFactory.INSTANCE.obtainContext(data);
        SearchResult sr = c.findElement(sp, f, null, null);
        List<Context> resList = sr.get(elemName).asArray();
        assertTrue(resList.size() == 1);
        assertTrue(resList.get(0).memberValue("MTCH_GRD_CMPT_RAT").stringRepresentation().equals("F"));
        assertTrue(resList.get(0).memberValue("DSPL_SEQ_NBR").stringRepresentation().equals("11"));
        assertTrue(resList.get(0).memberValue("MTCH_GRD_CMPT_SCR").stringRepresentation().equals("0"));
        assertTrue(resList.get(0).memberValue("MTCH_GRD_CMPT_TYP_CD").stringRepresentation().equals("12228"));
        assertTrue(resList.get(0).memberValue("ary").arrayContains("55"));
    }


    /**
     * When the search path specifies an element which value is an array of primitives, test that the correct
     * array entry is returned as per the filter argument passed by caller
     */
    @Test
    public void filterArrayOfPrimitives() {
        String topElemName = "node";
        String elemNameToFind = "key4";
        String data = "{\"node\": "
                + "{\"key1\": \"val1\","
                + "\"key2\": \"val2\","
                + "\"key3\": \"val3\","
                + "\"key4\": [\"ent1\", \"ent2\", \"ent3\", \"ent4\"]"
                + "}"
                + "}";
        SearchPath sp = SearchPath.valueOf(topElemName + "." + elemNameToFind);
        Filter f = Filter.valueOf(elemNameToFind + "=ent4");
        Context c = ContextFactory.INSTANCE.obtainContext(data);
        SearchResult sr = c.findElement(sp, f, null, null);
        Context foundElemVal = sr.get(elemNameToFind);
        // Assume the result is an array like it should be
        assertTrue(foundElemVal.asArray().size() == 1);
        Context aryEnt = foundElemVal.entryFromArray(0);
        assertTrue(aryEnt.stringRepresentation().equals("ent4"));

        // Try case where empty results expected
        f = Filter.valueOf(elemNameToFind + "=ent5");
        sr = c.findElement(sp, f, null, null);
        assertTrue(sr.isEmpty());
    }


    /**
     *
     */
    @Test
    public void filterUsingWildCard() {
        /*
         * Test filter when the value found is a primitive
         */
        String dataPrim = "{\"prim\": \"primVal\"}";
        SearchPath sp = SearchPath.valueOf("prim");
        Filter f = Filter.valueOf("prim=primVal");
        Context c = ContextFactory.INSTANCE.obtainContext(dataPrim);
        SearchResult sr = c.findElement(sp, f, null, null);
        assertTrue(sr.get("prim").stringRepresentation().equals("primVal"));
        // Try no result found
        f = Filter.valueOf("prim=prim");
        sr = c.findElement(sp, f, null, null);
        assertTrue(sr.isEmpty());

        /*
         * Data is an array of rows (I.e. array of other complex structures)
         */
        String data = "{\"node\": ["
                + "{\"key\": \"Microsoft\"},"
                + "{\"key\": \"Google\"},"
                + "{\"key\": \"Oracle Corporation\"},"
                + "{\"key\": [\"Facebook\", \"Testsoft\" , \"Time Warner\", \"Verizon Wireless\", \"Symantec\"]}"
                + "]}";

        // Test anchoring partial match at end
        String topElemName = "node";
        sp = SearchPath.valueOf(topElemName);
        f = Filter.valueOf("key=*soft");
        c = ContextFactory.INSTANCE.obtainContext(data);
        sr = c.findElement(sp, f, null, null);
        assertTrue(sr.size() == 1);
        assertTrue(sr.get(topElemName).asArray().get(0).memberValue("key").stringRepresentation().equals("Microsoft"));

        /*
         * Test anchoring partial match at beginning; in addition also tests filtering array of complex data structures
         * on a member which value is an array of primitives.
         */

        f = Filter.valueOf("key=Verizon*");
        sr = c.findElement(sp, f, null, null);
        assertTrue(sr.size() == 1);
        assertTrue(sr.get(topElemName).asArray().get(0).memberValue("key").arrayContains("Verizon Wireless"));

        // Test matching anywhere in the string
        f = Filter.valueOf("key=*cle Cor*");
        sr = c.findElement(sp, f, null, null);
        assertTrue(sr.size() == 1);
        assertTrue(sr.get(topElemName).asArray().get(0).memberValue("key").stringRepresentation().equals("Oracle Corporation"));
    }


    /**
     * Test scenario where values in the target data should themselves be treated as regular expressions
     * when evaluating search filters.
     */
    @Test
    public void searchWhenTargetFieldsAreRegularExpressions() {
        /*
         * Data is an array of rows (I.e. array of other complex structures)
         * Note: Escaping the "\s" like that is necessary, otherwise GSON API builds empty ("{}") object.
         */
        String data = "{\"node\": ["
                + "{\"key\": \"Micro.*\"},"
                + "{\"key\": \".*gle\"},"
                + "{\"key\": \"Oracle.*tion\"},"
                + "{\"key\": [\".acebook\", \"Testsoft\" , \"Time..arner\", \"Verizon\\\\s{2}Wireless\", \"Symantec\"]},"
                + "{\"key\": \"8|9|10\"}"
                + "]}";
        // Test anchoring partial match at end
        Map<String, String> params = new HashMap<>();
        params.put(AbstractContext.FOUND_ELEM_VAL_IS_REGEX, "1");
        String topElemName = "node";
        SearchPath sp = SearchPath.valueOf(topElemName);
        Filter f = Filter.valueOf("key=Micro");
        Context c = ContextFactory.INSTANCE.obtainContext(data);
        SearchResult sr = c.findElement(sp, f, null, params);
        assertTrue(sr.size() == 1);
        assertTrue(sr.get(topElemName).asArray().get(0).memberValue("key").stringRepresentation().equals("Micro.*"));

        f = Filter.valueOf("key=Facebook");
        sr = c.findElement(sp, f, null, params);
        assertTrue(sr.size() == 1);
        Context foundCtx = sr.get(topElemName).asArray().get(0).memberValue("key");
        assertTrue(foundCtx.isArray());
        Collection<Context> coll = foundCtx.asArray();
        assertTrue(coll.stream().filter(e -> e.stringRepresentation().equals(".acebook")).findAny().isPresent());

        f = Filter.valueOf("key=Google");
        sr = c.findElement(sp, f, null, params);
        assertTrue(sr.size() == 1);
        assertTrue(sr.get(topElemName).asArray().get(0).memberValue("key").stringRepresentation().equals(".*gle"));


        /*
         * Testing regular expressions int he form "x|y|z"
         */
        f = Filter.valueOf("key=10");
        sr = c.findElement(sp, f, null, params);
        assertTrue(sr.size() == 1);
        assertTrue(sr.get(topElemName).asArray().get(0).memberValue("key").stringRepresentation().equals("8|9|10"));

        /*
         * Search for "Verizon  Wireless", notice two spaces
         */
        f = Filter.valueOf("key=Verizon  Wireless");
        sr = c.findElement(sp, f, null, params);
        assertTrue(sr.size() == 1);
        foundCtx = sr.get(topElemName).asArray().get(0).memberValue("key");
        assertTrue(foundCtx.isArray());
        coll = foundCtx.asArray();
        assertTrue(coll.stream().filter(e -> e.stringRepresentation().equals("Verizon\\s{2}Wireless")).findAny().isPresent());

        /*
         * Test negative case scenario (no results found)
         */
        f = Filter.valueOf("key=Verizon Wireless");
        sr = c.findElement(sp, f, null, params);
        assertTrue(sr.size() == 0);

        f = Filter.valueOf("key=Verizon");
        sr = c.findElement(sp, f, null, params);
        assertTrue(sr.size() == 0);

        /*
         * Now try matching the regular expression on any region of passed in search term. The first attempt
         * will not yield results, then we try again with partial regex match flag "turned on"
         */
        f = Filter.valueOf("key=Company Verizon  Wireless");
        sr = c.findElement(sp, f, null, params);
        assertTrue(sr.size() == 0);

        params.put(AbstractContext.PARTIAL_REGEX_MATCH, "1");
        f = Filter.valueOf("key=Company Verizon  Wireless");
        sr = c.findElement(sp, f, null, params);
        assertTrue(sr.size() == 1);
        foundCtx = sr.get(topElemName).asArray().get(0).memberValue("key");
        assertTrue(foundCtx.isArray());
        coll = foundCtx.asArray();
        assertTrue(coll.stream().filter(e -> e.stringRepresentation().equals("Verizon\\s{2}Wireless")).
                findAny().isPresent());
    }

    /**
     * Test filter contains a range of values
     */
    @Test
    public void filterContainsList() {
        String data = "{\"node\": ["
                + "{\"key\": \"Microsoft\"},"
                + "{\"key\": \"Google\"},"
                + "{\"key\": \"Oracle\"}"
                + "]}";
        Filter f = Filter.valueOf("key=Google,Oracle");
        Context c = ContextFactory.INSTANCE.obtainContext(data);
        String topElem = c.startSearchPath().toString();
        SearchPath sp = SearchPath.valueOf(c.startSearchPath().toString());
        SearchResult sr = c.findElement(sp, f, null, null);
        Context foundCtx = sr.get(topElem);
        assertTrue(foundCtx.isArray());
        Collection<Context> coll = foundCtx.asArray();
        assertTrue(coll.size() == 2);
        assertTrue(coll.stream().map(e -> e.memberValue("key").stringRepresentation()).
                filter(e -> e.equals("Oracle") || e.equals("Google")).collect(Collectors.toList()).size() == 2);
    }


    /**
     * Check that valid filter validation is taking place. We pass an filter which we know is not valid
     * for the expected found element, and expect an {@link IllegalArgumentException} to be thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void invalidFilterThrowsException() {
        String data = "{\"node\": [\n" +
                "  {\"key\": \"val1\",\n" +
                "   \"nestedKey\": {\n" +
                "     \"nestedKeyVal\":[0,1,2,3,4]\n" +
                "   }\n" +
                "  },\n" +
                "  {\"key\": \"val2\"},\n" +
                "  {\"key\": \"val3\"}\n" +
                "]}";
        Context c = ContextFactory.INSTANCE.obtainContext(data);
        SearchPath sp = SearchPath.valueOf(c.startSearchPath().toString());
        Filter f = Filter.valueOf("foo=bar");
        c.findElement(sp, f, null, null);
    }


    @Test
    public void searchResultIsComplexObject() {

    }

    @Test
    public void canProduceContextFromXml() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "  <!DOCTYPE pricing-model  SYSTEM\n" +
                "      \"dynamosystemresource:/atg/dtds/pmdl/pmdl_1.0.dtd\">\n" +
                "    <pricing-model>\n" +
                "      <qualifier/>\n" +
                "      <offer>\n" +
                "        <discount-structure calculator-type=\"group\"\n" +
                "                discount-type=\"${discount_type_value}\"\n" +
                "                adjuster=\"${discount_value}\">\n" +
                "          <target>\n" +
                "            <anded-union>\n" +
                "              <group-iterator name=\"next\" number=\"1\" sort-by=\"priceInfo.listPrice\"\n" +
                "                  sort-order=\"descending\">\n" +
                "                <collection-name>items</collection-name>\n" +
                "                <element-name>item</element-name>\n" +
                "                <aggregator name=\"quantity\" operation=\"total\"/>\n" +
                "                  <equals><value>item.catalogRefId</value>\n" +
                "                    <constant>\n" +
                "                      <data-type>java.lang.String</data-type>\n" +
                "                      <string-value>sku40105</string-value>\n" +
                "                    </constant>\n" +
                "                  </equals>\n" +
                "              </group-iterator>\n" +
                "              <group-iterator name=\"next\" number=\"1\" sort-by=\"priceInfo.listPrice\"\n" +
                "                  sort-order=\"descending\">\n" +
                "                <collection-name>items</collection-name>\n" +
                "                <element-name>item</element-name>\n" +
                "                <aggregator name=\"quantity\" operation=\"total\"/>\n" +
                "                  <equals><value>item.catalogRefId</value>\n" +
                "                    <constant>\n" +
                "                      <data-type>java.lang.String</data-type>\n" +
                "                      <string-value>sku40109</string-value>\n" +
                "                    </constant>\n" +
                "                  </equals>\n" +
                "              </group-iterator>\n" +
                "            </anded-union>\n" +
                "          </target>\n" +
                "        </discount-structure>\n" +
                "      </offer>\n" +
                "    </pricing-model>";

        Context c = ContextFactory.INSTANCE.obtainContext(xml);
        System.out.println(c.stringRepresentation());
    }


    /**
     * Simple test to get {@code Context} object primitive values.
     */
    @Test
    public void produceContextForPrimitive() {
        /**
         * For some reason the Google JSON API will fail to create a JsonElement if
         * the primitive contains spaces, hence the reason for the __SPACE__ use below. Calling
         * code ust be aware of this and handle accordingly.
         */
        String primVal = "a primitive  value";
        primVal = primVal.replaceAll(" ", "__SPACE__");
        Context c = ContextFactory.INSTANCE.obtainContext(primVal);
        assertEquals(primVal, c.stringRepresentation());


        primVal = "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.MTCH_RSLT.CAND_REF||CAND_RNK=1;REGN_STAT_CD=15200||CFDC_LVL_VAL";
        primVal = EscapeUtil.escapeSpecialCharacters(primVal);
        c = ContextFactory.INSTANCE.obtainContext(primVal);
        assertEquals(primVal, c.stringRepresentation());

        // This one includes square brackets
        primVal = "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[0].NME_ENTR_VW.NME_TEXT__DP__null__DP__NULL";
        primVal = EscapeUtil.escapeSpecialCharacters(primVal);
        c = ContextFactory.INSTANCE.obtainContext(primVal);
        assertEquals(primVal, c.stringRepresentation());

        primVal = "^\\w$";
        primVal = EscapeUtil.escapeSpecialCharacters(primVal);
        c = ContextFactory.INSTANCE.obtainContext(primVal);
        assertEquals(primVal, c.stringRepresentation());
    }




    /**
     * Tests that client cannot break the invariants of {@link Context}, which is supposed
     * to be immutable. Thew test consists of creating a {@link JsonElement}, then using it
     * to generate a {@link Context}, then updating the {@link JsonElement} that was used
     * to create the {@link Context}, and finally check that {@link Context} object remained unchanged
     * throughout.
     */
    @Test
    public void contextObjectIsImmutable() {
        String data = "{\"node\": [\n" +
                "  {\"key\": \"val1\",\n" +
                "   \"nestedKey\": {\n" +
                "     \"nestedKeyVal\":[0,1,2,3,4]\n" +
                "   }\n" +
                "  },\n" +
                "  {\"key\": \"val2\"},\n" +
                "  {\"key\": \"val3\"}\n" +
                "]}";

        /*
         * First create a Context object using a JsonElement object
         */
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(data);
        Context c = ContextFactory.INSTANCE.obtainContext(je);
        String beforeChanges = c.stringRepresentation();

        /*
         * Now modify the original JsonElement which was used to created to Context
         */
        String data2 = "{\"someAry\": [1,2,3,4,5]}";
        je.getAsJsonObject().add("node2", jp.parse(data2));
        String afterChanges = c.stringRepresentation();

        /*
         * The Context object, being instantiated form immutable class that it claims to be,
         * should remain unchanged
         */
        assertEquals(beforeChanges, afterChanges);
    }


    /**
     * Checks that various invalid {@code SearchPath} related checks are
     * working as expected. This includes:
     * 1) Check that error is thrown if an array is found in a non-final search path node, yet it was not specified
     *    in the search path that an array node can be expected there.
     * 2) Check that {@link IncompatibleSearchPathException} gets thrown when a bad search path is specified in these
     *    {@link Context#findElement(SearchPath, Filter, TargetElements, Map)} arguments:
     *    a) search path
     *    b) Filter
     *    c) Target elements
     *    We also test that above get ignored when appropriate flags get passed in the extra parameters
     *
     * Note: This is to test features that deal with JSON produced from XML, and sometimes the same field can appear as a single
     * object, or an array of multiple objects, depending respectively on whether source XML contained single or multiple entries. It'd
     * be nice for it always to be an array if it is array, even if only one single element found, but I suspect you'd need a JSON schema for that. So the workaround is to
     * instruct API. The way this feature works is that by passing a flag in extra params, you can tell Context API to ignore error
     * if a non-array is encountered in non final node.
     */
    @Test
    public void invalidSearchPathValidations() {

        String data = "{\"BUS_ADR\":{  \n" +
                "                  \"ADR_ENTR\":[  \n" +
                "                     {  \n" +
                "                        \"ADR_ENTR_VW\":{  \n" +
                "                           \"LANG_CD\":331,\n" +
                "                           \"TERR\":\"FL\",\n" +
                "                           \"POST_CODE\":325791319,\n" +
                "                           \"ADR_LINE\":{  \n" +
                "                              \"DSPL_SEQ_NBR\":1,\n" +
                "                              \"TEXT\":\"1 11TH AVE STE D3\"\n" +
                "                           },\n" +
                "                           \"POST_TOWN\":\"SHALIMAR\",\n" +
                "                           \"GEO_REF_ID\":1073\n" +
                "                        },\n" +
                "                        \"ADR_USG_CD\":1114,\n" +
                "                        \"DNB_DATA_PRVD_CD\":20064\n" +
                "                     },\n" +
                "                     {  \n" +
                "                        \"STD_STRG_VW\":{  \n" +
                "                           \"LANG_CD\":331,\n" +
                "                           \"STDN_APPL_CD\":13135,\n" +
                "                           \"STD_RGN_LINE\":{  \n" +
                "                              \"DSPL_SEQ_NBR\":1,\n" +
                "                              \"TEXT\":\"SHALIMAR FL  32579-1319\"\n" +
                "                           },\n" +
                "                           \"STD_STR_LINE\":[  \n" +
                "                              {  \n" +
                "                                 \"DSPL_SEQ_NBR\":1,\n" +
                "                                 \"TEXT\":\"1 11TH AVENUE\"\n" +
                "                              },\n" +
                "                              {  \n" +
                "                                 \"DSPL_SEQ_NBR\":2,\n" +
                "                                 \"TEXT\":\"STE D3\"\n" +
                "                              }\n" +
                "                           ]\n" +
                "                        },\n" +
                "                        \"ADR_ENTR_VW\":{  \n" +
                "                           \"STDN_APPL_CD\":13135,\n" +
                "                           \"LANG_CD\":331,\n" +
                "                           \"STR_NME\":\"11TH\",\n" +
                "                           \"TERR\":\"FL\",\n" +
                "                           \"POST_CODE_EXTN\":1319,\n" +
                "                           \"POST_CODE\":32579,\n" +
                "                           \"STR_NBR\":1,\n" +
                "                           \"POST_TOWN\":\"SHALIMAR\",\n" +
                "                           \"GEO_REF_ID\":1073,\n" +
                "                           \"STR_TYP_TEXT\":\"AVE\"\n" +
                "                        },\n" +
                "                        \"ADR_USG_CD\":1114,\n" +
                "                        \"DNB_DATA_PRVD_CD\":20064\n" +
                "                     },\n" +
                "                     {  \n" +
                "                        \"STD_STRG_VW\":{  \n" +
                "                           \"LANG_CD\":331,\n" +
                "                           \"STDN_APPL_CD\":24099,\n" +
                "                           \"STD_RGN_LINE\":{  \n" +
                "                              \"DSPL_SEQ_NBR\":1,\n" +
                "                              \"TEXT\":\"SHALIMAR FL  32579-1319\"\n" +
                "                           },\n" +
                "                           \"STD_STR_LINE\":[  \n" +
                "                              {  \n" +
                "                                 \"DSPL_SEQ_NBR\":1,\n" +
                "                                 \"TEXT\":\"1 11TH AVENUE\"\n" +
                "                              },\n" +
                "                              {  \n" +
                "                                 \"DSPL_SEQ_NBR\":2,\n" +
                "                                 \"TEXT\":\"STE D3\"\n" +
                "                              }\n" +
                "                           ]\n" +
                "                        },\n" +
                "                        \"ADR_ENTR_VW\":{  \n" +
                "                           \"STDN_APPL_CD\":24099,\n" +
                "                           \"LANG_CD\":331,\n" +
                "                           \"STR_NME\":\"11TH\",\n" +
                "                           \"TERR\":\"FL\",\n" +
                "                           \"POST_CODE_EXTN\":1319,\n" +
                "                           \"MINR_TWN_NME\":\"SHALIMAR\",\n" +
                "                           \"POST_CODE\":32579,\n" +
                "                           \"STR_NBR\":1,\n" +
                "                           \"POST_TOWN\":\"SHALIMAR\",\n" +
                "                           \"GEO_REF_ID\":1073,\n" +
                "                           \"STR_TYP_TEXT\":\"AVE\",\n" +
                "                           \"CNTY\":\"OKALOOSA\"\n" +
                "                        },\n" +
                "                        \"ADR_USG_CD\":1114,\n" +
                "                        \"DNB_DATA_PRVD_CD\":20064\n" +
                "                     }\n" +
                "                  ]\n" +
                "               }}";



        /**
         * It should throw {@link UnexpectedArrayNodeException} if an array was not specified in a non-final node
         * of the given search path, yet one was found there
         */
        boolean error = false;
        Context c = ContextFactory.INSTANCE.obtainContext(data);
        SearchPath sp = SearchPath.valueOf("BUS_ADR.ADR_ENTR.ADR_ENTR_VW");
        try {
            c.findElement(sp, null, null, null);
        } catch (Exception e) {
            error = true;
            assertTrue(null != e.getCause() && e.getCause() instanceof UnexpectedArrayNodeException);
            UnexpectedArrayNodeException uane = (UnexpectedArrayNodeException) e.getCause();
            assertEquals("BUS_ADR.ADR_ENTR.ADR_ENTR_VW", uane.getSearchPath().toString());
            assertEquals("ADR_ENTR", uane.getNode());
        }
        assertTrue(error);
        error = false;

        // Same as above, but this time the bad path given in target elements
        SelectionCriteria sc =
                SelectionCriteria.valueOf("BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=24099||STD_STRG_VW.STD_STR_LINE.TEXT");
        try {
            c.findElement(sc, null);
        } catch (Exception e){
            error = true;
            assertTrue(null != e.getCause() && e.getCause() instanceof UnexpectedArrayNodeException);
            UnexpectedArrayNodeException uane = (UnexpectedArrayNodeException) e.getCause();
            assertEquals("STD_STRG_VW.STD_STR_LINE.TEXT", uane.getSearchPath().toString());
            assertEquals("STD_STR_LINE", uane.getNode());
        }

        assertTrue(error);
        error = false;

        /**
         * A bad search path given in the <code>SearchPath</code> argument of
         * {@link Context#findElement(SearchPath, Filter, TargetElements, Map)}
         */
        c = ContextFactory.INSTANCE.obtainContext(data);
        sp = SearchPath.valueOf("BUS_ADR.ADR_ENTR[0].BOGUS");
        try {
            c.findElement(sp, null, null, null);
        } catch (Exception e){
            error = true;
            assertTrue(null != e.getCause() && e.getCause() instanceof IncompatibleSearchPathException);
            IncompatibleSearchPathException ispe = (IncompatibleSearchPathException) e.getCause();
            assertEquals("BUS_ADR.ADR_ENTR[0].BOGUS", ispe.getSearchPath().toString());
        }
        assertTrue(error);
        error = false;

        // Try again but this time instruct API to ignore error
        Map<String, String> params = new HashMap<>();
        params.put(Context.IGNORE_INCOMPATIBLE_SEARCH_PATH_PROVIDED_ERROR, "1");
        SearchResult sr = c.findElement(sp, null, null, params);
        assertTrue(sr.isEmpty());

        /**
         * A bad search path given in the <code>Filter</code> argument of
         * {@link Context#findElement(SearchPath, Filter, TargetElements, Map)}
         */
        sp = SearchPath.valueOf("BUS_ADR.ADR_ENTR");
        Filter f = Filter.valueOf("ADR_USG_CD=1114;STD_STRG_VW.BOGUS=13135");
        try {
            c.findElement(sp, f, null, null);
        } catch (Exception e){
            error = true;
            assertTrue(null != e.getCause() && e.getCause() instanceof IncompatibleSearchPathException);
            IncompatibleSearchPathException ispe = (IncompatibleSearchPathException) e.getCause();
            assertEquals("STD_STRG_VW.BOGUS", ispe.getSearchPath().toString());
        }

        assertTrue(error);
        error = false;

        // Try again but this time instruct API to ignore error
        sr = c.findElement(sp, null, null, params);
        assertTrue(sr.containsKey("ADR_ENTR") && sr.get("ADR_ENTR").isArray() && sr.get("ADR_ENTR").asArray().size() == 3);

        /**
         * A bad search path given in the <code>TargetElements</code> argument of
         * {@link Context#findElement(SearchPath, Filter, TargetElements, Map)}
         */
        sp = SearchPath.valueOf("BUS_ADR.ADR_ENTR");
        f = Filter.valueOf("ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=13135");
        TargetElements te = TargetElements.valueOf("STD_STRG_VW.STD_RGN_LINE.BOGUS");
        try {
            c.findElement(sp, f, te, null);
        } catch (Exception e){
            error = true;
            assertTrue(null != e.getCause() && e.getCause() instanceof IncompatibleSearchPathException);
            IncompatibleSearchPathException ispe = (IncompatibleSearchPathException) e.getCause();
            assertEquals("STD_STRG_VW.STD_RGN_LINE.BOGUS", ispe.getSearchPath().toString());
        }
        assertTrue(error);
        error = false;
        // Try again but this time instruct API to ignore error
        sr = c.findElement(sp, null, null, params);
        assertTrue(sr.containsKey("ADR_ENTR") && sr.get("ADR_ENTR").isArray() && sr.get("ADR_ENTR").asArray().size() == 3);


        /**
         * Test scenario where a non-final array node gives an index which does not exist in the target
         * <code>Context</code> object being searched upon.
         */
        sc = SelectionCriteria.valueOf("BUS_ADR.ADR_ENTR||ADR_USG_CD=1114;STD_STRG_VW.STDN_APPL_CD=24099||STD_STRG_VW.STD_STR_LINE[2].TEXT");
        try {
            c.findElement(sc, null);
        } catch (Exception e) {
            error = true;
            assertTrue(null != e.getCause() && e.getCause() instanceof IncompatibleSearchPathException);
            IncompatibleSearchPathException ispe = (IncompatibleSearchPathException) e.getCause();
            assertEquals("STD_STRG_VW.STD_STR_LINE[2].TEXT", ispe.getSearchPath().toString());
            assertEquals("STD_STR_LINE[2]", ispe.getNode());
        }
        assertTrue(error);

        // Try again but this time instruct API to ignore error. Below flag needs to be given separately for
        // search paths given for target elements, which is what above did. This allows client more granular control.
        params.put(Context.IGNORE_INCOMPATIBLE_TARGET_ELEMENT_PROVIDED_ERROR, "1");
        sr = c.findElement(sc, params);
        assertTrue(sr.isEmpty());
    }
}
