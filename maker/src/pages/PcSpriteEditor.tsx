import { useEffect, useState } from 'react';
import api from '../api';
import ImagePreview from '../components/ImagePreview';
import type { CSSProperties } from 'react';

const RACES = ['HUMAN', 'DWARF', 'ELF', 'HALFLING', 'GNOME', 'HALF_ORC'];
const GENDERS = ['male', 'female', 'neutral'];
const CLASSES = [
  'WARRIOR', 'PALADIN', 'WITCHHUNTER', 'CLERIC', 'PRIEST', 'MISSIONARY',
  'MAGE', 'WARLOCK', 'DRUID', 'RANGER', 'THIEF', 'NINJA', 'MYSTIC', 'BARD', 'GYPSY',
];

interface PcSprite {
  id: string;
  race: string;
  gender: string;
  characterClass: string;
  imagePrompt: string;
  imageStyle: string;
  imageNegativePrompt: string;
  imageWidth: number;
  imageHeight: number;
}

const styles: Record<string, CSSProperties> = {
  container: {
    padding: 24,
    maxWidth: 1200,
  },
  header: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 16,
  },
  title: {
    fontSize: 22,
    fontWeight: 700,
    color: '#1a1a2e',
    margin: 0,
  },
  filterBar: {
    display: 'flex',
    gap: 12,
    marginBottom: 20,
    alignItems: 'center',
    flexWrap: 'wrap',
  },
  select: {
    padding: '6px 10px',
    fontSize: 13,
    border: '1px solid #ccc',
    borderRadius: 4,
    backgroundColor: '#fff',
    minWidth: 140,
  },
  btnOutline: {
    padding: '6px 14px',
    fontSize: 12,
    fontWeight: 600,
    backgroundColor: '#fff',
    color: '#1a1a2e',
    border: '1px solid #1a1a2e',
    borderRadius: 4,
    cursor: 'pointer',
  },
  count: {
    fontSize: 13,
    color: '#666',
    marginLeft: 'auto',
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(120, 1fr))',
    gap: 10,
    marginBottom: 24,
  },
  card: {
    border: '2px solid transparent',
    borderRadius: 6,
    overflow: 'hidden',
    cursor: 'pointer',
    backgroundColor: '#fff',
    transition: 'border-color 0.15s',
    textAlign: 'center',
  },
  cardSelected: {
    borderColor: '#1a1a2e',
  },
  cardImage: {
    width: '100%',
    height: 80,
    objectFit: 'contain',
    backgroundColor: '#f0f0f0',
  },
  cardLabel: {
    fontSize: 10,
    padding: '4px 2px',
    color: '#444',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  editPanel: {
    marginTop: 16,
    padding: 16,
    border: '1px solid #ddd',
    borderRadius: 6,
    backgroundColor: '#fff',
    display: 'flex',
    gap: 20,
    flexWrap: 'wrap',
  },
  editInfo: {
    flex: 1,
    minWidth: 200,
  },
  editTitle: {
    fontSize: 16,
    fontWeight: 600,
    marginBottom: 8,
  },
  editMeta: {
    fontSize: 12,
    color: '#666',
    marginBottom: 4,
  },
};

function formatLabel(sprite: PcSprite): string {
  const race = sprite.race.charAt(0) + sprite.race.slice(1).toLowerCase().replace('_', ' ');
  const gender = sprite.gender.charAt(0).toUpperCase() + sprite.gender.slice(1);
  const cls = sprite.characterClass.charAt(0) + sprite.characterClass.slice(1).toLowerCase();
  return `${race} ${gender} ${cls}`;
}

