import { useEffect, useState } from 'react';
import api from '../api';
import type { CSSProperties } from 'react';

interface ProviderConfig {
  label: string;
  apiUrl?: string;
  apiKey?: string;
}

interface CustomProvider {
  id: string;
  label: string;
  apiUrl?: string;
  apiKey?: string;
}

interface Settings {
  providers: {
    'stable-diffusion': ProviderConfig;
    openai: ProviderConfig;
    elevenlabs: ProviderConfig;
  };
  customProviders: CustomProvider[];
}

const styles: Record<string, CSSProperties> = {
  page: {
    padding: 32,
    maxWidth: 640,
  },
  title: {
    fontSize: 22,
    fontWeight: 700,
    color: '#1a1a2e',
    marginBottom: 24,
  },
  card: {
    backgroundColor: '#fff',
    borderRadius: 8,
    border: '1px solid #e0e0e0',
    padding: 20,
    marginBottom: 20,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: 600,
    color: '#1a1a2e',
    marginBottom: 16,
  },
  providerBlock: {
    marginBottom: 16,
    paddingBottom: 16,
    borderBottom: '1px solid #f0f0f0',
  },
  providerLabel: {
    fontSize: 14,
    fontWeight: 600,
    color: '#333',
    marginBottom: 8,
  },
  fieldRow: {
    display: 'flex',
    gap: 8,
    marginBottom: 6,
    alignItems: 'center',
  },
  fieldLabel: {
    fontSize: 12,
    color: '#666',
    width: 60,
    flexShrink: 0,
  },
  input: {
    flex: 1,
    padding: '6px 10px',
    fontSize: 13,
    border: '1px solid #ccc',
    borderRadius: 4,
    outline: 'none',
  },
  btn: {
    padding: '8px 16px',
    fontSize: 13,
    fontWeight: 600,
    backgroundColor: '#1a1a2e',
    color: '#fff',
    border: 'none',
    borderRadius: 5,
    cursor: 'pointer',
  },
  btnDanger: {
    padding: '4px 10px',
    fontSize: 12,
    fontWeight: 600,
    backgroundColor: '#d32f2f',
    color: '#fff',
    border: 'none',
    borderRadius: 4,
    cursor: 'pointer',
  },
  btnSecondary: {
    padding: '8px 16px',
    fontSize: 13,
    fontWeight: 600,
    backgroundColor: '#3949ab',
    color: '#fff',
    border: 'none',
    borderRadius: 5,
    cursor: 'pointer',
  },
  note: {
    fontSize: 12,
    color: '#888',
    marginTop: 12,
  },
  actions: {
    display: 'flex',
    gap: 8,
    marginTop: 16,
  },
  status: {
    fontSize: 13,
    marginLeft: 12,
    color: '#4caf50',
  },
};

