import { useEffect, useState } from 'react';
import api from '../api';
import ImagePreview from './ImagePreview';
import type { CSSProperties } from 'react';

export interface FieldConfig {
  key: string;
  label: string;
  type: 'text' | 'textarea' | 'number' | 'checkbox' | 'select' | 'json' | 'radio';
  options?: { value: string; label: string }[];
  placeholder?: string;
  disabled?: boolean;
  required?: boolean;
  rows?: number;
  help?: string;
  visibleWhen?: (form: Record<string, any>) => boolean;
}

interface GenericCrudEditorProps {
  entityName: string;
  apiPath: string;
  fields: FieldConfig[];
  idField?: string;
  imagePreview?: { entityType: string };
  disableCreate?: boolean;
  disableCreateMessage?: string;
}

const styles: Record<string, CSSProperties> = {
  container: {
    display: 'flex',
    height: '100%',
    overflow: 'hidden',
  },
  listPanel: {
    width: 240,
    minWidth: 240,
    borderRight: '1px solid #ddd',
    backgroundColor: '#fff',
    display: 'flex',
    flexDirection: 'column',
    overflow: 'hidden',
  },
  listTop: {
    padding: 12,
    borderBottom: '1px solid #eee',
  },
  newBtn: {
    width: '100%',
    padding: '8px',
    fontSize: 13,
    fontWeight: 600,
    backgroundColor: '#1a1a2e',
    color: '#fff',
    border: 'none',
    borderRadius: 4,
    cursor: 'pointer',
  },
  listItems: {
    flex: 1,
    overflowY: 'auto',
    padding: '4px 0',
  },
  listItem: {
    padding: '8px 12px',
    cursor: 'pointer',
    fontSize: 13,
    borderBottom: '1px solid #f0f0f0',
  },
  listItemSelected: {
    backgroundColor: '#e8eaf6',
    fontWeight: 600,
  },
  formPanel: {
    flex: 1,
    padding: 20,
    overflowY: 'auto',
    backgroundColor: '#fafafa',
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: 700,
    marginBottom: 16,
    color: '#1a1a2e',
  },
  field: {
    marginBottom: 12,
  },
  label: {
    display: 'block',
    fontSize: 11,
    fontWeight: 600,
    color: '#666',
    marginBottom: 3,
  },
  help: {
    fontSize: 10,
    color: '#999',
    marginTop: 2,
  },
  input: {
    width: '100%',
    padding: '6px 8px',
    fontSize: 13,
    border: '1px solid #ccc',
    borderRadius: 4,
    boxSizing: 'border-box' as const,
  },
  textarea: {
    width: '100%',
    padding: '6px 8px',
    fontSize: 13,
    border: '1px solid #ccc',
    borderRadius: 4,
    minHeight: 60,
    resize: 'vertical' as const,
    boxSizing: 'border-box' as const,
  },
  jsonTextarea: {
    width: '100%',
    padding: '6px 8px',
    fontSize: 12,
    fontFamily: 'monospace',
    border: '1px solid #ccc',
    borderRadius: 4,
    minHeight: 80,
    resize: 'vertical' as const,
    boxSizing: 'border-box' as const,
  },
  select: {
    width: '100%',
    padding: '6px 8px',
    fontSize: 13,
    border: '1px solid #ccc',
    borderRadius: 4,
    boxSizing: 'border-box' as const,
  },
  btnRow: {
    display: 'flex',
    gap: 8,
    marginTop: 16,
  },
  btnSmall: {
    padding: '8px 16px',
    fontSize: 13,
    fontWeight: 600,
    backgroundColor: '#1a1a2e',
    color: '#fff',
    border: 'none',
    borderRadius: 4,
    cursor: 'pointer',
  },
  btnDanger: {
    padding: '8px 16px',
    fontSize: 13,
    fontWeight: 600,
    backgroundColor: '#d32f2f',
    color: '#fff',
    border: 'none',
    borderRadius: 4,
    cursor: 'pointer',
  },
  error: {
    color: '#d32f2f',
    fontSize: 12,
    marginTop: 4,
  },
  empty: {
    color: '#999',
    fontSize: 13,
    textAlign: 'center',
    marginTop: 40,
  },
};

