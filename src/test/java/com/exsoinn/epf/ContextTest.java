package com.exsoinn.epf;

import com.com.exsoinn.test.TestData;
import com.com.exsoinn.test.TestUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
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

    private static final String verOrgXml = TestData.verOrgXml;
    private static final String jsonStr = TestUtils.convertXmlToJson(verOrgXml);
    private static final Context context = ContextFactory.INSTANCE.obtainContext(jsonStr);


    @Test
    public void obtainJsonContext() {
        Context c = ContextFactory.INSTANCE.obtainContext(jsonStr);
        assertTrue(c instanceof JsonContext);
    }

    @Test
    public void canSearchJson() {
        String key = "REGN_NBR_CD";
        SearchPath sp = SearchPath.valueOf(searchPath1);
        SearchResult searchRes = context.findElement(sp, null, null, null);
        assertTrue(searchRes.containsKey(key) && "15336".equals(searchRes.get(key).stringRepresentation()));
    }


    @Test
    public void canSearchJsonWithArrayElementInSearchPath() {
        String key = "NME_ENTR";
        SearchPath sp = SearchPath.valueOf(searchPath2);
        SearchResult sr = context.findElement(sp, null, null, null);
        assertTrue(sr.containsKey(key));
    }

    @Test
    public void canSearchJsonWithArrayElementInSearchPathThatIsNotLastNode() {
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
        SearchResult sr = context.findElement(sp, null, null, null);
    }


    @Test
    public void canSearchJsonUsingFilter() {
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


    @Test
    public void canSearchJsonUsingNestedFilter() {
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
}
