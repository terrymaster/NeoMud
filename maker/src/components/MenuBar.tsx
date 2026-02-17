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
  separator: {
    width: 1,
    height: 16,
    backgroundColor: '#2a2a4a',
    margin: '0 4px',
  },
  spacer: {
    flex: 1,
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

  const handleSwitchProject = () => {
    navigate('/');
  };

  const handleQuit = async () => {
    if (!confirm('Shut down the Maker server?')) return;
    try {
      await api.post('/shutdown');
    } catch {
      // server is already gone
    }
  };

  const handleValidate = async () => {
    try {
      const result = await api.get<{ errors: string[]; warnings: string[] }>('/export/validate');
      if (result.errors.length === 0 && result.warnings.length === 0) {
        alert('Validation passed — no errors or warnings.');
      } else {
        const parts: string[] = [];
        if (result.errors.length > 0) {
          parts.push('Errors:\n' + result.errors.map((e) => `  • ${e}`).join('\n'));
        }
        if (result.warnings.length > 0) {
          parts.push('Warnings:\n' + result.warnings.map((w) => `  • ${w}`).join('\n'));
        }
        alert(parts.join('\n\n'));
      }
    } catch (err: any) {
      alert(err.message || 'Validation failed');
    }
  };

  const handleExportNmd = () => {
    const a = document.createElement('a');
    a.href = '/api/export/nmd';
    a.click();
  };

  const handlePackage = async () => {
    try {
      const result = await api.get<{ errors: string[]; warnings: string[] }>('/export/validate');
      if (result.errors.length > 0) {
        alert(
          'Cannot package — validation errors:\n' +
            result.errors.map((e) => `  • ${e}`).join('\n')
        );
        return;
      }
      if (result.warnings.length > 0) {
        const proceed = confirm(
          'Validation warnings:\n' +
            result.warnings.map((w) => `  • ${w}`).join('\n') +
            '\n\nContinue with packaging?'
        );
        if (!proceed) return;
      }
      const a = document.createElement('a');
      a.href = '/api/export/package';
      a.click();
    } catch (err: any) {
      alert(err.message || 'Package failed');
    }
  };

  const hoverOn = (e: React.MouseEvent<HTMLButtonElement>) =>
    (e.currentTarget.style.backgroundColor = '#2a2a4a');
  const hoverOff = (e: React.MouseEvent<HTMLButtonElement>) =>
    (e.currentTarget.style.backgroundColor = 'transparent');

  return (
    <div style={styles.bar}>
      <button style={styles.btn} onClick={handleSaveAs} onMouseEnter={hoverOn} onMouseLeave={hoverOff}>
        Save As
      </button>
      <button style={styles.btn} onClick={handleSwitchProject} onMouseEnter={hoverOn} onMouseLeave={hoverOff}>
        Switch Project
      </button>
      <div style={styles.separator} />
      <button style={styles.btn} onClick={handleValidate} onMouseEnter={hoverOn} onMouseLeave={hoverOff}>
        Validate
      </button>
      <button style={styles.btn} onClick={handleExportNmd} onMouseEnter={hoverOn} onMouseLeave={hoverOff}>
        Export .nmd
      </button>
      <button style={styles.btn} onClick={handlePackage} onMouseEnter={hoverOn} onMouseLeave={hoverOff}>
        Package .nmd
      </button>
      <div style={styles.spacer} />
      <button style={{ ...styles.btn, color: '#ff6b6b' }} onClick={handleQuit} onMouseEnter={hoverOn} onMouseLeave={hoverOff}>
        Quit Server
      </button>
    </div>
  );
}

export default MenuBar;