function prettyJson(value: string): string {
  if (!value || value === '') return '';
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

function GenericCrudEditor({ entityName, apiPath, fields, idField = 'id', imagePreview, disableCreate, disableCreateMessage }: GenericCrudEditorProps) {
  const [items, setItems] = useState<any[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [form, setForm] = useState<Record<string, any>>({});
  const [isNew, setIsNew] = useState(false);
  const [error, setError] = useState('');
  const [jsonErrors, setJsonErrors] = useState<Record<string, string>>({});

  const loadList = () => {
    api.get<any[]>(apiPath).then(setItems).catch(() => {});
  };

  useEffect(() => {
    loadList();
  }, [apiPath]);

  const handleSelect = (id: string) => {
    const item = items.find((i) => i[idField] === id);
    if (!item) return;
    setSelectedId(id);
    setIsNew(false);
    setError('');
    setJsonErrors({});
    // Pretty-print JSON fields on load
    const formData: Record<string, any> = { ...item };
    for (const field of fields) {
      if (field.type === 'json' && typeof formData[field.key] === 'string') {
        formData[field.key] = prettyJson(formData[field.key]);
      }
    }
    setForm(formData);
  };

  const handleNew = () => {
    setSelectedId(null);
    setIsNew(true);
    setError('');
    setJsonErrors({});
    const defaults: Record<string, any> = {};
    for (const field of fields) {
      if (field.type === 'checkbox') defaults[field.key] = false;
      else if (field.type === 'number') defaults[field.key] = 0;
      else if (field.type === 'json') defaults[field.key] = '';
      else defaults[field.key] = '';
    }
    setForm(defaults);
  };

  const handleChange = (key: string, value: any) => {
    setForm((prev) => ({ ...prev, [key]: value }));
    // Clear json error for this field when user edits
    if (jsonErrors[key]) {
      setJsonErrors((prev) => {
        const next = { ...prev };
        delete next[key];
        return next;
      });
    }
  };

  const handleSave = async () => {
    setError('');
    // Validate required fields
    for (const field of fields) {
      if (field.required && (!field.visibleWhen || field.visibleWhen(form))) {
        const val = form[field.key];
        if (val === undefined || val === null || val === '') {
          setError(`${field.label} is required`);
          return;
        }
      }
    }
    // Validate JSON fields
    const newJsonErrors: Record<string, string> = {};
    const submitData: Record<string, any> = { ...form };
    for (const field of fields) {
      if (field.type === 'json') {
        const val = submitData[field.key];
        if (val && val.trim() !== '') {
          try {
            JSON.parse(val);
          } catch {
            newJsonErrors[field.key] = 'Invalid JSON';
          }
        }
      }
    }
    if (Object.keys(newJsonErrors).length > 0) {
      setJsonErrors(newJsonErrors);
      return;
    }

    try {
      if (isNew) {
        const created = await api.post<any>(apiPath, submitData);
        setItems((prev) => [...prev, created]);
        setSelectedId(created[idField]);
        setIsNew(false);
        setForm(created);
        // Pretty-print JSON fields
        const formData: Record<string, any> = { ...created };
        for (const field of fields) {
          if (field.type === 'json' && typeof formData[field.key] === 'string') {
            formData[field.key] = prettyJson(formData[field.key]);
          }
        }
        setForm(formData);
      } else if (selectedId) {
        const { [idField]: _id, ...updateData } = submitData;
        const updated = await api.put<any>(`${apiPath}/${selectedId}`, updateData);
        setItems((prev) => prev.map((i) => (i[idField] === selectedId ? updated : i)));
        const formData: Record<string, any> = { ...updated };
        for (const field of fields) {
          if (field.type === 'json' && typeof formData[field.key] === 'string') {
            formData[field.key] = prettyJson(formData[field.key]);
          }
        }
        setForm(formData);
      }
    } catch (err: any) {
      setError(err.message || 'Save failed');
    }
  };

  const handleDelete = async () => {
    if (!selectedId) return;
    if (!confirm(`Delete this ${entityName}?`)) return;
    try {
      await api.del(`${apiPath}/${selectedId}`);
      setItems((prev) => prev.filter((i) => i[idField] !== selectedId));
      setSelectedId(null);
      setForm({});
      setIsNew(false);
    } catch (err: any) {
      setError(err.message || 'Delete failed');
    }
  };

  const showForm = isNew || selectedId !== null;

  return (
    <div style={styles.container}>
      <div style={styles.listPanel}>
        <div style={styles.listTop}>
          <button
            style={{ ...styles.newBtn, ...(disableCreate ? { opacity: 0.5, cursor: 'not-allowed' } : {}) }}
            onClick={disableCreate ? undefined : handleNew}
            disabled={disableCreate}
          >
            + New {entityName}
          </button>
          {disableCreate && disableCreateMessage && (
            <div style={{ fontSize: 11, color: '#999', marginTop: 6, textAlign: 'center' }}>
              {disableCreateMessage}
            </div>
          )}
        </div>
        <div style={styles.listItems}>
          {items.map((item) => (
            <div
              key={item[idField]}
              style={{
                ...styles.listItem,
                ...(item[idField] === selectedId && !isNew ? styles.listItemSelected : {}),
              }}
              onClick={() => handleSelect(item[idField])}
            >
              {item.name || item[idField]}
            </div>
          ))}
        </div>
      </div>
      <div style={styles.formPanel}>
        {showForm ? (
          <>
            <div style={styles.sectionTitle}>
              {isNew ? `New ${entityName}` : `Edit ${entityName}`}
            </div>
            {imagePreview && !isNew && selectedId && (
              <ImagePreview
                entityType={imagePreview.entityType}
                entityId={selectedId}
                description={form.description}
                imagePrompt={form.imagePrompt}
                imageStyle={form.imageStyle}
                imageNegativePrompt={form.imageNegativePrompt}
                imageWidth={form.imageWidth}
                imageHeight={form.imageHeight}
                onUpdate={(fields) => setForm((f: any) => ({ ...f, ...fields }))}
              />
            )}
            {fields.filter((f) => !f.visibleWhen || f.visibleWhen(form)).map((field) => {
              const isIdDisabled = field.key === idField && !isNew;
              const disabled = field.disabled || isIdDisabled;
              return (
                <div key={field.key} style={styles.field}>
                  <label style={styles.label}>{field.label}</label>
                  {field.type === 'text' && (
                    <input
                      style={{ ...styles.input, ...(disabled ? { backgroundColor: '#eee' } : {}) }}
                      value={form[field.key] ?? ''}
                      placeholder={field.placeholder}
                      disabled={disabled}
                      onChange={(e) => handleChange(field.key, e.target.value)}
                    />
                  )}
                  {field.type === 'textarea' && (
                    <textarea
                      style={styles.textarea}
                      value={form[field.key] ?? ''}
                      placeholder={field.placeholder}
                      rows={field.rows ?? 3}
                      onChange={(e) => handleChange(field.key, e.target.value)}
                    />
                  )}
                  {field.type === 'number' && (
                    <input
                      style={styles.input}
                      type="number"
                      value={form[field.key] ?? 0}
                      onChange={(e) => handleChange(field.key, parseFloat(e.target.value) || 0)}
                    />
                  )}
                  {field.type === 'checkbox' && (
                    <label style={{ fontSize: 13 }}>
                      <input
                        type="checkbox"
                        checked={form[field.key] ?? false}
                        onChange={(e) => handleChange(field.key, e.target.checked)}
                      />{' '}
                      {field.placeholder || 'Enabled'}
                    </label>
                  )}
                  {field.type === 'select' && (
                    <select
                      style={styles.select}
                      value={form[field.key] ?? ''}
                      onChange={(e) => handleChange(field.key, e.target.value)}
                    >
                      <option value="">-- Select --</option>
                      {(field.options || []).map((opt) => (
                        <option key={opt.value} value={opt.value}>
                          {opt.label}
                        </option>
                      ))}
                    </select>
                  )}
                  {field.type === 'json' && (
                    <>
                      <textarea
                        style={styles.jsonTextarea}
                        value={form[field.key] ?? ''}
                        placeholder={field.placeholder || '{}'}
                        rows={field.rows ?? 4}
                        onChange={(e) => handleChange(field.key, e.target.value)}
                      />
                      {jsonErrors[field.key] && (
                        <div style={styles.error}>{jsonErrors[field.key]}</div>
                      )}
                    </>
                  )}
                  {field.type === 'radio' && (
                    <div style={{ display: 'flex', gap: 12 }}>
                      {(field.options || []).map((opt) => (
                        <label key={opt.value} style={{ fontSize: 13, cursor: 'pointer' }}>
                          <input
                            type="radio"
                            name={field.key}
                            value={opt.value}
                            checked={form[field.key] === opt.value}
                            onChange={() => handleChange(field.key, opt.value)}
                          />{' '}{opt.label}
                        </label>
                      ))}
                    </div>
                  )}
                  {field.help && <div style={styles.help}>{field.help}</div>}
                </div>
              );
            })}
            {error && <div style={styles.error}>{error}</div>}
            <div style={styles.btnRow}>
              <button style={styles.btnSmall} onClick={handleSave}>
                {isNew ? 'Create' : 'Save'}
              </button>
              {!isNew && selectedId && (
                <button style={styles.btnDanger} onClick={handleDelete}>
                  Delete
                </button>
              )}
            </div>
          </>
        ) : (
          <div style={styles.empty}>
            Select a {entityName.toLowerCase()} to edit, or click "+ New {entityName}" to create one.
          </div>
        )}
      </div>
    </div>
  );
}

export default GenericCrudEditor;
