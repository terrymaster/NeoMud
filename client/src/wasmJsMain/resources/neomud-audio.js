/**
 * NeoMud Web Audio Module
 * Handles SFX (Web Audio API) and BGM (HTML5 Audio) playback.
 * Called from Kotlin/WasmJs via external declarations.
 */
window.NeoMudAudio = (() => {
  let audioCtx = null;
  let audioResumed = false;
  let masterVol = 1.0;
  let sfxVol = 1.0;
  let bgmVol = 0.5;
  let bgmElement = null;
  let currentBgmTrack = '';
  const sfxCache = new Map();
  const sfxLoading = new Set();

  // Load persisted volumes
  try {
    const m = localStorage.getItem('neomud_vol_master');
    const s = localStorage.getItem('neomud_vol_sfx');
    const b = localStorage.getItem('neomud_vol_bgm');
    if (m !== null) masterVol = parseFloat(m);
    if (s !== null) sfxVol = parseFloat(s);
    if (b !== null) bgmVol = parseFloat(b);
  } catch (_) {}

  function ensureContext() {
    if (!audioCtx) {
      audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    }
    if (!audioResumed && audioCtx.state === 'suspended') {
      const resume = () => {
        audioCtx.resume().then(() => { audioResumed = true; });
        document.removeEventListener('click', resume);
        document.removeEventListener('keydown', resume);
        document.removeEventListener('touchstart', resume);
      };
      document.addEventListener('click', resume);
      document.addEventListener('keydown', resume);
      document.addEventListener('touchstart', resume);
    }
  }

  function effectiveVol() { return masterVol * sfxVol; }
  function effectiveBgmVol() { return masterVol * bgmVol; }

  return {
    init() {
      ensureContext();
    },

    playSfx(url) {
      if (!url || masterVol === 0 || sfxVol === 0) return;
      ensureContext();
      if (!audioResumed) return;

      const cached = sfxCache.get(url);
      if (cached) {
        const source = audioCtx.createBufferSource();
        source.buffer = cached;
        const gain = audioCtx.createGain();
        gain.gain.value = effectiveVol();
        source.connect(gain);
        gain.connect(audioCtx.destination);
        source.start(0);
        return;
      }

      if (sfxLoading.has(url)) return;
      sfxLoading.add(url);

      fetch(url)
        .then(r => r.arrayBuffer())
        .then(buf => audioCtx.decodeAudioData(buf))
        .then(decoded => {
          sfxCache.set(url, decoded);
          sfxLoading.delete(url);
          // Play immediately after first decode
          const source = audioCtx.createBufferSource();
          source.buffer = decoded;
          const gain = audioCtx.createGain();
          gain.gain.value = effectiveVol();
          source.connect(gain);
          gain.connect(audioCtx.destination);
          source.start(0);
        })
        .catch(err => {
          sfxLoading.delete(url);
          console.warn('[NeoMudAudio] SFX load failed:', url, err.message);
        });
    },

    playBgm(url) {
      if (!url || url === currentBgmTrack) return;
      this.stopBgm();
      currentBgmTrack = url;

      bgmElement = new Audio(url);
      bgmElement.loop = true;
      bgmElement.volume = effectiveBgmVol();
      bgmElement.play().catch(() => {
        // Autoplay blocked — will play after user gesture
      });
    },

    stopBgm() {
      if (bgmElement) {
        bgmElement.pause();
        bgmElement.src = '';
        bgmElement = null;
      }
      currentBgmTrack = '';
    },

    setVolumes(master, sfx, bgm) {
      masterVol = master;
      sfxVol = sfx;
      bgmVol = bgm;
      if (bgmElement) {
        bgmElement.volume = effectiveBgmVol();
      }
      try {
        localStorage.setItem('neomud_vol_master', master.toString());
        localStorage.setItem('neomud_vol_sfx', sfx.toString());
        localStorage.setItem('neomud_vol_bgm', bgm.toString());
      } catch (_) {}
    },

    getVolumes() {
      return { master: masterVol, sfx: sfxVol, bgm: bgmVol };
    }
  };
})();

// Auto-init
NeoMudAudio.init();
