import { useNavigate, useParams } from 'react-router-dom';
import api from '../api';
import type { CSSProperties } from 'react';

const styles: Record<string, CSSProperties> = {
  bar: {
    display: 'flex',
    alignItems: 'center',
    gap: 4,
    height: 32,
    backgroundColor: '#12122a',
    padding: '0 12px',
    borderBottom: '1px solid #2a2a4a',
  },
  btn: {
    background: 'none',
    border: 'none',
    color: '#b0b0cc',
    fontSize: 12,
    padding: '4px 10px',
    cursor: 'pointer',
    borderRadius: 3,
  },
};

function MenuBar() {
  const { name } = useParams<{ name: string }>();
  const navigate = useNavigate();

  const handleSaveAs = async () => {
    const newName = prompt('Save project as:', `${name}_copy`);
    if (!newName || !newName.trim()) return;
    try {
      await api.post(`/projects/${encodeURIComponent(name!)}/fork`, {
        newName: newName.trim(),
      });
      await api.post(`/projects/${encodeURIComponent(newName.trim())}/open`);
      navigate(`/project/${encodeURIComponent(newName.trim())}/zones`);
    } catch (err: any) {
      alert(err.message || 'Save As failed');
    }
  };

  const handleLoad = async () => {
    try {
      const data = await api.get<{ projects: { name: string }[] }>('/projects');
      const names = data.projects.map((p) => p.name);
      const choice = prompt(
        `Load project:\n${names.map((n, i) => `${i + 1}. ${n}`).join('\n')}\n\nEnter project name:`
      );
      if (!choice || !choice.trim()) return;
      await api.post(`/projects/${encodeURIComponent(choice.trim())}/open`);
      navigate(`/project/${encodeURIComponent(choice.trim())}/zones`);
    } catch (err: any) {
      alert(err.message || 'Load failed');
    }
  };

  const handleClose = () => {
    navigate('/');
  };

  return (
    <div style={styles.bar}>
      <button
        style={styles.btn}
        onClick={handleSaveAs}
        onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#2a2a4a')}
        onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
      >
        Save As
      </button>
      <button
        style={styles.btn}
        onClick={handleLoad}
        onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#2a2a4a')}
        onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
      >
        Load
      </button>
      <button
        style={styles.btn}
        onClick={handleClose}
        onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#2a2a4a')}
        onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
      >
        Close
      </button>
    </div>
  );
}

export default MenuBar;
