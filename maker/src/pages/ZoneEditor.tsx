import { useEffect, useState, useCallback, useMemo } from 'react';
import api from '../api';
import MapCanvas from '../components/MapCanvas';
import ImagePreview from '../components/ImagePreview';
import AudioPreview from '../components/AudioPreview';
import SfxPreview from '../components/SfxPreview';
import type { CSSProperties } from 'react';

interface Zone {
  id: string;
  name: string;
  description: string;
  safe: boolean;
  bgm: string;
  bgmPrompt: string;
  bgmDuration: number;
  spawnRoom: string | null;
  spawnMaxEntities: number;
  spawnMaxPerRoom: number;
  spawnRateTicks: number;
  imageStyle: string;
  imageNegativePrompt: string;
}

interface Exit {
  fromRoomId: string;
  toRoomId: string;
  direction: string;
}

interface Room {
  id: string;
  name: string;
  description: string;
  x: number;
  y: number;
  backgroundImage: string;
  bgm: string;
  bgmPrompt: string;
  bgmDuration: number;
  departSound: string;
  effects: string;
  lockedExits: string;
  imagePrompt: string;
  imageStyle: string;
  imageNegativePrompt: string;
  imageWidth: number;
  imageHeight: number;
  exits: Exit[];
}

interface ZoneWithRooms extends Zone {
  rooms: Room[];
}

