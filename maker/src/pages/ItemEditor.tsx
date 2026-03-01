import GenericCrudEditor from '../components/GenericCrudEditor';
import type { FieldConfig } from '../components/GenericCrudEditor';

const isEquipment = (f: Record<string, any>) => f.type === 'weapon' || f.type === 'armor';
const isConsumable = (f: Record<string, any>) => f.type === 'consumable';
const isStackable = (f: Record<string, any>) => !isEquipment(f);

const fields: FieldConfig[] = [
  { key: 'id', label: 'ID', type: 'text', placeholder: 'e.g. iron_sword' },
  { key: 'name', label: 'Name', type: 'text', placeholder: 'e.g. My New Item' },
  { key: 'description', label: 'Description', type: 'textarea', rows: 3 },
  {
    key: 'type', label: 'Category', type: 'select',
    options: [
      { value: 'weapon', label: 'Equipment — Weapon' },
      { value: 'armor', label: 'Equipment — Armor' },
      { value: 'consumable', label: 'Consumable' },
      { value: 'crafting', label: 'Crafting Material' },
      { value: 'misc', label: 'Misc' },
    ],
  },
  { key: 'value', label: 'Value (gold)', type: 'number' },
  { key: 'weight', label: 'Weight', type: 'number' },
  // Equipment fields
  {
    key: 'slot', label: 'Slot', type: 'select',
    visibleWhen: isEquipment,
    options: [
      { value: '', label: 'None' },
      { value: 'weapon', label: 'Main Hand' },
      { value: 'shield', label: 'Off Hand' },
      { value: 'head', label: 'Head' },
      { value: 'chest', label: 'Chest' },
      { value: 'legs', label: 'Legs' },
      { value: 'feet', label: 'Feet' },
      { value: 'hands', label: 'Hands' },
      { value: 'neck', label: 'Neck' },
      { value: 'ring', label: 'Ring' },
    ],
  },
  { key: 'damageBonus', label: 'Damage Bonus', type: 'number', visibleWhen: isEquipment },
  { key: 'damageRange', label: 'Damage Range', type: 'number', visibleWhen: isEquipment },
  { key: 'armorValue', label: 'Armor Value', type: 'number', visibleWhen: isEquipment },
  { key: 'levelRequirement', label: 'Level Requirement', type: 'number', visibleWhen: isEquipment },
  { key: 'attackSound', label: 'Attack Sound', type: 'sfx', visibleWhen: isEquipment },
  { key: 'missSound', label: 'Miss Sound', type: 'sfx', visibleWhen: isEquipment },
  // Consumable fields
  { key: 'useEffect', label: 'Use Effect', type: 'text', help: 'Effect string applied on use (e.g. heal:25)', visibleWhen: isConsumable },
  { key: 'useSound', label: 'Use Sound', type: 'sfx', visibleWhen: isConsumable },
  // Stacking (consumable, crafting, misc)
  { key: 'stackable', label: 'Stackable', type: 'checkbox', visibleWhen: isStackable },
  { key: 'maxStack', label: 'Max Stack', type: 'number', visibleWhen: isStackable },
  // Image fields (always visible)
  { key: 'imagePrompt', label: 'Image Prompt', type: 'textarea', rows: 3 },
  { key: 'imageStyle', label: 'Image Style', type: 'text' },
  { key: 'imageNegativePrompt', label: 'Image Negative Prompt', type: 'text' },
  { key: 'imageWidth', label: 'Image Width (max 256)', type: 'number', max: 256 },
  { key: 'imageHeight', label: 'Image Height (max 256)', type: 'number', max: 256 },
];

function ItemEditor() {
  return <GenericCrudEditor entityName="Item" apiPath="/items" fields={fields} imagePreview={{ entityType: 'item', maxWidth: 256, maxHeight: 256 }} />;
}

export default ItemEditor;
