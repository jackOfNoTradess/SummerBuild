import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { AuthGuard } from './components/auth/AuthGuard';
import { Layout } from './components/layout/Layout';

// Pages
import { Login } from './pages/Login';
import { Dashboard } from './pages/Dashboard';
import { Calendar } from './pages/Calendar';
import { EventDetails } from './pages/EventDetails';
import { BookingConfirmation } from './pages/BookingConfirmation';
import { Profile } from './pages/Profile';

// Organizer Pages
import { OrganizerDashboard } from './pages/organizer/OrganizerDashboard';
import { CreateEvent } from './pages/organizer/CreateEvent';
import { EditEvent } from './pages/organizer/EditEvent';
import { EventManagement } from './pages/organizer/EventManagement';

// Admin Pages
import { AdminDashboard } from './pages/admin/AdminDashboard';
import { UserManagement } from './pages/admin/UserManagement';
import { EventsManagement } from './pages/admin/EventsManagement';

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          {/* Public Routes */}
          <Route
            path="/login"
            element={
              <AuthGuard requireAuth={false}>
                <Login />
              </AuthGuard>
            }
          />

          {/* Protected Routes */}
          <Route
            path="/dashboard"
            element={
              <AuthGuard>
                <Layout>
                  <Dashboard />
                </Layout>
              </AuthGuard>
            }
          />

          <Route
            path="/calendar"
            element={
              <AuthGuard>
                <Layout>
                  <Calendar />
                </Layout>
              </AuthGuard>
            }
          />

          <Route
            path="/events/:id"
            element={
              <AuthGuard>
                <Layout>
                  <EventDetails />
                </Layout>
              </AuthGuard>
            }
          />

          <Route
            path="/booking-confirmation/:bookingRef"
            element={
              <AuthGuard>
                <Layout>
                  <BookingConfirmation />
                </Layout>
              </AuthGuard>
            }
          />

          <Route
            path="/profile"
            element={
              <AuthGuard>
                <Layout>
                  <Profile />
                </Layout>
              </AuthGuard>
            }
          />

          {/* Organizer Routes */}
          <Route
            path="/organizer/dashboard"
            element={
              <AuthGuard requiredRole="organizer">
                <Layout>
                  <OrganizerDashboard />
                </Layout>
              </AuthGuard>
            }
          />

          <Route
            path="/organizer/create-event"
            element={
              <AuthGuard requiredRole="organizer">
                <Layout>
                  <CreateEvent />
                </Layout>
              </AuthGuard>
            }
          />

          <Route
            path="/organizer/events/:id/edit"
            element={
              <AuthGuard requiredRole="organizer">
                <Layout>
                  <EditEvent />
                </Layout>
              </AuthGuard>
            }
          />

          <Route
            path="/organizer/events/:id/manage"
            element={
              <AuthGuard requiredRole="organizer">
                <Layout>
                  <EventManagement />
                </Layout>
              </AuthGuard>
            }
          />

          {/* Admin Routes */}
          <Route
            path="/admin"
            element={
              <AuthGuard requiredRole="admin">
                <Layout>
                  <AdminDashboard />
                </Layout>
              </AuthGuard>
            }
          />

          <Route
            path="/admin/users"
            element={
              <AuthGuard requiredRole="admin">
                <Layout>
                  <UserManagement />
                </Layout>
              </AuthGuard>
            }
          />

          <Route
            path="/admin/events"
            element={
              <AuthGuard requiredRole="admin">
                <Layout>
                  <EventsManagement />
                </Layout>
              </AuthGuard>
            }
          />

          {/* Default redirects */}
          <Route path="/" element={<Navigate to="/dashboard\" replace />} />
          <Route path="*" element={<Navigate to="/dashboard\" replace />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;