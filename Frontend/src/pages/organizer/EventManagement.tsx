import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Users, Download, Calendar, TrendingUp, CheckCircle } from 'lucide-react';
import { format, parseISO } from 'date-fns';
import { supabase } from '../../lib/supabase';
import { useAuth } from '../../contexts/AuthContext';
import type { Event, Participation, User } from '../../types/database';
import { LoadingSpinner } from '../../components/ui/LoadingSpinner';

interface ParticipationWithUser extends Participation {
  user: User;
}

export function EventManagement() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const [event, setEvent] = useState<Event | null>(null);
  const [participations, setParticipations] = useState<ParticipationWithUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'overview' | 'attendees' | 'analytics'>('overview');

  useEffect(() => {
    if (id && user) {
      fetchEventData();
    }
  }, [id, user]);

  const fetchEventData = async () => {
    try {
      setLoading(true);
      
      // Get the access token from the current session
      const { data: { session } } = await supabase.auth.getSession();
      if (!session?.access_token) {
        throw new Error('No authentication token available');
      }

      // Fetch event details
      const eventResponse = await fetch(`/api/events/${id}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${session.access_token}`
        }
      });

      if (!eventResponse.ok) {
        throw new Error(`Failed to fetch event: ${eventResponse.status}`);
      }

      const eventData = await eventResponse.json();
      
      // Check if the current user is the host
      if (eventData.hostUuid !== user!.id) {
        throw new Error('Unauthorized: You are not the host of this event');
      }
      
      setEvent(eventData);

      // Fetch participations for this event
      const participationsResponse = await fetch(`/api/participates/event/${id}`, {
        method: 'GET',
        headers: {
        'Content-Type': 'application/json',
          'Authorization': `Bearer ${session.access_token}`
        }
      });

      if (!participationsResponse.ok) {
        throw new Error(`Failed to fetch participations: ${participationsResponse.status}`);
      }

      const participationsData = await participationsResponse.json();
      
      // The backend returns ParticipatesDto objects, so we need to fetch user details for each
      const participationsWithUsers = await Promise.all(
        participationsData.map(async (participation: any) => {
          try {
            

            const userResponse = await fetch(`/api/users/${participation.userId}`, {
              method: 'GET',
              headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${session.access_token}`
              }
            });
            
            if (userResponse.ok) {
              const userData = await userResponse.text(); // Backend returns string response
              // Parse the JSON string response from Supabase
              const userJson = JSON.parse(userData);
              return {
                ...participation,
                user: userJson
              };
            } else {
              // If user fetch fails, return participation with minimal user info
              return {
                ...participation,
                user: { id: participation.userId }
              };
            }
          } catch (error) {
            console.error('Error fetching user data:', error);
            return {
              ...participation,
              user: { id: participation.userId }
            };
          }
        })
      );

      setParticipations(participationsWithUsers);
    } catch (error) {
      console.error('Error fetching event data:', error);
    } finally {
      setLoading(false);
    }
  };

  const exportAttendees = () => {
    if (participations.length === 0) return;

    const csvContent = [
      ['User ID', 'Participation Date'],
      ...participations.map(participation => [
        participation.user_id,
        format(parseISO(participation.created_at), 'yyyy-MM-dd HH:mm')
      ])
    ].map(row => row.join(',')).join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${event?.title}-attendees.csv`;
    link.click();
    window.URL.revokeObjectURL(url);
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (!event) {
    return (
      <div className="max-w-2xl mx-auto text-center py-12">
        <h1 className="text-2xl font-bold text-gray-900 mb-4">Event not found</h1>
        <Link
          to="/organizer/dashboard"
          className="text-blue-600 hover:text-blue-800"
        >
          Return to Dashboard
        </Link>
      </div>
    );
  }

  const attendanceRate = event.capacity ? (participations.length / event.capacity) * 100 : 0;
  const isUpcoming = new Date((event as any).startTime) >= new Date();

  return (
    <div className="max-w-6xl mx-auto space-y-8">
      {/* Header */}
      <div>
        <Link
          to="/organizer/dashboard"
          className="inline-flex items-center text-gray-600 hover:text-gray-900 mb-4"
        >
          <ArrowLeft className="w-5 h-5 mr-2" />
          Back to Dashboard
        </Link>
        <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold text-gray-900 mb-2">{event.title}</h1>
            <p className="text-gray-600">Manage your event and track attendees</p>
          </div>
          <div className="flex items-center space-x-3">
            <Link
              to={`/organizer/events/${event.id}/edit`}
              className="px-4 py-2 border border-gray-300 text-gray-700 font-medium rounded-lg hover:bg-gray-50 transition-colors duration-200"
            >
              Edit Event
            </Link>
            <button
              onClick={exportAttendees}
              disabled={participations.length === 0}
              className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Download className="w-4 h-4" />
              <span>Export Attendees</span>
            </button>
          </div>
        </div>
      </div>

      {/* Event Summary */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <div className="text-center">
            <div className="text-3xl font-bold text-blue-600 mb-2">{participations.length}</div>
            <div className="text-sm text-gray-600">Total Registrations</div>
          </div>
          <div className="text-center">
            <div className="text-3xl font-bold text-green-600 mb-2">{event.capacity || 'Unlimited'}</div>
            <div className="text-sm text-gray-600">Event Capacity</div>
          </div>
          <div className="text-center">
            <div className="text-3xl font-bold text-purple-600 mb-2">{attendanceRate.toFixed(0)}%</div>
            <div className="text-sm text-gray-600">Attendance Rate</div>
          </div>
          <div className="text-center">
            <div className="text-3xl font-bold text-orange-600 mb-2">
              {event.capacity ? event.capacity - participations.length : '∞'}
            </div>
            <div className="text-sm text-gray-600">Available Spots</div>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200">
        <div className="border-b border-gray-200">
          <nav className="flex space-x-8 px-8 pt-6">
            {[
              { id: 'overview', label: 'Overview', icon: Calendar },
              { id: 'attendees', label: 'Attendees', icon: Users },
              { id: 'analytics', label: 'Analytics', icon: TrendingUp },
            ].map(tab => {
              const Icon = tab.icon;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id as typeof activeTab)}
                  className={`flex items-center space-x-2 pb-4 px-1 border-b-2 font-medium text-sm ${
                    activeTab === tab.id
                      ? 'border-blue-500 text-blue-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  }`}
                >
                  <Icon className="w-5 h-5" />
                  <span>{tab.label}</span>
                </button>
              );
            })}
          </nav>
        </div>

        <div className="p-8">
          {activeTab === 'overview' && (
            <div className="space-y-8">
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-4">Event Details</h3>
                  <div className="space-y-4">
                    <div className="flex items-start space-x-3">
                      <Calendar className="w-5 h-5 text-blue-600 mt-1" />
                      <div>
                        <div className="font-medium text-gray-900">
                          {format(parseISO((event as any).startTime), 'EEEE, MMMM d, yyyy')}
                        </div>
                        <div className="text-sm text-gray-500">
                          {format(parseISO((event as any).startTime), 'h:mm a')} - {format(parseISO((event as any).endTime), 'h:mm a')}
                        </div>
                      </div>
                    </div>
                    {event.tag && event.tag.length > 0 && (
                      <div className="flex items-start space-x-3">
                        <div className="w-5 h-5 mt-1" />
                        <div>
                          <div className="font-medium text-gray-900">Tags</div>
                          <div className="flex flex-wrap gap-2 mt-1">
                            {event.tag.map((tag, index) => (
                              <span
                                key={index}
                                className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800"
                              >
                                {tag}
                              </span>
                            ))}
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                </div>

                <div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-4">Registration Progress</h3>
                  <div className="space-y-4">
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-gray-600">Progress</span>
                      <span className="text-gray-900 font-medium">{attendanceRate.toFixed(1)}%</span>
                    </div>
                    {event.capacity && (
                      <>
                        <div className="w-full bg-gray-200 rounded-full h-3">
                          <div
                            className="bg-blue-600 h-3 rounded-full transition-all duration-300"
                            style={{ width: `${Math.min(attendanceRate, 100)}%` }}
                          ></div>
                        </div>
                        <div className="flex items-center justify-between text-sm text-gray-600">
                          <span>{participations.length} registered</span>
                          <span>{event.capacity - participations.length} remaining</span>
                        </div>
                      </>
                    )}
                  </div>
                </div>
              </div>

              {event.description && (
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-4">Event Description</h3>
                  <div className="bg-gray-50 rounded-lg p-6">
                    <p className="text-gray-700 leading-relaxed">{event.description}</p>
                  </div>
                </div>
              )}
            </div>
          )}

          {activeTab === 'attendees' && (
            <div className="space-y-6">
              <div className="flex items-center justify-between">
                <h3 className="text-lg font-semibold text-gray-900">
                  Registered Attendees ({participations.length})
                </h3>
                <button
                  onClick={exportAttendees}
                  disabled={participations.length === 0}
                  className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <Download className="w-4 h-4" />
                  <span>Export CSV</span>
                </button>
              </div>

              {participations.length === 0 ? (
                <div className="text-center py-12">
                  <Users className="w-16 h-16 text-gray-400 mx-auto mb-4" />
                  <h4 className="text-lg font-semibold text-gray-900 mb-2">No attendees yet</h4>
                  <p className="text-gray-600">Registrations will appear here once people start participating.</p>
                </div>
              ) : (
                <div className="overflow-hidden border border-gray-200 rounded-lg">
                  <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          User ID
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Registration Date
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Status
                        </th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {participations.map(participation => (
                        <tr key={participation.id} className="hover:bg-gray-50">
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="flex items-center">
                              <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                                <span className="text-sm font-medium text-blue-600">
                                  {participation.user_id.slice(0, 2).toUpperCase()}
                                </span>
                              </div>
                              <div className="ml-4">
                                <div className="text-sm font-medium text-gray-900">{participation.user_id}</div>
                              </div>
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="text-sm text-gray-900">
                              {format(parseISO(participation.created_at), 'MMM d, yyyy HH:mm')}
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800">
                              <CheckCircle className="w-3 h-3 mr-1" />
                              Confirmed
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {activeTab === 'analytics' && (
            <div className="space-y-8">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                <div className="bg-gray-50 rounded-lg p-6">
                  <h4 className="text-lg font-semibold text-gray-900 mb-4">Registration Timeline</h4>
                  <div className="space-y-4">
                    {participations.length === 0 ? (
                      <p className="text-gray-600">No registrations yet</p>
                    ) : (
                      <div className="text-center">
                        <div className="text-2xl font-bold text-blue-600 mb-2">
                          {participations.length}
                        </div>
                        <div className="text-sm text-gray-600">Total Registrations</div>
                      </div>
                    )}
                  </div>
                </div>

                <div className="bg-gray-50 rounded-lg p-6">
                  <h4 className="text-lg font-semibold text-gray-900 mb-4">Event Status</h4>
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <span className="text-gray-600">Event Type</span>
                      <span className="font-medium text-gray-900">
                        {isUpcoming ? 'Upcoming' : 'Past'}
                      </span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-gray-600">Registration</span>
                      <span className={`font-medium ${
                        isUpcoming ? 'text-green-600' : 'text-red-600'
                      }`}>
                        {isUpcoming ? 'Open' : 'Closed'}
                      </span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-gray-600">Capacity Status</span>
                      <span className={`font-medium ${
                        event.capacity && participations.length >= event.capacity 
                          ? 'text-red-600' 
                          : 'text-green-600'
                      }`}>
                        {event.capacity && participations.length >= event.capacity ? 'Full' : 'Available'}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <div className="bg-gray-50 rounded-lg p-6">
                <h4 className="text-lg font-semibold text-gray-900 mb-4">Event Performance</h4>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
                  <div className="text-center">
                    <div className="text-xl font-bold text-blue-600 mb-1">
                      {attendanceRate.toFixed(1)}%
                    </div>
                    <div className="text-sm text-gray-600">Fill Rate</div>
                  </div>
                  <div className="text-center">
                    <div className="text-xl font-bold text-green-600 mb-1">
                      {participations.length}
                    </div>
                    <div className="text-sm text-gray-600">Total Participants</div>
                  </div>
                  <div className="text-center">
                    <div className="text-xl font-bold text-purple-600 mb-1">
                      {event.capacity || '∞'}
                    </div>
                    <div className="text-sm text-gray-600">Capacity</div>
                  </div>
                  <div className="text-center">
                    <div className="text-xl font-bold text-orange-600 mb-1">
                      {event.capacity ? event.capacity - participations.length : '∞'}
                    </div>
                    <div className="text-sm text-gray-600">Remaining Spots</div>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
