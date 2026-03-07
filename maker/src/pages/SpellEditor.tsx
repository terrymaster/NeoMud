import GenericCrudEditor from '../components/GenericCrudEditor';
import type { FieldConfig } from '../components/GenericCrudEditor';

const fields: FieldConfig[] = [
  { key: 'id', label: 'ID', type: 'text', placeholder: 'e.g. fireball' },
  { key: 'name', label: 'Name', type: 'text', placeholder: 'Fireball' },
  { key: 'description', label: 'Description', type: 'textarea', rows: 3 },
  {
    key: 'school', label: 'School', type: 'select',
    options: [
      { value: 'mage', label: 'Mage' },
      { value: 'priest', label: 'Priest' },
      { value: 'druid', label: 'Druid' },
      { value: 'kai', label: 'Kai' },
      { value: 'bard', label: 'Bard' },
    ],
  },
  {
    key: 'spellType', label: 'Spell Type', type: 'select',
    options: [
      { value: 'DAMAGE', label: 'Damage' },
      { value: 'HEAL', label: 'Heal' },
      { value: 'BUFF', label: 'Buff' },
      { value: 'DOT', label: 'DoT' },
      { value: 'HOT', label: 'HoT' },
    ],
  },
  { key: 'manaCost', label: 'Mana Cost', type: 'number' },
  { key: 'cooldownTicks', label: 'Cooldown (ticks)', type: 'number' },
  { key: 'levelRequired', label: 'Level Required', type: 'number' },
  {
    key: 'primaryStat', label: 'Primary Stat', type: 'select',
    options: [
      { value: 'strength', label: 'Strength' },
      { value: 'agility', label: 'Agility' },
      { value: 'intellect', label: 'Intellect' },
      { value: 'willpower', label: 'Willpower' },
      { value: 'health', label: 'Health' },
      { value: 'charm', label: 'Charm' },
    ],
  },
  { key: 'basePower', label: 'Base Power', type: 'number' },
  { key: 'tickPower', label: 'Tick Power (DoT/HoT per tick)', type: 'number' },
  {
    key: 'targetType', label: 'Target Type', type: 'select',
    options: [
      { value: 'SELF', label: 'Self' },
      { value: 'ALLY', label: 'Ally' },
      { value: 'ENEMY', label: 'Enemy' },
      { value: 'AOE', label: 'AoE' },
    ],
  },
  {
    key: 'effectType', label: 'Effect Type', type: 'select',
    options: [
      { value: '', label: 'None' },
      { value: 'BUFF_STRENGTH', label: 'Buff Strength' },
      { value: 'BUFF_AGILITY', label: 'Buff Agility' },
      { value: 'BUFF_WILLPOWER', label: 'Buff Willpower' },
      { value: 'POISON', label: 'Poison' },
      { value: 'HEAL_OVER_TIME', label: 'Heal Over Time' },
    ],
  },
  { key: 'effectDuration', label: 'Effect Duration (ticks)', type: 'number' },
  { key: 'castMessage', label: 'Cast Message', type: 'text' },
  { key: 'castSound', label: 'Cast Sound', type: 'sfx', audioCategory: 'spells' },
  { key: 'impactSound', label: 'Impact Sound', type: 'sfx', audioCategory: 'spells' },
  { key: 'missSound', label: 'Miss Sound', type: 'sfx', audioCategory: 'spells' },
  { key: 'imagePrompt', label: 'Image Prompt', type: 'textarea', rows: 3 },
  { key: 'imageStyle', label: 'Image Style', type: 'text' },
  { key: 'imageNegativePrompt', label: 'Negative Prompt', type: 'text' },
  { key: 'imageWidth', label: 'Image Width', type: 'number' },
  { key: 'imageHeight', label: 'Image Height', type: 'number' },
];

function SpellEditor() {
  return <GenericCrudEditor entityName="Spell" apiPath="/spells" fields={fields} imagePreview={{ entityType: 'spell', maxWidth: 256, maxHeight: 256 }} />;
}

export default SpellEditor;
