import GenericCrudEditor from '../components/GenericCrudEditor';
import type { FieldConfig } from '../components/GenericCrudEditor';

const fields: FieldConfig[] = [
  { key: 'id', label: 'ID', type: 'text', placeholder: 'e.g. fireball' },
  { key: 'name', label: 'Name', type: 'text', placeholder: 'Fireball' },
  { key: 'description', label: 'Description', type: 'textarea', rows: 3 },
  { key: 'school', label: 'School', type: 'text', placeholder: 'e.g. fire, ice, arcane' },
  { key: 'spellType', label: 'Spell Type', type: 'text', placeholder: 'e.g. damage, heal, buff' },
  { key: 'manaCost', label: 'Mana Cost', type: 'number' },
  { key: 'cooldownTicks', label: 'Cooldown (ticks)', type: 'number' },
  { key: 'levelRequired', label: 'Level Required', type: 'number' },
  {
    key: 'primaryStat', label: 'Primary Stat', type: 'select',
    options: [
      { value: 'intellect', label: 'Intellect' },
      { value: 'wisdom', label: 'Wisdom' },
      { value: 'strength', label: 'Strength' },
      { value: 'charisma', label: 'Charisma' },
    ],
  },
  { key: 'basePower', label: 'Base Power', type: 'number' },
  {
    key: 'targetType', label: 'Target Type', type: 'select',
    options: [
      { value: 'SELF', label: 'Self' },
      { value: 'ALLY', label: 'Ally' },
      { value: 'ENEMY', label: 'Enemy' },
      { value: 'AOE', label: 'AoE' },
    ],
  },
  { key: 'effectType', label: 'Effect Type', type: 'text', placeholder: 'e.g. DOT, HOT, STUN' },
  { key: 'effectDuration', label: 'Effect Duration (ticks)', type: 'number' },
  { key: 'castMessage', label: 'Cast Message', type: 'text' },
  { key: 'castSound', label: 'Cast Sound', type: 'sfx' },
  { key: 'impactSound', label: 'Impact Sound', type: 'sfx' },
  { key: 'missSound', label: 'Miss Sound', type: 'sfx' },
];

function SpellEditor() {
  return <GenericCrudEditor entityName="Spell" apiPath="/spells" fields={fields} />;
}

export default SpellEditor;
