import { useEffect, useState } from 'react';
import type { CSSProperties } from 'react';

interface AudioPreviewProps {
  entityType: string;          // "zone" | "room"
  entityId: string;
  bgm?: string;                // track ID (display + audio player)
  bgmPrompt?: string;
  bgmDuration?: number;
  defaultBgmPrompt?: string;   // zone fallback shown as placeholder
  defaultBgmDuration?: number;
  onUpdate?: (fields: { bgmPrompt: string; bgmDuration: number }) => void;
}

const styles: Record<string, CSSProperties> = {
  container: {
    marginBottom: 16,
    borderRadius: 6,
    border: '1px solid #ddd',
    overflow: 'hidden',
    backgroundColor: '#fff',
    maxWidth: 320,
  },
  audioArea: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#f0f0f0',
    padding: 12,
    gap: 6,
  },
  placeholder: {
    width: '100%',
    height: 80,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#e0e0e0',
    color: '#888',
  },
  placeholderIcon: {
    fontSize: 28,
    marginBottom: 4,
    color: '#bbb',
  },
  placeholderText: {
    fontSize: 11,
    fontWeight: 600,
  },
  trackId: {
    fontSize: 10,
    color: '#888',
    textAlign: 'center',
  },
  promptSection: {
    padding: 10,
    borderTop: '1px solid #eee',
  },
  promptLabel: {
    fontSize: 11,
    fontWeight: 600,
    color: '#666',
    marginBottom: 4,
  },
  fieldLabel: {
    fontSize: 10,
    fontWeight: 600,
    color: '#888',
    marginBottom: 1,
    marginTop: 4,
  },
  textarea: {
    width: '100%',
    padding: '4px 6px',
    fontSize: 12,
    border: '1px solid #ccc',
    borderRadius: 3,
    boxSizing: 'border-box' as const,
    resize: 'vertical' as const,
    minHeight: 48,
    marginBottom: 4,
  },
  input: {
    width: '100%',
    padding: '4px 6px',
    fontSize: 12,
    border: '1px solid #ccc',
    borderRadius: 3,
    boxSizing: 'border-box' as const,
    marginBottom: 4,
  },
  btnRow: {
    display: 'flex',
    gap: 6,
    marginTop: 6,
  },
  btnOutline: {
    padding: '4px 10px',
    fontSize: 11,
    fontWeight: 600,
    backgroundColor: '#fff',
    color: '#1a1a2e',
    border: '1px solid #1a1a2e',
    borderRadius: 3,
    cursor: 'pointer',
  },
};

function AudioPreview({ entityType, entityId, bgm, bgmPrompt, bgmDuration, defaultBgmPrompt, defaultBgmDuration, onUpdate }: AudioPreviewProps) {
  const [localPrompt, setLocalPrompt] = useState(bgmPrompt || '');
  const [localDuration, setLocalDuration] = useState(bgmDuration || 0);

  // Sync local state when props change (entity selection change)
  useEffect(() => {
    setLocalPrompt(bgmPrompt || '');
    setLocalDuration(bgmDuration || 0);
  }, [entityId, bgmPrompt, bgmDuration]);

  const fireUpdate = (overrides: Partial<{ bgmPrompt: string; bgmDuration: number }>) => {
    if (!onUpdate) return;
    onUpdate({
      bgmPrompt: overrides.bgmPrompt ?? localPrompt,
      bgmDuration: overrides.bgmDuration ?? localDuration,
    });
  };

  const audioUrl = bgm ? `/api/assets/audio/bgm/${bgm}.ogg` : '';

  const handleCopyPrompt = () => {
    const effectivePrompt = localPrompt || defaultBgmPrompt || '';
    const effectiveDuration = localDuration || defaultBgmDuration || 120;
    const payload = JSON.stringify({
      text: effectivePrompt,
      duration_seconds: effectiveDuration,
      prompt_influence: 0.3,
      output_format: 'ogg_48000',
    }, null, 2);
    navigator.clipboard.writeText(payload).then(() => {
      console.log('ElevenLabs payload copied:', payload);
    });
  };

  const promptPlaceholder = entityType === 'room' && defaultBgmPrompt
    ? `Zone default: ${defaultBgmPrompt}`
    : 'Describe the BGM atmosphere...';

  const durationPlaceholder = entityType === 'room' && defaultBgmDuration
    ? `Zone default: ${defaultBgmDuration}s`
    : '';

  return (
    <div style={styles.container}>
      {/* Audio Area */}
      <div style={styles.audioArea}>
        {bgm ? (
          <>
            <audio controls src={audioUrl} style={{ width: '100%', maxWidth: 280 }} />
            <div style={styles.trackId}>{bgm}.ogg</div>
          </>
        ) : (
          <div style={styles.placeholder}>
            <div style={styles.placeholderIcon}>
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#bbb" strokeWidth="1.5">
                <path d="M9 18V5l12-2v13" />
                <circle cx="6" cy="18" r="3" />
                <circle cx="18" cy="16" r="3" />
              </svg>
            </div>
            <div style={styles.placeholderText}>No BGM Track</div>
          </div>
        )}
      </div>

      {/* Prompt Fields Section */}
      <div style={styles.promptSection}>
        <div style={styles.promptLabel}>BGM Prompt</div>
        <div style={styles.fieldLabel}>Prompt</div>
        <textarea
          style={styles.textarea}
          value={localPrompt}
          onChange={(e) => { setLocalPrompt(e.target.value); fireUpdate({ bgmPrompt: e.target.value }); }}
          rows={3}
          placeholder={promptPlaceholder}
        />
        <div style={styles.fieldLabel}>Duration (seconds)</div>
        <input
          style={styles.input}
          type="number"
          value={localDuration || ''}
          onChange={(e) => { const v = parseInt(e.target.value) || 0; setLocalDuration(v); fireUpdate({ bgmDuration: v }); }}
          placeholder={durationPlaceholder}
        />
        <div style={styles.btnRow}>
          <button style={styles.btnOutline} onClick={handleCopyPrompt}>
            Copy Prompt
          </button>
        </div>
      </div>
    </div>
  );
}

export default AudioPreview;
