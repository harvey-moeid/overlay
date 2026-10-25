package com.byga.smartoverlay
import com.byga.smartoverlay.accessibility.ManualInputPermit
import org.junit.Assert.*
import org.junit.Test
class ManualInputTest{
    private fun permit(reviewed:Boolean=true,target:String="com.openai.chatgpt",text:String="draft")=ManualInputPermit(target,text,reviewed,1000)
    @Test fun humanReviewIsRequired(){assertFalse(permit(false).claim("com.openai.chatgpt",false,false,2000))}
    @Test fun unknownAppsCannotBePromotedByAProfile(){assertFalse(permit(target="com.bank.app").claim("com.bank.app",false,false,2000))}
    @Test fun changedForegroundDeniesInput(){assertFalse(permit().claim("com.instagram.android",false,false,2000))}
    @Test fun locksAndSensitiveFieldsDenyInput(){assertFalse(permit().claim("com.openai.chatgpt",true,false,2000));assertFalse(permit().claim("com.openai.chatgpt",false,true,2000))}
    @Test fun expiredAndFutureTokensAreDenied(){assertFalse(permit().claim("com.openai.chatgpt",false,false,11001));assertFalse(permit().claim("com.openai.chatgpt",false,false,999))}
    @Test fun permitIsSingleUse(){val p=permit();assertTrue(p.claim("com.openai.chatgpt",false,false,2000));assertFalse(p.claim("com.openai.chatgpt",false,false,2100))}
    @Test fun blankOrHugeTextIsRejected(){assertFalse(permit(text="").claim("com.openai.chatgpt",false,false,2000));assertFalse(permit(text="a".repeat(4001)).claim("com.openai.chatgpt",false,false,2000))}
    @Test fun changedFocusedFieldCannotReuseGrant(){assertFalse(permit().claim("com.openai.chatgpt",false,false,2000,"other-field"))}
}
