import React from 'react';
import { Link, Route, Routes } from 'react-router-dom';
import TaskList from './components/TaskList.jsx';
import TaskDetail from './components/TaskDetail.jsx';

export default function App() {
  return (
    <div className="layout">
      <header className="header">
        <Link to="/" className="brand">
          Agent Platform Ops
        </Link>
        <span className="subtitle">workflow-registry dashboard</span>
      </header>
      <main className="main">
        <Routes>
          <Route path="/" element={<TaskList />} />
          <Route path="/tasks/:taskId" element={<TaskDetail />} />
        </Routes>
      </main>
    </div>
  );
}
