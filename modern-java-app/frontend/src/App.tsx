import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Landing from './pages/Landing'
import Login from './pages/Login'
import RepoSelection from './pages/RepoSelection'
import Dashboard from './pages/Dashboard'
import Chat from './pages/Chat'
import PullRequestList from './pages/PullRequestList'
import ReviewResult from './pages/ReviewResult'
import AuditResults from './pages/AuditResults'
import './index.css'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/login" element={<Login />} />
        <Route path="/app/select-repo" element={<RepoSelection />} />
        <Route path="/app" element={<Dashboard />} />
        <Route path="/app/pull-requests" element={<PullRequestList />} />
        <Route path="/app/pull-requests/:prId/review" element={<ReviewResult />} />
        <Route path="/app/chat" element={<Chat />} />
        <Route path="/app/audit-results/:auditId" element={<AuditResults />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App


