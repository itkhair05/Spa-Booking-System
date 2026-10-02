import type { ReactNode } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './app/auth/AuthContext';
import { useAuth } from './app/auth/useAuth';
import LoginPage from './pages/LoginPage';
import Dashboard from './pages/Dashboard';

// Simple protected route component
const ProtectedRoute = ({ children }: { children: ReactNode }) => {
  const { user, isLoading } = useAuth();
  if (isLoading) return null;
  return user ? children : <Navigate to="/login" replace />;
};

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            }
          />
          {/* Fallback route */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
