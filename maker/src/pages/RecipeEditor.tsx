import GenericCrudEditor from '../components/GenericCrudEditor';
import type { FieldConfig } from '../components/GenericCrudEditor';

const fields: FieldConfig[] = [
  { key: 'id', label: 'ID', type: 'text', placeholder: 'e.g. recipe:antivenom_vial' },
  { key: 'name', label: 'Name', type: 'text', placeholder: 'Antivenom Vial' },
  { key: 'description', label: 'Description', type: 'textarea', rows: 3 },
  {
    key: 'category', label: 'Category', type: 'select',
    options: [
      { value: 'consumable', label: 'Consumable' },
      { value: 'weapon', label: 'Weapon' },
      { value: 'armor', label: 'Armor' },
      { value: 'accessory', label: 'Accessory' },
      { value: 'scroll', label: 'Scroll' },
    ],
  },
  { key: 'materials', label: 'Materials', type: 'textarea', rows: 4, help: 'JSON array: [{"itemId": "item:xxx", "quantity": 2}]' },
  { key: 'cost', label: 'Cost', type: 'textarea', rows: 2, help: 'JSON object: {"silver": 1, "gold": 0}' },
  { key: 'outputItemId', label: 'Output Item ID', type: 'text', placeholder: 'e.g. item:antivenom_vial' },
  { key: 'outputQuantity', label: 'Output Quantity', type: 'number' },
  { key: 'levelRequirement', label: 'Level Requirement', type: 'number' },
  { key: 'classRestriction', label: 'Class Restriction', type: 'text', help: 'Leave empty for no restriction' },
];

function RecipeEditor() {
  return <GenericCrudEditor entityName="Recipe" apiPath="/recipes" fields={fields} />;
}

export default RecipeEditor;
