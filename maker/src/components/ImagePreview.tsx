import { useEffect, useState } from 'react';
import api from '../api';
import type { CSSProperties } from 'react';

interface PromptTemplate {
  id: number;
  entityType: string;
  entityId: string;
  prompt: string;
  style: string;
  negativePrompt: string;
  width: number;
  height: number;
}

interface ImagePreviewProps {
  entityType: string;     // "room" | "npc" | "item"
  entityId: string;       // e.g. "town:square", "npc:town_guard"
  description?: string;   // entity description for auto-filling new templates
  assetPath?: string;     // override filename (rooms store backgroundImage field)
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
  noTemplate: {
    fontSize: 12,
    color: '#999',
    marginBottom: 6,
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
  btn: {
    padding: '4px 10px',
    fontSize: 11,
    fontWeight: 600,
    backgroundColor: '#1a1a2e',
    color: '#fff',
    border: 'none',
    borderRadius: 3,
    cursor: 'pointer',
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
  btnDanger: {
    padding: '4px 10px',
    fontSize: 11,
    fontWeight: 600,
    backgroundColor: '#d32f2f',
    color: '#fff',
    border: 'none',
    borderRadius: 3,
    cursor: 'pointer',
  },
};

function getImageUrl(entityType: string, entityId: string, assetPath?: string): string {
  if (entityType === 'room') {
    if (!assetPath) return '';
    // Strip leading path segments (e.g. "images/rooms/foo" -> "foo")
    const filename = assetPath.split('/').pop() || assetPath;
    // Add .webp if no extension present
    const withExt = filename.includes('.') ? filename : `${filename}.webp`;
    return `/api/assets/images/rooms/${withExt}`;
  }
  // NPCs and Items: derive from entityId
  const filename = entityId.replace(':', '_');
  return `/api/assets/images/rooms/${filename}.webp`;
}

function ImagePreview({ entityType, entityId, description, assetPath }: ImagePreviewProps) {
  const [imgError, setImgError] = useState(false);
  const [template, setTemplate] = useState<PromptTemplate | null>(null);
  const [templateLoaded, setTemplateLoaded] = useState(false);
  const [form, setForm] = useState({ prompt: '', style: '', negativePrompt: '', width: 1024, height: 576 });
  const [saving, setSaving] = useState(false);

  const imageUrl = getImageUrl(entityType, entityId, assetPath);

  // Reset image error state when url changes
  useEffect(() => {
    setImgError(false);
  }, [imageUrl]);

  // Load prompt template
  useEffect(() => {
    setTemplate(null);
    setTemplateLoaded(false);
    api
      .get<PromptTemplate>(`/prompt-templates?entityType=${entityType}&entityId=${encodeURIComponent(entityId)}`)
      .then((t) => {
        setTemplate(t);
        setForm({ prompt: t.prompt, style: t.style, negativePrompt: t.negativePrompt, width: t.width, height: t.height });
        setTemplateLoaded(true);
      })
      .catch(() => {
        setTemplateLoaded(true);
      });
  }, [entityType, entityId]);

  const handleCreateTemplate = async () => {
    setSaving(true);
    try {
      const data = {
        entityType,
        entityId,
        prompt: description || '',
        style: '',
        negativePrompt: '',
        width: 1024,
        height: 576,
      };
      const created = await api.post<PromptTemplate>('/prompt-templates', data);
      setTemplate(created);
      setForm({ prompt: created.prompt, style: created.style, negativePrompt: created.negativePrompt, width: created.width, height: created.height });
    } catch (err) {
      console.error('Failed to create template:', err);
    }
    setSaving(false);
  };

  const handleSave = async () => {
    if (!template) return;
    setSaving(true);
    try {
      const updated = await api.put<PromptTemplate>(`/prompt-templates/${template.id}`, form);
      setTemplate(updated);
      setForm({ prompt: updated.prompt, style: updated.style, negativePrompt: updated.negativePrompt, width: updated.width, height: updated.height });
    } catch (err) {
      console.error('Failed to save template:', err);
    }
    setSaving(false);
  };

  const handleDelete = async () => {
    if (!template) return;
    try {
      await api.del(`/prompt-templates/${template.id}`);
      setTemplate(null);
      setForm({ prompt: '', style: '', negativePrompt: '', width: 1024, height: 576 });
    } catch (err) {
      console.error('Failed to delete template:', err);
    }
  };

  const handleGenerate = () => {
    const fullPrompt = [form.prompt, form.style && `Style: ${form.style}`, form.negativePrompt && `Negative: ${form.negativePrompt}`]
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

      {/* Prompt Template Section */}
      <div style={styles.promptSection}>
        <div style={styles.promptLabel}>Prompt Template</div>
        {!templateLoaded ? (
          <div style={styles.noTemplate}>Loading...</div>
        ) : !template ? (
          <>
            <div style={styles.noTemplate}>No prompt template</div>
            <button style={styles.btn} onClick={handleCreateTemplate} disabled={saving}>
              {saving ? 'Creating...' : 'Create Template'}
            </button>
          </>
        ) : (
          <>
            <div style={styles.fieldLabel}>Prompt</div>
            <textarea
              style={styles.textarea}
              value={form.prompt}
              onChange={(e) => setForm((f) => ({ ...f, prompt: e.target.value }))}
              rows={3}
            />
            <div style={styles.fieldLabel}>Style</div>
            <input
              style={styles.input}
              value={form.style}
              onChange={(e) => setForm((f) => ({ ...f, style: e.target.value }))}
              placeholder="e.g. pixel art, watercolor"
            />
            <div style={styles.fieldLabel}>Negative Prompt</div>
            <input
              style={styles.input}
              value={form.negativePrompt}
              onChange={(e) => setForm((f) => ({ ...f, negativePrompt: e.target.value }))}
              placeholder="e.g. blurry, low quality"
            />
            <div style={styles.row}>
              <div style={{ flex: 1 }}>
                <div style={styles.fieldLabel}>Width</div>
                <input
                  style={styles.input}
                  type="number"
                  value={form.width}
                  onChange={(e) => setForm((f) => ({ ...f, width: parseInt(e.target.value) || 0 }))}
                />
              </div>
              <div style={{ flex: 1 }}>
                <div style={styles.fieldLabel}>Height</div>
                <input
                  style={styles.input}
                  type="number"
                  value={form.height}
                  onChange={(e) => setForm((f) => ({ ...f, height: parseInt(e.target.value) || 0 }))}
                />
              </div>
            </div>
            <div style={styles.btnRow}>
              <button style={styles.btn} onClick={handleSave} disabled={saving}>
                {saving ? 'Saving...' : 'Save'}
              </button>
              <button style={styles.btnOutline} onClick={handleGenerate}>
                Generate
              </button>
              <button style={styles.btnDanger} onClick={handleDelete}>
                Delete
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

export default ImagePreview;
