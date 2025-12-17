import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Landing from './pages/Landing'
import Login from './pages/Login'
import RepoSelection from './pages/RepoSelection'
import Dashboard from './pages/Dashboard'
import Chat from './pages/Chat'
import './index.css'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/login" element={<Login />} />
        <Route path="/app/select-repo" element={<RepoSelection />} />
        <Route path="/app" element={<Dashboard />} />
        <Route path="/app/chat" element={<Chat />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
