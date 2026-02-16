import GenericCrudEditor from '../components/GenericCrudEditor';
import type { FieldConfig } from '../components/GenericCrudEditor';

const fields: FieldConfig[] = [
  { key: 'id', label: 'ID', type: 'text', placeholder: 'e.g. iron_sword' },
  { key: 'name', label: 'Name', type: 'text', placeholder: 'Iron Sword' },
  { key: 'description', label: 'Description', type: 'textarea', rows: 3 },
  {
    key: 'type', label: 'Type', type: 'select',
    options: [
      { value: 'WEAPON', label: 'Weapon' },
      { value: 'ARMOR', label: 'Armor' },
      { value: 'CONSUMABLE', label: 'Consumable' },
      { value: 'MATERIAL', label: 'Material' },
      { value: 'KEY', label: 'Key' },
      { value: 'MISC', label: 'Misc' },
    ],
  },
  {
    key: 'slot', label: 'Slot', type: 'select',
    options: [
      { value: '', label: 'None' },
      { value: 'MAIN_HAND', label: 'Main Hand' },
      { value: 'OFF_HAND', label: 'Off Hand' },
      { value: 'HEAD', label: 'Head' },
      { value: 'CHEST', label: 'Chest' },
      { value: 'LEGS', label: 'Legs' },
      { value: 'FEET', label: 'Feet' },
      { value: 'HANDS', label: 'Hands' },
      { value: 'RING', label: 'Ring' },
      { value: 'NECK', label: 'Neck' },
    ],
  },
  { key: 'damageBonus', label: 'Damage Bonus', type: 'number' },
  { key: 'damageRange', label: 'Damage Range', type: 'number' },
  { key: 'armorValue', label: 'Armor Value', type: 'number' },
  { key: 'value', label: 'Value (gold)', type: 'number' },
  { key: 'weight', label: 'Weight', type: 'number' },
  { key: 'stackable', label: 'Stackable', type: 'checkbox' },
  { key: 'maxStack', label: 'Max Stack', type: 'number' },
  { key: 'useEffect', label: 'Use Effect', type: 'text', help: 'Effect string applied on use' },
  { key: 'levelRequirement', label: 'Level Requirement', type: 'number' },
  { key: 'attackSound', label: 'Attack Sound', type: 'text' },
  { key: 'missSound', label: 'Miss Sound', type: 'text' },
  { key: 'useSound', label: 'Use Sound', type: 'text' },
];

function ItemEditor() {
  return <GenericCrudEditor entityName="Item" apiPath="/items" fields={fields} />;
}

export default ItemEditor;
