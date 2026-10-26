// Fixed adapter; values arrive as JSON literals, never as executable plan content.
(async function(action, value, phase) {
  const visible = e => !!e && !!(e.offsetWidth || e.offsetHeight || e.getClientRects().length);
  const enabled = e => visible(e) && !e.disabled && e.getAttribute('aria-disabled') !== 'true';
  const all = (s, root=document) => Array.from(root.querySelectorAll(s));
  const label = e => (e.getAttribute('aria-label') || e.getAttribute('title') || e.innerText || '').trim().toLowerCase();
  const button = re => all('button,[role="button"]').filter(enabled).find(e => re.test(label(e)));
  const wait = note => ({status:'wait',note});
  const done = (result='') => ({status:'done',result});
  const dispatched = () => ({status:'dispatched'});
  const state = window.__bygaWorkflow || (window.__bygaWorkflow = {});
  const assistant = () => all('[data-message-author-role="assistant"]').filter(visible);
  const busy = () => !!document.querySelector('[data-testid="stop-button"]') ||
    !!button(/^(stop generating|stop streaming|hentikan pembuatan|berhenti menghasilkan)$/);
  const editor = () => all('textarea,[contenteditable="true"],[role="textbox"]').filter(enabled)
    .find(e => e.id==='prompt-textarea') || all('textarea,[contenteditable="true"],[role="textbox"]').filter(enabled)
    .find(e => !/search|cari|rename|nama proyek/i.test((e.getAttribute('placeholder')||'')+' '+label(e)));
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
  if(document.readyState!=='complete') return wait('Menunggu halaman selesai dimuat…');
  if(action==='OPEN_CHATGPT' || action==='OPEN_FLOW') return done();
  if(all('input[type="password"],input[type="email"],input[autocomplete="one-time-code"]').some(visible)) return wait('Selesaikan login/verifikasi akun secara manual. BYGA tidak mengisi kolom login.');
  if(action==='CHATGPT_SEND_PROMPT') {
    if(phase==='observe') {
      const users=all('[data-message-author-role="user"]');
      const normalize=s=>s.replace(/\s+/g,' ').trim();
      return users.length>state.users && normalize(users.at(-1).innerText)===normalize(value) ? done() : wait('Memverifikasi prompt terkirim…');
    }
    if(busy()) return wait('ChatGPT masih menghasilkan jawaban.');
    const e=editor(); if(!e) return wait('Login ChatGPT, lalu buka chat baru sampai kolom prompt terlihat.');
    if(!fill(e,value)) return {status:'error',note:'Prompt tidak dapat diverifikasi.'};
    const send=document.querySelector('[data-testid="send-button"]') || button(/^(send prompt|send message|send|kirim pesan|kirim)$/);
    if(!enabled(send)) return wait('Tombol Kirim belum siap. Periksa login, model, atau batas akun.');
    state.users=all('[data-message-author-role="user"]').length;
    state.answers=assistant().length;
    send.click(); return dispatched();
  }
  if(action==='CHATGPT_COPY_RESULT' || action==='CHATGPT_COLLECT_RESULT' || action==='CHATGPT_DOWNLOAD_IMAGE') {
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
    if(phase==='observe') return /\/project\/[^/]+/.test(location.pathname) && editor() ? done() : wait('Menunggu editor proyek. Selesaikan dialog proyek bila diminta.');
    const create=button(/^(\+\s*)?(new project|create project|proyek baru|buat proyek)$/);
    if(!create) return wait('Login Google Flow lalu buka daftar proyek. Tombol New project harus terlihat.');
    create.click(); return dispatched();
  }
  if(action==='FLOW_UPLOAD_IMAGE') {
    if(phase==='observe') {
      const changed=all('img').some(e=>visible(e) && e.complete && e.naturalWidth>=64 && e.naturalHeight>=64 && !state.images.includes(e.currentSrc || e.src));
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
    input.click(); return dispatched();
  }
  if(action==='FLOW_SET_PROMPT') {
    const e=editor(); if(!e) return wait('Buka editor prompt video di proyek Flow.');
    if(!fill(e,value))return {status:'error',note:'Isi prompt video tidak terverifikasi.'};
    state.flowPrompt=value;return done();
  }
  if(action==='FLOW_GENERATE') {
    if(phase==='observe') {
      const now=media('video');
      const pending=all('[role="progressbar"]').some(visible) || /\b\d{1,3}%/.test(document.body.innerText);
      return pending || now.some(src=>!state.videos.includes(src)) || !!button(/^(cancel generation|stop generation|batalkan pembuatan)$/) ? done() : wait('Memverifikasi permintaan generate diterima…');
    }
    const e=editor();
    if(state.flowPrompt===undefined || !e || (e.value ?? e.textContent)!==state.flowPrompt)return {status:'error',note:'Prompt berubah atau sesi hilang. Periksa proyek sebelum menjalankan workflow baru.'};
    if(all('[role="progressbar"]').some(visible))return {status:'error',note:'Flow sudah memproses pekerjaan. Periksa hasil manual sebelum generate lagi.'};
    const generate=button(/^(generate|create video|generate video|buat video|arrow_forward)$/);
    if(!generate) return wait('Tombol Generate belum siap. Periksa gambar, mode, prompt, kredit, dan model Flow.');
    state.videos=media('video'); generate.click(); return dispatched();
  }
  if(action==='FLOW_DOWNLOAD_VIDEO') {
    if(!Array.isArray(state.videos)) return {status:'error',note:'Sesi generate hilang. Periksa hasil manual; jangan generate ulang otomatis.'};
    const fresh=all('video').filter(e=>visible(e) && e.readyState>=2 && e.error===null)
      .map(e=>e.currentSrc || e.src).filter(src=>src && !state.videos.includes(src));
    return fresh.length ? done(fresh.at(-1)) : wait('Menunggu video baru selesai. Bila perlu buka preview hasil terbaru agar video dimuat.');
  }
  return {status:'error',note:'Aksi web tidak didukung.'};
})(BYGA_ACTION, BYGA_VALUE, BYGA_PHASE).then(result=>{window[BYGA_RESULT]=JSON.stringify(result)},error=>{window[BYGA_RESULT]=JSON.stringify({status:'error',note:String(error)})});
