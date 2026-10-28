// Offline DOM-contract checks. These fixtures do not prove compatibility with live account pages.
const {chromium}=require('playwright');
const fs=require('node:fs');
const assert=require('node:assert/strict');
const adapter=fs.readFileSync('app/src/main/assets/workflow-adapter.js','utf8');
(async()=>{
  const browser=await chromium.launch({executablePath:process.env.BYGA_CHROMIUM || '/usr/bin/chromium',headless:true,args:['--no-sandbox']});
  try {
    const context=await browser.newContext();
    await context.route('**/*',route=>route.fulfill({contentType:'text/html',body:'<html><body></body></html>'}));
    const page=await context.newPage();
    await page.goto('https://chatgpt.com/');
    let serial=0;
    async function invoke(action,value='',phase='begin',commandId=action) {
      const key='fixture'+serial++;
      await page.evaluate(adapter.replaceAll('BYGA_COMMAND',JSON.stringify(commandId)).replaceAll('BYGA_ACTION',JSON.stringify(action)).replaceAll('BYGA_PHASE',JSON.stringify(phase)).replaceAll('BYGA_RESULT',JSON.stringify(key)).replaceAll('BYGA_VALUE',JSON.stringify(value)));
      await page.waitForFunction(key=>window[key],key);
      return page.evaluate(key=>JSON.parse(window[key]),key);
    }
    assert.equal((await invoke('CHATGPT_SEND_PROMPT','x')).status,'wait');
    await page.setContent(`<div data-message-author-role="user">old prompt</div><div data-message-author-role="assistant"><div class="markdown">old answer</div><button aria-label="Copy">copy</button></div><textarea id="prompt-textarea"></textarea><button data-testid="send-button">Send</button>`);
    await page.evaluate(()=>{
      window.sends=0;
      document.querySelector('[data-testid="send-button"]').onclick=()=>{
        window.sends++;const turn=document.createElement('div');turn.dataset.messageAuthorRole='user';turn.textContent=document.querySelector('textarea').value;document.body.append(turn);
      };
    });
    const prompt='Line 1\n"quotes" `backticks` ${window.injected=true} BYGA_RESULT </script>';
    assert.equal((await invoke('CHATGPT_SEND_PROMPT',prompt)).status,'dispatched');
    assert.equal((await invoke('CHATGPT_SEND_PROMPT',prompt,'observe')).status,'done');
    assert.equal((await invoke('CHATGPT_SEND_PROMPT',prompt,'begin')).status,'done');
    assert.equal(await page.evaluate(()=>window.sends),1);
    assert.equal(await page.evaluate(()=>window.injected),undefined);
    assert.equal((await invoke('CHATGPT_COLLECT_RESULT')).status,'wait');
    await page.evaluate(()=>{
      const answer=document.createElement('div');answer.dataset.messageAuthorRole='assistant';answer.innerHTML='<div class="markdown">new answer</div><button aria-label="Copy">copy</button>';
      document.body.append(answer);const stop=document.createElement('button');stop.dataset.testid='stop-button';document.body.append(stop);
    });
    assert.equal((await invoke('CHATGPT_COLLECT_RESULT')).status,'wait');
    await page.locator('[data-testid="stop-button"]').evaluate(e=>e.remove());
    assert.equal((await invoke('CHATGPT_COLLECT_RESULT')).result,'new answer');
    await page.evaluate(()=>{
      const canvas=document.createElement('canvas');canvas.width=canvas.height=256;const image=new Image();image.src=canvas.toDataURL();document.querySelectorAll('[data-message-author-role="assistant"]')[1].append(image);
    });
    await page.waitForFunction(()=>document.querySelector('img').complete);
    assert.equal((await invoke('CHATGPT_COLLECT_RESULT')).kind,'image');
    assert.equal((await invoke('CHATGPT_COPY_RESULT')).result,'new answer');
    await page.goto('https://labs.google/fx/tools/flow');
    await page.setContent('<button>New project</button>');
    await page.evaluate(()=>{
      window.creates=0;
      document.querySelector('button').onclick=()=>{window.creates++;history.pushState({},'',location.pathname+'/project/123');document.body.insertAdjacentHTML('beforeend','<textarea placeholder="Describe video"></textarea>');};
    });
    assert.equal((await invoke('FLOW_CREATE_PROJECT')).status,'dispatched');
    assert.equal((await invoke('FLOW_CREATE_PROJECT','','observe')).status,'done');
    assert.equal((await invoke('FLOW_CREATE_PROJECT','CURRENT','begin','select-current')).status,'done');
    assert.equal(await page.evaluate(()=>window.creates),1);
    assert.equal((await invoke('FLOW_SET_PROMPT',prompt)).status,'done');
    assert.equal(await page.locator('textarea').inputValue(),prompt);
    assert.equal((await invoke('FLOW_UPLOAD_IMAGE')).status,'wait');
    await page.evaluate(()=>document.body.insertAdjacentHTML('beforeend','<input type="file" accept="image/*" hidden>'));
    assert.equal((await invoke('FLOW_UPLOAD_IMAGE')).status,'dispatched');
    assert.equal((await invoke('FLOW_UPLOAD_IMAGE','','observe')).status,'wait');
    assert.equal(await page.locator('input[type=file]').evaluate(e=>e.files.length),0);
    await page.evaluate(()=>{
      const input=document.querySelector('input[type="file"]');const data=new DataTransfer();data.items.add(new File(['fixture'],'image.png',{type:'image/png'}));input.files=data.files;input.dispatchEvent(new Event('change'));
      const canvas=document.createElement('canvas');canvas.width=canvas.height=100;const image=new Image();image.alt='Uploaded input image';image.src=canvas.toDataURL();document.body.append(image);
      document.body.insertAdjacentHTML('beforeend','<div role="progressbar">uploading</div>');
    });
    await page.waitForFunction(()=>document.querySelector('img').complete);
    assert.equal((await invoke('FLOW_UPLOAD_IMAGE','','observe')).status,'wait');
    await page.locator('[role="progressbar"]').evaluate(e=>e.remove());
    assert.equal((await invoke('FLOW_UPLOAD_IMAGE','','observe')).status,'done');
    await page.evaluate(()=>{
      document.body.insertAdjacentHTML('beforeend','<video width="100" height="100" src="https://labs.google/old.mp4"></video><button id="generate" disabled>Generate</button>');
      window.generates=0;document.querySelector('#generate').onclick=()=>{window.generates++;document.body.insertAdjacentHTML('beforeend','<div role="progressbar">Generating</div>');};
    });
    assert.equal((await invoke('FLOW_GENERATE')).status,'wait');
    await page.locator('#generate').evaluate(e=>e.disabled=false);
    assert.equal((await invoke('FLOW_GENERATE')).status,'dispatched');
    assert.equal((await invoke('FLOW_GENERATE','','observe')).status,'done');
    assert.equal((await invoke('FLOW_GENERATE','','begin')).status,'done');
    assert.equal(await page.evaluate(()=>window.generates),1);
    assert.equal((await invoke('FLOW_DOWNLOAD_VIDEO')).status,'wait');
    await page.evaluate(()=>{
      const video=document.createElement('video');video.width=100;video.height=100;video.src='https://labs.google/new.mp4';
      Object.defineProperty(video,'readyState',{get:()=>2});Object.defineProperty(video,'error',{get:()=>null});document.body.append(video);
    });
    assert.equal((await invoke('FLOW_DOWNLOAD_VIDEO')).result,'https://labs.google/new.mp4');
    await page.locator('[role=progressbar]').evaluateAll(es=>es.forEach(e=>e.remove()));
    await page.locator('#generate').evaluate(e=>e.insertAdjacentHTML('afterend','<button>Generate</button>'));
    assert.equal((await invoke('FLOW_GENERATE','','begin','ambiguous-generate')).status,'wait');
    assert.equal(await page.evaluate(()=>window.generates),1);
    await page.evaluate(()=>history.pushState({},'', '/fx/tools/flow/project/other'));
    assert.equal((await invoke('FLOW_SET_PROMPT','changed')).status,'error');
    await page.goto('https://evil.example/');
    assert.equal((await invoke('FLOW_GENERATE')).status,'error');
    await page.goto('https://chatgpt.com/');
    await page.setContent('<textarea id="prompt-textarea"></textarea><button>Send</button><button>Send</button>');
    assert.equal((await invoke('CHATGPT_SEND_PROMPT','ambiguous','begin','ambiguous-send')).status,'wait');
    console.log('PASS: ChatGPT/Flow offline DOM contracts, literal prompt, old-result isolation, upload readiness, and single dispatch.');
  } finally { await browser.close(); }
})().catch(error=>{console.error(error);process.exitCode=1;});
