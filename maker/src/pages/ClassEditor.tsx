import GenericCrudEditor from '../components/GenericCrudEditor';
import type { FieldConfig } from '../components/GenericCrudEditor';

const fields: FieldConfig[] = [
  { key: 'id', label: 'ID', type: 'text', placeholder: 'e.g. warrior' },
  { key: 'name', label: 'Name', type: 'text', placeholder: 'Warrior' },
  { key: 'description', label: 'Description', type: 'textarea', rows: 3 },
  { key: 'hpPerLevelMin', label: 'HP Per Level (Min)', type: 'number' },
  { key: 'hpPerLevelMax', label: 'HP Per Level (Max)', type: 'number' },
  { key: 'mpPerLevelMin', label: 'MP Per Level (Min)', type: 'number' },
  { key: 'mpPerLevelMax', label: 'MP Per Level (Max)', type: 'number' },
  { key: 'xpModifier', label: 'XP Modifier', type: 'number', help: '1.0 = normal rate' },
  { key: 'minimumStats', label: 'Minimum Stats', type: 'stat-grid' as const, help: 'Minimum stat requirements to choose this class' },
  {
    key: 'skills', label: 'Skills', type: 'checklist' as const,
    checklistOptions: [
      { value: 'BASH', label: 'Bash', group: 'combat' },
      { value: 'KICK', label: 'Kick', group: 'combat' },
      { value: 'BACKSTAB', label: 'Backstab', group: 'combat' },
      { value: 'PARRY', label: 'Parry', group: 'defense' },
      { value: 'DODGE', label: 'Dodge', group: 'defense' },
      { value: 'SNEAK', label: 'Sneak', group: 'stealth' },
      { value: 'TRACK', label: 'Track', group: 'utility' },
      { value: 'MEDITATE', label: 'Meditate', group: 'utility' },
      { value: 'PERCEPTION', label: 'Perception', group: 'utility' },
      { value: 'PICK_LOCK', label: 'Pick Lock', group: 'utility' },
      { value: 'HAGGLE', label: 'Haggle', group: 'utility' },
      { value: 'REST', label: 'Rest', group: 'utility' },
    ],
    help: 'Skills available to this class',
  },
  { key: 'properties', label: 'Properties (JSON)', type: 'json', rows: 4, help: 'Arbitrary class properties object' },
  { key: 'magicSchools', label: 'Magic Schools', type: 'school-levels' as const, help: 'Access level per magic school (0 = none, 3 = master)' },
  { key: 'imagePrompt', label: 'Image Prompt', type: 'textarea', rows: 3 },
  { key: 'imageStyle', label: 'Image Style', type: 'text' },
  { key: 'imageNegativePrompt', label: 'Negative Prompt', type: 'text' },
  { key: 'imageWidth', label: 'Image Width', type: 'number' },
  { key: 'imageHeight', label: 'Image Height', type: 'number' },
];

function ClassEditor() {
  return <GenericCrudEditor entityName="Class" apiPath="/character-classes" fields={fields} imagePreview={{ entityType: 'character-class', maxWidth: 256, maxHeight: 256 }} />;
}

export default ClassEditor;
