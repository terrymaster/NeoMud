import GenericCrudEditor from '../components/GenericCrudEditor';
import type { FieldConfig } from '../components/GenericCrudEditor';

const fields: FieldConfig[] = [
  { key: 'id', label: 'ID', type: 'text', placeholder: 'e.g. slash' },
  { key: 'name', label: 'Name', type: 'text', placeholder: 'Slash' },
  { key: 'description', label: 'Description', type: 'textarea', rows: 3 },
  {
    key: 'category', label: 'Category', type: 'select',
    options: [
      { value: 'combat', label: 'Combat' },
      { value: 'defense', label: 'Defense' },
      { value: 'stealth', label: 'Stealth' },
      { value: 'utility', label: 'Utility' },
    ],
  },
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
  {
    key: 'secondaryStat', label: 'Secondary Stat', type: 'select',
    options: [
      { value: '', label: 'None' },
      { value: 'strength', label: 'Strength' },
      { value: 'agility', label: 'Agility' },
      { value: 'intellect', label: 'Intellect' },
      { value: 'willpower', label: 'Willpower' },
      { value: 'health', label: 'Health' },
      { value: 'charm', label: 'Charm' },
    ],
  },
  { key: 'cooldownTicks', label: 'Cooldown (ticks)', type: 'number' },
  { key: 'manaCost', label: 'Mana Cost', type: 'number' },
  { key: 'difficulty', label: 'Difficulty', type: 'number', help: 'Default check DC (15)' },
  { key: 'isPassive', label: 'Passive', type: 'checkbox', placeholder: 'Is a passive skill' },
  {
    key: 'classRestrictions', label: 'Class Restrictions', type: 'checklist' as const,
    checklistOptions: [
      { value: 'BARD', label: 'Bard' },
      { value: 'CLERIC', label: 'Cleric' },
      { value: 'DRUID', label: 'Druid' },
      { value: 'GYPSY', label: 'Gypsy' },
      { value: 'MAGE', label: 'Mage' },
      { value: 'MISSIONARY', label: 'Missionary' },
      { value: 'MYSTIC', label: 'Mystic' },
      { value: 'NINJA', label: 'Ninja' },
      { value: 'PALADIN', label: 'Paladin' },
      { value: 'PRIEST', label: 'Priest' },
      { value: 'RANGER', label: 'Ranger' },
      { value: 'THIEF', label: 'Thief' },
      { value: 'WARLOCK', label: 'Warlock' },
      { value: 'WARRIOR', label: 'Warrior' },
      { value: 'WITCHHUNTER', label: 'Witch Hunter' },
    ],
    help: 'Classes that can use this skill (empty = all classes)',
  },
  { key: 'properties', label: 'Properties (JSON)', type: 'json', rows: 4, help: 'Arbitrary skill properties object' },
  { key: 'imagePrompt', label: 'Image Prompt', type: 'textarea', rows: 3 },
  { key: 'imageStyle', label: 'Image Style', type: 'text' },
  { key: 'imageNegativePrompt', label: 'Negative Prompt', type: 'text' },
  { key: 'imageWidth', label: 'Image Width', type: 'number' },
  { key: 'imageHeight', label: 'Image Height', type: 'number' },
];

function SkillEditor() {
  return <GenericCrudEditor entityName="Skill" apiPath="/skills" fields={fields} imagePreview={{ entityType: 'skill', maxWidth: 256, maxHeight: 256 }} />;
}

export default SkillEditor;
