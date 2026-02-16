import { useEffect, useState } from 'react';
import GenericCrudEditor from '../components/GenericCrudEditor';
import type { FieldConfig } from '../components/GenericCrudEditor';
import api from '../api';

interface Zone {
  id: string;
  name: string;
}

interface Room {
  id: string;
  name: string;
}

function NpcEditor() {
  const [zoneOptions, setZoneOptions] = useState<{ value: string; label: string }[]>([]);
  const [roomOptions, setRoomOptions] = useState<{ value: string; label: string }[]>([]);

  useEffect(() => {
    api.get<Zone[]>('/zones').then((zones) => {
      setZoneOptions(zones.map((z) => ({ value: z.id, label: z.name })));
      // Load all rooms across all zones
      const promises = zones.map((z) =>
        api.get<Room[]>(`/zones/${z.id}/rooms`).then((rooms) =>
          rooms.map((r) => ({ value: r.id, label: `${z.name} > ${r.name}` }))
        )
      );
      Promise.all(promises).then((results) => {
        setRoomOptions(results.flat());
      });
    }).catch(() => {});
  }, []);

  const fields: FieldConfig[] = [
    { key: 'id', label: 'ID', type: 'text', placeholder: 'e.g. goblin_guard' },
    { key: 'name', label: 'Name', type: 'text', placeholder: 'Goblin Guard' },
    { key: 'description', label: 'Description', type: 'textarea', rows: 3 },
    { key: 'zoneId', label: 'Zone', type: 'select', options: zoneOptions },
    { key: 'startRoomId', label: 'Start Room', type: 'select', options: roomOptions },
    {
      key: 'behaviorType', label: 'Behavior Type', type: 'select',
      options: [
        { value: 'idle', label: 'Idle' },
        { value: 'patrol', label: 'Patrol' },
        { value: 'aggressive', label: 'Aggressive' },
        { value: 'vendor', label: 'Vendor' },
        { value: 'quest', label: 'Quest Giver' },
      ],
    },
    { key: 'hostile', label: 'Hostile', type: 'checkbox' },
    { key: 'level', label: 'Level', type: 'number' },
    { key: 'maxHp', label: 'Max HP', type: 'number' },
    { key: 'damage', label: 'Damage', type: 'number' },
    { key: 'accuracy', label: 'Accuracy', type: 'number' },
    { key: 'defense', label: 'Defense', type: 'number' },
    { key: 'evasion', label: 'Evasion', type: 'number' },
    { key: 'agility', label: 'Agility', type: 'number' },
    { key: 'perception', label: 'Perception', type: 'number' },
    { key: 'xpReward', label: 'XP Reward', type: 'number' },
    { key: 'patrolRoute', label: 'Patrol Route (JSON)', type: 'json', rows: 3, help: 'Array of room IDs, e.g. ["zone:room1","zone:room2"]' },
    { key: 'vendorItems', label: 'Vendor Items (JSON)', type: 'json', rows: 3, help: 'Array of item IDs this NPC sells' },
    { key: 'attackSound', label: 'Attack Sound', type: 'text' },
    { key: 'missSound', label: 'Miss Sound', type: 'text' },
    { key: 'deathSound', label: 'Death Sound', type: 'text' },
    { key: 'interactSound', label: 'Interact Sound', type: 'text' },
  ];

  return <GenericCrudEditor entityName="NPC" apiPath="/npcs" fields={fields} imagePreview={{ entityType: 'npc' }} />;
}

export default NpcEditor;
