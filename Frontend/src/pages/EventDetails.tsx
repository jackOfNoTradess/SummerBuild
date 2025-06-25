import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Calendar, Users, Clock, ArrowLeft } from 'lucide-react';
import { format, parseISO, isBefore } from 'date-fns';
import { useAuth } from '../contexts/AuthContext';
import type { Event, Participation } from '../types/database';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';

const TAG_COLORS: Record<string, string> = {
  Academic: 'bg-blue-100 text-blue-800',
  Social: 'bg-green-100 text-green-800',
  Sports: 'bg-orange-100 text-orange-800',
  Career: 'bg-purple-100 text-purple-800',
  Workshop: 'bg-indigo-100 text-indigo-800',
  Cultural: 'bg-pink-100 text-pink-800',
  Technology: 'bg-gray-100 text-gray-800',
  Arts: 'bg-yellow-100 text-yellow-800',
};

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL;

export function EventDetails() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [event, setEvent] = useState<Event | null>(null);
  const [participation, setParticipation] = useState<Participation | null>(null);
  const [participationCount, setParticipationCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    if (id) {
      fetchEventDetails();
    }
  }, [id, user]);

  const fetchEventDetails = async () => {
    try {
      setLoading(true);
      
      // Fetch event details
      const eventResponse = await fetch(`${BACKEND_URL}/api/events/${id}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      if (!eventResponse.ok) {
        throw new Error(`HTTP error! status: ${eventResponse.status}`);
      }

      const eventData = await eventResponse.json();
      setEvent(eventData);

      // Get participation count
      const countResponse = await fetch(`${BACKEND_URL}/api/participates/count/event/${id}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      if (countResponse.ok) {
        const count = await countResponse.json();
        setParticipationCount(count);
      } else {
        console.warn('Failed to fetch participation count');
        setParticipationCount(0);
      }

      // Check if user has registered for this event
      if (user && user.id) {
        const participationResponse = await fetch(`${BACKEND_URL}/api/participates/check?userId=${user.id}&eventId=${id}`, {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
          },
        });

        if (participationResponse.ok) {
          const isParticipating = await participationResponse.json();
          if (isParticipating) {
            // Simple participation object - just need to know they're registered
            setParticipation({
              id: 'not in use',
              event_id: id!,
              user_id: user.id,
              created_at: new Date().toISOString(), // not in use but part of response type
              updated_at: new Date().toISOString(), // not in use but part of response type
            });
          } else {
            setParticipation(null);
          }
        } else {
          console.warn('Failed to check participation status');
          setParticipation(null);
        }
      }
    } catch (error) {
      console.error('Error fetching event details:', error);
      setEvent(null);
    } finally {
      setLoading(false);
    }
  };

  const handleRegisterEvent = async () => {
    if (!user || !event || !user.id) return;

    try {
      setActionLoading(true);
      
      // Create participation
      const response = await fetch(`${BACKEND_URL}/api/participates/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          userId: user.id,
          eventId: event.id,
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      // Refresh data
      await fetchEventDetails();
      
      alert('Successfully registered for the event!');
    } catch (error) {
      console.error('Error registering for event:', error);
      alert('Failed to register for event. Please try again.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleCancelRegistration = async () => {
    if (!participation || !event || !user?.id) return;

    const confirmed = window.confirm('Are you sure you want to cancel your registration?');
    if (!confirmed) return;

    try {
      setActionLoading(true);
      
      // Delete participation
      const response = await fetch(`${BACKEND_URL}/api/participates/unregister`, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          userId: user.id,
          eventId: event.id,
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      // Refresh data
      await fetchEventDetails();
      
      alert('Registration cancelled successfully.');
    } catch (error) {
      console.error('Error cancelling registration:', error);
      alert('Failed to cancel registration. Please try again.');
    } finally {
      setActionLoading(false);
    }
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
      <div className="text-center py-12">
        <h1 className="text-2xl font-bold text-gray-900 mb-4">Event not found</h1>
        <button
          onClick={() => navigate('/dashboard')}
          className="text-blue-600 hover:text-blue-800"
        >
          Return to Dashboard
        </button>
      </div>
    );
  }

  const isEventFull = event.capacity ? participationCount >= event.capacity : false;
  const isEventPast = isBefore(parseISO(event.start_time), new Date());
  const canRegister = !isEventFull && !isEventPast && !participation && user;

  return (
    <div className="max-w-4xl mx-auto">
      {/* Back Button */}
      <button
        onClick={() => navigate(-1)}
        className="flex items-center space-x-2 text-gray-600 hover:text-gray-900 mb-6"
      >
        <ArrowLeft className="w-5 h-5" />
        <span>Back</span>
      </button>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        {/* Event Header Image */}
        <div className="h-64 md:h-80 bg-gradient-to-r from-blue-500 to-purple-600 relative">
          <div className="w-full h-full flex items-center justify-center">
            <Calendar className="w-24 h-24 text-white/80" />
          </div>
          {/* Status Badge */}
          <div className="absolute top-6 right-6">
            {isEventFull ? (
              <span className="bg-red-100 text-red-800 text-sm font-medium px-3 py-1 rounded-full border border-red-200">
                Fully Booked
              </span>
            ) : participation ? (
              <span className="bg-green-100 text-green-800 text-sm font-medium px-3 py-1 rounded-full border border-green-200">
                You're Registered
              </span>
            ) : isEventPast ? (
              <span className="bg-gray-100 text-gray-800 text-sm font-medium px-3 py-1 rounded-full border border-gray-200">
                Event Ended
              </span>
            ) : (
              <span className="bg-blue-100 text-blue-800 text-sm font-medium px-3 py-1 rounded-full border border-blue-200">
                Available
              </span>
            )}
          </div>
        </div>

        <div className="p-8">
          {/* Tags */}
          {event.tag && event.tag.length > 0 && (
            <div className="flex flex-wrap gap-2 mb-4">
              {event.tag.map((tag, index) => (
                <span
                  key={index}
                  className={`text-sm font-medium px-3 py-1 rounded-full ${TAG_COLORS[tag] || 'bg-gray-100 text-gray-800'}`}
                >
                  {tag}
                </span>
              ))}
            </div>
          )}

          {/* Title and Description */}
          <h1 className="text-3xl font-bold text-gray-900 mb-4">{event.title}</h1>
          {event.description && (
            <p className="text-lg text-gray-600 mb-8 leading-relaxed">{event.description}</p>
          )}

          {/* Event Details Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mb-8">
            <div className="space-y-6">
              <div className="flex items-start space-x-3">
                <Calendar className="w-6 h-6 text-blue-600 mt-1" />
                <div>
                  <h3 className="font-semibold text-gray-900">Date & Time</h3>
                  <p className="text-gray-600">
                    {format(parseISO(event.start_time), 'EEEE, MMMM d, yyyy')}
                  </p>
                  <p className="text-gray-600">
                    {format(parseISO(event.start_time), 'h:mm a')} - {format(parseISO(event.end_time), 'h:mm a')}
                  </p>
                </div>
              </div>
            </div>

            <div className="space-y-6">
              <div className="flex items-start space-x-3">
                <Users className="w-6 h-6 text-blue-600 mt-1" />
                <div>
                  <h3 className="font-semibold text-gray-900">Capacity</h3>
                  <p className="text-gray-600">
                    {participationCount} / {event.capacity || 'Unlimited'} registered
                  </p>
                  {event.capacity && (
                    <div className="w-full bg-gray-200 rounded-full h-2 mt-2">
                      <div
                        className="bg-blue-600 h-2 rounded-full"
                        style={{ width: `${Math.min((participationCount / event.capacity) * 100, 100)}%` }}
                      ></div>
                    </div>
                  )}
                </div>
              </div>

              <div className="flex items-start space-x-3">
                <Clock className="w-6 h-6 text-blue-600 mt-1" />
                <div>
                  <h3 className="font-semibold text-gray-900">Event Status</h3>
                  <p className="text-gray-600">
                    {isEventPast ? 'Event has ended' : 'Registration open'}
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Action Buttons */}
          {user ? (
            <div className="flex flex-col sm:flex-row gap-4">
              {participation ? (
                <button
                  onClick={handleCancelRegistration}
                  disabled={actionLoading || isEventPast}
                  className="flex-1 bg-red-600 text-white py-4 px-6 rounded-lg font-semibold hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {actionLoading ? (
                    <div className="flex items-center justify-center space-x-2">
                      <LoadingSpinner size="sm" className="text-white" />
                      <span>Cancelling...</span>
                    </div>
                  ) : (
                    'Cancel Registration'
                  )}
                </button>
              ) : canRegister ? (
                <button
                  onClick={handleRegisterEvent}
                  disabled={actionLoading}
                  className="flex-1 bg-blue-600 text-white py-4 px-6 rounded-lg font-semibold hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {actionLoading ? (
                    <div className="flex items-center justify-center space-x-2">
                      <LoadingSpinner size="sm" className="text-white" />
                      <span>Registering...</span>
                    </div>
                  ) : (
                    'Register Now'
                  )}
                </button>
              ) : (
                <button
                  disabled
                  className="flex-1 bg-gray-400 text-white py-4 px-6 rounded-lg font-semibold cursor-not-allowed"
                >
                  {isEventFull ? 'Event Full' : isEventPast ? 'Event Ended' : 'Registration Closed'}
                </button>
              )}
            </div>
          ) : (
            <div className="text-center py-4">
              <p className="text-gray-600 mb-4">Please log in to register for this event.</p>
              <button
                onClick={() => navigate('/login')}
                className="bg-blue-600 text-white py-3 px-6 rounded-lg font-semibold hover:bg-blue-700 transition-colors duration-200"
              >
                Log In
              </button>
            </div>
          )}

          {/* Registration Status - Simplified */}
          {participation && (
            <div className="mt-6 p-4 bg-green-50 border border-green-200 rounded-lg">
              <div className="flex items-center space-x-2">
                <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                <span className="text-sm font-medium text-green-800">
                  You are registered for this event
                </span>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
