import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import ProjectList from './pages/ProjectList';
import Dashboard from './pages/Dashboard';
import ZoneEditor from './pages/ZoneEditor';
import ItemEditor from './pages/ItemEditor';
import NpcEditor from './pages/NpcEditor';
import ClassEditor from './pages/ClassEditor';
import RaceEditor from './pages/RaceEditor';
import SkillEditor from './pages/SkillEditor';
import SpellEditor from './pages/SpellEditor';
import LootTableEditor from './pages/LootTableEditor';
import PcSpriteEditor from './pages/PcSpriteEditor';
import SettingsEditor from './pages/SettingsEditor';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<ProjectList />} />
        <Route path="/project/:name" element={<Dashboard />}>
          <Route index element={<Navigate to="zones" replace />} />
          <Route path="zones" element={<ZoneEditor />} />
          <Route path="items" element={<ItemEditor />} />
          <Route path="npcs" element={<NpcEditor />} />
          <Route path="classes" element={<ClassEditor />} />
          <Route path="races" element={<RaceEditor />} />
          <Route path="skills" element={<SkillEditor />} />
          <Route path="spells" element={<SpellEditor />} />
          <Route path="loot-tables" element={<LootTableEditor />} />
          <Route path="default-players" element={<PcSpriteEditor />} />
          <Route path="settings" element={<SettingsEditor />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
