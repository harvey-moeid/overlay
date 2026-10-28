// Fixed adapter; values arrive as JSON literals, never as executable plan content.
(async function(action, value, phase, commandId) {
  const visible = e => !!e && !!(e.offsetWidth || e.offsetHeight || e.getClientRects().length);
  const enabled = e => visible(e) && !e.disabled && e.getAttribute('aria-disabled') !== 'true';
  const all = (s, root=document) => Array.from(root.querySelectorAll(s));
  const label = e => (e.getAttribute('aria-label') || e.getAttribute('title') || e.innerText || '').trim().toLowerCase();
  const button = re => {
    const matches=all('button,[role="button"]').filter(enabled).filter(e=>re.test(label(e)));
    return matches.length===1 ? matches[0] : null;
  };
  const wait = note => ({status:'wait',note});
  const done = (result='') => ({status:'done',result});
  const dispatched = () => ({status:'dispatched'});
  const state = window.__bygaWorkflow || (window.__bygaWorkflow = {});
  const dispatches=state.dispatches || (state.dispatches={});
  if(dispatches[commandId]) phase='observe';
  function dispatch(element) {
    if(dispatches[commandId]) return {status:'error',note:'Duplicate dispatch diblokir.'};
    if(Object.keys(dispatches).length>=64)return {status:'error',note:'Batas sesi tercapai; periksa hasil sebelum membuka sesi baru.'};
    dispatches[commandId]=true; // Written BEFORE click: exceptions must not cause redispatch.
    element.click(); return dispatched();
  }
  const assistant = () => all('[data-message-author-role="assistant"]').filter(visible);
  const busy = () => !!document.querySelector('[data-testid="stop-button"]') ||
    !!button(/^(stop generating|stop streaming|hentikan pembuatan|berhenti menghasilkan)$/);
  const editor = () => {
    const candidates=all('textarea,[contenteditable="true"],[role="textbox"]').filter(enabled);
    const prompt=candidates.filter(e=>e.id==='prompt-textarea');
    if(prompt.length) return prompt.length===1 ? prompt[0] : null;
    const safe=candidates.filter(e=>!e.closest('[role="dialog"]') && !/search|cari|rename|nama proyek|password|verification|otp/i.test((e.getAttribute('placeholder')||'')+' '+label(e)));
    return safe.length===1 ? safe[0] : null;
  };
  function fill(e,text) {
    e.focus();
    if(e instanceof HTMLTextAreaElement || e instanceof HTMLInputElement) {
      const proto=e instanceof HTMLTextAreaElement ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
      Object.getOwnPropertyDescriptor(proto,'value').set.call(e,text);
      e.dispatchEvent(new Event('input',{bubbles:true}));
      e.dispatchEvent(new Event('change',{bubbles:true}));
      return e.value===text;
    }
    const selection=window.getSelection();selection.removeAllRanges();
    const range=document.createRange();range.selectNodeContents(e);selection.addRange(range);
    if(!document.execCommand('insertText',false,text)) {
      e.textContent=text;e.dispatchEvent(new InputEvent('input',{bubbles:true,inputType:'insertText',data:text}));
    }
    return e.textContent===text;
  }
  const media = kind => all(kind).filter(visible).map(e => e.currentSrc || e.src).filter(Boolean);
  const url=new URL(location.href);
  const chat=action.includes('CHATGPT');
  if(url.protocol!=='https:' || url.username || url.password || (url.port && url.port!=='443') ||
    (chat ? url.hostname!=='chatgpt.com' || /^\/(auth|login|signup|settings)(\/|$)/i.test(url.pathname) :
      url.hostname!=='labs.google' || !/^\/fx\/tools\/flow(\/|$)/.test(url.pathname)))return {status:'error',note:'Origin atau halaman tidak diizinkan.'};
  if(document.readyState!=='complete') return wait('Menunggu halaman selesai dimuat…');
  if(all('input[type="password"],input[type="email"],input[autocomplete="one-time-code"]').some(visible)) return wait('Selesaikan login/verifikasi akun secara manual. BYGA tidak mengisi kolom login.');
  if(action==='OPEN_CHATGPT') return editor() && !button(/^(log in|sign in|masuk)$/) ? done() : wait('Login manual dan buka kolom ChatGPT.');
  if(action==='OPEN_FLOW') return editor() || button(/^(\+\s*)?(new project|create project|proyek baru|buat proyek)$/) ? done() : wait('Login manual dan buka proyek Flow.');
  if(action==='CHATGPT_SEND_PROMPT') {
    if(phase==='observe') {
      const users=all('[data-message-author-role="user"]');
      const normalize=s=>s.replace(/\s+/g,' ').trim();
      return users.length>state.users && normalize(users.at(-1).innerText)===normalize(value) ? done() : wait('Memverifikasi prompt terkirim…');
    }
    if(busy()) return wait('ChatGPT masih menghasilkan jawaban.');
    const e=editor(); if(!e) return wait('Login ChatGPT, lalu buka chat baru sampai kolom prompt terlihat.');
    if(!fill(e,value)) return {status:'error',note:'Prompt tidak dapat diverifikasi.'};
    const sends=all('[data-testid="send-button"]').filter(enabled);
    const send=(sends.length===1 ? sends[0] : null) || (sends.length===0 ? button(/^(send prompt|send message|send|kirim pesan|kirim)$/) : null);
    if(!enabled(send)) return wait('Tombol Kirim belum siap. Periksa login, model, atau batas akun.');
    state.users=all('[data-message-author-role="user"]').length;state.prompt=value;
    state.answers=assistant().length;
    return dispatch(send);
  }
  if(action==='CHATGPT_COPY_RESULT' || action==='CHATGPT_COLLECT_RESULT' || action==='CHATGPT_DOWNLOAD_IMAGE') {
    const users=all('[data-message-author-role="user"]');
    const normalize=s=>s.replace(/\s+/g,' ').trim();
    if(state.prompt!==undefined && (users.length!==state.users+1 || normalize(users.at(-1)?.innerText || '')!==normalize(state.prompt)))return {status:'error',note:'Percakapan berubah setelah prompt. Periksa hasil manual.'};
    const turns=assistant(); const turn=turns.at(-1);
    if(state.answers===undefined || turns.length<=state.answers || !turn || busy()) return wait('Menunggu jawaban baru ChatGPT selesai…');
    const complete=all('button',turn).some(e=>/copy|salin/i.test(label(e))) ||
      !!turn.closest('article')?.querySelector('[data-testid="copy-turn-action-button"]');
    if(!complete) return wait('Menunggu penanda jawaban selesai (Salin/Copy)…');
    const images=all('img',turn).filter(e=>visible(e) && e.complete && e.naturalWidth>=256 && e.naturalHeight>=256);
    if(action==='CHATGPT_COLLECT_RESULT' && images.length) return {status:'done',kind:'image',result:images.at(-1).currentSrc || images.at(-1).src};
    if(action==='CHATGPT_COPY_RESULT' || action==='CHATGPT_COLLECT_RESULT') {
      const text=(turn.querySelector('.markdown') || turn).innerText.trim();
      return text ? done(text) : wait('Jawaban belum memuat teks yang bisa disalin.');
    }
    return images.length ? done(images.at(-1).currentSrc || images.at(-1).src) : wait('Menunggu gambar pada jawaban baru. Pastikan prompt meminta gambar.');
  }
  if(action==='FLOW_CREATE_PROJECT') {
    if(value==='CURRENT') {
      if(/\/project\/[^/]+/.test(location.pathname) && editor()){state.projectPath=location.pathname;return done()};
      return wait('Pilih proyek yang Anda setujui secara manual sampai editor terlihat.');
    }
    if(phase==='observe') {if(/\/project\/[^/]+/.test(location.pathname) && editor()){state.projectPath=location.pathname;return done()};return wait('Menunggu editor proyek. Selesaikan dialog proyek bila diminta.');}
    const create=button(/^(\+\s*)?(new project|create project|proyek baru|buat proyek)$/);
    if(!create) return wait('Login Google Flow lalu buka daftar proyek. Tombol New project harus terlihat.');
    return dispatch(create);
  }
  if(action.startsWith('FLOW_') && action!=='FLOW_CREATE_PROJECT' && state.projectPath!==location.pathname)return {status:'error',note:'Proyek berubah atau bukti sesi hilang; periksa manual.'};
  if(action==='FLOW_UPLOAD_IMAGE') {
    if(phase==='observe') {
      if(!Array.isArray(state.images))return {status:'error',note:'Bukti upload hilang; periksa proyek manual.'};
      const changed=all('img').some(e=>visible(e) && e.complete && e.naturalWidth>=64 && e.naturalHeight>=64 && /uploaded|input image|start frame|end frame|bingkai|gambar unggah/i.test((e.alt||'')+' '+(e.getAttribute('aria-label')||'')) && !state.images.includes(e.currentSrc || e.src));
      const pending=all('[role="progressbar"]').some(visible);
      const dialog=all('[role="dialog"]').some(visible);
      return state.uploadSelected && changed && !pending && !dialog ? done() : wait('Menunggu gambar masuk ke proyek. Pilih mode Frames to Video dan konfirmasi crop bila diminta.');
    }
    if(phase==='begin')state.uploadControls=[];
    const input=all('input[type="file"]').find(e=>!e.disabled && (!e.accept || /image|png|jpg|jpeg|webp/i.test(e.accept)));
    if(!input) {
      const frames=all('[role="option"],[role="menuitem"],button').filter(enabled).find(e=>/^(frames to video|bingkai ke video)$/i.test(label(e)));
      const control=frames || button(/^(text to video|teks ke video|add image|upload image|upload|tambah gambar|unggah gambar|add_photo_alternate)$/);
      if(control && !state.uploadControls.includes(label(control)) && state.uploadControls.length<3) {
        state.uploadControls.push(label(control));control.click();return {status:'preparing'};
      }
      return wait('Di Flow pilih Frames to Video, lalu buka tombol tambah/upload gambar sampai pemilih file tersedia.');
    }
    state.images=media('img');state.uploadSelected=false;
    input.addEventListener('change',()=>{state.uploadSelected=input.files?.length===1},{once:true});
    return dispatch(input);
  }
  if(action==='FLOW_SET_PROMPT') {
    const e=editor(); if(!e) return wait('Buka editor prompt video di proyek Flow.');
    if(!fill(e,value))return {status:'error',note:'Isi prompt video tidak terverifikasi.'};
    state.flowPrompt=value;return done();
  }
  if(action==='FLOW_GENERATE') {
    if(phase==='observe') {
      const now=media('video');
      const pending=all('[role="progressbar"]').some(visible);
      return pending || now.some(src=>!state.videos.includes(src)) || !!button(/^(cancel generation|stop generation|batalkan pembuatan)$/) ? done() : wait('Memverifikasi permintaan generate diterima…');
    }
    const e=editor();
    if(state.flowPrompt===undefined || !e || (e.value ?? e.textContent)!==state.flowPrompt)return {status:'error',note:'Prompt berubah atau sesi hilang. Periksa proyek sebelum menjalankan workflow baru.'};
    if(all('[role="progressbar"]').some(visible))return {status:'error',note:'Flow sudah memproses pekerjaan. Periksa hasil manual sebelum generate lagi.'};
    const generate=button(/^(generate|create video|generate video|buat video|arrow_forward)$/);
    if(!generate) return wait('Tombol Generate belum siap. Periksa gambar, mode, prompt, kredit, dan model Flow.');
    state.videos=media('video'); return dispatch(generate);
  }
  if(action==='FLOW_DOWNLOAD_VIDEO') {
    if(!Array.isArray(state.videos)) return {status:'error',note:'Sesi generate hilang. Periksa hasil manual; jangan generate ulang otomatis.'};
    const fresh=all('video').filter(e=>visible(e) && e.readyState>=2 && e.error===null)
      .map(e=>e.currentSrc || e.src).filter(src=>src && !state.videos.includes(src));
    return fresh.length ? done(fresh.at(-1)) : wait('Menunggu video baru selesai. Bila perlu buka preview hasil terbaru agar video dimuat.');
  }
  return {status:'error',note:'Aksi web tidak didukung.'};
})(BYGA_ACTION, BYGA_VALUE, BYGA_PHASE, BYGA_COMMAND).then(result=>{window[BYGA_RESULT]=JSON.stringify(result)},error=>{window[BYGA_RESULT]=JSON.stringify({status:'error',note:String(error)})});
