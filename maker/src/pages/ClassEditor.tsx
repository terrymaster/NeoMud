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
  { key: 'minimumStats', label: 'Minimum Stats (JSON)', type: 'json', rows: 4, help: 'e.g. {"strength":12,"intellect":8}' },
  { key: 'skills', label: 'Skills (JSON)', type: 'json', rows: 3, help: 'Array of skill IDs, e.g. ["slash","parry"]' },
  { key: 'properties', label: 'Properties (JSON)', type: 'json', rows: 4, help: 'Arbitrary class properties object' },
  { key: 'magicSchools', label: 'Magic Schools (JSON)', type: 'json', rows: 3, help: 'e.g. {"fire":true,"ice":false}' },
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
