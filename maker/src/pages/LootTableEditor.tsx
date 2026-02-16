import GenericCrudEditor from '../components/GenericCrudEditor';
import type { FieldConfig } from '../components/GenericCrudEditor';

const fields: FieldConfig[] = [
  { key: 'id', label: 'NPC ID', type: 'text', placeholder: 'e.g. goblin_warrior', help: 'The NPC this loot table belongs to' },
  {
    key: 'items', label: 'Items (JSON)', type: 'json', rows: 8,
    help: 'Array of loot entries, e.g. [{"itemId":"iron_sword","chance":0.5,"minQty":1,"maxQty":1}]',
    placeholder: '[\n  { "itemId": "iron_sword", "chance": 0.5, "minQty": 1, "maxQty": 1 }\n]',
  },
  {
    key: 'coinDrop', label: 'Coin Drop (JSON)', type: 'json', rows: 4,
    help: 'e.g. {"min":5,"max":20,"chance":0.8}',
    placeholder: '{ "min": 5, "max": 20, "chance": 0.8 }',
  },
];

function LootTableEditor() {
  return <GenericCrudEditor entityName="Loot Table" apiPath="/loot-tables" fields={fields} />;
}

export default LootTableEditor;