function PcSpriteEditor() {
  const [sprites, setSprites] = useState<PcSprite[]>([]);
  const [filterRace, setFilterRace] = useState('');
  const [filterGender, setFilterGender] = useState('');
  const [filterClass, setFilterClass] = useState('');
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const fetchSprites = () => {
    const params = new URLSearchParams();
    if (filterRace) params.set('race', filterRace);
    if (filterGender) params.set('gender', filterGender);
    if (filterClass) params.set('class', filterClass);
    const qs = params.toString();
    api.get<PcSprite[]>(`/pc-sprites${qs ? `?${qs}` : ''}`).then(setSprites).catch(console.error);
  };

  useEffect(() => { fetchSprites(); }, [filterRace, filterGender, filterClass]);

  const selected = sprites.find((s) => s.id === selectedId) || null;

  const handleUpdate = (fields: { imagePrompt: string; imageStyle: string; imageNegativePrompt: string; imageWidth: number; imageHeight: number }) => {
    if (!selectedId) return;
    api.put<PcSprite>(`/pc-sprites/${selectedId}`, fields).then((updated) => {
      setSprites((prev) => prev.map((s) => (s.id === updated.id ? updated : s)));
    }).catch(console.error);
  };

  const handleCopyAllPrompts = () => {
    const prompts = sprites.map((s) => {
      return [s.imagePrompt, s.imageStyle && `Style: ${s.imageStyle}`, s.imageNegativePrompt && `Negative: ${s.imageNegativePrompt}`]
        .filter(Boolean)
        .join('\n');
    }).join('\n---\n');
    navigator.clipboard.writeText(prompts).catch(console.error);
  };

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h2 style={styles.title}>Default Player Sprites</h2>
      </div>

      <div style={styles.filterBar}>
        <select style={styles.select} value={filterRace} onChange={(e) => setFilterRace(e.target.value)}>
          <option value="">All Races</option>
          {RACES.map((r) => <option key={r} value={r}>{r.charAt(0) + r.slice(1).toLowerCase().replace('_', ' ')}</option>)}
        </select>
        <select style={styles.select} value={filterGender} onChange={(e) => setFilterGender(e.target.value)}>
          <option value="">All Genders</option>
          {GENDERS.map((g) => <option key={g} value={g}>{g.charAt(0).toUpperCase() + g.slice(1)}</option>)}
        </select>
        <select style={styles.select} value={filterClass} onChange={(e) => setFilterClass(e.target.value)}>
          <option value="">All Classes</option>
          {CLASSES.map((c) => <option key={c} value={c}>{c.charAt(0) + c.slice(1).toLowerCase()}</option>)}
        </select>
        <button style={styles.btnOutline} onClick={handleCopyAllPrompts}>
          Copy All Prompts
        </button>
        <span style={styles.count}>{sprites.length} sprites</span>
      </div>

      <div style={styles.grid}>
        {sprites.map((sprite) => (
          <div
            key={sprite.id}
            style={{ ...styles.card, ...(selectedId === sprite.id ? styles.cardSelected : {}) }}
            onClick={() => setSelectedId(sprite.id)}
          >
            <img
              src={`/api/assets/images/players/${sprite.id}.webp`}
              alt={sprite.id}
              style={styles.cardImage}
              onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
            />
            <div style={styles.cardLabel}>{formatLabel(sprite)}</div>
          </div>
        ))}
      </div>

      {selected && (
        <div style={styles.editPanel}>
          <div style={styles.editInfo}>
            <div style={styles.editTitle}>{formatLabel(selected)}</div>
            <div style={styles.editMeta}>ID: {selected.id}</div>
            <div style={styles.editMeta}>Race: {selected.race} | Gender: {selected.gender} | Class: {selected.characterClass}</div>
          </div>
          <ImagePreview
            entityType="pc-sprite"
            entityId={selected.id}
            assetPath={`images/players/${selected.id}.webp`}
            imagePrompt={selected.imagePrompt}
            imageStyle={selected.imageStyle}
            imageNegativePrompt={selected.imageNegativePrompt}
            imageWidth={selected.imageWidth}
            imageHeight={selected.imageHeight}
            onUpdate={handleUpdate}
          />
        </div>
      )}
    </div>
  );
}

export default PcSpriteEditor;
