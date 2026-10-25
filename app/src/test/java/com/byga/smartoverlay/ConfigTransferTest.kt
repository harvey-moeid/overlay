package com.byga.smartoverlay
import com.byga.smartoverlay.data.*
import com.byga.smartoverlay.profiles.AccountProfile
import com.byga.smartoverlay.workflow.*
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import org.json.JSONArray

class ConfigTransferTest {
    @Test fun exportNeverIncludesProviderOrChatOrScopeRecords(){val output=ConfigTransfer.export(listOf(Record("provider","p","secret"),Record("chat","c","private conversation"),Record("scope","s","approved"),Record("prompt","x","hello")));assertFalse(output.contains("secret"));assertFalse(output.contains("private conversation"));assertFalse(output.contains("approved"));assertEquals(1,JSONObject(output).getJSONArray("records").length())}
    @Test fun workflowImportHasFreshIdAndNoApproval(){val p=Plan("x",listOf(Step(Action.COPY_TEXT,"safe")));val original=Record("workflow","original-id",WorkflowCodec.encode(p));val imported=ConfigTransfer.parse(ConfigTransfer.export(listOf(original))).single();assertNotEquals(original.id,imported.id);assertEquals(p,WorkflowCodec.decode(imported.body))}
    @Test fun importedAccountMustBeReconfirmed(){val a=AccountProfile("old","com.instagram.android","personal","@me","USER_CONFIRMED",1234);val imported=ConfigTransfer.parse(ConfigTransfer.export(listOf(Record("account",a.id,a.encode())))).single();val result=AccountProfile.decode(imported);assertEquals("UNKNOWN",result.status);assertEquals(0L,result.confirmedAt)}
    @Test fun providerImportIsRejected(){try{ConfigTransfer.parse("""{"format":"byga-local-config","version":1,"records":[{"kind":"provider","body":"{}"}]}""");fail()}catch(_:IllegalArgumentException){}}
    @Test fun unknownImportVersionRejected(){try{ConfigTransfer.parse("""{"format":"byga-local-config","version":2,"records":[]}""");fail()}catch(_:IllegalArgumentException){}}
    @Test fun executableWorkflowImportRejected(){try{ConfigTransfer.parse(JSONObject().put("format","byga-local-config").put("version",1).put("records",JSONArray().put(JSONObject().put("kind","workflow").put("body","""{"name":"x","steps":[{"action":"SHELL","value":"arbitrary"}]}"""))).toString());fail()}catch(_:IllegalArgumentException){}}
    @Test fun importNeverReusesSourceIdentity(){val raw=ConfigTransfer.export(listOf(Record("prompt","same","hello")));assertNotEquals(ConfigTransfer.parse(raw).single().id,ConfigTransfer.parse(raw).single().id)}
    @Test fun exportDoesNotSilentlyTruncate(){try{ConfigTransfer.export(List(101){Record("prompt","$it","text")});fail()}catch(_:IllegalArgumentException){}}
    @Test fun malformedAppProfileIsRejected(){try{ConfigTransfer.parse(ConfigTransfer.export(listOf(Record("app","a","file:///private"))));fail()}catch(_:IllegalArgumentException){}}
}
