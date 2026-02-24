import GenericCrudEditor from '../components/GenericCrudEditor';
import type { FieldConfig } from '../components/GenericCrudEditor';

const fields: FieldConfig[] = [
  { key: 'id', label: 'ID', type: 'text', placeholder: 'e.g. elf' },
  { key: 'name', label: 'Name', type: 'text', placeholder: 'Elf' },
  { key: 'description', label: 'Description', type: 'textarea', rows: 3 },
  { key: 'xpModifier', label: 'XP Modifier', type: 'number', help: '1.0 = normal rate' },
  { key: 'statModifiers', label: 'Stat Modifiers (JSON)', type: 'json', rows: 5, help: 'e.g. {"strength":-1,"intellect":2,"agility":1}' },
  { key: 'imagePrompt', label: 'Image Prompt', type: 'textarea', rows: 3 },
  { key: 'imageStyle', label: 'Image Style', type: 'text' },
  { key: 'imageNegativePrompt', label: 'Negative Prompt', type: 'text' },
  { key: 'imageWidth', label: 'Image Width', type: 'number' },
  { key: 'imageHeight', label: 'Image Height', type: 'number' },
];

function RaceEditor() {
  return <GenericCrudEditor entityName="Race" apiPath="/races" fields={fields} imagePreview={{ entityType: 'race', maxWidth: 256, maxHeight: 256 }} />;
}

export default RaceEditor;
