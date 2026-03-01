import type { CSSProperties } from 'react';

interface ValidationModalProps {
  errors: string[];
  warnings: string[];
  onClose: () => void;
}

const styles: Record<string, CSSProperties> = {
  overlay: {
    position: 'fixed',
    inset: 0,
    backgroundColor: 'rgba(0,0,0,0.6)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 9999,
  },
  modal: {
    backgroundColor: '#1a1a2e',
    color: '#e0e0e0',
    borderRadius: 8,
    padding: '24px 28px',
    minWidth: 420,
    maxWidth: 600,
    maxHeight: '70vh',
    display: 'flex',
    flexDirection: 'column',
    boxShadow: '0 8px 32px rgba(0,0,0,0.5)',
  },
  title: {
    fontSize: 16,
    fontWeight: 700,
    marginBottom: 16,
  },
  body: {
    flex: 1,
    overflowY: 'auto' as const,
    marginBottom: 16,
  },
  sectionLabel: {
    fontSize: 13,
    fontWeight: 600,
    marginBottom: 6,
    marginTop: 12,
  },
  list: {
    listStyle: 'none',
    padding: 0,
    margin: 0,
  },
  item: {
    fontSize: 12,
    lineHeight: 1.6,
    padding: '2px 0',
  },
  bullet: {
    marginRight: 6,
  },
  success: {
    color: '#66bb6a',
    fontSize: 14,
    marginBottom: 8,
  },
  closeBtn: {
    alignSelf: 'flex-end',
    padding: '6px 20px',
    borderRadius: 4,
    border: 'none',
    backgroundColor: '#3949ab',
    color: '#fff',
    fontSize: 13,
    cursor: 'pointer',
  },
};

function ValidationModal({ errors, warnings, onClose }: ValidationModalProps) {
  const hasIssues = errors.length > 0 || warnings.length > 0;

  return (
    <div style={styles.overlay} onClick={onClose}>
      <div style={styles.modal} onClick={(e) => e.stopPropagation()}>
        <div style={styles.title}>Validation Results</div>
        <div style={styles.body}>
          {!hasIssues && (
            <div style={styles.success}>Validation passed — no errors or warnings.</div>
          )}
          {errors.length > 0 && (
            <>
              <div style={{ ...styles.sectionLabel, color: '#ef5350', marginTop: 0 }}>
                Errors ({errors.length})
              </div>
              <ul style={styles.list}>
                {errors.map((e, i) => (
                  <li key={i} style={styles.item}>
                    <span style={{ ...styles.bullet, color: '#ef5350' }}>●</span>
                    {e}
                  </li>
                ))}
              </ul>
            </>
          )}
          {warnings.length > 0 && (
            <>
              <div style={{ ...styles.sectionLabel, color: '#ffa726', marginTop: errors.length > 0 ? 12 : 0 }}>
                Warnings ({warnings.length})
              </div>
              <ul style={styles.list}>
                {warnings.map((w, i) => (
                  <li key={i} style={styles.item}>
                    <span style={{ ...styles.bullet, color: '#ffa726' }}>●</span>
                    {w}
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
        <button style={styles.closeBtn} onClick={onClose}>Close</button>
      </div>
    </div>
  );
}

export default ValidationModal;
