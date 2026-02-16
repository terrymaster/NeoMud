import { useEffect, useState, useCallback } from 'react';
import api from '../api';
import MapCanvas from '../components/MapCanvas';
import type { CSSProperties } from 'react';

interface Zone {
  id: string;
  name: string;
  description: string;
  safe: boolean;
  bgm: string;
  spawnRoom: string | null;
  spawnMaxEntities: number;
  spawnMaxPerRoom: number;
  spawnRateTicks: number;
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
  departSound: string;
  healPerTick: number;
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

let zoneCounter = 0;

function ZoneEditor() {
  const [zones, setZones] = useState<Zone[]>([]);
  const [selectedZoneId, setSelectedZoneId] = useState<string | null>(null);
  const [rooms, setRooms] = useState<Room[]>([]);
  const [selectedRoomId, setSelectedRoomId] = useState<string | null>(null);

  const [zoneForm, setZoneForm] = useState<Partial<Zone>>({});
  const [roomForm, setRoomForm] = useState<Partial<Room>>({});

  // Load zones list
  useEffect(() => {
    api.get<Zone[]>('/zones').then(setZones).catch(() => {});
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
          spawnRoom: data.spawnRoom,
          spawnMaxEntities: data.spawnMaxEntities,
          spawnMaxPerRoom: data.spawnMaxPerRoom,
          spawnRateTicks: data.spawnRateTicks,
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
            <label style={styles.label}>BGM</label>
            <input
              style={styles.input}
              value={zoneForm.bgm || ''}
              onChange={(e) => setZoneForm((f) => ({ ...f, bgm: e.target.value }))}
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
          <MapCanvas
            rooms={rooms.map((r) => ({ id: r.id, name: r.name, x: r.x, y: r.y }))}
            exits={allExits}
            selectedRoomId={selectedRoomId}
            onSelectRoom={handleSelectRoom}
            onCreateRoom={handleCreateRoom}
            onCreateExit={handleCreateExit}
          />
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
            <label style={styles.label}>BGM</label>
            <input
              style={styles.input}
              value={roomForm.bgm || ''}
              onChange={(e) => setRoomForm((f) => ({ ...f, bgm: e.target.value }))}
            />
            <label style={styles.label}>Depart Sound</label>
            <input
              style={styles.input}
              value={roomForm.departSound || ''}
              onChange={(e) => setRoomForm((f) => ({ ...f, departSound: e.target.value }))}
            />
            <label style={styles.label}>Heal Per Tick</label>
            <input
              style={styles.input}
              type="number"
              value={roomForm.healPerTick ?? 0}
              onChange={(e) =>
                setRoomForm((f) => ({ ...f, healPerTick: parseInt(e.target.value) || 0 }))
              }
            />
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
                  return (
                    <div
                      key={exit.direction}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '4px 8px',
                        backgroundColor: '#f5f5f5',
                        borderRadius: 4,
                        fontSize: 12,
                      }}
                    >
                      <span>
                        <strong>{exit.direction}</strong>{' '}
                        <span style={{ color: '#666' }}>
                          {targetRoom ? targetRoom.name : exit.toRoomId}
                        </span>
                      </span>
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
                No exits. Drag from this room to another to connect them.
              </div>
            )}
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
