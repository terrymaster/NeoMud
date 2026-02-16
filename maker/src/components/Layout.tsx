import { useEffect, useState } from 'react';
import { NavLink, Outlet, useParams } from 'react-router-dom';
import api from '../api';
import MenuBar from './MenuBar';
import type { CSSProperties } from 'react';

const navItems = [
  { label: 'Zones', path: 'zones' },
  { label: 'Items', path: 'items' },
  { label: 'NPCs', path: 'npcs' },
  { label: 'Classes', path: 'classes' },
  { label: 'Races', path: 'races' },
  { label: 'Skills', path: 'skills' },
  { label: 'Spells', path: 'spells' },
  { label: 'Loot Tables', path: 'loot-tables' },
];

const styles: Record<string, CSSProperties> = {
  wrapper: {
    display: 'flex',
    flexDirection: 'column',
    height: '100vh',
    overflow: 'hidden',
  },
  container: {
    display: 'flex',
    flex: 1,
    overflow: 'hidden',
  },
  sidebar: {
    width: 220,
    minWidth: 220,
    backgroundColor: '#1a1a2e',
    color: '#e0e0e0',
    display: 'flex',
    flexDirection: 'column',
    padding: 0,
  },
  projectName: {
    padding: '20px 16px 12px',
    fontSize: 18,
    fontWeight: 700,
    color: '#ffffff',
    borderBottom: '1px solid #2a2a4a',
    marginBottom: 8,
  },
  readOnlyBadge: {
    display: 'inline-block',
    fontSize: 10,
    fontWeight: 600,
    padding: '2px 6px',
    borderRadius: 3,
    backgroundColor: '#3949ab',
    color: '#fff',
    marginLeft: 8,
    verticalAlign: 'middle',
  },
  nav: {
    display: 'flex',
    flexDirection: 'column',
    gap: 2,
    padding: '0 8px',
  },
  link: {
    display: 'block',
    padding: '10px 12px',
    borderRadius: 6,
    color: '#b0b0cc',
    textDecoration: 'none',
    fontSize: 14,
    transition: 'background 0.15s, color 0.15s',
  },
  linkActive: {
    backgroundColor: '#16213e',
    color: '#ffffff',
    fontWeight: 600,
  },
  content: {
    flex: 1,
    backgroundColor: '#f5f5f5',
    overflow: 'auto',
  },
};

function Layout() {
  const { name } = useParams<{ name: string }>();
  const [readOnly, setReadOnly] = useState(false);

  useEffect(() => {
    api
      .get<{ active: string | null; activeReadOnly: boolean }>('/projects')
      .then((data) => {
        if (data.active === name) {
          setReadOnly(data.activeReadOnly);
        }
      })
      .catch(() => {});
  }, [name]);

  return (
    <div style={styles.wrapper}>
      <MenuBar />
      <div style={styles.container}>
      <div style={styles.sidebar}>
        <div style={styles.projectName}>
          {name}
          {readOnly && <span style={styles.readOnlyBadge}>Read Only</span>}
        </div>
        <nav style={styles.nav}>
          {navItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              style={({ isActive }) => ({
                ...styles.link,
                ...(isActive ? styles.linkActive : {}),
              })}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </div>
      <div style={styles.content}>
        <Outlet />
      </div>
      </div>
    </div>
  );
}

export default Layout;
