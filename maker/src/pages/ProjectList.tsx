import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import type { CSSProperties } from 'react';

interface ProjectsResponse {
  projects: string[];
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
    cursor: 'pointer',
    fontSize: 15,
    fontWeight: 500,
    color: '#1a1a2e',
    border: '1px solid #e0e0e0',
    transition: 'background 0.15s',
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
};

function ProjectList() {
  const [projects, setProjects] = useState<string[]>([]);
  const [newName, setNewName] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    api
      .get<ProjectsResponse>('/projects')
      .then((data) => setProjects(data.projects))
      .catch(() => {});
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
        {error && <p style={styles.error}>{error}</p>}
        {projects.length === 0 ? (
          <p style={styles.empty}>No projects yet. Create one above.</p>
        ) : (
          <ul style={styles.list}>
            {projects.map((name) => (
              <li
                key={name}
                style={styles.listItem}
                onClick={() => handleOpen(name)}
                onMouseEnter={(e) =>
                  ((e.currentTarget as HTMLElement).style.backgroundColor = '#f0f0ff')
                }
                onMouseLeave={(e) =>
                  ((e.currentTarget as HTMLElement).style.backgroundColor = '#fff')
                }
              >
                {name}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

export default ProjectList;
