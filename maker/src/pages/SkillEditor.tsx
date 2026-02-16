import GenericCrudEditor from '../components/GenericCrudEditor';
import type { FieldConfig } from '../components/GenericCrudEditor';

const fields: FieldConfig[] = [
  { key: 'id', label: 'ID', type: 'text', placeholder: 'e.g. slash' },
  { key: 'name', label: 'Name', type: 'text', placeholder: 'Slash' },
  { key: 'description', label: 'Description', type: 'textarea', rows: 3 },
  { key: 'category', label: 'Category', type: 'text', placeholder: 'e.g. combat, crafting' },
  {
    key: 'primaryStat', label: 'Primary Stat', type: 'select',
    options: [
      { value: 'strength', label: 'Strength' },
      { value: 'agility', label: 'Agility' },
      { value: 'intellect', label: 'Intellect' },
      { value: 'wisdom', label: 'Wisdom' },
      { value: 'constitution', label: 'Constitution' },
      { value: 'charisma', label: 'Charisma' },
    ],
  },
  {
    key: 'secondaryStat', label: 'Secondary Stat', type: 'select',
    options: [
      { value: '', label: 'None' },
      { value: 'strength', label: 'Strength' },
      { value: 'agility', label: 'Agility' },
      { value: 'intellect', label: 'Intellect' },
      { value: 'wisdom', label: 'Wisdom' },
      { value: 'constitution', label: 'Constitution' },
      { value: 'charisma', label: 'Charisma' },
    ],
  },
  { key: 'cooldownTicks', label: 'Cooldown (ticks)', type: 'number' },
  { key: 'manaCost', label: 'Mana Cost', type: 'number' },
  { key: 'difficulty', label: 'Difficulty', type: 'number', help: 'Default check DC (15)' },
  { key: 'isPassive', label: 'Passive', type: 'checkbox', placeholder: 'Is a passive skill' },
  { key: 'classRestrictions', label: 'Class Restrictions (JSON)', type: 'json', rows: 2, help: 'Array of class IDs, e.g. ["warrior","paladin"]' },
  { key: 'properties', label: 'Properties (JSON)', type: 'json', rows: 4, help: 'Arbitrary skill properties object' },
];

function SkillEditor() {
  return <GenericCrudEditor entityName="Skill" apiPath="/skills" fields={fields} />;
}

export default SkillEditor;