function SettingsEditor() {
  const [settings, setSettings] = useState<Settings | null>(null);
  const [saveStatus, setSaveStatus] = useState('');

  useEffect(() => {
    api
      .get<Settings>('/settings')
      .then(setSettings)
      .catch(() => {});
  }, []);

  const save = async () => {
    if (!settings) return;
    try {
      await api.put('/settings', settings);
      setSaveStatus('Saved');
      setTimeout(() => setSaveStatus(''), 2000);
    } catch (err: any) {
      alert(err.message || 'Failed to save settings');
    }
  };

  const updateProvider = (
    key: keyof Settings['providers'],
    field: 'apiUrl' | 'apiKey',
    value: string
  ) => {
    if (!settings) return;
    setSettings({
      ...settings,
      providers: {
        ...settings.providers,
        [key]: { ...settings.providers[key], [field]: value },
      },
    });
  };

  const addCustomProvider = () => {
    if (!settings) return;
    const id = `custom_${Date.now()}`;
    setSettings({
      ...settings,
      customProviders: [
        ...settings.customProviders,
        { id, label: '', apiUrl: '', apiKey: '' },
      ],
    });
  };

  const updateCustomProvider = (
    index: number,
    field: keyof CustomProvider,
    value: string
  ) => {
    if (!settings) return;
    const updated = [...settings.customProviders];
    updated[index] = { ...updated[index], [field]: value };
    setSettings({ ...settings, customProviders: updated });
  };

  const removeCustomProvider = (index: number) => {
    if (!settings) return;
    const updated = settings.customProviders.filter((_, i) => i !== index);
    setSettings({ ...settings, customProviders: updated });
  };

  if (!settings) return null;

  return (
    <div style={styles.page}>
      <h2 style={styles.title}>Settings</h2>

      <div style={styles.card}>
        <div style={styles.sectionTitle}>Built-in Providers</div>

        <div style={styles.providerBlock}>
          <div style={styles.providerLabel}>Stable Diffusion</div>
          <div style={styles.fieldRow}>
            <span style={styles.fieldLabel}>URL</span>
            <input
              style={styles.input}
              type="text"
              value={settings.providers['stable-diffusion'].apiUrl || ''}
              onChange={(e) =>
                updateProvider('stable-diffusion', 'apiUrl', e.target.value)
              }
              placeholder="http://localhost:7860"
            />
          </div>
          <div style={styles.fieldRow}>
            <span style={styles.fieldLabel}>Key</span>
            <input
              style={styles.input}
              type="password"
              value={settings.providers['stable-diffusion'].apiKey || ''}
              onChange={(e) =>
                updateProvider('stable-diffusion', 'apiKey', e.target.value)
              }
            />
          </div>
        </div>

        <div style={styles.providerBlock}>
          <div style={styles.providerLabel}>OpenAI</div>
          <div style={styles.fieldRow}>
            <span style={styles.fieldLabel}>URL</span>
            <input
              style={styles.input}
              type="text"
              value={settings.providers.openai.apiUrl || ''}
              onChange={(e) =>
                updateProvider('openai', 'apiUrl', e.target.value)
              }
              placeholder="https://api.openai.com/v1"
            />
          </div>
          <div style={styles.fieldRow}>
            <span style={styles.fieldLabel}>Key</span>
            <input
              style={styles.input}
              type="password"
              value={settings.providers.openai.apiKey || ''}
              onChange={(e) =>
                updateProvider('openai', 'apiKey', e.target.value)
              }
            />
          </div>
        </div>

        <div style={{ ...styles.providerBlock, borderBottom: 'none', marginBottom: 0, paddingBottom: 0 }}>
          <div style={styles.providerLabel}>ElevenLabs</div>
          <div style={styles.fieldRow}>
            <span style={styles.fieldLabel}>URL</span>
            <input
              style={styles.input}
              type="text"
              value={settings.providers.elevenlabs.apiUrl || ''}
              onChange={(e) =>
                updateProvider('elevenlabs', 'apiUrl', e.target.value)
              }
              placeholder="https://api.elevenlabs.io/v1"
            />
          </div>
          <div style={styles.fieldRow}>
            <span style={styles.fieldLabel}>Key</span>
            <input
              style={styles.input}
              type="password"
              value={settings.providers.elevenlabs.apiKey || ''}
              onChange={(e) =>
                updateProvider('elevenlabs', 'apiKey', e.target.value)
              }
            />
          </div>
        </div>
      </div>

      <div style={styles.card}>
        <div style={styles.sectionTitle}>Custom Providers</div>
        {settings.customProviders.map((cp, i) => (
          <div key={cp.id} style={styles.providerBlock}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
              <input
                style={{ ...styles.input, fontWeight: 600, maxWidth: 200 }}
                type="text"
                value={cp.label}
                onChange={(e) => updateCustomProvider(i, 'label', e.target.value)}
                placeholder="Provider name"
              />
              <button style={styles.btnDanger} onClick={() => removeCustomProvider(i)}>
                Remove
              </button>
            </div>
            <div style={styles.fieldRow}>
              <span style={styles.fieldLabel}>URL</span>
              <input
                style={styles.input}
                type="text"
                value={cp.apiUrl || ''}
                onChange={(e) => updateCustomProvider(i, 'apiUrl', e.target.value)}
                placeholder="https://..."
              />
            </div>
            <div style={styles.fieldRow}>
              <span style={styles.fieldLabel}>Key</span>
              <input
                style={styles.input}
                type="password"
                value={cp.apiKey || ''}
                onChange={(e) => updateCustomProvider(i, 'apiKey', e.target.value)}
              />
            </div>
          </div>
        ))}
        <button style={styles.btnSecondary} onClick={addCustomProvider}>
          Add Provider
        </button>
      </div>

      <div style={styles.actions}>
        <button style={styles.btn} onClick={save}>
          Save Settings
        </button>
        {saveStatus && <span style={styles.status}>{saveStatus}</span>}
      </div>
      <p style={styles.note}>
        Keys are stored locally in maker/settings.json
      </p>
    </div>
  );
}

export default SettingsEditor;
