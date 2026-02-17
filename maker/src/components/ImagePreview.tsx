import { useEffect, useState } from 'react';
import type { CSSProperties } from 'react';

interface ImagePreviewProps {
  entityType: string;     // "room" | "npc" | "item"
  entityId: string;       // e.g. "town:square", "npc:town_guard"
  description?: string;   // entity description for auto-filling new templates
  assetPath?: string;     // override filename (rooms store backgroundImage field)
  imagePrompt?: string;
  imageStyle?: string;
  imageNegativePrompt?: string;
  imageWidth?: number;
  imageHeight?: number;
  onUpdate?: (fields: { imagePrompt: string; imageStyle: string; imageNegativePrompt: string; imageWidth: number; imageHeight: number }) => void;
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
  imageArea: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#f0f0f0',
    maxHeight: 180,
    overflow: 'hidden',
  },
  image: {
    display: 'block',
    maxWidth: '100%',
    maxHeight: 180,
    objectFit: 'contain',
  },
  placeholder: {
    width: '100%',
    height: 100,
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
  placeholderName: {
    fontSize: 10,
    color: '#aaa',
    marginTop: 2,
    maxWidth: '80%',
    textAlign: 'center',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
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
  input: {
    width: '100%',
    padding: '4px 6px',
    fontSize: 12,
    border: '1px solid #ccc',
    borderRadius: 3,
    boxSizing: 'border-box' as const,
    marginBottom: 4,
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
  fieldLabel: {
    fontSize: 10,
    fontWeight: 600,
    color: '#888',
    marginBottom: 1,
    marginTop: 4,
  },
  row: {
    display: 'flex',
    gap: 6,
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

function getImageUrl(entityType: string, entityId: string, assetPath?: string): string {
  if (entityType === 'room') {
    if (!assetPath) return '';
    const filename = assetPath.split('/').pop() || assetPath;
    const withExt = filename.includes('.') ? filename : `${filename}.webp`;
    return `/api/assets/images/rooms/${withExt}`;
  }
  const filename = entityId.replace(':', '_');
  return `/api/assets/images/rooms/${filename}.webp`;
}

function ImagePreview({ entityType, entityId, description, assetPath, imagePrompt, imageStyle, imageNegativePrompt, imageWidth, imageHeight, onUpdate }: ImagePreviewProps) {
  const [imgError, setImgError] = useState(false);
  const [localPrompt, setLocalPrompt] = useState(imagePrompt || '');
  const [localStyle, setLocalStyle] = useState(imageStyle || '');
  const [localNeg, setLocalNeg] = useState(imageNegativePrompt || '');
  const [localW, setLocalW] = useState(imageWidth || 1024);
  const [localH, setLocalH] = useState(imageHeight || 576);

  const imageUrl = getImageUrl(entityType, entityId, assetPath);

  // Sync local state when props change (entity selection change)
  useEffect(() => {
    setLocalPrompt(imagePrompt || '');
    setLocalStyle(imageStyle || '');
    setLocalNeg(imageNegativePrompt || '');
    setLocalW(imageWidth || 1024);
    setLocalH(imageHeight || 576);
  }, [entityId, imagePrompt, imageStyle, imageNegativePrompt, imageWidth, imageHeight]);

  useEffect(() => {
    setImgError(false);
  }, [imageUrl]);

  const fireUpdate = (overrides: Partial<{ imagePrompt: string; imageStyle: string; imageNegativePrompt: string; imageWidth: number; imageHeight: number }>) => {
    if (!onUpdate) return;
    onUpdate({
      imagePrompt: overrides.imagePrompt ?? localPrompt,
      imageStyle: overrides.imageStyle ?? localStyle,
      imageNegativePrompt: overrides.imageNegativePrompt ?? localNeg,
      imageWidth: overrides.imageWidth ?? localW,
      imageHeight: overrides.imageHeight ?? localH,
    });
  };

  const handleGenerate = () => {
    const fullPrompt = [localPrompt, localStyle && `Style: ${localStyle}`, localNeg && `Negative: ${localNeg}`]
      .filter(Boolean)
      .join('\n');
    navigator.clipboard.writeText(fullPrompt).then(() => {
      console.log('Prompt copied to clipboard:', fullPrompt);
    });
  };

  return (
    <div style={styles.container}>
      {/* Image Area */}
      <div style={styles.imageArea}>
        {imageUrl && !imgError ? (
          <img
            src={imageUrl}
            alt={entityId}
            style={styles.image}
            onError={() => setImgError(true)}
          />
        ) : (
          <div style={styles.placeholder}>
            <div style={styles.placeholderIcon}>
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#bbb" strokeWidth="1.5">
                <rect x="3" y="3" width="18" height="18" rx="2" />
                <circle cx="8.5" cy="8.5" r="1.5" />
                <path d="M21 15l-5-5L5 21" />
              </svg>
            </div>
            <div style={styles.placeholderText}>No Image</div>
            <div style={styles.placeholderName}>{entityId}</div>
          </div>
        )}
      </div>

      {/* Prompt Fields Section */}
      <div style={styles.promptSection}>
        <div style={styles.promptLabel}>Image Prompt</div>
        <div style={styles.fieldLabel}>Prompt</div>
        <textarea
          style={styles.textarea}
          value={localPrompt}
          onChange={(e) => { setLocalPrompt(e.target.value); fireUpdate({ imagePrompt: e.target.value }); }}
          rows={3}
          placeholder={description || 'Describe the image to generate...'}
        />
        <div style={styles.fieldLabel}>Style</div>
        <input
          style={styles.input}
          value={localStyle}
          onChange={(e) => { setLocalStyle(e.target.value); fireUpdate({ imageStyle: e.target.value }); }}
          placeholder="e.g. pixel art, watercolor"
        />
        <div style={styles.fieldLabel}>Negative Prompt</div>
        <input
          style={styles.input}
          value={localNeg}
          onChange={(e) => { setLocalNeg(e.target.value); fireUpdate({ imageNegativePrompt: e.target.value }); }}
          placeholder="e.g. blurry, low quality"
        />
        <div style={styles.row}>
          <div style={{ flex: 1 }}>
            <div style={styles.fieldLabel}>Width</div>
            <input
              style={styles.input}
              type="number"
              value={localW}
              onChange={(e) => { const v = parseInt(e.target.value) || 0; setLocalW(v); fireUpdate({ imageWidth: v }); }}
            />
          </div>
          <div style={{ flex: 1 }}>
            <div style={styles.fieldLabel}>Height</div>
            <input
              style={styles.input}
              type="number"
              value={localH}
              onChange={(e) => { const v = parseInt(e.target.value) || 0; setLocalH(v); fireUpdate({ imageHeight: v }); }}
            />
          </div>
        </div>
        <div style={styles.btnRow}>
          <button style={styles.btnOutline} onClick={handleGenerate}>
            Copy Prompt
          </button>
        </div>
      </div>
    </div>
  );
}

export default ImagePreview;
