import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Plus, Calendar, Users, TrendingUp, Edit, Trash2, Settings, Eye, BarChart3 } from 'lucide-react';
import { format, parseISO } from 'date-fns';
import { useAuth } from '../../contexts/AuthContext';
import { supabase } from '../../lib/supabase';
import type { Event } from '../../types/database';
import { LoadingSpinner } from '../../components/ui/LoadingSpinner';

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL;

export function OrganizerDashboard() {
  const { user } = useAuth();
  const [events, setEvents] = useState<Event[]>([]);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({
    totalEvents: 0,
    totalRegistrations: 0,
    upcomingEvents: 0,
    pastEvents: 0,
  });
  const [eventRegistrations, setEventRegistrations] = useState<Record<string, number>>({});

  useEffect(() => {
    if (user) {
      fetchOrganizerEvents();
    }
  }, [user]);

  const fetchOrganizerEvents = async () => {
    try {
      setLoading(true);
      
      if (!user?.id) {
        setEvents([]);
        return;
      }

      // Get the access token from the current session
      const { data: { session } } = await supabase.auth.getSession();
      if (!session?.access_token) {
        throw new Error('No authentication token available');
      }

      // Fetch events hosted by the organizer
      const eventsResponse = await fetch(`${BACKEND_URL}/api/events/host/${user.id}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${session.access_token}`
        },
      });

      if (!eventsResponse.ok) {
        throw new Error(`HTTP error! status: ${eventsResponse.status}`);
      }

      const eventsData = await eventsResponse.json();
      setEvents(eventsData || []);
      
      // Calculate stats
      const now = new Date();
      
      // Get registration counts for all events
      let totalRegistrations = 0;
      const registrationCounts: Record<string, number> = {};
      
      if (eventsData && eventsData.length > 0) {
        // Fetch registration counts for each event
        const registrationPromises = eventsData.map(async (event: Event) => {
          try {
            const countResponse = await fetch(`${BACKEND_URL}/api/participates/count/event/${event.id}`, {
              method: 'GET',
              headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${session.access_token}`
              },
            });

            if (countResponse.ok) {
              const count = await countResponse.json();
              registrationCounts[event.id] = count;
              totalRegistrations += count;
            } else {
              registrationCounts[event.id] = 0;
            }
          } catch (error) {
            console.error(`Error fetching registrations for event ${event.id}:`, error);
            registrationCounts[event.id] = 0;
          }
        });

        await Promise.all(registrationPromises);
      }
      
      setEventRegistrations(registrationCounts);
      
      const upcomingEvents = eventsData?.filter((event: Event) => new Date(event.start_time) >= now).length || 0;
      const pastEvents = eventsData?.filter((event: Event) => new Date(event.start_time) < now).length || 0;

      setStats({
        totalEvents: eventsData?.length || 0,
        totalRegistrations,
        upcomingEvents,
        pastEvents,
      });
    } catch (error) {
      console.error('Error fetching organizer events:', error);
      setEvents([]);
      setStats({
        totalEvents: 0,
        totalRegistrations: 0,
        upcomingEvents: 0,
        pastEvents: 0,
      });
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteEvent = async (eventId: string) => {
    const confirmed = window.confirm('Are you sure you want to delete this event? This action cannot be undone.');
    if (!confirmed) return;

    // Get the access token from the current session
      const { data: { session } } = await supabase.auth.getSession();
      if (!session?.access_token) {
        throw new Error('No authentication token available');
      }

    try {
      // Delete the event (backend should handle cascade deletion of participations)
      const response = await fetch(`${BACKEND_URL}/api/events/${eventId}`, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${session.access_token}`
        },
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      // Refresh the events list
      await fetchOrganizerEvents();
      
      alert('Event deleted successfully.');
    } catch (error) {
      console.error('Error deleting event:', error);
      alert('Failed to delete event. Please try again.');
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Organizer Dashboard</h1>
          <p className="text-gray-600 mt-1">Manage your events and track performance</p>
        </div>
        <Link
          to="/organizer/create-event"
          className="inline-flex items-center px-6 py-3 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 transition-colors duration-200"
        >
          <Plus className="w-5 h-5 mr-2" />
          Create New Event
        </Link>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center space-x-3">
            <div className="bg-blue-100 p-3 rounded-lg">
              <Calendar className="w-6 h-6 text-blue-600" />
            </div>
            <div>
              <div className="text-2xl font-bold text-gray-900">{stats.totalEvents}</div>
              <div className="text-sm text-gray-600">Total Events</div>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center space-x-3">
            <div className="bg-green-100 p-3 rounded-lg">
              <Users className="w-6 h-6 text-green-600" />
            </div>
            <div>
              <div className="text-2xl font-bold text-gray-900">{stats.totalRegistrations}</div>
              <div className="text-sm text-gray-600">Total Registrations</div>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center space-x-3">
            <div className="bg-purple-100 p-3 rounded-lg">
              <TrendingUp className="w-6 h-6 text-purple-600" />
            </div>
            <div>
              <div className="text-2xl font-bold text-gray-900">{stats.upcomingEvents}</div>
              <div className="text-sm text-gray-600">Upcoming Events</div>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center space-x-3">
            <div className="bg-orange-100 p-3 rounded-lg">
              <BarChart3 className="w-6 h-6 text-orange-600" />
            </div>
            <div>
              <div className="text-2xl font-bold text-gray-900">{stats.pastEvents}</div>
              <div className="text-sm text-gray-600">Past Events</div>
            </div>
          </div>
        </div>
      </div>

      {/* Events List */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-xl font-semibold text-gray-900">Your Events</h2>
        </div>

        {events.length === 0 ? (
          <div className="text-center py-12">
            <Calendar className="w-16 h-16 text-gray-400 mx-auto mb-4" />
            <h3 className="text-xl font-semibold text-gray-900 mb-2">No events created yet</h3>
            <p className="text-gray-600 mb-6">Start creating engaging events for your community.</p>
            <Link
              to="/organizer/create-event"
              className="inline-flex items-center px-6 py-3 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 transition-colors duration-200"
            >
              <Plus className="w-5 h-5 mr-2" />
              Create Your First Event
            </Link>
          </div>
        ) : (
          <div className="divide-y divide-gray-200">
            {events.map(event => {
              const isUpcoming = new Date(event.start_time) >= new Date();
              const registrationCount = eventRegistrations[event.id] || 0;
              const fillPercentage = event.capacity ? (registrationCount / event.capacity) * 100 : 0;
              
              return (
                <div key={event.id} className="p-6 hover:bg-gray-50 transition-colors duration-200">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center space-x-3 mb-2">
                        <h3 className="text-lg font-semibold text-gray-900">{event.title}</h3>
                        <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${
                          isUpcoming 
                            ? 'bg-green-100 text-green-800' 
                            : 'bg-gray-100 text-gray-800'
                        }`}>
                          {isUpcoming ? 'Upcoming' : 'Past'}
                        </span>
                      </div>
                      
                      <p className="text-gray-600 mb-3 line-clamp-2">{event.description}</p>
                      
                      <div className="flex flex-wrap items-center gap-4 text-sm text-gray-500">
                        <div className="flex items-center space-x-1">
                          <Calendar className="w-4 h-4" />
                          <span>{format(parseISO(event.start_time), 'MMM d, yyyy')}</span>
                        </div>
                        <div className="flex items-center space-x-1">
                          <Users className="w-4 h-4" />
                          <span>{registrationCount} / {event.capacity || 'Unlimited'} registered</span>
                        </div>
                      </div>
                      
                      {/* Registration Progress */}
                      {event.capacity && (
                        <div className="mt-3">
                          <div className="flex items-center justify-between text-sm mb-1">
                            <span className="text-gray-600">Registration Progress</span>
                            <span className="text-gray-900 font-medium">{fillPercentage.toFixed(0)}%</span>
                          </div>
                          <div className="w-full bg-gray-200 rounded-full h-2">
                            <div
                              className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                              style={{ width: `${Math.min(fillPercentage, 100)}%` }}
                            ></div>
                          </div>
                        </div>
                      )}
                    </div>

                    {/* Actions */}
                    <div className="flex items-center space-x-2 ml-6">
                      <Link
                        to={`/events/${event.id}`}
                        className="p-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors duration-200"
                        title="View Event"
                      >
                        <Eye className="w-5 h-5" />
                      </Link>
                      
                      <Link
                        to={`/organizer/events/${event.id}/manage`}
                        className="p-2 text-gray-400 hover:text-green-600 hover:bg-green-50 rounded-lg transition-colors duration-200"
                        title="Manage Event"
                      >
                        <Settings className="w-5 h-5" />
                      </Link>
                      
                      <Link
                        to={`/organizer/events/${event.id}/edit`}
                        className="p-2 text-gray-400 hover:text-purple-600 hover:bg-purple-50 rounded-lg transition-colors duration-200"
                        title="Edit Event"
                      >
                        <Edit className="w-5 h-5" />
                      </Link>
                      
                      <button
                        onClick={() => handleDeleteEvent(event.id)}
                        className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors duration-200"
                        title="Delete Event"
                      >
                        <Trash2 className="w-5 h-5" />
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