const styles: Record<string, CSSProperties> = {
  container: {
    display: 'flex',
    height: '100%',
    overflow: 'hidden',
  },
  leftPanel: {
    width: 200,
    minWidth: 200,
    borderRight: '1px solid #ddd',
    backgroundColor: '#fff',
    display: 'flex',
    flexDirection: 'column',
    overflow: 'hidden',
  },
  leftTop: {
    padding: 12,
    borderBottom: '1px solid #eee',
  },
  zoneList: {
    flex: 1,
    overflowY: 'auto',
    padding: '4px 0',
  },
  zoneItem: {
    padding: '8px 12px',
    cursor: 'pointer',
    fontSize: 13,
    borderBottom: '1px solid #f0f0f0',
  },
  zoneItemSelected: {
    backgroundColor: '#e8eaf6',
    fontWeight: 600,
  },
  zoneProps: {
    borderTop: '1px solid #ddd',
    padding: 12,
    overflowY: 'auto',
    maxHeight: 320,
  },
  centerPanel: {
    flex: 1,
    position: 'relative',
    overflow: 'hidden',
  },
  rightPanel: {
    width: 250,
    minWidth: 250,
    borderLeft: '1px solid #ddd',
    backgroundColor: '#fff',
    padding: 12,
    overflowY: 'auto',
  },
  label: {
    display: 'block',
    fontSize: 11,
    fontWeight: 600,
    color: '#666',
    marginBottom: 2,
    marginTop: 8,
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
  btnSmall: {
    padding: '6px 12px',
    fontSize: 12,
    fontWeight: 600,
    backgroundColor: '#1a1a2e',
    color: '#fff',
    border: 'none',
    borderRadius: 4,
    cursor: 'pointer',
    marginTop: 8,
  },
  btnDanger: {
    padding: '6px 12px',
    fontSize: 12,
    fontWeight: 600,
    backgroundColor: '#d32f2f',
    color: '#fff',
    border: 'none',
    borderRadius: 4,
    cursor: 'pointer',
    marginTop: 8,
  },
  sectionTitle: {
    fontSize: 14,
    fontWeight: 700,
    marginBottom: 8,
    color: '#1a1a2e',
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
};

const ALL_DIRECTIONS = [
  'NORTH', 'SOUTH', 'EAST', 'WEST',
  'NORTHEAST', 'NORTHWEST', 'SOUTHEAST', 'SOUTHWEST',
  'UP', 'DOWN',
];

interface AllRoomsGroup {
  zoneId: string;
  zoneName: string;
  rooms: { id: string; name: string }[];
}

function computeLayerMap(rooms: Room[], exits: Exit[]) {
  const downTargets = new Map<string, string[]>();
  const upTargets = new Map<string, string[]>();
  const roomIds = new Set(rooms.map((r) => r.id));

  for (const exit of exits) {
    if (exit.direction === 'DOWN' && roomIds.has(exit.toRoomId)) {
      const list = downTargets.get(exit.fromRoomId) || [];
      list.push(exit.toRoomId);
      downTargets.set(exit.fromRoomId, list);
    }
    if (exit.direction === 'UP' && roomIds.has(exit.toRoomId)) {
      const list = upTargets.get(exit.fromRoomId) || [];
      list.push(exit.toRoomId);
      upTargets.set(exit.fromRoomId, list);
    }
  }

  const layerMap = new Map<string, number>();
  for (const room of rooms) {
    if (layerMap.has(room.id)) continue;
    layerMap.set(room.id, 0);
    const queue = [room.id];
    while (queue.length > 0) {
      const current = queue.shift()!;
      const layer = layerMap.get(current)!;
      for (const target of downTargets.get(current) || []) {
        if (!layerMap.has(target)) {
          layerMap.set(target, layer - 1);
          queue.push(target);
        }
      }
      for (const target of upTargets.get(current) || []) {
        if (!layerMap.has(target)) {
          layerMap.set(target, layer + 1);
          queue.push(target);
        }
      }
    }
  }

  const layers = [...new Set(layerMap.values())].sort((a, b) => b - a);
  return { layerMap, layers };
}

let zoneCounter = 0;

function ZoneEditor() {
  const [zones, setZones] = useState<Zone[]>([]);
  const [selectedZoneId, setSelectedZoneId] = useState<string | null>(null);
  const [rooms, setRooms] = useState<Room[]>([]);
  const [selectedRoomId, setSelectedRoomId] = useState<string | null>(null);

  const [zoneForm, setZoneForm] = useState<Partial<Zone>>({});
  const [roomForm, setRoomForm] = useState<Partial<Room>>({});

  // Layer navigation state
  const [currentLayer, setCurrentLayer] = useState(0);

  // Manual exit creation state
  const [newExitDir, setNewExitDir] = useState('');
  const [newExitTarget, setNewExitTarget] = useState('');
  const [allRooms, setAllRooms] = useState<AllRoomsGroup[]>([]);
  const [movementSfx, setMovementSfx] = useState<{ id: string; label: string }[]>([]);

  // Load zones list + all rooms for exit picker
  useEffect(() => {
    api.get<Zone[]>('/zones').then((zoneList) => {
      setZones(zoneList);
      // Load all rooms across all zones for the manual exit dropdown
      const promises = zoneList.map((z) =>
        api.get<Room[]>(`/zones/${z.id}/rooms`).then((zoneRooms) => ({
          zoneId: z.id,
          zoneName: z.name,
          rooms: zoneRooms.map((r) => ({ id: r.id, name: r.name })),
        }))
      );
      Promise.all(promises).then(setAllRooms).catch(() => {});
    }).catch(() => {});
    api.get<{ id: string; label: string }[]>('/default-sfx?category=movement')
      .then(setMovementSfx).catch(() => {});
  }, []);

  // Load zone detail when selected
  useEffect(() => {
    if (!selectedZoneId) {
      setRooms([]);
      setSelectedRoomId(null);
      return;
    }
    api
      .get<ZoneWithRooms>(`/zones/${selectedZoneId}`)
      .then((data) => {
        setRooms(data.rooms || []);
        setZoneForm({
          id: data.id,
          name: data.name,
          description: data.description,
          safe: data.safe,
          bgm: data.bgm,
          bgmPrompt: data.bgmPrompt,
          bgmDuration: data.bgmDuration,
          spawnRoom: data.spawnRoom,
          spawnMaxEntities: data.spawnMaxEntities,
          spawnMaxPerRoom: data.spawnMaxPerRoom,
          spawnRateTicks: data.spawnRateTicks,
          imageStyle: data.imageStyle,
          imageNegativePrompt: data.imageNegativePrompt,
        });
      })
      .catch(() => {});
  }, [selectedZoneId]);

  // Sync room form when selection changes
  useEffect(() => {
    if (!selectedRoomId) {
      setRoomForm({});
      return;
    }
    const room = rooms.find((r) => r.id === selectedRoomId);
    if (room) setRoomForm({ ...room });
  }, [selectedRoomId, rooms]);

  // Collect all exits from rooms
  const allExits: Exit[] = rooms.flatMap((r) => r.exits || []);

  // Compute layers from UP/DOWN exit topology
  const { layerMap, layers } = useMemo(
    () => {
      const result = computeLayerMap(rooms, allExits);
      if (allExits.some((e) => e.direction === 'UP' || e.direction === 'DOWN')) {
        console.log('[LayerDebug] UP/DOWN exits found:', allExits.filter((e) => e.direction === 'UP' || e.direction === 'DOWN'));
        console.log('[LayerDebug] layerMap:', Object.fromEntries(result.layerMap));
        console.log('[LayerDebug] layers:', result.layers);
      }
      return result;
    },
    [rooms, allExits]
  );

  // Reset layer to 0 when zone changes (if 0 exists), otherwise pick highest
  useEffect(() => {
    if (layers.length === 0) {
      setCurrentLayer(0);
    } else if (!layers.includes(currentLayer)) {
      setCurrentLayer(layers.includes(0) ? 0 : layers[0]);
    }
  }, [layers]);

  // Filter rooms and exits to the current layer
  const layerRoomIds = useMemo(() => {
    const ids = new Set<string>();
    for (const room of rooms) {
      if ((layerMap.get(room.id) ?? 0) === currentLayer) ids.add(room.id);
    }
    return ids;
  }, [rooms, layerMap, currentLayer]);

  const filteredRooms = useMemo(
    () => rooms.filter((r) => layerRoomIds.has(r.id)),
    [rooms, layerRoomIds]
  );

  const filteredExits = useMemo(
    () => allExits.filter(
      (e) =>
        layerRoomIds.has(e.fromRoomId) &&
        layerRoomIds.has(e.toRoomId) &&
        e.direction !== 'UP' &&
        e.direction !== 'DOWN'
    ),
    [allExits, layerRoomIds]
  );

  // Vertical exit indicators for rooms on the current layer
  const verticalExits = useMemo(() => {
    const result: { roomId: string; direction: string }[] = [];
    for (const exit of allExits) {
      if (
        (exit.direction === 'UP' || exit.direction === 'DOWN') &&
        layerRoomIds.has(exit.fromRoomId)
      ) {
        result.push({ roomId: exit.fromRoomId, direction: exit.direction });
      }
    }
    return result;
  }, [allExits, layerRoomIds]);

  const layerLabel = (layer: number) => {
    if (layer === 0) return 'Ground';
    if (layer > 0) return `Above +${layer}`;
    return `Below ${layer}`;
  };

  const canGoUp = layers.some((l) => l > currentLayer);
  const canGoDown = layers.some((l) => l < currentLayer);

  const handleNewZone = async () => {
    const id = `zone_${++zoneCounter}_${Date.now()}`;
    try {
      const zone = await api.post<Zone>('/zones', {
        id,
        name: 'New Zone',
        description: '',
      });
      setZones((prev) => [...prev, zone]);
      setSelectedZoneId(zone.id);
    } catch {}
  };

  const handleSaveZone = async () => {
    if (!selectedZoneId) return;
    try {
      const { id: _, ...fields } = zoneForm;
      const updated = await api.put<Zone>(`/zones/${selectedZoneId}`, fields);
      setZones((prev) => prev.map((z) => (z.id === updated.id ? updated : z)));
      setZoneForm(updated);
    } catch {}
  };

  const handleDeleteZone = async () => {
    if (!selectedZoneId) return;
    try {
      await api.del(`/zones/${selectedZoneId}`);
      setZones((prev) => prev.filter((z) => z.id !== selectedZoneId));
      setSelectedZoneId(null);
    } catch {}
  };

  const handleCreateRoom = useCallback(
    async (x: number, y: number) => {
      if (!selectedZoneId) return;
      const roomSlug = `room_${x}_${y}`;
      try {
        const room = await api.post<Room>(`/zones/${selectedZoneId}/rooms`, {
          id: roomSlug,
          name: 'New Room',
          description: '',
          x,
          y,
        });
        setRooms((prev) => [...prev, { ...room, exits: room.exits || [] }]);
        setSelectedRoomId(room.id);
      } catch {}
    },
    [selectedZoneId]
  );

  const handleSelectRoom = useCallback((id: string) => {
    setSelectedRoomId(id);
  }, []);

  const handleCreateExit = useCallback(
    async (fromId: string, toId: string) => {
      // Determine direction from grid positions
      const fromRoom = rooms.find((r) => r.id === fromId);
      const toRoom = rooms.find((r) => r.id === toId);
      if (!fromRoom || !toRoom) return;

      const dx = toRoom.x - fromRoom.x;
      const dy = toRoom.y - fromRoom.y;
      let direction = '';
      if (dy < 0 && dx === 0) direction = 'NORTH';
      else if (dy > 0 && dx === 0) direction = 'SOUTH';
      else if (dx > 0 && dy === 0) direction = 'EAST';
      else if (dx < 0 && dy === 0) direction = 'WEST';
      else if (dy < 0 && dx > 0) direction = 'NORTHEAST';
      else if (dy < 0 && dx < 0) direction = 'NORTHWEST';
      else if (dy > 0 && dx > 0) direction = 'SOUTHEAST';
      else if (dy > 0 && dx < 0) direction = 'SOUTHWEST';
      else return;

      try {
        await api.post(`/rooms/${fromId}/exits`, { direction, toRoomId: toId });
        // Reload zone to get updated exits
        const data = await api.get<ZoneWithRooms>(`/zones/${selectedZoneId}`);
        setRooms(data.rooms || []);
      } catch {}
    },
    [rooms, selectedZoneId]
  );

  const handleSaveRoom = async () => {
    if (!selectedZoneId || !selectedRoomId) return;
    try {
      const roomSuffix = selectedRoomId.split(':').slice(1).join(':');
      const { exits: _, ...fields } = roomForm;
      const updated = await api.put<Room>(
        `/zones/${selectedZoneId}/rooms/${roomSuffix}`,
        fields
      );
      setRooms((prev) =>
        prev.map((r) =>
          r.id === updated.id ? { ...updated, exits: r.exits } : r
        )
      );
      setRoomForm((prev) => ({ ...prev, ...updated }));
    } catch {}
  };

  const handleDeleteRoom = async () => {
    if (!selectedZoneId || !selectedRoomId) return;
    try {
      const roomSuffix = selectedRoomId.split(':').slice(1).join(':');
      await api.del(`/zones/${selectedZoneId}/rooms/${roomSuffix}`);
      setRooms((prev) => prev.filter((r) => r.id !== selectedRoomId));
      setSelectedRoomId(null);
    } catch {}
  };

  const handleDeleteExit = async (roomId: string, direction: string) => {
    try {
      await api.del(`/rooms/${roomId}/exits/${direction}`);
      const data = await api.get<ZoneWithRooms>(`/zones/${selectedZoneId}`);
      setRooms(data.rooms || []);
    } catch {}
  };

  const handleAddExitManual = async () => {
    if (!selectedRoomId || !newExitDir || !newExitTarget) return;
    try {
      await api.post(`/rooms/${selectedRoomId}/exits`, {
        direction: newExitDir,
        toRoomId: newExitTarget,
      });
      // Reload zone data to get updated exits
      if (selectedZoneId) {
        const data = await api.get<ZoneWithRooms>(`/zones/${selectedZoneId}`);
        setRooms(data.rooms || []);
      }
      setNewExitDir('');
      setNewExitTarget('');
      // Refresh all rooms list too
      const zoneList = await api.get<Zone[]>('/zones');
      const promises = zoneList.map((z) =>
        api.get<Room[]>(`/zones/${z.id}/rooms`).then((zoneRooms) => ({
          zoneId: z.id,
          zoneName: z.name,
          rooms: zoneRooms.map((r) => ({ id: r.id, name: r.name })),
        }))
      );
      Promise.all(promises).then(setAllRooms).catch(() => {});
    } catch {}
  };

  const selectedZone = zones.find((z) => z.id === selectedZoneId);
  const selectedRoom = rooms.find((r) => r.id === selectedRoomId);

  return (
    <div style={styles.container}>
      {/* Left Panel */}
      <div style={styles.leftPanel}>
        <div style={styles.leftTop}>
          <button style={styles.newBtn} onClick={handleNewZone}>
            + New Zone
          </button>
        </div>
        <div style={styles.zoneList}>
          {zones.map((z) => (
            <div
              key={z.id}
              style={{
                ...styles.zoneItem,
                ...(z.id === selectedZoneId ? styles.zoneItemSelected : {}),
              }}
              onClick={() => setSelectedZoneId(z.id)}
            >
              {z.name}
            </div>
          ))}
        </div>
        {selectedZone && (
          <div style={styles.zoneProps}>
            <div style={styles.sectionTitle}>Zone Properties</div>
            <label style={styles.label}>Name</label>
            <input
              style={styles.input}
              value={zoneForm.name || ''}
              onChange={(e) => setZoneForm((f) => ({ ...f, name: e.target.value }))}
            />
            <label style={styles.label}>Description</label>
            <textarea
              style={styles.textarea}
              value={zoneForm.description || ''}
              onChange={(e) => setZoneForm((f) => ({ ...f, description: e.target.value }))}
            />
            <label style={styles.label}>
              <input
                type="checkbox"
                checked={zoneForm.safe || false}
                onChange={(e) => setZoneForm((f) => ({ ...f, safe: e.target.checked }))}
              />{' '}
              Safe Zone
            </label>
            <label style={styles.label}>BGM Track ID</label>
            <input
              style={styles.input}
              value={zoneForm.bgm || ''}
              placeholder="e.g. town_peaceful"
              onChange={(e) => setZoneForm((f) => ({ ...f, bgm: e.target.value }))}
            />
            <AudioPreview
              entityType="zone"
              entityId={selectedZoneId!}
              bgm={zoneForm.bgm}
              bgmPrompt={zoneForm.bgmPrompt}
              bgmDuration={zoneForm.bgmDuration}
              onUpdate={(fields) => setZoneForm((f) => ({ ...f, ...fields }))}
            />
            <label style={styles.label}>Spawn Room</label>
            <input
              style={styles.input}
              value={zoneForm.spawnRoom || ''}
              onChange={(e) => setZoneForm((f) => ({ ...f, spawnRoom: e.target.value }))}
            />
            <label style={styles.label}>Max Entities</label>
            <input
              style={styles.input}
              type="number"
              value={zoneForm.spawnMaxEntities ?? 0}
              onChange={(e) =>
                setZoneForm((f) => ({ ...f, spawnMaxEntities: parseInt(e.target.value) || 0 }))
              }
            />
            <label style={styles.label}>Max Per Room</label>
            <input
              style={styles.input}
              type="number"
              value={zoneForm.spawnMaxPerRoom ?? 0}
              onChange={(e) =>
                setZoneForm((f) => ({ ...f, spawnMaxPerRoom: parseInt(e.target.value) || 0 }))
              }
            />
            <label style={styles.label}>Spawn Rate (ticks)</label>
            <input
              style={styles.input}
              type="number"
              value={zoneForm.spawnRateTicks ?? 0}
              onChange={(e) =>
                setZoneForm((f) => ({ ...f, spawnRateTicks: parseInt(e.target.value) || 0 }))
              }
            />
            <label style={styles.label}>Image Style (zone default)</label>
            <input
              style={styles.input}
              value={zoneForm.imageStyle || ''}
              onChange={(e) => setZoneForm((f) => ({ ...f, imageStyle: e.target.value }))}
              placeholder="e.g. pixel art, watercolor"
            />
            <label style={styles.label}>Image Negative Prompt (zone default)</label>
            <input
              style={styles.input}
              value={zoneForm.imageNegativePrompt || ''}
              onChange={(e) => setZoneForm((f) => ({ ...f, imageNegativePrompt: e.target.value }))}
              placeholder="e.g. blurry, low quality"
            />
            <div style={{ display: 'flex', gap: 8 }}>
              <button style={styles.btnSmall} onClick={handleSaveZone}>
                Save Zone
              </button>
              <button style={styles.btnDanger} onClick={handleDeleteZone}>
                Delete
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Center Panel */}
      <div style={styles.centerPanel}>
        {selectedZoneId ? (
          <>
            {/* Layer navigation bar */}
            {layers.length > 1 && (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 12,
                  padding: '6px 12px',
                  backgroundColor: '#fff',
                  borderBottom: '1px solid #ddd',
                  fontSize: 13,
                  fontWeight: 600,
                  color: '#1a1a2e',
                  userSelect: 'none',
                }}
              >
                <button
                  onClick={() => {
                    const above = layers.filter((l) => l > currentLayer);
                    if (above.length > 0) setCurrentLayer(above[above.length - 1]);
                  }}
                  disabled={!canGoUp}
                  style={{
                    padding: '2px 10px',
                    fontSize: 14,
                    fontWeight: 700,
                    border: '1px solid #ccc',
                    borderRadius: 4,
                    backgroundColor: canGoUp ? '#e8eaf6' : '#f5f5f5',
                    color: canGoUp ? '#1a1a2e' : '#bbb',
                    cursor: canGoUp ? 'pointer' : 'default',
                  }}
                  title="Go up one layer"
                >
                  &#9650;
                </button>
                <span>Layer {currentLayer} ({layerLabel(currentLayer)})</span>
                <button
                  onClick={() => {
                    const below = layers.filter((l) => l < currentLayer);
                    if (below.length > 0) setCurrentLayer(below[0]);
                  }}
                  disabled={!canGoDown}
                  style={{
                    padding: '2px 10px',
                    fontSize: 14,
                    fontWeight: 700,
                    border: '1px solid #ccc',
                    borderRadius: 4,
                    backgroundColor: canGoDown ? '#e8eaf6' : '#f5f5f5',
                    color: canGoDown ? '#1a1a2e' : '#bbb',
                    cursor: canGoDown ? 'pointer' : 'default',
                  }}
                  title="Go down one layer"
                >
                  &#9660;
                </button>
              </div>
            )}
            <MapCanvas
              rooms={filteredRooms.map((r) => ({ id: r.id, name: r.name, x: r.x, y: r.y }))}
              exits={filteredExits}
              selectedRoomId={selectedRoomId}
              onSelectRoom={handleSelectRoom}
              onCreateRoom={handleCreateRoom}
              onCreateExit={handleCreateExit}
              verticalExits={verticalExits}
            />
          </>
        ) : (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              height: '100%',
              color: '#999',
              fontSize: 16,
            }}
          >
            Select or create a zone to begin editing
          </div>
        )}
      </div>

      {/* Right Panel */}
      <div style={styles.rightPanel}>
        {selectedRoom ? (
          <>
            <ImagePreview
              entityType="room"
              entityId={selectedRoom.id}
              description={roomForm.description}
              assetPath={roomForm.backgroundImage}
              imagePrompt={roomForm.imagePrompt}
              imageStyle={roomForm.imageStyle}
              imageNegativePrompt={roomForm.imageNegativePrompt}
              imageWidth={roomForm.imageWidth}
              imageHeight={roomForm.imageHeight}
              onUpdate={(fields) => setRoomForm((f) => ({ ...f, ...fields }))}
            />
            <div style={styles.sectionTitle}>Room Properties</div>
            <label style={styles.label}>ID</label>
            <input style={styles.input} value={selectedRoom.id} disabled />
            <label style={styles.label}>Name</label>
            <input
              style={styles.input}
              value={roomForm.name || ''}
              onChange={(e) => setRoomForm((f) => ({ ...f, name: e.target.value }))}
            />
            <label style={styles.label}>Description</label>
            <textarea
              style={styles.textarea}
              value={roomForm.description || ''}
              onChange={(e) => setRoomForm((f) => ({ ...f, description: e.target.value }))}
            />
            <label style={styles.label}>Background Image</label>
            <input
              style={styles.input}
              value={roomForm.backgroundImage || ''}
              onChange={(e) => setRoomForm((f) => ({ ...f, backgroundImage: e.target.value }))}
            />
            <label style={styles.label}>
              BGM Track ID <span style={{fontWeight:400,color:'#999',fontSize:10}}>(leave blank to use zone: {zoneForm.bgm || 'none'})</span>
            </label>
            <input
              style={styles.input}
              value={roomForm.bgm || ''}
              placeholder={`Zone default: ${zoneForm.bgm || 'none'}`}
              onChange={(e) => setRoomForm((f) => ({ ...f, bgm: e.target.value }))}
            />
            <AudioPreview
              entityType="room"
              entityId={selectedRoom!.id}
              bgm={roomForm.bgm || zoneForm.bgm}
              bgmPrompt={roomForm.bgmPrompt}
              bgmDuration={roomForm.bgmDuration}
              defaultBgmPrompt={zoneForm.bgmPrompt}
              defaultBgmDuration={zoneForm.bgmDuration}
              onUpdate={(fields) => setRoomForm((f) => ({ ...f, ...fields }))}
            />
            <label style={styles.label}>Depart Sound</label>
            <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
              <select
                style={{ ...styles.input, flex: 1 }}
                value={roomForm.departSound || ''}
                onChange={(e) => setRoomForm((f) => ({ ...f, departSound: e.target.value }))}
              >
                <option value="">None</option>
                {movementSfx.map((sfx) => (
                  <option key={sfx.id} value={sfx.id}>{sfx.label}</option>
                ))}
              </select>
              <button
                style={{ padding: '4px 8px', fontSize: 11, fontWeight: 600, backgroundColor: '#fff', color: '#1a1a2e', border: '1px solid #ccc', borderRadius: 3, cursor: 'pointer', whiteSpace: 'nowrap' }}
                disabled={!roomForm.departSound}
                onClick={() => {
                  if (roomForm.departSound) {
                    const audio = new Audio(`/api/assets/audio/sfx/${roomForm.departSound}.ogg`);
                    audio.play().catch(() => {});
                  }
                }}
              >
                Play
              </button>
            </div>
            {/* Effects */}
            <div style={{ ...styles.sectionTitle, marginTop: 12 }}>Effects</div>
            {(() => {
              const effectsList: { type: string; value: number; message: string; sound: string }[] = (() => {
                try { return JSON.parse(roomForm.effects || '[]'); } catch { return []; }
              })();
              const updateEffects = (updated: typeof effectsList) =>
                setRoomForm((f) => ({ ...f, effects: JSON.stringify(updated) }));
              return (
                <>
                  {effectsList.map((eff, i) => (
                    <div key={i} style={{ padding: '6px 8px', backgroundColor: '#f9f9f9', borderRadius: 4, marginBottom: 4, border: '1px solid #eee' }}>
                      <div style={{ display: 'flex', gap: 4, alignItems: 'center', marginBottom: 4 }}>
                        <select
                          style={{ ...styles.input, flex: 1 }}
                          value={eff.type}
                          onChange={(e) => { const u = [...effectsList]; u[i] = { ...u[i], type: e.target.value }; updateEffects(u); }}
                        >
                          <option value="HEAL">HEAL</option>
                          <option value="POISON">POISON</option>
                          <option value="DAMAGE">DAMAGE</option>
                          <option value="MANA_REGEN">MANA_REGEN</option>
                          <option value="MANA_DRAIN">MANA_DRAIN</option>
                          <option value="SANCTUARY">SANCTUARY</option>
                        </select>
                        <input
                          style={{ ...styles.input, width: 50 }}
                          type="number"
                          title="Value"
                          value={eff.value}
                          onChange={(e) => { const u = [...effectsList]; u[i] = { ...u[i], value: parseInt(e.target.value) || 0 }; updateEffects(u); }}
                        />
                        <button
                          onClick={() => { const u = effectsList.filter((_, j) => j !== i); updateEffects(u); }}
                          style={{ background: 'none', border: 'none', color: '#d32f2f', cursor: 'pointer', fontWeight: 700, fontSize: 14, padding: '0 4px' }}
                          title="Remove effect"
                        >x</button>
                      </div>
                      <input
                        style={{ ...styles.input, marginBottom: 2 }}
                        placeholder="Message (optional)"
                        value={eff.message}
                        onChange={(e) => { const u = [...effectsList]; u[i] = { ...u[i], message: e.target.value }; updateEffects(u); }}
                      />
                      <SfxPreview
                        soundId={eff.sound || ''}
                        onSoundIdChange={(id) => { const u = [...effectsList]; u[i] = { ...u[i], sound: id }; updateEffects(u); }}
                        entityLabel={`${eff.type.toLowerCase()} effect`}
                      />
                    </div>
                  ))}
                  <button
                    style={{ ...styles.btnSmall, width: '100%' }}
                    onClick={() => updateEffects([...effectsList, { type: 'HEAL', value: 0, message: '', sound: '' }])}
                  >+ Add Effect</button>
                </>
              );
            })()}
            <div style={{ display: 'flex', gap: 8 }}>
              <button style={styles.btnSmall} onClick={handleSaveRoom}>
                Save Room
              </button>
              <button style={styles.btnDanger} onClick={handleDeleteRoom}>
                Delete Room
              </button>
            </div>

            {/* Exits section */}
            <div style={{ ...styles.sectionTitle, marginTop: 20 }}>Exits</div>
            {selectedRoom.exits && selectedRoom.exits.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                {selectedRoom.exits.map((exit) => {
                  const targetRoom = rooms.find((r) => r.id === exit.toRoomId);
                  const lockedMap: Record<string, number> = (() => {
                    try { return JSON.parse(roomForm.lockedExits || '{}'); } catch { return {}; }
                  })();
                  const lockDc = lockedMap[exit.direction] ?? 0;
                  return (
                    <div
                      key={exit.direction}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '4px 8px',
                        backgroundColor: lockDc > 0 ? '#fff8e1' : '#f5f5f5',
                        borderRadius: 4,
                        fontSize: 12,
                        gap: 4,
                      }}
                    >
                      <span style={{ flex: 1, minWidth: 0 }}>
                        <strong>{exit.direction}</strong>{' '}
                        <span style={{ color: '#666' }}>
                          {targetRoom ? targetRoom.name : exit.toRoomId}
                        </span>
                      </span>
                      <button
                        onClick={() => {
                          const updated = { ...lockedMap };
                          if (lockDc > 0) {
                            delete updated[exit.direction];
                          } else {
                            updated[exit.direction] = 10;
                          }
                          setRoomForm((f) => ({ ...f, lockedExits: JSON.stringify(updated) }));
                        }}
                        title={lockDc > 0 ? 'Remove lock' : 'Add lock'}
                        style={{
                          background: 'none',
                          border: 'none',
                          cursor: 'pointer',
                          fontSize: 14,
                          padding: '0 2px',
                          color: lockDc > 0 ? '#e65100' : '#bbb',
                        }}
                      >
                        {lockDc > 0 ? '\uD83D\uDD12' : '\uD83D\uDD13'}
                      </button>
                      {lockDc > 0 && (
                        <input
                          type="number"
                          title="Lock DC"
                          value={lockDc}
                          min={1}
                          onChange={(e) => {
                            const val = parseInt(e.target.value) || 0;
                            const updated = { ...lockedMap };
                            if (val > 0) updated[exit.direction] = val;
                            else delete updated[exit.direction];
                            setRoomForm((f) => ({ ...f, lockedExits: JSON.stringify(updated) }));
                          }}
                          style={{ width: 40, padding: '2px 4px', fontSize: 11, border: '1px solid #ccc', borderRadius: 3, textAlign: 'center' as const }}
                        />
                      )}
                      <button
                        onClick={() => handleDeleteExit(selectedRoomId!, exit.direction)}
                        style={{
                          background: 'none',
                          border: 'none',
                          color: '#d32f2f',
                          cursor: 'pointer',
                          fontWeight: 700,
                          fontSize: 14,
                          padding: '0 4px',
                        }}
                        title="Delete exit"
                      >
                        x
                      </button>
                    </div>
                  );
                })}
              </div>
            ) : (
              <div style={{ fontSize: 12, color: '#999' }}>
                No exits. Drag on the map or use the form below to add one.
              </div>
            )}

            {/* Add Exit form */}
            <div style={{ marginTop: 12, padding: '10px', backgroundColor: '#f9f9f9', borderRadius: 4, border: '1px solid #eee' }}>
              <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 6, color: '#444' }}>Add Exit</div>
              <label style={{ ...styles.label, marginTop: 0 }}>Direction</label>
              <select
                style={styles.input}
                value={newExitDir}
                onChange={(e) => setNewExitDir(e.target.value)}
              >
                <option value="">-- Direction --</option>
                {ALL_DIRECTIONS.map((d) => (
                  <option key={d} value={d}>{d}</option>
                ))}
              </select>
              <label style={styles.label}>Target Room</label>
              <select
                style={styles.input}
                value={newExitTarget}
                onChange={(e) => setNewExitTarget(e.target.value)}
              >
                <option value="">-- Target Room --</option>
                {allRooms.map((group) => (
                  <optgroup key={group.zoneId} label={group.zoneName}>
                    {group.rooms.map((r) => (
                      <option key={r.id} value={r.id}>{r.name} ({r.id})</option>
                    ))}
                  </optgroup>
                ))}
              </select>
              <button
                style={{ ...styles.btnSmall, marginTop: 8, width: '100%', opacity: newExitDir && newExitTarget ? 1 : 0.5 }}
                disabled={!newExitDir || !newExitTarget}
                onClick={handleAddExitManual}
              >
                Create Exit
              </button>
            </div>
          </>
        ) : (
          <div style={{ color: '#999', fontSize: 13, textAlign: 'center', marginTop: 40 }}>
            {selectedZoneId
              ? 'Click a room on the map to edit, or click an empty cell to create one'
              : 'Select a zone first'}
          </div>
        )}
      </div>
    </div>
  );
}

export default ZoneEditor;
