import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import type { CSSProperties } from 'react';

interface ProjectInfo {
  name: string;
  readOnly: boolean;
}

interface ProjectsResponse {
  projects: ProjectInfo[];
  active: string | null;
}

const styles: Record<string, CSSProperties> = {
  page: {
    minHeight: '100vh',
    backgroundColor: '#f5f5f5',
    display: 'flex',
    justifyContent: 'center',
    paddingTop: 80,
  },
  container: {
    width: 480,
  },
  title: {
    fontSize: 28,
    fontWeight: 700,
    marginBottom: 24,
    color: '#1a1a2e',
  },
  form: {
    display: 'flex',
    gap: 8,
    marginBottom: 32,
  },
  input: {
    flex: 1,
    padding: '10px 14px',
    fontSize: 14,
    border: '1px solid #ccc',
    borderRadius: 6,
    outline: 'none',
  },
  button: {
    padding: '10px 20px',
    fontSize: 14,
    fontWeight: 600,
    backgroundColor: '#1a1a2e',
    color: '#fff',
    border: 'none',
    borderRadius: 6,
    cursor: 'pointer',
  },
  list: {
    listStyle: 'none',
    padding: 0,
    margin: 0,
  },
  listItem: {
    padding: '14px 16px',
    backgroundColor: '#fff',
    borderRadius: 6,
    marginBottom: 8,
    fontSize: 15,
    fontWeight: 500,
    color: '#1a1a2e',
    border: '1px solid #e0e0e0',
    transition: 'background 0.15s',
    display: 'flex',
    alignItems: 'center',
    gap: 10,
  },
  empty: {
    color: '#888',
    fontSize: 14,
    textAlign: 'center',
    marginTop: 16,
  },
  error: {
    color: '#d32f2f',
    fontSize: 13,
    marginBottom: 12,
  },
  badge: {
    fontSize: 11,
    fontWeight: 600,
    padding: '2px 8px',
    borderRadius: 4,
    backgroundColor: '#e8eaf6',
    color: '#3949ab',
    whiteSpace: 'nowrap',
  },
  projectName: {
    flex: 1,
    cursor: 'pointer',
  },
  forkBtn: {
    padding: '6px 12px',
    fontSize: 12,
    fontWeight: 600,
    backgroundColor: '#3949ab',
    color: '#fff',
    border: 'none',
    borderRadius: 4,
    cursor: 'pointer',
    whiteSpace: 'nowrap',
  },
};

function ProjectList() {
  const [projects, setProjects] = useState<ProjectInfo[]>([]);
  const [newName, setNewName] = useState('');
  const [importPath, setImportPath] = useState('');
  const [importName, setImportName] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const loadProjects = () => {
    api
      .get<ProjectsResponse>('/projects')
      .then((data) => setProjects(data.projects))
      .catch(() => {});
  };

  useEffect(() => {
    loadProjects();
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = newName.trim();
    if (!trimmed) return;
    setError('');
    try {
      await api.post('/projects', { name: trimmed });
      navigate(`/project/${encodeURIComponent(trimmed)}/zones`);
    } catch (err: any) {
      setError(err.message || 'Failed to create project');
    }
  };

  const handleOpen = async (name: string) => {
    setError('');
    try {
      await api.post(`/projects/${encodeURIComponent(name)}/open`);
      navigate(`/project/${encodeURIComponent(name)}/zones`);
    } catch (err: any) {
      setError(err.message || 'Failed to open project');
    }
  };

  const handleFork = async (sourceName: string) => {
    const forkName = prompt(`Fork "${sourceName}" as:`, `${sourceName}_copy`);
    if (!forkName || !forkName.trim()) return;
    setError('');
    try {
      await api.post(`/projects/${encodeURIComponent(sourceName)}/fork`, {
        newName: forkName.trim(),
      });
      loadProjects();
    } catch (err: any) {
      setError(err.message || 'Failed to fork project');
    }
  };

  const handleImport = async (e: React.FormEvent) => {
    e.preventDefault();
    const trimmedPath = importPath.trim();
    if (!trimmedPath) return;
    setError('');
    try {
      const result = await api.post<{ name: string }>('/projects/import', {
        path: trimmedPath,
        name: importName.trim() || undefined,
      });
      await api.post(`/projects/${encodeURIComponent(result.name)}/open`);
      navigate(`/project/${encodeURIComponent(result.name)}/zones`);
    } catch (err: any) {
      setError(err.message || 'Import failed');
    }
  };

  return (
    <div style={styles.page}>
      <div style={styles.container}>
        <h1 style={styles.title}>NeoMUD Maker</h1>
        <form style={styles.form} onSubmit={handleCreate}>
          <input
            style={styles.input}
            type="text"
            placeholder="New project name..."
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
          />
          <button style={styles.button} type="submit">
            New Project
          </button>
        </form>
        <form style={{ ...styles.form, marginBottom: 12 }} onSubmit={handleImport}>
          <input
            style={styles.input}
            type="text"
            placeholder="Path to .nmd file..."
            value={importPath}
            onChange={(e) => setImportPath(e.target.value)}
          />
          <input
            style={{ ...styles.input, maxWidth: 140 }}
            type="text"
            placeholder="Name (optional)"
            value={importName}
            onChange={(e) => setImportName(e.target.value)}
          />
          <button style={styles.button} type="submit">
            Import .nmd
          </button>
        </form>
        {error && <p style={styles.error}>{error}</p>}
        {projects.length === 0 ? (
          <p style={styles.empty}>No projects yet. Create one above.</p>
        ) : (
          <ul style={styles.list}>
            {projects.map((proj) => (
              <li
                key={proj.name}
                style={styles.listItem}
                onMouseEnter={(e) =>
                  ((e.currentTarget as HTMLElement).style.backgroundColor = '#f0f0ff')
                }
                onMouseLeave={(e) =>
                  ((e.currentTarget as HTMLElement).style.backgroundColor = '#fff')
                }
              >
                <span
                  style={styles.projectName}
                  onClick={() => handleOpen(proj.name)}
                >
                  {proj.name}
                </span>
                {proj.readOnly && (
                  <>
                    <span style={styles.badge}>Read Only</span>
                    <button
                      style={styles.forkBtn}
                      onClick={(e) => {
                        e.stopPropagation();
                        handleFork(proj.name);
                      }}
                    >
                      Fork
                    </button>
                  </>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

export default ProjectList;
